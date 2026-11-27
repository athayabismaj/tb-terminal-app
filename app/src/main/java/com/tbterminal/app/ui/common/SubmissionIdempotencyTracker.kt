package com.tbterminal.app.ui.common

import com.tbterminal.app.data.local.database.PendingSubmissionRecord
import com.tbterminal.app.data.local.database.PendingSubmissionStatus
import com.tbterminal.app.data.local.database.PendingSubmissionStore
import com.tbterminal.app.data.sync.runSuspendCatching
import java.math.BigDecimal
import java.util.UUID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

private val TrackerPersistenceJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

/**
 * Tracks a single submission intent and its idempotency key so retries after
 * ambiguous outcomes reuse the exact key while modified payloads rotate it.
 */
internal class SubmissionIdempotencyTracker<Intent : Any>(
    private val prefix: String,
    private val keyGenerator: () -> String = { UUID.randomUUID().toString() },
    private val store: PendingSubmissionStore? = null,
    private val scope: String? = null,
    private val intentSerializer: KSerializer<Intent>? = null,
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) {
    private var activeIntent: Intent? = null
    private var activeKey: String? = null
    private var activeCreatedAt: Long? = null
    var isOutcomeAmbiguous: Boolean = false
        private set

    fun resolveKey(intent: Intent): String {
        val currentKey = activeKey
        if (activeIntent == intent && currentKey != null) {
            return currentKey
        }
        val freshKey = "$prefix-${keyGenerator()}"
        activeIntent = intent
        activeKey = freshKey
        activeCreatedAt = null
        isOutcomeAmbiguous = false
        return freshKey
    }

    suspend fun restore(): String? {
        val backingStore = store ?: return null
        val targetScope = scope ?: return null
        val serializer = intentSerializer ?: return null

        val record = backingStore.load(targetScope) ?: return null
        val decodedIntent = runSuspendCatching {
            TrackerPersistenceJson.decodeFromString(serializer, record.intentJson)
        }.getOrElse {
            backingStore.clear(targetScope)
            return null
        }

        if (record.idempotencyKey.isBlank()) {
            backingStore.clear(targetScope)
            return null
        }

        activeIntent = decodedIntent
        activeKey = record.idempotencyKey
        activeCreatedAt = record.createdAt
        // Both IN_FLIGHT (process died mid-request) and AMBIGUOUS require key reuse on retry.
        isOutcomeAmbiguous = true
        return record.payloadJson
    }

    suspend fun resolveKeyForSubmission(intent: Intent, payloadJson: String? = null): String? {
        val prevIntent = activeIntent
        val prevKey = activeKey
        val prevCreatedAt = activeCreatedAt
        val prevAmbiguous = isOutcomeAmbiguous
        val key = resolveKey(intent)
        if (!persist(PendingSubmissionStatus.IN_FLIGHT, payloadJson)) {
            activeIntent = prevIntent
            activeKey = prevKey
            activeCreatedAt = prevCreatedAt
            isOutcomeAmbiguous = prevAmbiguous
            return null
        }
        return key
    }

    fun onSuccess() {
        clear()
    }

    suspend fun onSuccessAndPersist() {
        clearAndPersist()
    }

    fun onError(code: String, message: String? = null) {
        if (isIdempotencyKeyConflict(code, message)) {
            clear()
        } else {
            isOutcomeAmbiguous = isAmbiguousSubmissionErrorCode(code)
        }
    }

    suspend fun onErrorAndPersist(
        code: String,
        message: String? = null,
        payloadJson: String? = null
    ) {
        onError(code, message)
        if (isOutcomeAmbiguous) {
            persist(PendingSubmissionStatus.AMBIGUOUS, payloadJson)
        } else {
            activeCreatedAt = null
            clearStore()
        }
    }

    fun onException() {
        isOutcomeAmbiguous = true
    }

    suspend fun onExceptionAndPersist(payloadJson: String? = null) {
        onException()
        persist(PendingSubmissionStatus.AMBIGUOUS, payloadJson)
    }

    fun onDialogDismissed() {
        if (!isOutcomeAmbiguous) {
            clear()
        }
    }

    suspend fun onDialogDismissedAndPersist() {
        onDialogDismissed()
        if (!isOutcomeAmbiguous) {
            activeCreatedAt = null
            clearStore()
        }
    }

    fun hasPendingAmbiguousIntent(predicate: (Intent) -> Boolean): Boolean {
        return isOutcomeAmbiguous && activeIntent?.let(predicate) == true
    }

    fun clear() {
        activeIntent = null
        activeKey = null
        activeCreatedAt = null
        isOutcomeAmbiguous = false
    }

    suspend fun clearAndPersist() {
        clear()
        clearStore()
    }

    private suspend fun persist(status: PendingSubmissionStatus, payloadJson: String?): Boolean {
        val backingStore = store ?: return true
        val targetScope = scope ?: return true
        val serializer = intentSerializer ?: return true
        val intent = activeIntent ?: return false
        val key = activeKey ?: return false

        val now = clockMillis()
        val prevCreatedAt = activeCreatedAt
        val createdAt = prevCreatedAt ?: now
        activeCreatedAt = createdAt

        val intentJson = runSuspendCatching {
            TrackerPersistenceJson.encodeToString(serializer, intent)
        }.getOrElse {
            activeCreatedAt = prevCreatedAt
            return false
        }

        val saved = backingStore.save(
            PendingSubmissionRecord(
                scope = targetScope,
                idempotencyKey = key,
                intentJson = intentJson,
                payloadJson = payloadJson,
                status = status,
                createdAt = createdAt,
                updatedAt = now
            )
        )
        if (!saved) {
            activeCreatedAt = prevCreatedAt
        }
        return saved
    }

    private suspend fun clearStore() {
        val backingStore = store ?: return
        val targetScope = scope ?: return
        backingStore.clear(targetScope)
    }
}

internal fun BigDecimal.toNormalizedIdempotencyNumber(): String =
    stripTrailingZeros().toPlainString()

internal fun isAmbiguousSubmissionErrorCode(code: String): Boolean {
    val normalized = code.trim().uppercase()
    return normalized.startsWith("HTTP_5") || normalized in AMBIGUOUS_SUBMISSION_ERROR_CODES
}

internal fun isIdempotencyKeyConflict(code: String, message: String?): Boolean {
    val normalized = code.trim().uppercase()
    return normalized in CONFLICT_ERROR_CODES &&
        message?.contains("idempotency key", ignoreCase = true) == true
}

private val AMBIGUOUS_SUBMISSION_ERROR_CODES = setOf(
    "NETWORK_TIMEOUT",
    "NETWORK_UNAVAILABLE",
    "CONNECTION_FAILED",
    "CONNECTION_INTERRUPTED",
    "INVALID_RESPONSE",
    "EMPTY_BODY"
)

private val CONFLICT_ERROR_CODES = setOf(
    "VALIDATION_ERROR",
    "HTTP_400",
    "IDEMPOTENCY_CONFLICT"
)

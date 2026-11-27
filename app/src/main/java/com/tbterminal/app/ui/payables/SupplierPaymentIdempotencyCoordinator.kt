package com.tbterminal.app.ui.payables

import com.tbterminal.app.data.local.database.PendingSubmissionScopes
import com.tbterminal.app.data.local.database.PendingSubmissionStore
import com.tbterminal.app.ui.common.SubmissionIdempotencyTracker

/**
 * Coordinates supplier payment idempotency trackers and drafts per payableId so
 * list and detail screens share the exact same persistence scope and opening/closing
 * one payable never discards another payable's ambiguous key.
 */
internal class SupplierPaymentIdempotencyCoordinator(
    private val keyGenerator: () -> String,
    private val store: PendingSubmissionStore? = null,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
) {
    private val trackersByPayable = mutableMapOf<String, SubmissionIdempotencyTracker<SupplierPaymentIntent>>()
    private val draftsByPayable = mutableMapOf<String, SupplierPaymentDraft>()

    private fun trackerFor(payableId: String): SubmissionIdempotencyTracker<SupplierPaymentIntent> {
        return trackersByPayable.getOrPut(payableId) {
            SubmissionIdempotencyTracker(
                prefix = "supplier-pay",
                keyGenerator = keyGenerator,
                store = store,
                scope = PendingSubmissionScopes.supplierPayment(payableId),
                intentSerializer = SupplierPaymentIntent.serializer(),
                clockMillis = clockMillis,
            )
        }
    }

    suspend fun restoreAll() {
        val backingStore = store ?: return
        val records = backingStore.loadByScopePrefix(PendingSubmissionScopes.SUPPLIER_PAYMENT_PREFIX)
        for (record in records) {
            val payableId = record.scope
                .removePrefix(PendingSubmissionScopes.SUPPLIER_PAYMENT_PREFIX)
                .trim()
            if (payableId.isBlank()) {
                backingStore.clear(record.scope)
                continue
            }
            val tracker = trackerFor(payableId)
            val payloadJson = tracker.restore() ?: continue
            val draft = decodeSupplierPaymentDraft(payloadJson)
            if (draft == null || draft.payableId != payableId) {
                tracker.clearAndPersist()
                draftsByPayable.remove(payableId)
                trackersByPayable.remove(payableId)
            } else {
                draftsByPayable[payableId] = draft
            }
        }
    }

    suspend fun syncPayableFromStore(payableId: String): SupplierPaymentDraft? {
        if (store == null) {
            return getRestorableDraft(payableId)
        }
        val tracker = trackerFor(payableId)
        val payloadJson = tracker.restore()
        if (payloadJson == null) {
            tracker.clear()
            draftsByPayable.remove(payableId)
            return null
        }
        val draft = decodeSupplierPaymentDraft(payloadJson)
        if (draft == null || draft.payableId != payableId) {
            tracker.clearAndPersist()
            draftsByPayable.remove(payableId)
            return null
        }
        draftsByPayable[payableId] = draft
        return draft
    }

    fun getRestorableDraft(payableId: String): SupplierPaymentDraft? {
        val tracker = trackersByPayable[payableId] ?: return null
        val draft = draftsByPayable[payableId] ?: return null
        return draft.takeIf {
            it.payableId == payableId &&
                tracker.hasPendingAmbiguousIntent { intent -> intent.payableId == payableId }
        }
    }

    fun onDialogDismissedInMemory(payableId: String) {
        val tracker = trackersByPayable[payableId] ?: return
        tracker.onDialogDismissed()
        if (!tracker.isOutcomeAmbiguous) {
            draftsByPayable.remove(payableId)
            trackersByPayable.remove(payableId)
        }
    }

    suspend fun persistDismissalIfNotAmbiguous(payableId: String) {
        val tracker = trackersByPayable[payableId] ?: run {
            if (store != null && !draftsByPayable.containsKey(payableId)) {
                store.clear(PendingSubmissionScopes.supplierPayment(payableId))
            }
            return
        }
        tracker.onDialogDismissedAndPersist()
        if (!tracker.isOutcomeAmbiguous) {
            draftsByPayable.remove(payableId)
            trackersByPayable.remove(payableId)
        }
    }

    suspend fun resolveKeyForSubmission(
        payableId: String,
        intent: SupplierPaymentIntent,
        draftJson: String,
    ): String {
        return trackerFor(payableId).resolveKeyForSubmission(intent, draftJson)
    }

    suspend fun onSuccess(payableId: String) {
        trackerFor(payableId).onSuccessAndPersist()
        draftsByPayable.remove(payableId)
        trackersByPayable.remove(payableId)
    }

    suspend fun onError(
        payableId: String,
        code: String,
        message: String,
        draft: SupplierPaymentDraft,
        draftJson: String,
    ) {
        val tracker = trackerFor(payableId)
        tracker.onErrorAndPersist(code, message, draftJson)
        if (tracker.isOutcomeAmbiguous) {
            draftsByPayable[payableId] = draft
        } else {
            draftsByPayable.remove(payableId)
            trackersByPayable.remove(payableId)
        }
    }

    suspend fun onException(
        payableId: String,
        draft: SupplierPaymentDraft,
        draftJson: String,
    ) {
        trackerFor(payableId).onExceptionAndPersist(draftJson)
        draftsByPayable[payableId] = draft
    }
}

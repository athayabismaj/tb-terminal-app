package com.tbterminal.app.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class OfflinePinVerifierRecord(
    val userId: String,
    val saltBase64: String,
    val hashBase64: String,
    val iterations: Int,
    val lastOnlineVerifiedAtMillis: Long,
    val verifiedAtElapsedMillis: Long? = null,
    val verifiedAtBootCount: Long? = null,
    val highWaterWallClockMillis: Long? = null,
    val highWaterElapsedMillis: Long? = null,
    val highWaterBootCount: Long? = null,
    val accumulatedOfflineElapsedMillis: Long = 0L,
)

@Serializable
data class OfflinePinLockoutRecord(
    val userId: String,
    val failedAttempts: Int,
    val lockedUntilEpochMillis: Long? = null,
    val lockedUntilElapsedMillis: Long? = null,
    val lockDurationMillis: Long? = null,
    val lockBootCount: Long? = null,
)

interface PinVerifierKeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String): Boolean
    fun remove(key: String): Boolean
    fun clear(): Boolean
}

interface PinVerifierCrypto {
    fun encrypt(plaintext: String): String
    fun decrypt(ciphertext: String): String?
}

interface OfflinePinVerifierStore {
    fun saveVerifier(
        userId: String,
        pin: String,
        verifiedAtMillis: Long,
        verifiedAtElapsedMillis: Long? = null,
        verifiedAtBootCount: Long? = null,
    )
    fun updateVerifierRecord(record: OfflinePinVerifierRecord)
    fun readVerifier(userId: String): OfflinePinVerifierRecord?
    fun verifyPin(record: OfflinePinVerifierRecord, pin: String): Boolean
    fun readLockout(userId: String): OfflinePinLockoutRecord?
    fun saveLockout(record: OfflinePinLockoutRecord)
    fun clearLockout(userId: String)
    fun clearAll()

    companion object {
        const val DEFAULT_PBKDF2_ITERATIONS = 120_000
        const val VERIFIER_TTL_MILLIS = 72L * 60L * 60L * 1000L
        const val SALT_LENGTH_BYTES = 16
        const val DERIVED_KEY_LENGTH_BITS = 256
    }
}

class SharedPreferencesPinVerifierKeyValueStore(
    context: Context,
) : PinVerifierKeyValueStore {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun putString(key: String, value: String): Boolean =
        preferences.edit().putString(key, value).commit()

    override fun remove(key: String): Boolean =
        preferences.edit().remove(key).commit()

    override fun clear(): Boolean =
        preferences.edit().clear().commit()

    private companion object {
        const val PREFERENCES_NAME = "tb_terminal_pin_verifier_secure"
    }
}

class AndroidKeyStorePinVerifierCrypto : PinVerifierCrypto {
    override fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encoder = Base64.getEncoder()
        return listOf(
            ENCRYPTED_VALUE_VERSION,
            encoder.encodeToString(cipher.iv),
            encoder.encodeToString(cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))),
        ).joinToString(":")
    }

    override fun decrypt(ciphertext: String): String? {
        if (!ciphertext.startsWith("$ENCRYPTED_VALUE_VERSION:")) {
            return null
        }
        return try {
            val parts = ciphertext.split(':', limit = 3)
            if (parts.size != 3) return null
            val decoder = Base64.getDecoder()
            val iv = decoder.decode(parts[1])
            val payload = decoder.decode(parts[2])
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
            )
            cipher.doFinal(payload).toString(Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
        }.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "tb_terminal_pin_verifier_key_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val ENCRYPTED_VALUE_VERSION = "v1"
        const val GCM_TAG_LENGTH_BITS = 128
    }
}

class EncryptedOfflinePinVerifierStore(
    private val keyValueStore: PinVerifierKeyValueStore,
    private val crypto: PinVerifierCrypto,
    private val pbkdf2Iterations: Int = OfflinePinVerifierStore.DEFAULT_PBKDF2_ITERATIONS,
    private val secureRandom: SecureRandom = SecureRandom(),
) : OfflinePinVerifierStore {

    constructor(
        context: Context,
        pbkdf2Iterations: Int = OfflinePinVerifierStore.DEFAULT_PBKDF2_ITERATIONS,
    ) : this(
        keyValueStore = SharedPreferencesPinVerifierKeyValueStore(context),
        crypto = AndroidKeyStorePinVerifierCrypto(),
        pbkdf2Iterations = pbkdf2Iterations,
    )

    private val lock = Any()

    init {
        require(pbkdf2Iterations > 0) { "pbkdf2Iterations must be positive" }
    }

    override fun saveVerifier(
        userId: String,
        pin: String,
        verifiedAtMillis: Long,
        verifiedAtElapsedMillis: Long?,
        verifiedAtBootCount: Long?,
    ) {
        if (userId.isBlank()) return
        val salt = ByteArray(OfflinePinVerifierStore.SALT_LENGTH_BYTES).also(secureRandom::nextBytes)
        val hash = derivePinHash(pin, salt, pbkdf2Iterations)
        val record = try {
            OfflinePinVerifierRecord(
                userId = userId,
                saltBase64 = Base64.getEncoder().encodeToString(salt),
                hashBase64 = Base64.getEncoder().encodeToString(hash),
                iterations = pbkdf2Iterations,
                lastOnlineVerifiedAtMillis = verifiedAtMillis,
                verifiedAtElapsedMillis = verifiedAtElapsedMillis,
                verifiedAtBootCount = verifiedAtBootCount,
                highWaterWallClockMillis = if (verifiedAtElapsedMillis != null) verifiedAtMillis else null,
                highWaterElapsedMillis = verifiedAtElapsedMillis,
                highWaterBootCount = verifiedAtBootCount,
                accumulatedOfflineElapsedMillis = 0L,
            )
        } finally {
            salt.fill(0)
            hash.fill(0)
        }
        updateVerifierRecord(record)
    }

    override fun updateVerifierRecord(record: OfflinePinVerifierRecord) {
        if (record.userId.isBlank()) return
        val encoded = json.encodeToString(record)
        val encrypted = crypto.encrypt(encoded)
        synchronized(lock) {
            check(keyValueStore.putString(VERIFIER_KEY, encrypted)) {
                "Verifier PIN terenkripsi gagal disimpan"
            }
        }
    }

    override fun readVerifier(userId: String): OfflinePinVerifierRecord? {
        if (userId.isBlank()) return null
        val encrypted = synchronized(lock) { keyValueStore.getString(VERIFIER_KEY) } ?: return null
        val decrypted = crypto.decrypt(encrypted)
        if (decrypted.isNullOrBlank()) {
            synchronized(lock) { keyValueStore.remove(VERIFIER_KEY) }
            return null
        }
        val record = runCatching { json.decodeFromString<OfflinePinVerifierRecord>(decrypted) }.getOrNull()
        if (record == null || record.userId != userId || record.iterations <= 0) {
            return null
        }
        return record
    }

    override fun verifyPin(record: OfflinePinVerifierRecord, pin: String): Boolean {
        if (record.iterations <= 0) return false
        val decoder = Base64.getDecoder()
        val salt = runCatching { decoder.decode(record.saltBase64) }.getOrNull() ?: return false
        val expectedHash = runCatching { decoder.decode(record.hashBase64) }.getOrNull() ?: return false
        if (salt.size != OfflinePinVerifierStore.SALT_LENGTH_BYTES || expectedHash.isEmpty()) {
            return false
        }
        val candidateHash = derivePinHash(pin, salt, record.iterations)
        return try {
            MessageDigest.isEqual(expectedHash, candidateHash)
        } finally {
            salt.fill(0)
            expectedHash.fill(0)
            candidateHash.fill(0)
        }
    }

    override fun readLockout(userId: String): OfflinePinLockoutRecord? {
        if (userId.isBlank()) return null
        return synchronized(lock) { readLockoutLocked(userId) }
    }

    private fun readLockoutLocked(userId: String): OfflinePinLockoutRecord? {
        val encrypted = keyValueStore.getString(LOCKOUT_KEY) ?: return null
        val decrypted = crypto.decrypt(encrypted)
        if (decrypted.isNullOrBlank()) {
            keyValueStore.remove(LOCKOUT_KEY)
            return null
        }
        val record = runCatching { json.decodeFromString<OfflinePinLockoutRecord>(decrypted) }.getOrNull()
        if (record == null || record.userId != userId) {
            return null
        }
        return record
    }

    override fun saveLockout(record: OfflinePinLockoutRecord) {
        if (record.userId.isBlank()) return
        val encoded = json.encodeToString(record)
        val encrypted = crypto.encrypt(encoded)
        synchronized(lock) {
            check(keyValueStore.putString(LOCKOUT_KEY, encrypted)) {
                "State lockout PIN terenkripsi gagal disimpan"
            }
        }
    }

    override fun clearLockout(userId: String) {
        if (userId.isBlank()) return
        synchronized(lock) {
            val existing = readLockoutLocked(userId) ?: return
            if (existing.userId == userId) {
                keyValueStore.remove(LOCKOUT_KEY)
            }
        }
    }

    override fun clearAll() {
        synchronized(lock) {
            keyValueStore.remove(VERIFIER_KEY)
            keyValueStore.remove(LOCKOUT_KEY)
        }
    }

    private companion object {
        const val VERIFIER_KEY = "offline_pin_verifier"
        const val LOCKOUT_KEY = "offline_pin_lockout"
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}

/** In-memory verifier store for legacy unit tests that instantiate RemoteAuthRepository directly. */
internal class InMemoryOfflinePinVerifierStore(
    pbkdf2Iterations: Int = 1_000,
) : OfflinePinVerifierStore {
    private val mapStore = object : PinVerifierKeyValueStore {
        private val values = mutableMapOf<String, String>()
        override fun getString(key: String): String? = values[key]
        override fun putString(key: String, value: String): Boolean {
            values[key] = value
            return true
        }
        override fun remove(key: String): Boolean {
            values.remove(key)
            return true
        }
        override fun clear(): Boolean {
            values.clear()
            return true
        }
    }

    private val delegate = EncryptedOfflinePinVerifierStore(
        keyValueStore = mapStore,
        crypto = object : PinVerifierCrypto {
            override fun encrypt(plaintext: String): String = plaintext
            override fun decrypt(ciphertext: String): String = ciphertext
        },
        pbkdf2Iterations = pbkdf2Iterations,
    )

    override fun saveVerifier(
        userId: String,
        pin: String,
        verifiedAtMillis: Long,
        verifiedAtElapsedMillis: Long?,
        verifiedAtBootCount: Long?,
    ) =
        delegate.saveVerifier(userId, pin, verifiedAtMillis, verifiedAtElapsedMillis, verifiedAtBootCount)

    override fun updateVerifierRecord(record: OfflinePinVerifierRecord) =
        delegate.updateVerifierRecord(record)

    override fun readVerifier(userId: String): OfflinePinVerifierRecord? =
        delegate.readVerifier(userId)

    override fun verifyPin(record: OfflinePinVerifierRecord, pin: String): Boolean =
        delegate.verifyPin(record, pin)

    override fun readLockout(userId: String): OfflinePinLockoutRecord? =
        delegate.readLockout(userId)

    override fun saveLockout(record: OfflinePinLockoutRecord) =
        delegate.saveLockout(record)

    override fun clearLockout(userId: String) =
        delegate.clearLockout(userId)

    override fun clearAll() =
        delegate.clearAll()
}

internal fun derivePinHash(
    pin: String,
    salt: ByteArray,
    iterations: Int,
): ByteArray {
    val pinChars = pin.toCharArray()
    val spec = PBEKeySpec(
        pinChars,
        salt,
        iterations,
        OfflinePinVerifierStore.DERIVED_KEY_LENGTH_BITS,
    )
    return try {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        factory.generateSecret(spec).encoded
    } finally {
        pinChars.fill('\u0000')
        spec.clearPassword()
    }
}

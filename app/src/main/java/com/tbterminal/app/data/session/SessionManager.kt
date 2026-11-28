package com.tbterminal.app.data.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface TokenStore {
    fun readAccessToken(): String?
    /**
     * Checks session presence without decrypting the token. Implementations backed by
     * Android Keystore must override this so startup never waits for Keystore I/O.
     */
    fun hasStoredAccessToken(): Boolean = !readAccessToken().isNullOrBlank()
    fun readRefreshToken(): String?
    fun saveAccessToken(token: String)
    fun saveTokens(accessToken: String, refreshToken: String)
    fun readSessionUser(): SessionUser?
    fun saveSessionUser(user: SessionUser)
    fun clearAccessToken()
}

data class SessionUser(
    val name: String,
    val role: String,
    val isActive: Boolean,
    val joinedAt: String,
    val lastLoginAt: String?,
    val userId: String? = null,
    val username: String? = null,
    val email: String? = null
)

class SharedPreferencesTokenStore(
    context: Context
) : TokenStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val cacheLock = Any()

    @Volatile
    private var hasStoredAccessTokenLoaded = false

    @Volatile
    private var cachedHasStoredAccessToken = false

    @Volatile
    private var sessionUserLoaded = false

    @Volatile
    private var cachedSessionUser: SessionUser? = null

    override fun readAccessToken(): String? {
        return readSecret(ACCESS_TOKEN_KEY)
    }

    override fun hasStoredAccessToken(): Boolean {
        if (hasStoredAccessTokenLoaded) return cachedHasStoredAccessToken
        return synchronized(cacheLock) {
            if (!hasStoredAccessTokenLoaded) {
                cachedHasStoredAccessToken = !preferences.getString(ACCESS_TOKEN_KEY, null).isNullOrBlank()
                hasStoredAccessTokenLoaded = true
            }
            cachedHasStoredAccessToken
        }
    }

    override fun saveAccessToken(token: String) {
        writeSecret(ACCESS_TOKEN_KEY, token)
    }

    override fun readRefreshToken(): String? {
        return readSecret(REFRESH_TOKEN_KEY)
    }

    override fun saveTokens(accessToken: String, refreshToken: String) {
        check(
            preferences.edit()
                .putString(ACCESS_TOKEN_KEY, encrypt(accessToken))
                .putString(REFRESH_TOKEN_KEY, encrypt(refreshToken))
                .commit()
        ) { "Token terenkripsi gagal disimpan" }
        synchronized(cacheLock) {
            cachedHasStoredAccessToken = accessToken.isNotBlank()
            hasStoredAccessTokenLoaded = true
        }
    }

    override fun readSessionUser(): SessionUser? {
        if (sessionUserLoaded) return cachedSessionUser
        return synchronized(cacheLock) {
            if (!sessionUserLoaded) {
                val name = preferences.getString(USER_NAME_KEY, null)
                val role = preferences.getString(USER_ROLE_KEY, null)
                val userId = preferences.getString(USER_ID_KEY, null)
                val username = preferences.getString(USER_USERNAME_KEY, null)
                val email = preferences.getString(USER_EMAIL_KEY, null)
                val isActive = preferences.getBoolean(USER_IS_ACTIVE_KEY, true)
                val joinedAt = preferences.getString(USER_JOINED_AT_KEY, null)
                val lastLoginAt = preferences.getString(USER_LAST_LOGIN_KEY, null)

                cachedSessionUser = if (name.isNullOrBlank() || role.isNullOrBlank() || joinedAt.isNullOrBlank()) {
                    null
                } else {
                    SessionUser(
                        name = name,
                        role = role,
                        isActive = isActive,
                        joinedAt = joinedAt,
                        lastLoginAt = lastLoginAt,
                        userId = userId?.takeIf(String::isNotBlank),
                        username = username?.takeIf(String::isNotBlank),
                        email = email?.takeIf(String::isNotBlank)
                    )
                }
                sessionUserLoaded = true
            }
            cachedSessionUser
        }
    }

    override fun saveSessionUser(user: SessionUser) {
        val normalizedUserId = user.userId?.takeIf(String::isNotBlank)
        val normalizedUsername = user.username?.takeIf(String::isNotBlank)
        val normalizedEmail = user.email?.takeIf(String::isNotBlank)
        val normalizedUser = user.copy(
            userId = normalizedUserId,
            username = normalizedUsername,
            email = normalizedEmail,
        )
        synchronized(cacheLock) {
            cachedSessionUser = normalizedUser
            sessionUserLoaded = true
        }
        preferences.edit().apply {
            if (normalizedUserId == null) {
                remove(USER_ID_KEY)
            } else {
                putString(USER_ID_KEY, normalizedUserId)
            }
            if (normalizedUsername == null) remove(USER_USERNAME_KEY) else putString(USER_USERNAME_KEY, normalizedUsername)
            if (normalizedEmail == null) remove(USER_EMAIL_KEY) else putString(USER_EMAIL_KEY, normalizedEmail)
        }
            .putString(USER_NAME_KEY, user.name)
            .putString(USER_ROLE_KEY, user.role)
            .putBoolean(USER_IS_ACTIVE_KEY, user.isActive)
            .putString(USER_JOINED_AT_KEY, user.joinedAt)
            .putString(USER_LAST_LOGIN_KEY, user.lastLoginAt)
            .apply()
    }

    override fun clearAccessToken() {
        synchronized(cacheLock) {
            cachedHasStoredAccessToken = false
            hasStoredAccessTokenLoaded = true
            cachedSessionUser = null
            sessionUserLoaded = true
        }
        preferences.edit()
            .remove(ACCESS_TOKEN_KEY)
            .remove(REFRESH_TOKEN_KEY)
            .remove(USER_ID_KEY)
            .remove(USER_USERNAME_KEY)
            .remove(USER_EMAIL_KEY)
            .remove(USER_NAME_KEY)
            .remove(USER_ROLE_KEY)
            .remove(USER_IS_ACTIVE_KEY)
            .remove(USER_JOINED_AT_KEY)
            .remove(USER_LAST_LOGIN_KEY)
            .apply()
    }

    private fun writeSecret(key: String, value: String) {
        check(preferences.edit().putString(key, encrypt(value)).commit()) { "Token terenkripsi gagal disimpan" }
        if (key == ACCESS_TOKEN_KEY) {
            synchronized(cacheLock) {
                cachedHasStoredAccessToken = value.isNotBlank()
                hasStoredAccessTokenLoaded = true
            }
        }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        return listOf(
            ENCRYPTED_VALUE_VERSION,
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            Base64.encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        ).joinToString(":")
    }

    private fun readSecret(key: String): String? {
        val stored = preferences.getString(key, null)?.takeIf(String::isNotBlank) ?: return null
        if (!stored.startsWith("$ENCRYPTED_VALUE_VERSION:")) {
            // One-time migration for sessions created before production hardening.
            writeSecret(key, stored)
            return stored
        }
        return try {
            val parts = stored.split(':', limit = 3)
            require(parts.size == 3)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(128, Base64.decode(parts[1], Base64.NO_WRAP))
            )
            cipher.doFinal(Base64.decode(parts[2], Base64.NO_WRAP)).toString(Charsets.UTF_8)
        } catch (_: Exception) {
            // A key invalidated by device security changes must not expose or reuse a stale session.
            preferences.edit().remove(key).commit()
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
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
        }.generateKey()
    }

    private companion object {
        const val PREFERENCES_NAME = "session"
        const val ACCESS_TOKEN_KEY = "access_token"
        const val REFRESH_TOKEN_KEY = "refresh_token"
        const val USER_ID_KEY = "user_id"
        const val USER_USERNAME_KEY = "user_username"
        const val USER_EMAIL_KEY = "user_email"
        const val USER_NAME_KEY = "user_name"
        const val USER_ROLE_KEY = "user_role"
        const val USER_IS_ACTIVE_KEY = "user_is_active"
        const val USER_JOINED_AT_KEY = "user_joined_at"
        const val USER_LAST_LOGIN_KEY = "user_last_login"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "tb_terminal_session_key_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val ENCRYPTED_VALUE_VERSION = "v1"
    }
}

sealed interface SessionEvent {
    data class Unauthorized(val reason: String? = null) : SessionEvent
    data object PasswordChanged : SessionEvent
}

class SessionManager(
    private val tokenStore: TokenStore
) {
    @Volatile
    private var remoteLogout: ((String) -> Unit)? = null
    private var requiresPinUnlock = hasAccessToken()
    private val sessionClearedListeners = CopyOnWriteArrayList<() -> Unit>()
    private val beforeSessionClearedListeners = CopyOnWriteArrayList<() -> Unit>()
    private val _activeUserIdFlow = MutableStateFlow(resolveOwnerUserId())
    val activeUserIdFlow: StateFlow<String?> = _activeUserIdFlow.asStateFlow()
    private val _pinUnlockRequiredFlow = MutableStateFlow(requiresPinUnlock)
    val pinUnlockRequiredFlow: StateFlow<Boolean> = _pinUnlockRequiredFlow.asStateFlow()

    private val _events = MutableSharedFlow<SessionEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    fun addSessionClearedListener(listener: () -> Unit) {
        sessionClearedListeners.addIfAbsent(listener)
    }

    fun removeSessionClearedListener(listener: () -> Unit) {
        sessionClearedListeners.remove(listener)
    }

    fun addBeforeSessionClearedListener(listener: () -> Unit) {
        beforeSessionClearedListeners.addIfAbsent(listener)
    }

    fun removeBeforeSessionClearedListener(listener: () -> Unit) {
        beforeSessionClearedListeners.remove(listener)
    }

    fun resolveOwnerUserId(): String? {
        return resolveSessionIdentity(tokenStore.readSessionUser())
    }

    fun saveAccessToken(token: String) {
        tokenStore.saveAccessToken(token)
    }

    fun saveAuthenticatedSession(token: String, refreshToken: String, user: SessionUser) {
        val previousIdentity = resolveSessionIdentity(tokenStore.readSessionUser())
        val nextIdentity = resolveSessionIdentity(user)
        if (previousIdentity != null && nextIdentity != null && previousIdentity != nextIdentity) {
            notifyBeforeSessionClearedListeners()
            notifySessionClearedListeners()
        }
        tokenStore.saveTokens(token, refreshToken)
        tokenStore.saveSessionUser(user)
        _activeUserIdFlow.value = nextIdentity
        requiresPinUnlock = false
        _pinUnlockRequiredFlow.value = false
    }

    fun updateTokens(accessToken: String, refreshToken: String) {
        tokenStore.saveTokens(accessToken, refreshToken)
    }

    fun readSessionUser(): SessionUser? {
        return tokenStore.readSessionUser()
    }

    fun updateSessionUser(user: SessionUser) {
        tokenStore.saveSessionUser(user)
        _activeUserIdFlow.value = resolveSessionIdentity(user)
    }

    fun hasAccessToken(): Boolean {
        return tokenStore.hasStoredAccessToken()
    }

    fun lockForResume() {
        if (hasAccessToken()) {
            requiresPinUnlock = true
            _pinUnlockRequiredFlow.value = true
        }
    }

    fun markPinUnlocked() {
        if (hasAccessToken()) {
            requiresPinUnlock = false
            _pinUnlockRequiredFlow.value = false
        }
    }

    fun requiresPinUnlock(): Boolean {
        return hasAccessToken() && requiresPinUnlock
    }

    fun clearSession() {
        notifyBeforeSessionClearedListeners()
        tokenStore.clearAccessToken()
        requiresPinUnlock = false
        _pinUnlockRequiredFlow.value = false
        _activeUserIdFlow.value = null
        notifySessionClearedListeners()
    }

    private fun notifyBeforeSessionClearedListeners() {
        beforeSessionClearedListeners.forEach { listener ->
            runCatching { listener() }
        }
    }

    private fun notifySessionClearedListeners() {
        sessionClearedListeners.forEach { listener ->
            runCatching { listener() }
        }
    }

    private fun resolveSessionIdentity(user: SessionUser?): String? {
        if (user == null) return null
        return user.userId?.takeIf(String::isNotBlank)
            ?: user.username?.takeIf(String::isNotBlank)
            ?: user.name.takeIf(String::isNotBlank)
    }

    fun configureRemoteLogout(action: (String) -> Unit) {
        remoteLogout = action
    }

    fun logout() {
        tokenStore.readAccessToken()?.takeIf(String::isNotBlank)?.let { token ->
            runCatching { remoteLogout?.invoke(token) }
        }
        clearSession()
    }

    fun handleUnauthorized(reason: String? = null) {
        clearSession()
        _events.tryEmit(SessionEvent.Unauthorized(reason))
    }

    fun handlePasswordChanged() {
        clearSession()
        _events.tryEmit(SessionEvent.PasswordChanged)
    }
}

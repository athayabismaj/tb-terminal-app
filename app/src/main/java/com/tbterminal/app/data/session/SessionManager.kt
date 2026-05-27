package com.tbterminal.app.data.session

import android.content.Context
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface TokenStore {
    fun readAccessToken(): String?
    fun saveAccessToken(token: String)
    fun readSessionUser(): SessionUser?
    fun saveSessionUser(user: SessionUser)
    fun clearAccessToken()
}

data class SessionUser(
    val name: String,
    val role: String,
    val isActive: Boolean,
    val joinedAt: String,
    val lastLoginAt: String?
)

class SharedPreferencesTokenStore(
    context: Context
) : TokenStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun readAccessToken(): String? {
        return preferences.getString(ACCESS_TOKEN_KEY, null)
    }

    override fun saveAccessToken(token: String) {
        preferences.edit()
            .putString(ACCESS_TOKEN_KEY, token)
            .commit()
    }

    override fun readSessionUser(): SessionUser? {
        val name = preferences.getString(USER_NAME_KEY, null)
        val role = preferences.getString(USER_ROLE_KEY, null)
        val isActive = preferences.getBoolean(USER_IS_ACTIVE_KEY, true)
        val joinedAt = preferences.getString(USER_JOINED_AT_KEY, null)
        val lastLoginAt = preferences.getString(USER_LAST_LOGIN_KEY, null)

        if (name.isNullOrBlank() || role.isNullOrBlank() || joinedAt.isNullOrBlank()) {
            return null
        }

        return SessionUser(
            name = name, 
            role = role, 
            isActive = isActive, 
            joinedAt = joinedAt, 
            lastLoginAt = lastLoginAt
        )
    }

    override fun saveSessionUser(user: SessionUser) {
        preferences.edit()
            .putString(USER_NAME_KEY, user.name)
            .putString(USER_ROLE_KEY, user.role)
            .putBoolean(USER_IS_ACTIVE_KEY, user.isActive)
            .putString(USER_JOINED_AT_KEY, user.joinedAt)
            .putString(USER_LAST_LOGIN_KEY, user.lastLoginAt)
            .commit()
    }

    override fun clearAccessToken() {
        preferences.edit()
            .remove(ACCESS_TOKEN_KEY)
            .remove(USER_NAME_KEY)
            .remove(USER_ROLE_KEY)
            .remove(USER_IS_ACTIVE_KEY)
            .remove(USER_JOINED_AT_KEY)
            .remove(USER_LAST_LOGIN_KEY)
            .commit()
    }

    private companion object {
        const val PREFERENCES_NAME = "session"
        const val ACCESS_TOKEN_KEY = "access_token"
        const val USER_NAME_KEY = "user_name"
        const val USER_ROLE_KEY = "user_role"
        const val USER_IS_ACTIVE_KEY = "user_is_active"
        const val USER_JOINED_AT_KEY = "user_joined_at"
        const val USER_LAST_LOGIN_KEY = "user_last_login"
    }
}

sealed interface SessionEvent {
    data object Unauthorized : SessionEvent
}

class SessionManager(
    private val tokenStore: TokenStore
) {
    private var requiresPinUnlock = hasAccessToken()

    private val _events = MutableSharedFlow<SessionEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    fun saveAccessToken(token: String) {
        tokenStore.saveAccessToken(token)
    }

    fun saveAuthenticatedSession(token: String, user: SessionUser) {
        tokenStore.saveAccessToken(token)
        tokenStore.saveSessionUser(user)
        requiresPinUnlock = false
    }

    fun readSessionUser(): SessionUser? {
        return tokenStore.readSessionUser()
    }

    fun hasAccessToken(): Boolean {
        return !tokenStore.readAccessToken().isNullOrBlank()
    }

    fun lockForResume() {
        if (hasAccessToken()) {
            requiresPinUnlock = true
        }
    }

    fun markPinUnlocked() {
        if (hasAccessToken()) {
            requiresPinUnlock = false
        }
    }

    fun requiresPinUnlock(): Boolean {
        return hasAccessToken() && requiresPinUnlock
    }

    fun clearSession() {
        tokenStore.clearAccessToken()
        requiresPinUnlock = false
    }

    fun handleUnauthorized() {
        clearSession()
        _events.tryEmit(SessionEvent.Unauthorized)
    }
}

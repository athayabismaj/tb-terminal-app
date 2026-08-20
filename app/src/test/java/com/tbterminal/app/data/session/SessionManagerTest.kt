package com.tbterminal.app.data.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {
    @Test
    fun saveAuthenticatedSession_persistsBothTokensAndUser() {
        val store = FakeTokenStore()
        val manager = SessionManager(store)
        val user = SessionUser("Kasir", "kasir", true, "2026-01-01", null, "user-1")

        manager.saveAuthenticatedSession("access", "refresh", user)

        assertEquals("access", store.readAccessToken())
        assertEquals("refresh", store.readRefreshToken())
        assertEquals(user, manager.readSessionUser())
        assertFalse(manager.requiresPinUnlock())
    }

    @Test
    fun logout_notifiesBackendThenClearsEntireSession() {
        val store = FakeTokenStore().apply {
            saveTokens("access", "refresh")
            saveSessionUser(SessionUser("Admin", "admin", true, "2026-01-01", null))
        }
        val manager = SessionManager(store)
        var notifiedToken: String? = null
        manager.configureRemoteLogout { notifiedToken = it }

        manager.logout()

        assertEquals("access", notifiedToken)
        assertNull(store.readAccessToken())
        assertNull(store.readRefreshToken())
        assertNull(store.readSessionUser())
        assertFalse(manager.hasAccessToken())
    }

    @Test
    fun handleUnauthorized_clearsSession() {
        val store = FakeTokenStore().apply { saveTokens("access", "refresh") }
        val manager = SessionManager(store)

        manager.handleUnauthorized()

        assertFalse(manager.hasAccessToken())
        assertNull(store.readRefreshToken())
    }
}

private class FakeTokenStore : TokenStore {
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var user: SessionUser? = null

    override fun readAccessToken(): String? = accessToken
    override fun readRefreshToken(): String? = refreshToken
    override fun saveAccessToken(token: String) { accessToken = token }
    override fun saveTokens(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }
    override fun readSessionUser(): SessionUser? = user
    override fun saveSessionUser(user: SessionUser) { this.user = user }
    override fun clearAccessToken() {
        accessToken = null
        refreshToken = null
        user = null
    }
}

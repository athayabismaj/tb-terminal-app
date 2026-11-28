package com.tbterminal.app.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object OfflineSyncCoordinator {
    private val mutex = Mutex()
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    suspend fun <T> withSyncLock(block: suspend () -> T): T {
        return mutex.withLock {
            _isSyncing.value = true
            try {
                block()
            } finally {
                _isSyncing.value = false
            }
        }
    }
}

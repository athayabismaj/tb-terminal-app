package com.tbterminal.app.data.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object OfflineSyncCoordinator {
    private val mutex = Mutex()

    suspend fun <T> withSyncLock(block: suspend () -> T): T {
        return mutex.withLock {
            block()
        }
    }
}

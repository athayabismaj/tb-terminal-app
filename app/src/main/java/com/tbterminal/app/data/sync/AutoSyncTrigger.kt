package com.tbterminal.app.data.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

internal suspend fun observeAutoSyncTriggers(
    onlineFlow: Flow<Boolean>,
    backendStatusFlow: Flow<BackendStatus>,
    pinUnlockRequiredFlow: Flow<Boolean>,
    onTriggerSyncNow: suspend () -> Unit,
) {
    combine(
        onlineFlow,
        backendStatusFlow,
        pinUnlockRequiredFlow,
    ) { isOnline, backendStatus, requiresPinUnlock ->
        isOnline && backendStatus == BackendStatus.CONNECTED && !requiresPinUnlock
    }
        .distinctUntilChanged()
        .collect { shouldSync ->
            if (shouldSync) {
                onTriggerSyncNow()
            }
        }
}

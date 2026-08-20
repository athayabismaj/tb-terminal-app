package com.tbterminal.app.data.local.model

enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED,
    CONFLICT
}

enum class SyncOperation {
    CREATE,
    UPDATE,
    DELETE
}

enum class SyncEntityType {
    TRANSACTION,
    CASH_SESSION,
    CASH_EXPENSE,
    /** Only used to read legacy rows created before Batch 8A. Never enqueue this value. */
    UNSUPPORTED;

    val isSupported: Boolean
        get() = this != UNSUPPORTED

    companion object {
        val supported: Set<SyncEntityType> = setOf(TRANSACTION, CASH_SESSION, CASH_EXPENSE)
    }
}

enum class AppDataMode {
    OFFLINE_ONLY,
    SYNC_SERVER
}

package com.tbterminal.app.data.sync

enum class InternetStatus {
    ONLINE,
    OFFLINE
}

enum class BackendStatus {
    UNKNOWN,
    CONNECTED,
    UNREACHABLE
}

enum class DataSourceStatus {
    SERVER,
    CACHE,
    LOCAL_ONLY
}

enum class SyncStatusView {
    IDLE,
    SYNCING,
    FAILED
}

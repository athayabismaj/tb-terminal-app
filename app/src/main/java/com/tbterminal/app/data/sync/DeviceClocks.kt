package com.tbterminal.app.data.sync

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings

internal fun defaultElapsedRealtimeMillis(): Long {
    if (Build.VERSION.SDK_INT == 0) {
        return System.nanoTime() / 1_000_000L
    }
    return runCatching { SystemClock.elapsedRealtime() }
        .getOrElse { System.nanoTime() / 1_000_000L }
}

internal fun readDeviceBootCount(context: Context): Long? {
    return runCatching {
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
    }.getOrNull()?.takeIf { it >= 0 }?.toLong()
}

internal fun isSameBootSession(savedBootCount: Long?, currentBootCount: Long?): Boolean {
    return when {
        savedBootCount != null && currentBootCount != null -> savedBootCount == currentBootCount
        savedBootCount == null && currentBootCount == null -> true
        else -> false
    }
}

package com.tbterminal.app.data.local.id

import android.content.Context
import android.provider.Settings

class DeviceIdProvider(context: Context) {
    private val appContext = context.applicationContext

    val deviceId: String by lazy {
        val androidId = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        "android-${androidId?.takeIf { it.isNotBlank() } ?: "unknown"}"
    }
}

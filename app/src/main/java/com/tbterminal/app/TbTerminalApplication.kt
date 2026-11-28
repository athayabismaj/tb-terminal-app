package com.tbterminal.app

import android.app.Application
import android.os.Build
import android.os.StrictMode
import coil.Coil
import com.tbterminal.app.data.di.AppContainer
import com.tbterminal.app.data.di.DefaultAppContainer
import com.tbterminal.app.data.media.CoilImageLoaderFactory
import com.tbterminal.app.data.repository.LocalBackupRepository
import com.tbterminal.app.data.worker.StockAlertScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TbTerminalApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        enableDebugStrictModeIfApplicable()
        Coil.setImageLoader(CoilImageLoaderFactory.create(this))
        LocalBackupRepository.applyPendingRestoreIfAny(this)
        appContainer = DefaultAppContainer(this)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                StockAlertScheduler.schedulePeriodic(this@TbTerminalApplication)
            }
            runCatching {
                val prefs = appContainer.localAppSettingsDataSource.getDevicePreferences()
                com.tbterminal.app.ui.barcode.BarcodeFeedbackHelper.isSoundEnabled = prefs.scannerSoundFeedback
                com.tbterminal.app.ui.barcode.BarcodeFeedbackHelper.isVibrateEnabled = prefs.scannerVibrateFeedback
            }
        }
    }

    private fun enableDebugStrictModeIfApplicable() {
        if (!BuildConfig.DEBUG) return
        if (Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) return
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .detectLeakedSqlLiteObjects()
                .detectActivityLeaks()
                .penaltyLog()
                .build()
        )
    }
}

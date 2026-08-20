package com.tbterminal.app

import android.app.Application
import com.tbterminal.app.data.di.AppContainer
import com.tbterminal.app.data.di.DefaultAppContainer
import com.tbterminal.app.data.repository.LocalBackupRepository

class TbTerminalApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        LocalBackupRepository.applyPendingRestoreIfAny(this)
        appContainer = DefaultAppContainer(this)
    }
}

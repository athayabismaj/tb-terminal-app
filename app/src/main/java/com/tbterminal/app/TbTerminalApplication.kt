package com.tbterminal.app

import android.app.Application
import com.tbterminal.app.data.di.AppContainer
import com.tbterminal.app.data.di.DefaultAppContainer

class TbTerminalApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = DefaultAppContainer(this)
    }
}

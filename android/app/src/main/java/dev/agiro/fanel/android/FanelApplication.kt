package dev.agiro.fanel.android

import android.app.Application

class FanelApplication : Application() {
    lateinit var appContainer: AppContainerContract
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }

    internal fun setAppContainerForTest(appContainer: AppContainerContract) {
        this.appContainer = appContainer
    }
}

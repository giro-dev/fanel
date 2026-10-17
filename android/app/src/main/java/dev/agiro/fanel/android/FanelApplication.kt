package dev.agiro.fanel.android

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

class FanelApplication : Application() {
    lateinit var appContainer: AppContainerContract
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                appContainer.onForeground()
            }

            override fun onStop(owner: LifecycleOwner) {
                appContainer.onBackground()
            }
        })
    }

    internal fun setAppContainerForTest(appContainer: AppContainerContract) {
        this.appContainer = appContainer
    }
}

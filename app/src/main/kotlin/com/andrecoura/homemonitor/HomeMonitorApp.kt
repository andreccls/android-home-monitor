package com.andrecoura.homemonitor

import android.app.Application
import com.andrecoura.homemonitor.data.monitoring.MonitoringService
import com.andrecoura.homemonitor.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@HiltAndroidApp
class HomeMonitorApp : Application() {
    @Inject lateinit var monitoring: MonitoringService

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        monitoring.start(scope)
    }
}

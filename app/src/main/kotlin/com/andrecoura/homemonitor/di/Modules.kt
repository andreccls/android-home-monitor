package com.andrecoura.homemonitor.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.andrecoura.homemonitor.data.local.AlertDao
import com.andrecoura.homemonitor.data.local.AppDatabase
import com.andrecoura.homemonitor.data.local.CallDao
import com.andrecoura.homemonitor.data.local.DemoSeed
import com.andrecoura.homemonitor.data.local.DeviceDao
import com.andrecoura.homemonitor.data.local.GateDao
import com.andrecoura.homemonitor.data.repository.RoomAlertRepository
import com.andrecoura.homemonitor.data.repository.RoomDeviceRepository
import com.andrecoura.homemonitor.data.repository.RoomGateRepository
import com.andrecoura.homemonitor.data.repository.RoomIntercomRepository
import com.andrecoura.homemonitor.data.simulation.SimulatedCameraStream
import com.andrecoura.homemonitor.data.simulation.SimulatedDeviceProbe
import com.andrecoura.homemonitor.data.simulation.SimulatedGateDriver
import com.andrecoura.homemonitor.data.simulation.SimulatedIntercomDriver
import com.andrecoura.homemonitor.data.simulation.SimulationConfig
import com.andrecoura.homemonitor.domain.port.CameraStream
import com.andrecoura.homemonitor.domain.port.DeviceProbe
import com.andrecoura.homemonitor.domain.port.GateDriver
import com.andrecoura.homemonitor.domain.port.IntercomDriver
import com.andrecoura.homemonitor.domain.repository.AlertRepository
import com.andrecoura.homemonitor.domain.repository.DeviceRepository
import com.andrecoura.homemonitor.domain.repository.GateRepository
import com.andrecoura.homemonitor.domain.repository.IntercomRepository
import com.andrecoura.homemonitor.domain.rules.MonitoringPolicy
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
annotation class ApplicationScope

/** Room database and DAOs. Instrumented tests swap it for an in-memory database. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(context, AppDatabase::class.java, "home-monitor.db")
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) = DemoSeed.populate(db, System.currentTimeMillis())
                },
            ).build()

    @Provides fun deviceDao(db: AppDatabase): DeviceDao = db.deviceDao()

    @Provides fun gateDao(db: AppDatabase): GateDao = db.gateDao()

    @Provides fun callDao(db: AppDatabase): CallDao = db.callDao()

    @Provides fun alertDao(db: AppDatabase): AlertDao = db.alertDao()
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides fun clock(): Clock = Clock.systemDefaultZone()

    @Provides fun policy() = MonitoringPolicy()

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds fun devices(impl: RoomDeviceRepository): DeviceRepository

    @Binds fun gates(impl: RoomGateRepository): GateRepository

    @Binds fun intercom(impl: RoomIntercomRepository): IntercomRepository

    @Binds fun alerts(impl: RoomAlertRepository): AlertRepository
}

/** How lively the simulated home is. Instrumented tests swap it for an instant, failure-free config. */
@Module
@InstallIn(SingletonComponent::class)
object SimulationModule {
    @Provides fun config(): SimulationConfig = SimulationConfig.Default
}

/** The single place where simulators are chosen. A real integration replaces these bindings. */
@Module
@InstallIn(SingletonComponent::class)
object PortsModule {
    @Provides
    @Singleton
    fun gateDriver(
        @ApplicationScope scope: CoroutineScope,
        config: SimulationConfig,
    ): GateDriver = SimulatedGateDriver(scope, config)

    @Provides
    @Singleton
    fun intercomDriver(
        config: SimulationConfig,
        clock: Clock,
    ): IntercomDriver = SimulatedIntercomDriver(config, clock)

    @Provides
    @Singleton
    fun probe(
        config: SimulationConfig,
        clock: Clock,
    ): DeviceProbe = SimulatedDeviceProbe(config, clock)

    @Provides
    @Singleton
    fun cameraStream(
        config: SimulationConfig,
        clock: Clock,
    ): CameraStream = SimulatedCameraStream(config, clock)
}

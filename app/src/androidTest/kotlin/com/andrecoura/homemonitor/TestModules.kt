package com.andrecoura.homemonitor

import android.content.Context
import androidx.room.Room
import com.andrecoura.homemonitor.data.local.AlertDao
import com.andrecoura.homemonitor.data.local.AppDatabase
import com.andrecoura.homemonitor.data.local.CallDao
import com.andrecoura.homemonitor.data.local.DeviceDao
import com.andrecoura.homemonitor.data.local.GateDao
import com.andrecoura.homemonitor.data.simulation.SimulationConfig
import com.andrecoura.homemonitor.di.DatabaseModule
import com.andrecoura.homemonitor.di.SimulationModule
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/** Same graph as production, but with an empty in-memory Room database. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object TestDatabaseModule {
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

    @Provides fun deviceDao(db: AppDatabase): DeviceDao = db.deviceDao()

    @Provides fun gateDao(db: AppDatabase): GateDao = db.gateDao()

    @Provides fun callDao(db: AppDatabase): CallDao = db.callDao()

    @Provides fun alertDao(db: AppDatabase): AlertDao = db.alertDao()
}

/** Keeps the production simulators and wiring, but with no latency or random failures and one call ringing early. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [SimulationModule::class])
object TestSimulationModule {
    @Provides
    fun config(): SimulationConfig = SimulationConfig.Instant.copy(callEveryMillis = 300L..300L, ringTimeoutMillis = 120_000)
}

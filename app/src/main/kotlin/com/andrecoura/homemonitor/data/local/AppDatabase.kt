package com.andrecoura.homemonitor.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DeviceEntity::class, GateEntity::class, CallEntity::class, AlertEntity::class],
    version = 1,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao

    abstract fun gateDao(): GateDao

    abstract fun callDao(): CallDao

    abstract fun alertDao(): AlertDao
}

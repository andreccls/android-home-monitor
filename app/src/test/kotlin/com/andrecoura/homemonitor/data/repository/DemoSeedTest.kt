package com.andrecoura.homemonitor.data.repository

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.andrecoura.homemonitor.data.local.AppDatabase
import com.andrecoura.homemonitor.data.local.DemoSeed
import com.andrecoura.homemonitor.domain.model.DeviceKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DemoSeedTest {
    @Test
    fun `fresh database is seeded once with cameras, alexas, calls and alerts`() =
        runTest {
            val db =
                Room
                    .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
                    .allowMainThreadQueries()
                    .addCallback(
                        object : RoomDatabase.Callback() {
                            override fun onCreate(db: SupportSQLiteDatabase) = DemoSeed.populate(db, 1_000_000_000L)
                        },
                    ).build()
            val devices = db.deviceDao().getAll()
            assertEquals(3, devices.count { it.kind == DeviceKind.CAMERA })
            assertEquals(2, devices.count { it.kind == DeviceKind.ALEXA })
            assertEquals(3, db.callDao().count())
            assertEquals(
                2,
                db
                    .alertDao()
                    .observeRecent(10)
                    .first()
                    .size,
            )
            db.close()
        }
}

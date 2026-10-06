package com.andrecoura.homemonitor.support

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.andrecoura.homemonitor.data.local.AppDatabase

/** Real Room, real SQLite (through Robolectric), in memory. */
fun inMemoryDatabase(): AppDatabase =
    Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

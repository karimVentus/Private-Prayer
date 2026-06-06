package com.prayertime.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PrayerTimeEntity::class], version = 4, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun prayerTimeDao(): PrayerTimeDao
}

package com.prayertime.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object PrayerTimeMigrations {
    /** Rebuilds table so v1 dateLabel index is removed and cityKey is added cleanly. */
    val MIGRATION_1_2 =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS prayer_times_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        prayer TEXT NOT NULL,
                        displayTime TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        dateLabel TEXT NOT NULL,
                        cityKey TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO prayer_times_new (id, prayer, displayTime, timestamp, dateLabel, cityKey)
                    SELECT id, prayer, displayTime, timestamp, dateLabel, '' FROM prayer_times
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE prayer_times")
                db.execSQL("ALTER TABLE prayer_times_new RENAME TO prayer_times")
                db.execSQL("DROP INDEX IF EXISTS index_prayer_times_dateLabel")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_prayer_times_cityKey_dateLabel ON prayer_times(cityKey, dateLabel)",
                )
            }
        }

    /** Fixes devices that reached v2 via the old ALTER TABLE migration (stale dateLabel index). */
    val MIGRATION_2_3 =
        object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS index_prayer_times_dateLabel")
            }
        }

    /** Adds nullable Hijri date columns for Phase 4. */
    val MIGRATION_3_4 =
        object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE prayer_times ADD COLUMN hijriYear INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE prayer_times ADD COLUMN hijriMonth INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE prayer_times ADD COLUMN hijriDay INTEGER DEFAULT NULL")
            }
        }
}

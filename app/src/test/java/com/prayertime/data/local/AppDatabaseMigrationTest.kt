package com.prayertime.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseMigrationTest {
    private val dbName = "migration-test.db"

    @After
    fun tearDown() {
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(dbName)
    }

    @Test
    fun migratesFromV1ToV4_withoutSchemaValidationError() {
        seedV1Database()
        val db =
            Room.databaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
                dbName,
            )
                .addMigrations(PrayerTimeMigrations.MIGRATION_1_2, PrayerTimeMigrations.MIGRATION_2_3, PrayerTimeMigrations.MIGRATION_3_4)
                .build()
        runBlocking {
            assertTrue(db.prayerTimeDao().getByCityAndDate("DE_Hameln", "2026-06-04").isEmpty())
        }
        db.close()
    }

    @Test
    fun migratesFromBrokenV2ToV4_dropsStaleDateLabelIndex() {
        seedBrokenV2Database()
        val db =
            Room.databaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
                dbName,
            )
                .addMigrations(PrayerTimeMigrations.MIGRATION_1_2, PrayerTimeMigrations.MIGRATION_2_3, PrayerTimeMigrations.MIGRATION_3_4)
                .build()
        runBlocking {
            db.prayerTimeDao().insertAll(
                listOf(
                    PrayerTimeEntity(
                        prayer = "FAJR",
                        displayTime = "05:00",
                        timestamp = 1L,
                        dateLabel = "2026-06-04",
                        cityKey = "DE_Hameln",
                    ),
                ),
            )
            assertTrue(db.prayerTimeDao().getByCityAndDate("DE_Hameln", "2026-06-04").isNotEmpty())
        }
        db.close()
    }

    @Test
    fun migratesFromV3ToV4_addsHijriColumns() {
        seedV3Database()
        val db =
            Room.databaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
                dbName,
            )
                .addMigrations(PrayerTimeMigrations.MIGRATION_3_4)
                .build()
        runBlocking {
            // Insert a row — hijri columns should accept NULL (default)
            db.prayerTimeDao().insertAll(
                listOf(
                    PrayerTimeEntity(
                        prayer = "FAJR",
                        displayTime = "05:00",
                        timestamp = 1L,
                        dateLabel = "2026-06-04",
                        cityKey = "DE_Hameln",
                    ),
                ),
            )
            val rows = db.prayerTimeDao().getByCityAndDate("DE_Hameln", "2026-06-04")
            assertTrue(rows.isNotEmpty())
            // After migration, existing rows have null hijri fields
            org.junit.Assert.assertNull(rows[0].hijriYear)
        }
        db.close()
    }

    /** Hand-seeded v1 layout — must stay aligned with exported [app/schemas/.../1.json]. */
    private fun seedV1Database() {
        val path = ApplicationProvider.getApplicationContext<Context>().getDatabasePath(dbName)
        SQLiteDatabase.openOrCreateDatabase(path.path, null).use { db ->
            db.execSQL(
                """
                CREATE TABLE prayer_times (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    prayer TEXT NOT NULL,
                    displayTime TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    dateLabel TEXT NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX index_prayer_times_dateLabel ON prayer_times(dateLabel)")
            db.version = 1
        }
    }

    /** Matches the schema left by the old broken ALTER TABLE migration. */
    private fun seedBrokenV2Database() {
        val path = ApplicationProvider.getApplicationContext<Context>().getDatabasePath(dbName)
        SQLiteDatabase.openOrCreateDatabase(path.path, null).use { db ->
            db.execSQL(
                """
                CREATE TABLE prayer_times (
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
                "CREATE INDEX index_prayer_times_cityKey_dateLabel ON prayer_times(cityKey, dateLabel)",
            )
            db.execSQL("CREATE INDEX index_prayer_times_dateLabel ON prayer_times(dateLabel)")
            db.version = 2
        }
    }

    /** v3 schema (current before Phase 4) — same as v2 post-migration, no hijri columns. */
    private fun seedV3Database() {
        val path = ApplicationProvider.getApplicationContext<Context>().getDatabasePath(dbName)
        SQLiteDatabase.openOrCreateDatabase(path.path, null).use { db ->
            db.execSQL(
                """
                CREATE TABLE prayer_times (
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
                "CREATE INDEX index_prayer_times_cityKey_dateLabel ON prayer_times(cityKey, dateLabel)",
            )
            db.version = 3
        }
    }
}

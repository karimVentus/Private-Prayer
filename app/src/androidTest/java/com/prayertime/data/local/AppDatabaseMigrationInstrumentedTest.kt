package com.prayertime.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Schema validation against exported JSON under [app/schemas] (v1–v4).
 * JVM smoke for migration paths: [AppDatabaseMigrationTest] (Robolectric).
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationInstrumentedTest {
    private val testDb = "migration-instrumented-test"

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun migrate1To2_validatesExportedSchemaAndPreservesRows() {
        helper.createDatabase(testDb, 1).apply {
            execSQL(
                """
                INSERT INTO prayer_times (prayer, displayTime, timestamp, dateLabel)
                VALUES ('FAJR', '05:00', 1, '2026-06-04')
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            testDb,
            2,
            true,
            PrayerTimeMigrations.MIGRATION_1_2,
        ).apply {
            assertTrue(tableColumns(this).contains("cityKey"))
            query("SELECT cityKey FROM prayer_times WHERE prayer = 'FAJR'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("", cursor.getString(0))
            }
            val indexes = indexNames(this)
            assertTrue(indexes.contains("index_prayer_times_cityKey_dateLabel"))
            assertFalse(indexes.contains("index_prayer_times_dateLabel"))
            close()
        }
    }

    @Test
    fun migrate2To3_validatesExportedSchema_dropsStaleDateLabelIndex() {
        helper.createDatabase(testDb, 2).apply {
            execSQL("CREATE INDEX index_prayer_times_dateLabel ON prayer_times(dateLabel)")
            close()
        }

        helper.runMigrationsAndValidate(
            testDb,
            3,
            true,
            PrayerTimeMigrations.MIGRATION_2_3,
        ).apply {
            val indexes = indexNames(this)
            assertTrue(indexes.contains("index_prayer_times_cityKey_dateLabel"))
            assertFalse(indexes.contains("index_prayer_times_dateLabel"))
            close()
        }
    }

    @Test
    fun migrate3To4_validatesExportedSchemaAndPreservesRows() {
        helper.createDatabase(testDb, 3).apply {
            execSQL(
                """
                INSERT INTO prayer_times (prayer, displayTime, timestamp, dateLabel, cityKey)
                VALUES ('FAJR', '05:00', 1, '2026-06-04', 'DE_Hameln')
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            testDb,
            4,
            true,
            PrayerTimeMigrations.MIGRATION_3_4,
        ).apply {
            assertTrue(tableColumns(this).containsAll(listOf("hijriYear", "hijriMonth", "hijriDay")))
            query("SELECT hijriYear FROM prayer_times WHERE prayer = 'FAJR'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.isNull(0))
            }
            close()
        }
    }

    private fun tableColumns(db: androidx.sqlite.db.SupportSQLiteDatabase): Set<String> =
        buildSet {
            db.query("PRAGMA table_info(prayer_times)").use { cursor ->
                while (cursor.moveToNext()) {
                    add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
            }
        }

    private fun indexNames(db: androidx.sqlite.db.SupportSQLiteDatabase): Set<String> =
        buildSet {
            db.query("PRAGMA index_list(prayer_times)").use { cursor ->
                while (cursor.moveToNext()) {
                    add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
            }
        }
}

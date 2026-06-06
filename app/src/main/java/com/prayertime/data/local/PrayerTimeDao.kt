package com.prayertime.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PrayerTimeDao {
    @Query("SELECT * FROM prayer_times WHERE cityKey = :cityKey AND dateLabel = :dateLabel ORDER BY timestamp ASC")
    suspend fun getByCityAndDate(
        cityKey: String,
        dateLabel: String,
    ): List<PrayerTimeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(times: List<PrayerTimeEntity>)

    @Query("DELETE FROM prayer_times WHERE cityKey = :cityKey AND dateLabel = :dateLabel")
    suspend fun deleteByCityAndDate(
        cityKey: String,
        dateLabel: String,
    )

    @Query("DELETE FROM prayer_times WHERE cityKey = :cityKey")
    suspend fun deleteByCityKey(cityKey: String)

    @Query("DELETE FROM prayer_times")
    suspend fun deleteAll()

    @Query("DELETE FROM prayer_times WHERE cityKey = :cityKey AND dateLabel < :dateLabel")
    suspend fun deleteOlderThan(
        cityKey: String,
        dateLabel: String,
    )

    @Query("SELECT dateLabel FROM prayer_times WHERE cityKey = :cityKey GROUP BY dateLabel ORDER BY dateLabel DESC LIMIT 1")
    suspend fun getLatestDateLabel(cityKey: String): String?
}

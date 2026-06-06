package com.prayertime.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "prayer_times",
    indices = [Index(value = ["cityKey", "dateLabel"], unique = false)],
)
data class PrayerTimeEntity(
    @androidx.room.PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val prayer: String,
    val displayTime: String,
    val timestamp: Long,
    val dateLabel: String,
    @ColumnInfo(defaultValue = "''")
    val cityKey: String = "",
    val hijriYear: Int? = null,
    val hijriMonth: Int? = null,
    val hijriDay: Int? = null,
)

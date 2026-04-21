package com.example.nearnote.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val noteText: String,
    val isActive: Boolean = true,
    val cooldownUntil: Long = 0L
)

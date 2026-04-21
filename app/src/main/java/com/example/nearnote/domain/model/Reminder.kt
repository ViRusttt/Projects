package com.example.nearnote.domain.model

data class Reminder(
    val id: Long = 0,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val noteText: String,
    val isActive: Boolean = true,
    val cooldownUntil: Long = 0L
)

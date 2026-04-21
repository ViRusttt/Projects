package com.example.nearnote.data.repository

import com.example.nearnote.data.local.NearNoteDatabase
import com.example.nearnote.data.local.ReminderEntity
import com.example.nearnote.domain.model.Reminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReminderRepository(db: NearNoteDatabase) {
    private val dao = db.reminderDao()

    val allReminders: Flow<List<Reminder>> = dao.getAllReminders().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getById(id: Long): Reminder? = dao.getReminderById(id)?.toDomain()

    suspend fun insert(reminder: Reminder): Long = dao.insertReminder(reminder.toEntity())

    suspend fun update(reminder: Reminder) = dao.updateReminder(reminder.toEntity())

    suspend fun delete(reminder: Reminder) = dao.deleteReminder(reminder.toEntity())

    suspend fun setActive(id: Long, isActive: Boolean) = dao.updateActiveStatus(id, isActive)

    suspend fun setCooldown(id: Long, until: Long) = dao.updateCooldown(id, until)

    suspend fun getActiveReminders(): List<Reminder> = dao.getActiveReminders().map { it.toDomain() }

    private fun ReminderEntity.toDomain() = Reminder(id, title, latitude, longitude, radiusMeters, noteText, isActive, cooldownUntil)
    private fun Reminder.toEntity() = ReminderEntity(id, title, latitude, longitude, radiusMeters, noteText, isActive, cooldownUntil)
}

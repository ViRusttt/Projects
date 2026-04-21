package com.example.nearnote.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nearnote.data.local.NearNoteDatabase
import com.example.nearnote.data.repository.ReminderRepository
import com.example.nearnote.domain.model.Reminder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ReminderRepository(NearNoteDatabase.getInstance(app))

    val reminders: StateFlow<List<Reminder>> = repo.allReminders.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _isDarkMode = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun addReminder(reminder: Reminder) = viewModelScope.launch { repo.insert(reminder) }

    fun updateReminder(reminder: Reminder) = viewModelScope.launch { repo.update(reminder) }

    fun deleteReminder(reminder: Reminder) = viewModelScope.launch { repo.delete(reminder) }

    fun toggleActive(reminder: Reminder) = viewModelScope.launch {
        repo.setActive(reminder.id, !reminder.isActive)
    }

    suspend fun getById(id: Long): Reminder? = repo.getById(id)
}

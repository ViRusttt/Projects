package com.example.nearnote.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.nearnote.data.local.NearNoteDatabase
import com.example.nearnote.data.repository.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminder_id", -1L)
        if (reminderId == -1L) return

        // บันทึก cooldown 2 ชั่วโมงลง DB
        CoroutineScope(Dispatchers.IO).launch {
            val db = NearNoteDatabase.getInstance(context)
            val until = System.currentTimeMillis() + 2 * 60 * 60 * 1000L
            ReminderRepository(db).setCooldown(reminderId, until)
        }

        // ยกเลิก notification ออกจาก shade
        NotificationManagerCompat.from(context).cancel(reminderId.toInt())
    }
}

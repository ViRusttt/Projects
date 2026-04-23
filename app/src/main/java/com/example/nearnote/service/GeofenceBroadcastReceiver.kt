package com.example.nearnote.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.LocationServices
import com.example.nearnote.MainActivity
import com.example.nearnote.R
import com.example.nearnote.data.local.NearNoteDatabase
import com.example.nearnote.data.repository.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Support test triggers
        if (intent.hasExtra("test_reminder_id")) {
            val id = intent.getLongExtra("test_reminder_id", 0L)
            val title = intent.getStringExtra("test_title") ?: ""
            val note = intent.getStringExtra("test_note") ?: ""
            sendNotification(context, id, title, note)
            return
        }

        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) return
        if (geofencingEvent.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        geofencingEvent.triggeringGeofences?.forEach { geofence ->
            val parts = geofence.requestId.split("|")
            if (parts.size >= 3) {
                val reminderId = parts[0].toLongOrNull() ?: return@forEach
                val title = parts[1]
                val note = parts[2]

                // Send notification
                sendNotification(context, reminderId, title, note)

                // Re-register geofence after a short delay so it can trigger again on the next visit
                CoroutineScope(Dispatchers.IO).launch {
                    delay(5000) // wait 5 seconds before removing
                    val db = NearNoteDatabase.getInstance(context)
                    val reminder = ReminderRepository(db).getById(reminderId)
                    if (reminder != null && reminder.isActive) {
                        // Remove old geofence entry
                        LocationServices.getGeofencingClient(context)
                            .removeGeofences(listOf(geofence.requestId))
                        delay(2000) // wait for removal to complete
                        // Re-add the geofence so it can fire again on re-entry
                        GeofenceManager(context).addGeofence(reminder)
                    }
                }
            }
        }
    }

    private fun sendNotification(context: Context, reminderId: Long, title: String, note: String) {
        val channelId = "nearnote_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, "NearNote Alerts", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("reminder_id", reminderId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, reminderId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val firstLine = note.lines().firstOrNull { it.isNotBlank() } ?: ""
        val preview = if (note.lines().size > 1) "$firstLine, ..." else firstLine

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Near $title!")
            .setContentText(preview)
            .setStyle(NotificationCompat.BigTextStyle().bigText(note))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(reminderId.toInt(), notification)
    }
}

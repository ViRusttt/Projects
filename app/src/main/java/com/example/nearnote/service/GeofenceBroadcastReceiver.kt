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
import com.example.nearnote.MainActivity
import com.example.nearnote.R

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
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
                val title = parts[1]
                val note = parts[2]
                sendNotification(context, geofence.requestId.split("|")[0].toLongOrNull() ?: 0L, title, note)
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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, reminderId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

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

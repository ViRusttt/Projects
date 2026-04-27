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

        when (geofencingEvent.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                // ผู้ใช้เข้าสู่พื้นที่ → แจ้งเตือน
                geofencingEvent.triggeringGeofences?.forEach { geofence ->
                    val parts = geofence.requestId.split("|")
                    if (parts.size >= 3) {
                        val reminderId = parts[0].toLongOrNull() ?: return@forEach
                        val title = parts[1]
                        val note = parts[2]
                        sendNotification(context, reminderId, title, note)
                    }
                }
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                // ผู้ใช้ออกจากพื้นที่ → reset state (ENTER จะยิงใหม่เมื่อกลับเข้ามา)
                // ไม่ต้องทำอะไร — geofence NEVER_EXPIRE จัดการเองโดยอัตโนมัติ
            }
        }
    }

    private fun sendNotification(context: Context, reminderId: Long, title: String, note: String) {
        val channelId = "nearnote_alerts_v2"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, "NearNote Alerts", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        // contentIntent → เปิด DetailScreen เมื่อกด notification
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("reminder_id", reminderId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, reminderId.toInt(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // snoozeIntent → ส่งไป SnoozeReceiver โดยตรง ไม่ต้องเปิดแอป
        val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply {
            putExtra("reminder_id", reminderId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt() + 10000, // offset เพื่อไม่ให้ชนกับ openPendingIntent
            snoozeIntent,
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
            // แสดงบน lock screen และ notification shade เสมอ
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPendingIntent)           // กด notification → DetailScreen
            .addAction(                                     // ปุ่ม Snooze 2h ใน notification shade
                android.R.drawable.ic_menu_recent_history,
                "Snooze 2h",
                snoozePendingIntent
            )
            // false = notification ไม่หายเองเมื่อกด (ต้อง swipe ออกเอง หรือกด Snooze)
            .setAutoCancel(false)
            .build()

        notificationManager.notify(reminderId.toInt(), notification)
    }
}

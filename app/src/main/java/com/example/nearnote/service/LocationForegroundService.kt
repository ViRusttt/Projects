package com.example.nearnote.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.nearnote.data.local.NearNoteDatabase
import com.example.nearnote.data.repository.ReminderRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocationForegroundService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startForeground(1, buildNotification())
        startLocationUpdates()
        
        // Re-register geofences
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val db = NearNoteDatabase.getInstance(applicationContext)
            val repo = ReminderRepository(db)
            val geofenceManager = GeofenceManager(applicationContext)
            repo.getActiveReminders().forEach { reminder ->
                geofenceManager.addGeofence(reminder)
            }
        }
    }

    private fun startLocationUpdates() {
        // HIGH_ACCURACY + ช่วงเวลาสั้น เพื่อให้ Geofence ทำงานได้แม่นยำโดยไม่ต้องพึ่ง Google Maps
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 10000L // every 10 seconds
        ).setMinUpdateDistanceMeters(10f).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                // Keeping active high-accuracy location for geofence to work in background
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        }
    }

    private fun buildNotification(): Notification {
        val channelId = "location_service"
        val channel = NotificationChannel(channelId, "Location Service",
            NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("NearNote")
            .setContentText("Monitoring nearby locations...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // auto-restart if killed by OS
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}

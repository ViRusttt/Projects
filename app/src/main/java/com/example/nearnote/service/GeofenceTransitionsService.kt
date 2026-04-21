package com.example.nearnote.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class GeofenceTransitionsService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}

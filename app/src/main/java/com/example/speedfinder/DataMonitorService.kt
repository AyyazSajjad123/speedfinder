package com.example.speedfinder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.speedfinder.R

class DataMonitorService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Notification dikhana zaroori hai Foreground Service ke liye
        startForeground(1, createNotification())

        // Yahan hum data monitor karne ka logic lagayenge future mein
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val channelId = "SpeedFinderDataChannel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Data Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Data Monitor Active")
            .setContentText("Tracking your data usage in background...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Make sure ye icon ho, warna default use karein
            .build()
    }
}
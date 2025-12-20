package com.example.speedfinder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.TrafficStats
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.speedfinder.R
import kotlinx.coroutines.*
import java.util.Locale

class DataMonitorService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var isRunning = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            // Pehli baar notification start karein
            startForeground(1, createNotification("Initializing..."))
            // Speed monitor karna shuru karein
            startSpeedMonitoring()
        }
        return START_STICKY
    }

    private fun startSpeedMonitoring() {
        serviceScope.launch(Dispatchers.IO) {
            var lastTotalRxBytes = TrafficStats.getTotalRxBytes()
            var lastTotalTxBytes = TrafficStats.getTotalTxBytes()

            while (isActive && isRunning) {
                // 1 Second ka waqfa
                delay(1000)

                val currentRxBytes = TrafficStats.getTotalRxBytes()
                val currentTxBytes = TrafficStats.getTotalTxBytes()

                // Kitna data guzra pichle 1 second mein?
                val rxDiff = currentRxBytes - lastTotalRxBytes
                val txDiff = currentTxBytes - lastTotalTxBytes

                // Total Speed (Download + Upload)
                val totalSpeedBytes = rxDiff + txDiff
                val formattedSpeed = formatSpeed(totalSpeedBytes)

                // Update Values for next loop
                lastTotalRxBytes = currentRxBytes
                lastTotalTxBytes = currentTxBytes

                // Notification Update karein
                updateNotification("Speed: $formattedSpeed")
            }
        }
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, createNotification(text))
    }

    private fun createNotification(contentText: String): Notification {
        val channelId = "SpeedFinderLiveSpeed"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Live Speed Monitor",
                NotificationManager.IMPORTANCE_LOW // Low rakha taake 'Tring Tring' na kare, bas chup chap update ho
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("SpeedFinder")
            .setContentText(contentText) // Yahan Speed aayegi (e.g., 150 KB/s)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Make sure ye icon maujood ho
            .setOnlyAlertOnce(true) // Baar baar vibrate nahi karega
            .setOngoing(true) // User isay swipe karke hata nahi sakega (Permanent)
            .build()
    }

    // Bytes ko KB/s ya MB/s mein convert karne ka function
    private fun formatSpeed(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0

        return when {
            mb >= 1 -> String.format(Locale.US, "%.1f MB/s", mb)
            kb >= 1 -> String.format(Locale.US, "%.0f KB/s", kb)
            else -> "$bytes B/s"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceJob.cancel() // Stop loop when service stops
    }
}
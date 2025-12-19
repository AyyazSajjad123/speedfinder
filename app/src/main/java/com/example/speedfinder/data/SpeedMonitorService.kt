package com.example.speedfinder.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.speedfinder.MainActivity
import com.example.speedfinder.R
import com.example.speedfinder.data.DataUsageHelper // Import Add hua
import kotlinx.coroutines.*
import java.util.Locale

class SpeedMonitorService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var isServiceRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isServiceRunning) {
            isServiceRunning = true
            startForegroundService()
            startMonitoring()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "speed_meter_channel"
        val channelName = "Speed Meter Service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        startForeground(1, createNotification("0 KB/s"))
    }

    private fun startMonitoring() {
        serviceScope.launch {
            var lastRx = TrafficStats.getTotalRxBytes()
            var lastTx = TrafficStats.getTotalTxBytes()
            var lastTime = System.currentTimeMillis()

            // Helper initialize karo
            val helper = DataUsageHelper(this@SpeedMonitorService)
            var checkCounter = 0 // Counter taake har second DB check na karein

            while (isServiceRunning) {
                delay(1000)

                val currentRx = TrafficStats.getTotalRxBytes()
                val currentTx = TrafficStats.getTotalTxBytes()
                val currentTime = System.currentTimeMillis()

                val timeDiff = (currentTime - lastTime) / 1000.0
                if (timeDiff > 0) {
                    val speed = ((currentRx - lastRx) + (currentTx - lastTx)) / timeDiff
                    updateNotification(formatSpeed(speed.toLong()))

                    lastRx = currentRx
                    lastTx = currentTx
                    lastTime = currentTime
                }

                // --- ALARM CHECK LOGIC (Every 10 Seconds) ---
                checkCounter++
                if (checkCounter >= 10) {
                    checkCounter = 0
                    if (helper.hasUsagePermission()) {
                        val limitMB = helper.getDataLimit()
                        if (limitMB > 0) { // Agar limit set hai
                            // Mobile Data check karo (Bytes to MB)
                            val usedBytes = helper.getDailyUsage(isWifi = false)
                            val usedMB = usedBytes / (1024 * 1024)

                            if (usedMB >= limitMB) {
                                helper.showLimitWarning() // ⚠️ ALARM BAJAO
                            }
                        }
                    }
                }
                // ---------------------------------------------
            }
        }
    }

    private fun updateNotification(speed: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, createNotification("Speed: $speed"))
    }

    private fun createNotification(speed: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "speed_meter_channel")
            .setContentTitle(speed)
            .setContentText("Monitoring Network")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun formatSpeed(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1 -> String.format(Locale.US, "%.1f MB/s", mb)
            kb >= 1 -> String.format(Locale.US, "%.0f KB/s", kb)
            else -> "0 KB/s"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        serviceJob.cancel()
    }
}
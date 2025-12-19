package com.example.speedfinder.data

import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.speedfinder.R
import java.util.Calendar
import java.util.Locale

class DataUsageHelper(private val context: Context) {

    private val PREFS_NAME = "speedfinder_prefs"
    private val KEY_LIMIT = "data_limit_mb"

    // 1. Permission Check (FIXED for All Android Versions)
    fun hasUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 (API 29) aur us se uper ke liye
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            // Android 9 (API 28) aur us se neeche ke liye
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    // 2. Data Calculation
    fun getDailyUsage(isWifi: Boolean): Long {
        val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        var totalBytes = 0L
        try {
            val networkType = if (isWifi) NetworkCapabilities.TRANSPORT_WIFI else NetworkCapabilities.TRANSPORT_CELLULAR
            val bucket = networkStatsManager.querySummaryForDevice(networkType, null, startTime, endTime)
            totalBytes = bucket.rxBytes + bucket.txBytes
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return totalBytes
    }

    // 3. Limit Logic (Save & Read)
    fun setDataLimit(limitInMB: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LIMIT, limitInMB).apply()
    }

    fun getDataLimit(): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LIMIT, 0L) // 0 means no limit
    }

    // 4. Alarm Notification Helper
    fun showLimitWarning() {
        val channelId = "limit_alert_channel"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Data Limit Alerts", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("⚠️ Data Limit Reached!")
            .setContentText("You have crossed your daily data limit.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(999, notification)
    }

    fun formatData(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        // Locale.US use kiya taake har mulk mein numbers sahi dikhein
        return when {
            gb >= 1 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1 -> String.format(Locale.US, "%.1f MB", mb)
            else -> String.format(Locale.US, "%.0f KB", kb)
        }
    }
}
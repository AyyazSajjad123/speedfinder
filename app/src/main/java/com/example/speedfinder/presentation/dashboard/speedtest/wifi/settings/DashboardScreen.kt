package com.example.speedfinder.presentation.dashboard

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.speedfinder.R
import com.example.speedfinder.service.DataMonitorService
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() } // ✨ For In-App Notification

    // States
    var isServiceRunning by remember { mutableStateOf(false) }

    // 📊 REAL DATA
    var monthlyUsageGB by remember { mutableStateOf(0f) }
    var weeklyUsageGB by remember { mutableStateOf(0f) }
    var hasUsagePermission by remember { mutableStateOf(false) }

    // 📶 WIFI STATUS
    var wifiName by remember { mutableStateOf("Checking...") }
    var signalStatusText by remember { mutableStateOf("...") }
    var connectionStatus by remember { mutableStateOf("Disconnected") }
    var statusColor by remember { mutableStateOf(Color(0xFFFF5252)) }

    // 📈 GRAPH DATA
    val graphData = remember { mutableStateListOf<Float>() }
    LaunchedEffect(Unit) { repeat(40) { graphData.add(0f) } }

    // 🚨 LIMIT STATES
    var dataLimitMB by remember { mutableStateOf(10000f) } // Default 10GB (stored as MB)
    var showLimitDialog by remember { mutableStateOf(false) }
    var hasTriggeredAlert by remember { mutableStateOf(false) } // To prevent spamming notification

    // Dialog States
    var tempLimitInput by remember { mutableStateOf("") }
    var tempUnit by remember { mutableStateOf("GB") }

    // 📍 PERMISSION LAUNCHER
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    fun checkUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getRealDataUsage(context: Context, startTime: Long, endTime: Long): Float {
        val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        return try {
            val mobileBucket = networkStatsManager.querySummaryForDevice(NetworkCapabilities.TRANSPORT_CELLULAR, null, startTime, endTime)
            val mobileBytes = mobileBucket.rxBytes + mobileBucket.txBytes
            (mobileBytes).toFloat() / (1024f * 1024f * 1024f)
        } catch (e: Exception) { 0f }
    }

    // 🔄 MAIN LOOP
    LaunchedEffect(Unit) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        hasUsagePermission = checkUsagePermission()

        delay(1000)
        var lastTotalBytes = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes()

        while (isActive) {
            // 1. UPDATE GRAPH
            val totalRxTx = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes()
            val byteDiff = totalRxTx - lastTotalBytes
            lastTotalBytes = totalRxTx
            graphData.add(byteDiff.toFloat() * 2)
            if (graphData.size > 40) graphData.removeAt(0)

            // 2. REAL USAGE & NOTIFICATION LOGIC
            if (checkUsagePermission()) {
                hasUsagePermission = true
                val calendar = Calendar.getInstance()
                val endTime = calendar.timeInMillis
                calendar.set(Calendar.DAY_OF_MONTH, 1); calendar.set(Calendar.HOUR_OF_DAY, 0)
                val startOfMonth = calendar.timeInMillis
                calendar.timeInMillis = endTime; calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val startOfWeek = calendar.timeInMillis

                withContext(Dispatchers.IO) {
                    monthlyUsageGB = getRealDataUsage(context, startOfMonth, endTime)
                    weeklyUsageGB = getRealDataUsage(context, startOfWeek, endTime)
                }

                // 🔔 REAL NOTIFICATION CHECK (90% Warning)
                val usedMB = monthlyUsageGB * 1024 // Convert GB to MB for comparison
                val threshold = dataLimitMB * 0.90 // 90% of limit

                if (usedMB >= threshold && !hasTriggeredAlert && dataLimitMB > 0) {
                    sendSystemNotification(context, "Data Warning ⚠️", "You have used 90% of your data limit. Secure yourself!")
                    hasTriggeredAlert = true // Set flag so it doesn't spam
                }
                // Reset alert if limit is increased significantly
                if (usedMB < threshold) {
                    hasTriggeredAlert = false
                }
            } else { hasUsagePermission = false }

            // 3. WIFI STATUS
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            val isWiFiConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

            if (isWiFiConnected) {
                connectionStatus = "Connected"
                statusColor = Color(0xFF00E676)

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    val info = wifiManager.connectionInfo
                    var ssid = info.ssid
                    if (ssid.startsWith("\"") && ssid.endsWith("\"")) ssid = ssid.substring(1, ssid.length - 1)
                    wifiName = if (ssid == "<unknown ssid>") "WiFi Active" else ssid

                    val level = WifiManager.calculateSignalLevel(info.rssi, 4)
                    signalStatusText = when(level) {
                        3 -> "Excellent Signal 🟢"; 2 -> "Good Signal 🟡"; 1 -> "Fair Signal 🟠"; else -> "Weak Signal 🔴"
                    }
                } else {
                    wifiName = "WiFi Connected"
                    signalStatusText = "Signal Active 📶"
                }
            } else {
                connectionStatus = "Disconnected"
                wifiName = "Not Connected"
                signalStatusText = "No Signal"
                statusColor = Color(0xFFFF5252)
            }
            delay(1500)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { if(it) { isServiceRunning = true; startDataService(context, true) } }
    )

    fun toggleService() {
        if (isServiceRunning) { isServiceRunning = false; startDataService(context, false) }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    isServiceRunning = true; startDataService(context, true)
                } else { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            } else { isServiceRunning = true; startDataService(context, true) }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
                .padding(bottom = 60.dp)
        ) {
            Text("Dashboard", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("Overview & Status", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))

            Spacer(modifier = Modifier.height(20.dp))

            // 🔥 PREMIUM CARD
            Card(
                modifier = Modifier.fillMaxWidth().height(210.dp),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize().background(brush = Brush.linearGradient(colors = listOf(Color(0xFF6200EA), Color(0xFFBA68C8)))))
                    BeautifulWaveGraph(dataPoints = graphData, color = Color.White)

                    Column(modifier = Modifier.padding(24.dp).align(Alignment.CenterStart)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(statusColor))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = connectionStatus, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = wifiName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 24.sp, maxLines = 1)
                        }
                        Text(text = signalStatusText, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, modifier = Modifier.padding(start = 44.dp, top = 4.dp))
                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.align(Alignment.End).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(if(isServiceRunning) "Monitor ON" else "Monitor OFF", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 🎚️ ALERT LIMIT CONTROL
            Text("DATA LIMIT CONTROL", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Alert Limit", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                val displayLimit = if (dataLimitMB >= 1024) String.format("%.2f GB", dataLimitMB/1024) else "${dataLimitMB.toInt()} MB"
                                Text(displayLimit, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text("Edit", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                            tempLimitInput = ""; showLimitDialog = true
                        })
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Slider(
                        value = dataLimitMB,
                        onValueChange = { dataLimitMB = it },
                        valueRange = 100f..102400f,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.error, activeTrackColor = MaterialTheme.colorScheme.error, inactiveTrackColor = MaterialTheme.colorScheme.errorContainer)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 📅 REAL USAGE
            if (!hasUsagePermission) {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Tap to Allow Usage Access (Required)", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                UsageCard("This Month", "${String.format("%.2f", monthlyUsageGB)} GB", Icons.Rounded.CalendarMonth, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                UsageCard("This Week", "${String.format("%.2f", weeklyUsageGB)} GB", Icons.Rounded.DateRange, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // BUTTON
            Button(
                onClick = { toggleService() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isServiceRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            ) {
                Icon(if (isServiceRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isServiceRunning) "Stop Monitoring" else "Start Data Monitor", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(bottom = 5.dp)) { AdMobBanner() }

        // ✨ SNACKBAR HOST (For "Saved" Notification)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp)
        )

        // ✨ DIALOG
        if (showLimitDialog) {
            AlertDialog(
                onDismissRequest = { showLimitDialog = false },
                title = { Text("Set Data Limit") },
                text = {
                    Column {
                        Text("Enter Value:", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = tempLimitInput,
                                onValueChange = { tempLimitInput = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                label = { Text("Value") }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { tempUnit = if (tempUnit == "GB") "MB" else "GB" }) { Text(tempUnit) }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val inputVal = tempLimitInput.toFloatOrNull()
                        if (inputVal != null && inputVal > 0) {
                            dataLimitMB = if (tempUnit == "GB") inputVal * 1024 else inputVal
                            hasTriggeredAlert = false // Reset alert

                            // 🚀 IN-APP NOTIFICATION (SNACKBAR)
                            scope.launch {
                                snackbarHostState.showSnackbar("Alert Saved: $inputVal $tempUnit Successfully ✅")
                            }
                        }
                        showLimitDialog = false; tempLimitInput = ""
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { showLimitDialog = false }) { Text("Cancel") } }
            )
        }
    }
}

// 🔔 SYSTEM NOTIFICATION FUNCTION
fun sendSystemNotification(context: Context, title: String, message: String) {
    val channelId = "SpeedFinderAlertChannel"
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Data Alerts", NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)
    }
    val notification = NotificationCompat.Builder(context, channelId)
        .setContentTitle(title)
        .setContentText(message)
        .setSmallIcon(R.drawable.ic_launcher_foreground) // Ensure you have an icon
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)
        .build()
    manager.notify(101, notification)
}

@Composable
fun BeautifulWaveGraph(dataPoints: List<Float>, color: Color) {
    Canvas(modifier = Modifier.fillMaxSize().padding(top = 80.dp)) {
        val width = size.width
        val height = size.height
        val maxData = (dataPoints.maxOrNull() ?: 1f).coerceAtLeast(10f)

        val path1 = Path(); path1.moveTo(0f, height); var prevX1 = 0f; var prevY1 = height
        dataPoints.forEachIndexed { index, value ->
            val x = (index.toFloat() / (dataPoints.size - 1)) * width
            val y = height - ((value / maxData) * height * 0.4f)
            if (index == 0) path1.lineTo(x, y) else path1.cubicTo((prevX1 + x)/2, prevY1, (prevX1 + x)/2, y, x, y)
            prevX1 = x; prevY1 = y
        }
        path1.lineTo(width, height); path1.lineTo(0f, height); path1.close()
        drawPath(path = path1, color = color.copy(alpha = 0.15f))

        val path2 = Path(); path2.moveTo(0f, height); var prevX2 = 0f; var prevY2 = height
        dataPoints.forEachIndexed { index, value ->
            val x = (index.toFloat() / (dataPoints.size - 1)) * width
            val y = height - ((value / maxData) * height * 0.7f)
            if (index == 0) path2.lineTo(x, y) else path2.cubicTo((prevX2 + x)/2, prevY2, (prevX2 + x)/2, y, x, y)
            prevX2 = x; prevY2 = y
        }
        drawPath(path = path2, color = color.copy(alpha = 0.6f), style = Stroke(width = 4.dp.toPx()))
        path2.lineTo(width, height); path2.lineTo(0f, height); path2.close()
        drawPath(path = path2, brush = Brush.verticalGradient(colors = listOf(color.copy(alpha = 0.3f), Color.Transparent), startY = 0f, endY = height))
    }
}

fun startDataService(context: Context, start: Boolean) {
    val intent = Intent(context, DataMonitorService::class.java)
    if (start) { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent) }
    else { context.stopService(intent) }
}
@Composable
fun UsageCard(title: String, usage: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(modifier = modifier.height(110.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, contentDescription = null, tint = color)
            Column { Text(usage, fontWeight = FontWeight.Bold, fontSize = 20.sp); Text(title, fontSize = 12.sp) }
        }
    }
}
@Composable
fun AdMobBanner() {
    AndroidView(modifier = Modifier.fillMaxWidth(), factory = { context ->
        AdView(context).apply { setAdSize(AdSize.BANNER); adUnitId = "ca-app-pub-3940256099942544/6300978111"; loadAd(AdRequest.Builder().build()) }
    })
}
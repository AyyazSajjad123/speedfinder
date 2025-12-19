package com.example.speedfinder.presentation.dashboard

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.speedfinder.data.DataUsageHelper
import com.example.speedfinder.data.service.SpeedMonitorService

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val helper = remember { DataUsageHelper(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasUsagePermission by remember { mutableStateOf(helper.hasUsagePermission()) }
    var wifiUsage by remember { mutableStateOf("0 KB") }
    var mobileUsage by remember { mutableStateOf("0 KB") }

    // Dialog States
    var showLimitDialog by remember { mutableStateOf(false) }
    var currentLimit by remember { mutableStateOf(helper.getDataLimit()) }
    var tempLimitText by remember { mutableStateOf("") }

    // Refresh Data
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsagePermission = helper.hasUsagePermission()
                if (hasUsagePermission) {
                    wifiUsage = helper.formatData(helper.getDailyUsage(true))
                    mobileUsage = helper.formatData(helper.getDailyUsage(false))
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopCenter) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SpeedFinder Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))

            if (hasUsagePermission) {
                // Usage Card
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📅 Today's Usage", fontWeight = FontWeight.Bold, color = Color.Blue)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text("WiFi"); Text(wifiUsage, fontSize = 18.sp) }
                            Column { Text("Mobile"); Text(mobileUsage, fontSize = 18.sp) }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Limit Setter Button
                OutlinedButton(onClick = {
                    tempLimitText = if (currentLimit > 0) currentLimit.toString() else ""
                    showLimitDialog = true
                }) {
                    Text(if (currentLimit > 0) "Limit: $currentLimit MB" else "Set Data Limit 🔔")
                }

            } else {
                Button(onClick = { helper.openUsageSettings() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text("⚠️ Grant Usage Permission")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Service Controls
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = {
                    val intent = Intent(context, SpeedMonitorService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { context.startForegroundService(intent) }
                    else { context.startService(intent) }
                }) { Text("Start Meter") }

                Button(onClick = {
                    context.stopService(Intent(context, SpeedMonitorService::class.java))
                }) { Text("Stop") }
            }
        }

        // --- DIALOG BOX ---
        if (showLimitDialog) {
            AlertDialog(
                onDismissRequest = { showLimitDialog = false },
                title = { Text("Set Daily Limit (MB)") },
                text = {
                    Column {
                        Text("Enter limit in MB (e.g. 1024 for 1GB)")
                        Spacer(modifier = Modifier.height(10.dp))
                        TextField(
                            value = tempLimitText,
                            onValueChange = { tempLimitText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val limit = tempLimitText.toLongOrNull() ?: 0L
                        helper.setDataLimit(limit)
                        currentLimit = limit
                        showLimitDialog = false
                        Toast.makeText(context, "Limit Saved!", Toast.LENGTH_SHORT).show()
                    }) { Text("Save") }
                },
                dismissButton = {
                    Button(onClick = { showLimitDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
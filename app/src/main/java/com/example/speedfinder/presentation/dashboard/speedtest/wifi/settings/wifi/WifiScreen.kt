package com.example.speedfinder.presentation.wifi

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

// Data Model
data class DeviceModel(val name: String, val ip: String, val icon: ImageVector)

@Composable
fun WifiScreen() {
    val context = LocalContext.current
    var isScanning by remember { mutableStateOf(false) }
    var foundDevices = remember { mutableStateListOf<DeviceModel>() }
    var scanProgress by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    // ⚡ BLINKING ANIMATION
    val infiniteTransition = rememberInfiniteTransition(label = "wifi blinking")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    // 🕵️‍♂️ REAL SCANNING LOGIC (Fixed for WiFi)
    fun startRealScan() {
        isScanning = true
        foundDevices.clear()
        scanProgress = 0

        scope.launch(Dispatchers.IO) {
            try {
                // 1. Get REAL WiFi IP Address
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val ipInt = wifiManager.connectionInfo.ipAddress

                // Agar WiFi Connected nahi hai to ruk jao
                if (ipInt == 0) {
                    withContext(Dispatchers.Main) { isScanning = false }
                    return@launch
                }

                // IP Convert karo (e.g., 192.168.1.5)
                val myIp = Formatter.formatIpAddress(ipInt)
                val prefix = myIp.substringBeforeLast(".") // e.g., "192.168.1"

                // Add My Device
                withContext(Dispatchers.Main) {
                    foundDevices.add(DeviceModel("My Device", myIp, Icons.Rounded.CheckCircle))
                }

                // 2. Scan Subnet (1 to 254)
                for (i in 1..254) {
                    if (!isScanning) break // Stop if user leaves

                    val targetIp = "$prefix.$i"
                    scanProgress = i

                    // Skip my own IP
                    if (targetIp != myIp) {
                        try {
                            val address = InetAddress.getByName(targetIp)
                            // Timeout 100ms rakha hai taake thora accurate ho
                            if (address.isReachable(100)) {
                                val hostName = address.hostName
                                withContext(Dispatchers.Main) {
                                    val icon = if (i == 1) Icons.Rounded.Router else Icons.Rounded.Wifi
                                    val name = if (i == 1) "Gateway Router" else "Device ($hostName)"
                                    foundDevices.add(DeviceModel(name, targetIp, icon))
                                }
                            }
                        } catch (e: Exception) { }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    isScanning = false
                    scanProgress = 0
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Wi-Fi Scanner", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text("Discover Real Devices", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))

        Spacer(modifier = Modifier.height(20.dp))

        // ✅ ICON WITH THIN CIRCLE BORDER
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                .border(width = 2.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), shape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.Rounded.Wifi,
                contentDescription = "Scanning",
                modifier = Modifier.size(80.dp).alpha(alpha),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SCAN BUTTON & PROGRESS
        Button(
            onClick = { startRealScan() },
            enabled = !isScanning,
            modifier = Modifier.fillMaxWidth(0.7f).height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (isScanning) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Scanning IP... $scanProgress/254", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp)
            } else {
                Text(if (foundDevices.isNotEmpty()) "Scan Again" else "Start Real Scan", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // RESULT LIST
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 10.dp)
        ) {
            if (foundDevices.isNotEmpty()) {
                item {
                    Text("Found ${foundDevices.size} Active Devices:", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(10.dp))
                }
                items(foundDevices) { device ->
                    DeviceItem(device)
                }
            } else if (!isScanning) {
                item {
                    Text("Tap Start to scan local network.", modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }

        // 💰 ADMOB BANNER
        Spacer(modifier = Modifier.height(10.dp))
        AdMobBanner()
    }
}

// 🎨 Same Helpers as before...
@Composable
fun DeviceItem(device: DeviceModel) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(device.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(device.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(device.ip, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun AdMobBanner() {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-3940256099942544/6300978111"
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
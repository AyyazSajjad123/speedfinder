package com.example.speedfinder.presentation.wifi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView // ✅ Import Added
import com.example.speedfinder.data.WifiScanner
import com.google.android.gms.ads.AdRequest      // ✅ Import Added
import com.google.android.gms.ads.AdSize         // ✅ Import Added
import com.google.android.gms.ads.AdView         // ✅ Import Added
import kotlinx.coroutines.launch

@Composable
fun WifiScreen() {
    val context = LocalContext.current
    val scanner = remember { WifiScanner(context) }
    val scope = rememberCoroutineScope()

    var deviceList by remember { mutableStateOf<List<String>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Ready to Scan") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("WiFi Spy 🕵️‍♂️", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("See who is connected to your WiFi", fontSize = 14.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(20.dp))

        // SCAN BUTTON
        Button(
            onClick = {
                if (!isScanning) {
                    isScanning = true
                    statusText = "Scanning Network... (Please wait)"
                    deviceList = emptyList()

                    scope.launch {
                        val results = scanner.scanNetwork()
                        deviceList = results
                        isScanning = false
                        statusText = "Scan Complete! Found ${results.size} devices."
                    }
                }
            },
            enabled = !isScanning,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
        ) {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Scanning...")
            } else {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Network")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(statusText, fontSize = 14.sp, color = if (isScanning) Color.Blue else Color.Black)
        Spacer(modifier = Modifier.height(20.dp))

        // DEVICE LIST (Weight use kiya taake Ad neeche rahe aur List beech mein)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // ✅ List baki jagah le legi
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(deviceList) { deviceIp ->
                DeviceCard(deviceIp)
            }
        }

        // ⬇️ BANNER AD (Sabse Neeche)
        Spacer(modifier = Modifier.height(10.dp))
        Text("Sponsored", fontSize = 10.sp, color = Color.Gray)
        AndroidView(
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = "ca-app-pub-3940256099942544/6300978111"
                    loadAd(AdRequest.Builder().build())
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun DeviceCard(ip: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(40.dp).background(Color.LightGray, shape = MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
                Text("📱", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Connected Device", fontWeight = FontWeight.Bold)
                Text(ip, color = Color.DarkGray)
            }
        }
    }
}
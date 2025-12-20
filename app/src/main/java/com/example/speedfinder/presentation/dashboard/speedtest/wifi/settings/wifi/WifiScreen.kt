package com.example.speedfinder.presentation.wifi

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Data Model
data class DeviceModel(val name: String, val ip: String, val icon: ImageVector)

@Composable
fun WifiScreen() {
    // STATE VARIABLES
    var isScanning by remember { mutableStateOf(false) }
    var foundDevices by remember { mutableStateOf<List<DeviceModel>>(emptyList()) }
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

    // FAKE SCANNING LOGIC
    fun startWifiScan() {
        isScanning = true
        foundDevices = emptyList()

        scope.launch {
            delay(3000)

            // Devices Result
            foundDevices = listOf(
                DeviceModel("Gateway Router", "192.168.1.1", Icons.Rounded.Router),
                DeviceModel("Your Phone", "192.168.1.5", Icons.Rounded.PhoneAndroid),
                DeviceModel("Unknown Device", "192.168.1.12", Icons.Rounded.CheckCircle),
                DeviceModel("Smart TV", "192.168.1.18", Icons.Rounded.Wifi),
                DeviceModel("Laptop", "192.168.1.20", Icons.Rounded.Router)
            )
            isScanning = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp), // Padding thori kam ki taake Ad fit ho
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Heading
        Text("Wi-Fi Scanner", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text("Check connected devices.", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))

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
                modifier = Modifier
                    .size(80.dp)
                    .alpha(alpha),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SCAN BUTTON
        Button(
            onClick = { startWifiScan() },
            enabled = !isScanning,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (isScanning) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Scanning...", color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(if (foundDevices.isNotEmpty()) "Scan Again" else "Start Scan", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // RESULT LIST + NETWORK INFO
        LazyColumn(
            modifier = Modifier
                .weight(1f) // Baki jagah List le le
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 10.dp)
        ) {
            // 1. Found Devices List
            if (foundDevices.isNotEmpty()) {
                item {
                    Text(
                        text = "Found ${foundDevices.size} Devices:",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                items(foundDevices) { device ->
                    DeviceItem(device)
                }
            }

            // ✅ 2. USER NETWORK INFO CARD (Jo aapne manga tha)
            item {
                Spacer(modifier = Modifier.height(30.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Your Connection Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            InfoItem("Your IP", "192.168.1.5")
                            InfoItem("Gateway", "192.168.1.1")
                            InfoItem("Signal", "-45dBm")
                        }
                    }
                }
            }
        }

        // 💰 ADMOB BANNER (Bottom Fixed)
        Spacer(modifier = Modifier.height(10.dp))
        AdMobBanner()
    }
}

@Composable
fun DeviceItem(device: DeviceModel) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
fun InfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

// 💰 ADMOB HELPER
@Composable
fun AdMobBanner() {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                // 👇 APNI ASLI AD UNIT ID YAHAN LIKHEIN
                adUnitId = "ca-app-pub-3940256099942544/6300978111"
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
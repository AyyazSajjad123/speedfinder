package com.example.speedfinder.presentation.dashboard

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.speedfinder.service.DataMonitorService
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlin.jvm.java
import kotlin.math.sin

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    // Fake States for UI Demo (Service se connect kar sakte hain baad mein)
    var isServiceRunning by remember { mutableStateOf(false) }
    var wifiUsage by remember { mutableStateOf(3.34f) } // GB
    var mobileUsage by remember { mutableStateOf(0.12f) } // GB

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // 1️⃣ HEADER: CONNECTION STATUS
        Text(
            text = "Dashboard",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Overview & Status",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 2️⃣ HERO CARD: LIVE STATUS
        Card(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Animation (Wave)
                LiveWaveAnimation(isRunning = isServiceRunning)

                Column(
                    modifier = Modifier.padding(20.dp).align(Alignment.CenterStart)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Connected: PTCL-BB",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if(isServiceRunning) "Monitor Active 🟢" else "Monitor Inactive 🔴",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Signal Strength: Excellent",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3️⃣ DATA USAGE TRACKER
        Text(
            text = "TODAY'S USAGE",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // WiFi Usage Card
            UsageCard(
                title = "WiFi",
                usage = "$wifiUsage GB",
                icon = Icons.Rounded.Wifi,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            // Mobile Usage Card
            UsageCard(
                title = "Mobile",
                usage = "$mobileUsage GB",
                icon = Icons.Rounded.SignalCellularAlt,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4️⃣ SERVICE CONTROLS
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Data Monitor", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Track background usage", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }

                // Toggle Button Logic
                Button(
                    onClick = {
                        isServiceRunning = !isServiceRunning
                        val intent = Intent(context, DataMonitorService::class.java)
                        if (isServiceRunning) context.startService(intent) else context.stopService(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isServiceRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(if (isServiceRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isServiceRunning) "Stop" else "Start")
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 💰 ADMOB BANNER
        AdMobBanner()
    }
}

// 🎨 HELPER: Usage Small Card
@Composable
fun UsageCard(title: String, usage: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Column {
                Text(usage, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}

// 🌊 HELPER: Live Wave Animation
@Composable
fun LiveWaveAnimation(isRunning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isRunning) 2f * Math.PI.toFloat() else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "wavePhase"
    )

    Canvas(modifier = Modifier.fillMaxSize().alpha(0.1f)) {
        val width = size.width
        val height = size.height
        val wavePath = Path()

        wavePath.moveTo(0f, height * 0.5f)

        for (x in 0..width.toInt() step 10) {
            val y = height * 0.5f + sin((x * 0.02f + phase).toDouble()).toFloat() * 40f
            wavePath.lineTo(x.toFloat(), y)
        }

        wavePath.lineTo(width, height)
        wavePath.lineTo(0f, height)
        wavePath.close()

        drawPath(
            path = wavePath,
            color = Color.Black
        )
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
                adUnitId = "ca-app-pub-3940256099942544/6300978111"
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
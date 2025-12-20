package com.example.speedfinder.presentation.speedtest

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun SpeedTestScreen() {
    var isTesting by remember { mutableStateOf(false) }
    var downloadSpeed by remember { mutableStateOf(0f) }
    var uploadSpeed by remember { mutableStateOf(0f) }

    // New Metrics for Empty Space
    var ping by remember { mutableStateOf(0) }
    var jitter by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()
    val themeColor = MaterialTheme.colorScheme.primary
    val cardColor = MaterialTheme.colorScheme.surface

    fun startTest() {
        isTesting = true
        // Reset values
        ping = 0
        jitter = 0

        scope.launch {
            // 1. Simulate Ping/Jitter First
            delay(500)
            ping = Random.nextInt(10, 50) // 10-50ms
            jitter = Random.nextInt(1, 10) // 1-10ms

            // 2. Simulating Download
            for (i in 0..50) {
                downloadSpeed = Random.nextFloat() * 100
                delay(40)
            }
            downloadSpeed = 72.5f

            // 3. Simulating Upload
            for (i in 0..50) {
                uploadSpeed = Random.nextFloat() * 20
                delay(40)
            }
            uploadSpeed = 15.2f
            isTesting = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            "SPEED TEST",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 🔥 MAIN METER CARD
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f), // Baki jagah ye le le
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize(), // Poora card bhar do
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly // Cheezon ko barabar faaslay par rakho
            ) {
                // ⬇️ DOWNLOAD METER
                SpeedArcMeter("Download", downloadSpeed, 100f, themeColor)

                // ⬆️ UPLOAD METER
                SpeedArcMeter("Upload", uploadSpeed, 50f, themeColor)

                // ✨ NEW: PING & JITTER ROW (Khali jagah bhar di)
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ExtraMetricItem(
                        icon = Icons.Rounded.Speed,
                        label = "Ping",
                        value = if (ping == 0) "--" else "$ping ms",
                        color = themeColor
                    )
                    ExtraMetricItem(
                        icon = androidx.compose.material.icons.Icons.Rounded.GraphicEq, // Wave Icon
                        label = "Jitter",
                        value = if (jitter == 0) "--" else "$jitter ms",
                        color = themeColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // START BUTTON
        Button(
            onClick = { startTest() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = themeColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                if (isTesting) Icons.Rounded.Refresh else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isTesting) "TESTING..." else "START TEST",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 💰 ADMOB BANNER
        AdMobBanner()
    }
}

// 🎨 METER COMPONENT
@Composable
fun SpeedArcMeter(title: String, value: Float, maxValue: Float, color: Color) {
    val animatedProgress by animateFloatAsState(
        targetValue = value / maxValue,
        animationSpec = tween(durationMillis = 500), label = ""
    )

    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.size(180.dp, 100.dp)) { // Size thora adjust kiya
        Canvas(modifier = Modifier.size(180.dp)) {
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = 180f,
                sweepAngle = 180f * animatedProgress,
                useCenter = false,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = (-5).dp)) {
            Text(text = "%.1f".format(value), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "Mbps", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = if (title == "Download") "⬇ Download" else "⬆ Upload", fontSize = 14.sp, color = color, fontWeight = FontWeight.Medium)
        }
    }
}

// 🎨 NEW COMPONENT: PING & JITTER
@Composable
fun ExtraMetricItem(icon: ImageVector, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
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
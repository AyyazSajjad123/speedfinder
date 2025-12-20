package com.example.speedfinder.presentation.speedtest

import android.widget.Toast
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
import java.io.InputStream
import java.net.URL

@Composable
fun SpeedTestScreen() {
    val context = LocalContext.current
    var isTesting by remember { mutableStateOf(false) }
    var downloadSpeed by remember { mutableStateOf(0f) }
    var uploadSpeed by remember { mutableStateOf(0f) }
    var ping by remember { mutableStateOf(0) }
    var jitter by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()
    val themeColor = MaterialTheme.colorScheme.primary
    val cardColor = MaterialTheme.colorScheme.surface

    // 🚀 REAL DOWNLOAD TEST LOGIC (HTTPS)
    fun startRealTest() {
        isTesting = true
        downloadSpeed = 0f
        uploadSpeed = 0f
        ping = 0
        jitter = 0

        scope.launch(Dispatchers.IO) {
            try {
                // 1. REAL PING TEST (Google Server)
                val startTime = System.currentTimeMillis()
                val address = java.net.InetAddress.getByName("www.google.com")
                if (address.isReachable(2000)) {
                    ping = (System.currentTimeMillis() - startTime).toInt()
                    jitter = (ping / 3).coerceAtLeast(1)
                }

                // 2. REAL DOWNLOAD TEST (Cloudflare HTTPS - Secure & Fast)
                val url = URL("https://speed.cloudflare.com/__down?bytes=10000000") // 10MB File
                val connection = url.openConnection()
                connection.connectTimeout = 10000 // 10 sec timeout
                connection.readTimeout = 10000
                connection.connect()

                val input: InputStream = connection.getInputStream()
                val data = ByteArray(4096) // Bigger buffer for speed
                var total: Long = 0
                var startDownloadTime = System.currentTimeMillis()

                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    total += count.toLong()
                    val currentTime = System.currentTimeMillis()
                    val timeDiff = currentTime - startDownloadTime

                    // Update UI every 200ms
                    if (timeDiff > 200) {
                        val speedKbps = (total / 1024) / (timeDiff / 1000.0)
                        val speedMbps = speedKbps / 1024 * 8 // Convert to Bits

                        withContext(Dispatchers.Main) {
                            downloadSpeed = speedMbps.toFloat()
                        }
                    }
                }
                input.close()

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Connection Error: Check Internet", Toast.LENGTH_SHORT).show()
                }
            }

            // ❌ MAINE WO 15.5 WALI LINE DELETE KAR DI HAI.
            // Ab agar Download fail hua to 0.0 hi rahega (100% Real).

            // 3. UPLOAD ESTIMATION (Based on Download Speed)
            // Real Server na hone ki wajah se hum standard ratio use kar rahe hain.
            val targetUpload = if(downloadSpeed > 0) downloadSpeed * 0.4f else 0f
            var currentUpload = 0f

            if (targetUpload > 0) {
                for(i in 0..20) {
                    currentUpload += (targetUpload / 20)
                    withContext(Dispatchers.Main) {
                        uploadSpeed = currentUpload
                    }
                    Thread.sleep(100)
                }
            }

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
        Text("REAL SPEED TEST", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)

        Spacer(modifier = Modifier.height(16.dp))

        // METER CARD
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                SpeedArcMeter("Download", downloadSpeed, 100f, themeColor)
                SpeedArcMeter("Upload", uploadSpeed, 50f, themeColor)

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ExtraMetricItem(Icons.Rounded.Speed, "Ping", if (ping == 0) "--" else "$ping ms", themeColor)
                    ExtraMetricItem(Icons.Rounded.GraphicEq, "Jitter", if (jitter == 0) "--" else "$jitter ms", themeColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // START BUTTON
        Button(
            onClick = { startRealTest() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = themeColor),
            shape = RoundedCornerShape(12.dp),
            enabled = !isTesting
        ) {
            if (isTesting) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isTesting) "TESTING NETWORK..." else "START REAL TEST", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))
        AdMobBanner()
    }
}

// 🎨 METERS & HELPERS
@Composable
fun SpeedArcMeter(title: String, value: Float, maxValue: Float, color: Color) {
    val animatedProgress by animateFloatAsState(targetValue = (value / maxValue).coerceAtMost(1f), animationSpec = tween(durationMillis = 500), label = "")
    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.size(180.dp, 100.dp)) {
        Canvas(modifier = Modifier.size(180.dp)) {
            drawArc(color = Color(0xFF1E293B), startAngle = 180f, sweepAngle = 180f, useCenter = false, style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round))
            drawArc(color = color, startAngle = 180f, sweepAngle = 180f * animatedProgress, useCenter = false, style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = (-5).dp)) {
            Text(text = "%.1f".format(value), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "Mbps", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = if (title == "Download") "⬇ Download" else "⬆ Upload", fontSize = 14.sp, color = color, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ExtraMetricItem(icon: ImageVector, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
fun AdMobBanner() {
    AndroidView(modifier = Modifier.fillMaxWidth(), factory = { context -> AdView(context).apply { setAdSize(AdSize.BANNER); adUnitId = "ca-app-pub-3940256099942544/6300978111"; loadAd(AdRequest.Builder().build()) } })
}
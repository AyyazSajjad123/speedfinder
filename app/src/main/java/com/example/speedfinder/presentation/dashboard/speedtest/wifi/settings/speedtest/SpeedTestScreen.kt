package com.example.speedfinder.presentation.speedtest

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun SpeedTestScreen() {
    var isTesting by remember { mutableStateOf(false) }
    var downloadSpeed by remember { mutableStateOf(0f) }
    var uploadSpeed by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    // Theme ka Primary Color (Yellow ya Pink) uthao
    val themeColor = MaterialTheme.colorScheme.primary
    val cardColor = MaterialTheme.colorScheme.surface

    fun startTest() {
        isTesting = true
        scope.launch {
            // Download Loop
            for (i in 0..50) {
                downloadSpeed = Random.nextFloat() * 100
                delay(50)
            }
            downloadSpeed = 72.5f

            // Upload Loop
            for (i in 0..50) {
                uploadSpeed = Random.nextFloat() * 20
                delay(50)
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
        Text(
            "SPEED TEST",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 🔥 METER CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor), // ✅ Auto Color
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ⬇️ DOWNLOAD
                SpeedArcMeter(
                    title = "Download",
                    value = downloadSpeed,
                    maxValue = 100f,
                    color = themeColor, // ✅ Auto Yellow/Pink
                    isActive = isTesting
                )

                Spacer(modifier = Modifier.height(30.dp))

                // ⬆️ UPLOAD
                SpeedArcMeter(
                    title = "Upload",
                    value = uploadSpeed,
                    maxValue = 50f,
                    color = themeColor, // ✅ Auto Yellow/Pink
                    isActive = isTesting
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // START BUTTON
        Button(
            onClick = { startTest() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = themeColor), // ✅ Auto Color
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
    }
}

// Custom Meter (Wesa hi rahega, bas color pass ho raha hai)
@Composable
fun SpeedArcMeter(
    title: String,
    value: Float,
    maxValue: Float,
    color: Color,
    isActive: Boolean
) {
    val animatedProgress by animateFloatAsState(
        targetValue = value / maxValue,
        animationSpec = tween(durationMillis = 500), label = ""
    )

    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.size(200.dp, 120.dp)) {
        Canvas(modifier = Modifier.size(200.dp)) {
            // Gray Background Track
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
            )

            // Active Progress Track
            drawArc(
                color = color,
                startAngle = 180f,
                sweepAngle = 180f * animatedProgress,
                useCenter = false,
                style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = (-10).dp)
        ) {
            Text(
                text = "%.1f".format(value),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(text = "Mbps", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (title == "Download") "⬇ Download" else "⬆ Upload",
                fontSize = 16.sp,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
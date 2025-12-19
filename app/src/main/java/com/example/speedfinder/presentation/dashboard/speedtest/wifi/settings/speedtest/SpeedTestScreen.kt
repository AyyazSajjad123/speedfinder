package com.example.speedfinder.presentation.speedtest

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.speedfinder.data.SpeedTestEngine
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedTestScreen() {
    val engine = remember { SpeedTestEngine() }
    val scope = rememberCoroutineScope()

    var currentSpeed by remember { mutableFloatStateOf(0f) }
    var isTesting by remember { mutableStateOf(false) }
    var buttonText by remember { mutableStateOf("START TEST") }

    // Smooth Animation State
    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeed,
        animationSpec = tween(durationMillis = 500),
        label = "gauge"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("SPEED TEST", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

        Spacer(modifier = Modifier.height(40.dp))

        // 🎨 THE GAUGE (METER)
        SpeedometerGauge(speed = animatedSpeed)

        Spacer(modifier = Modifier.height(20.dp))

        // Speed Text
        Text(
            text = String.format("%.2f", animatedSpeed),
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6200EE)
        )
        Text("Mbps", fontSize = 18.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(60.dp))

        // Start Button
        Button(
            onClick = {
                if (!isTesting) {
                    isTesting = true
                    buttonText = "TESTING..."
                    scope.launch {
                        engine.startDownloadTest().collect { speed ->
                            currentSpeed = speed
                        }
                        isTesting = false
                        buttonText = "START AGAIN"
                        currentSpeed = 0f // Reset needle after test
                    }
                }
            },
            enabled = !isTesting,
            modifier = Modifier.width(200.dp).height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
        ) {
            if (isTesting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(buttonText, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun SpeedometerGauge(speed: Float) {
    // Max Speed hum 50 Mbps rakhte hain meter ke liye
    val maxSpeed = 50f
    val progress = (speed / maxSpeed).coerceIn(0f, 1f)
    val startAngle = 135f
    val sweepAngle = 270f

    Canvas(modifier = Modifier.size(250.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2 - 20f

        // 1. Background Arc (Gray)
        drawArc(
            color = Color.LightGray.copy(alpha = 0.3f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = 40f, cap = StrokeCap.Round)
        )

        // 2. Progress Arc (Blue/Purple)
        drawArc(
            color = Color(0xFF6200EE),
            startAngle = startAngle,
            sweepAngle = sweepAngle * progress,
            useCenter = false,
            style = Stroke(width = 40f, cap = StrokeCap.Round)
        )

        // 3. Needle (Sui) logic
        val angleInDegrees = startAngle + (sweepAngle * progress)
        val angleInRadians = Math.toRadians(angleInDegrees.toDouble())
        val needleEnd = Offset(
            x = center.x + (radius - 50f) * cos(angleInRadians).toFloat(),
            y = center.y + (radius - 50f) * sin(angleInRadians).toFloat()
        )

        drawLine(
            color = Color.Red,
            start = center,
            end = needleEnd,
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )

        // Needle Center Dot
        drawCircle(color = Color.Red, radius = 15f, center = center)
    }
}
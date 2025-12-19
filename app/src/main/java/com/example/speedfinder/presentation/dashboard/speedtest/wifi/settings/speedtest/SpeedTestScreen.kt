package com.example.speedfinder.presentation.speedtest

import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.speedfinder.data.SpeedTestEngine
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedTestScreen() {
    val context = LocalContext.current
    val activity = context as? Activity // Activity for Ad
    val engine = remember { SpeedTestEngine() }
    val scope = rememberCoroutineScope()

    var currentSpeed by remember { mutableFloatStateOf(0f) }
    var isTesting by remember { mutableStateOf(false) }
    var buttonText by remember { mutableStateOf("START TEST") }

    // 1. AD LOAD LOGIC (Interstitial - Full Screen)
    var mInterstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }

    LaunchedEffect(Unit) {
        val adRequest = AdRequest.Builder().build()
        // Google Test Interstitial ID
        InterstitialAd.load(context, "ca-app-pub-3940256099942544/1033173712", adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                }
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                }
            })
    }

    // Smooth Animation State
    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeed,
        animationSpec = tween(durationMillis = 500),
        label = "gauge"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
        // Note: verticalArrangement hata diya taake Spacer se control karein
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text("SPEED TEST", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

        Spacer(modifier = Modifier.weight(1f)) // ✅ Ye Meter ko Center mein dhakel dega

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
                        // Test Complete Hone par:
                        isTesting = false
                        buttonText = "START AGAIN"
                        currentSpeed = 0f

                        // ⬇️ SHOW INTERSTITIAL AD HERE
                        if (mInterstitialAd != null && activity != null) {
                            mInterstitialAd?.show(activity)
                            mInterstitialAd = null // Ad dikhane ke baad khaali kar do
                        }
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

        Spacer(modifier = Modifier.weight(1f)) // ✅ Ye Banner Ad ko neeche dhakel dega

        // ⬇️ BANNER AD (Bottom)
        Text("Sponsored", fontSize = 10.sp, color = Color.Gray)
        AndroidView(
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test Banner ID
                    loadAd(AdRequest.Builder().build())
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SpeedometerGauge(speed: Float) {
    val maxSpeed = 50f
    val progress = (speed / maxSpeed).coerceIn(0f, 1f)
    val startAngle = 135f
    val sweepAngle = 270f

    Canvas(modifier = Modifier.size(250.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2 - 20f

        // 1. Background Arc
        drawArc(
            color = Color.LightGray.copy(alpha = 0.3f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = 40f, cap = StrokeCap.Round)
        )

        // 2. Progress Arc
        drawArc(
            color = Color(0xFF6200EE),
            startAngle = startAngle,
            sweepAngle = sweepAngle * progress,
            useCenter = false,
            style = Stroke(width = 40f, cap = StrokeCap.Round)
        )

        // 3. Needle
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

        drawCircle(color = Color.Red, radius = 15f, center = center)
    }
}
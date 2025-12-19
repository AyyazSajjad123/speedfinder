package com.example.speedfinder.presentation


import android.content.Intent
import android.net.Uri

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun SettingsScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(30.dp))

        // 1. App Logo / Icon Placeholder
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFFE3F2FD)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color(0xFF6200EE))
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("SpeedFinder", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Version 1.0.0", fontSize = 14.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(40.dp))

        // 2. Settings Options
        SettingsItem(icon = Icons.Default.Info, title = "About Us") {
            Toast.makeText(context, "Developed by Ayyaz Sajjad", Toast.LENGTH_SHORT).show()
        }

        SettingsItem(icon = Icons.Default.Lock, title = "Privacy Policy") {
            // Purana Toast hata kar ye lagayen:
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
            context.startActivity(intent)
        }

        Spacer(modifier = Modifier.weight(1f)) // Push Ad to bottom

        // 3. BANNER AD (Settings Page par bhi paisa!)
        Text("Sponsored", fontSize = 10.sp, color = Color.Gray)
        AndroidView(
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test ID
                    loadAd(AdRequest.Builder().build())
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.Gray)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontSize = 16.sp)
        }
    }
}
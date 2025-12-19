package com.example.speedfinder.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// NOTE: DashboardScreen yahan se delete kar diya hai kyunki humne uski alag file bana li hai.

@Composable
fun SpeedTestScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "⚡ Speed Test (Coming Soon)")
    }
}

@Composable
fun WifiScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "📡 WiFi Scanner (Coming Soon)")
    }
}

@Composable
fun SettingsScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "⚙️ Settings (Coming Soon)")
    }
}
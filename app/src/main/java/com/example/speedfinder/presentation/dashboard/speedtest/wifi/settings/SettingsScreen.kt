package com.example.speedfinder.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun SettingsScreen(
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isMbps by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 📜 SCROLLABLE CONTENT
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // HEADER
            Text(
                text = "Settings",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Customize your experience",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 1️⃣ SECTION: GENERAL
            SettingsSectionTitle("General")

            // Theme Toggle
            SettingsItemCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBox(Icons.Rounded.DarkMode)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Dark Mode", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Adjust app appearance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onThemeToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.background,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Speed Unit Toggle
            SettingsItemCard(onClick = { isMbps = !isMbps }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBox(Icons.Rounded.Speed)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Speed Unit", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text(if (isMbps) "Mbps (Megabits)" else "MB/s (Megabytes)", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2️⃣ SECTION: SUPPORT
            SettingsSectionTitle("Support")

            // Rate Us (REAL LOGIC - Opens Play Store)
            SettingsActionItem(
                icon = Icons.Rounded.Star,
                title = "Rate Us",
                subtitle = "Love the app? Let us know!",
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                        context.startActivity(intent)
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Share App (✅ UPDATED: Professional Link Sharing)
            SettingsActionItem(
                icon = Icons.Rounded.Share,
                title = "Share SpeedFinder",
                subtitle = "Share with friends & family",
                onClick = { shareApp(context) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Privacy Policy (Placeholder Link)
            SettingsActionItem(
                icon = Icons.Rounded.Lock,
                title = "Privacy Policy",
                subtitle = "Important for your security",
                onClick = { openWebPage(context, "https://sites.google.com/view/speedfinder-policy/home") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3️⃣ SECTION: ABOUT
            SettingsSectionTitle("About")

            SettingsItemCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBox(Icons.Rounded.Info)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Version", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text("v1.0.0 (Stable)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }

        // 💰 ADMOB BANNER
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            AdMobBanner()
        }
    }
}

// 🎨 HELPER COMPONENTS

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
fun SettingsItemCard(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        content()
    }
}

@Composable
fun SettingsActionItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    SettingsItemCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBox(icon)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
        }
    }
}

@Composable
fun IconBox(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

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

// 🔗 HELPER FUNCTIONS

// ✅ Updated Share Function (Ab Link bhi bhejega)
fun shareApp(context: Context) {
    val appPackageName = context.packageName
    val playStoreLink = "https://play.google.com/store/apps/details?id=$appPackageName"

    val shareMessage = """
        Hey! Check out SpeedFinder 🚀
        
        It's the best app to test internet speed & scan WiFi devices.
        
        Download here:
        $playStoreLink
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Download SpeedFinder")
        putExtra(Intent.EXTRA_TEXT, shareMessage)
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}

fun openWebPage(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}
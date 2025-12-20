package com.example.speedfinder.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 🌑 THEME 1: NetWiz (Dark)
private val NetWizScheme = darkColorScheme(
    primary = NetWizYellow,
    onPrimary = NetWizDarkBg,
    background = NetWizDarkBg,
    surface = NetWizCardBg,
    onBackground = TextWhite,
    onSurface = TextWhite
)

// 🩷 THEME 2: Pink & White (Light)
private val PinkWhiteScheme = lightColorScheme(
    primary = LovelyPink,       // 💖 Buttons Pink honge
    onPrimary = Color.White,    // Pink button par text White hoga
    background = WhiteBg,       // ⚪ Background White
    surface = LightPinkSurface, // 🌸 Cards Light Pinkish
    onBackground = TextBlack,   // ⚫ Text Black hoga
    onSurface = TextBlack
)

@Composable
fun SpeedFinderTheme(
    darkTheme: Boolean, // Switch State
    content: @Composable () -> Unit
) {
    // Logic: Switch ON = NetWiz, Switch OFF = Pink/White
    val colorScheme = if (darkTheme) NetWizScheme else PinkWhiteScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()

            // ⚠️ IMP: Agar Dark Mode hai to Icons Light (White) honge,
            // Agar Light Mode (Pink) hai to Icons Dark (Black) honge.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
package com.example.speedfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.WifiFind
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.speedfinder.data.ThemeManager
import com.example.speedfinder.presentation.SettingsScreen
import com.example.speedfinder.presentation.dashboard.DashboardScreen
import com.example.speedfinder.presentation.speedtest.SpeedTestScreen
import com.example.speedfinder.presentation.wifi.WifiScreen
import com.example.speedfinder.ui.theme.SpeedFinderTheme
import com.google.android.gms.ads.MobileAds // 👈 Import Added
import kotlinx.coroutines.CoroutineScope // 👈 Import Added
import kotlinx.coroutines.Dispatchers // 👈 Import Added
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🚀 FIX: Ads ko Background Thread par load kar rahe hain
        // Taake Splash Screen foran show ho aur App Hang na kare
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@MainActivity)
        }

        setContent {
            val context = LocalContext.current
            val themeManager = remember { ThemeManager(context) }
            val isDarkMode by themeManager.isDarkMode.collectAsState(initial = true)

            // Splash Screen State
            var showSplash by remember { mutableStateOf(true) }

            // 2 Seconds Timer (Visual Splash)
            LaunchedEffect(Unit) {
                delay(1000)
                showSplash = false
            }

            SpeedFinderTheme(darkTheme = isDarkMode) {
                if (showSplash) {
                    // ✨ 1. SPLASH SCREEN
                    SplashScreen()
                } else {
                    // 🏠 2. MAIN APP
                    MainAppStructure(
                        isDarkMode = isDarkMode,
                        onThemeToggle = { themeManager.toggleTheme() }
                    )
                }
            }
        }
    }
}

// ✨ Splash Screen
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .fillMaxWidth()
                .scale(1.3f),
            contentScale = ContentScale.Fit
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppStructure(isDarkMode: Boolean, onThemeToggle: () -> Unit) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // ✅ Route Checker (Back Arrow ke liye)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
            ) {
                // 🔥 SIDEBAR HEADER (Zoomed Logo)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(1.5f),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Navigation Items
                DrawerItem(Icons.Default.Home, "Home", currentRoute == "dashboard") {
                    navController.navigate("dashboard") { launchSingleTop = true }
                    scope.launch { drawerState.close() }
                }
                DrawerItem(Icons.Rounded.NetworkCheck, "Speed Test", currentRoute == "speedtest") {
                    navController.navigate("speedtest") { launchSingleTop = true }
                    scope.launch { drawerState.close() }
                }
                DrawerItem(Icons.Rounded.WifiFind, "WiFi Spy", currentRoute == "wifi") {
                    navController.navigate("wifi") { launchSingleTop = true }
                    scope.launch { drawerState.close() }
                }
                DrawerItem(Icons.Default.Settings, "Settings", currentRoute == "settings") {
                    navController.navigate("settings") { launchSingleTop = true }
                    scope.launch { drawerState.close() }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Theme Toggle
                Divider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.WbSunny, contentDescription = "Theme", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(if (isDarkMode) "Dark Mode" else "Light Mode", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Switch(checked = isDarkMode, onCheckedChange = { onThemeToggle() })
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("SpeedFinder", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        // ✅ BACK ARROW LOGIC
                        if (currentRoute == "dashboard") {
                            // Dashboard par MENU Button
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        } else {
                            // Doosre pages par BACK ARROW
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("dashboard") { DashboardScreen() }
                composable("wifi") { WifiScreen() }
                composable("speedtest") { SpeedTestScreen() }
                composable("settings") {
                    SettingsScreen(
                        isDarkMode = isDarkMode,
                        onThemeToggle = { onThemeToggle() }
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedContainerColor = Color.Transparent,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}
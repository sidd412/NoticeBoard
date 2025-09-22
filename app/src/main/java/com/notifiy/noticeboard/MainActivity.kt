package com.notifiy.noticeboard

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.notifiy.noticeboard.navigation.BottomNavScreen
import com.notifiy.noticeboard.navigation.NoticeBoardNavigation
import com.notifiy.noticeboard.navigation.Screen
import com.notifiy.noticeboard.services.UpdateService
import com.notifiy.noticeboard.ui.components.UpdateDialog
import com.notifiy.noticeboard.ui.theme.NoticeBoardTheme
import com.notifiy.noticeboard.ui.viewmodel.ThemeViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private var backPressTime: Long = 0
    private var isOnHomeScreen: Boolean = false
    private var isOnHomeTab: Boolean = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val context = LocalContext.current
            
            LaunchedEffect(Unit) {
                themeViewModel.initialize(context)
            }
            
            NoticeBoardTheme(themeMode = themeViewModel.themeMode) {
                // Track bottom navigation bar visibility
                var isBottomNavBarVisible by remember { mutableStateOf(true) }
                
                // Configure status bar colors based on theme and bottom nav bar visibility
                ConfigureStatusBar(themeViewModel = themeViewModel, isBottomNavBarVisible = isBottomNavBarVisible)
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NoticeBoardApp(
                        themeViewModel = themeViewModel,
                        onBottomNavBarVisibilityChanged = { isVisible ->
                            isBottomNavBarVisible = isVisible
                        },
                        onHomeScreenChanged = { isHome -> isOnHomeScreen = isHome },
                        onHomeTabChanged = { isHome -> isOnHomeTab = isHome }
                    )
                }
            }
        }
    }
    
    override fun onBackPressed() {
        println("backLogging: onBackPressed triggered!")
        println("backLogging: isOnHomeScreen: $isOnHomeScreen")
        println("backLogging: isOnHomeTab: $isOnHomeTab")
        
        // If not on main container, allow normal navigation
        if (!isOnHomeScreen) {
            println("backLogging: Not on main container, allowing normal navigation")
            super.onBackPressed()
            return
        }
        
        // If on main container but not on home tab, allow normal navigation
        if (!isOnHomeTab) {
            println("backLogging: Not on home tab, allowing normal navigation")
            super.onBackPressed()
            return
        }
        
        // Only handle double-back-press when on home tab
        println("backLogging: On home tab, checking for double-back-press")
        val currentTime = System.currentTimeMillis()
        println("backLogging: Current time: $currentTime, Last back press time: $backPressTime")
        
        if (currentTime - backPressTime > 3000) {
            // First back press - show toast
            println("backLogging: First back press - showing toast")
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()
            backPressTime = currentTime
        } else {
            // Second back press within 3 seconds - exit app
            println("backLogging: Second back press - exiting app")
            finishAffinity()
        }
    }
}

@Composable
fun ConfigureStatusBar(themeViewModel: ThemeViewModel, isBottomNavBarVisible: Boolean = true) {
    val view = LocalView.current
    val isSystemInDarkTheme = isSystemInDarkTheme()
    
    // Determine if we should use dark theme
    val isDarkTheme = when (themeViewModel.themeMode) {
        com.notifiy.noticeboard.data.preferences.PreferencesManager.THEME_DARK -> true
        com.notifiy.noticeboard.data.preferences.PreferencesManager.THEME_LIGHT -> false
        com.notifiy.noticeboard.data.preferences.PreferencesManager.THEME_SYSTEM -> isSystemInDarkTheme
        else -> isSystemInDarkTheme
    }
    
    // Get the appropriate color for navigation bar based on bottom nav bar visibility
    val navigationBarColor = if (isBottomNavBarVisible) {
        // Match app's bottom navigation bar color when visible
        MaterialTheme.colorScheme.surfaceContainer.toArgb()
    } else {
        // Match app's background color when bottom nav bar is hidden
        MaterialTheme.colorScheme.background.toArgb()
    }
    
    SideEffect {
        val window = (view.context as ComponentActivity).window
        window.statusBarColor = Color.Transparent.toArgb()
        
        // Set navigation bar color conditionally
        window.navigationBarColor = navigationBarColor
        
        // Configure status bar appearance based on theme
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !isDarkTheme
            isAppearanceLightNavigationBars = !isDarkTheme
        }
    }
}

@Composable
fun NoticeBoardApp(
    themeViewModel: ThemeViewModel, 
    onBottomNavBarVisibilityChanged: (Boolean) -> Unit,
    onHomeScreenChanged: (Boolean) -> Unit,
    onHomeTabChanged: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    // Update check state
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateConfig by remember { mutableStateOf<com.notifiy.noticeboard.data.model.UpdateConfig?>(null) }
    var isForceUpdate by remember { mutableStateOf(false) }
    var isSkipable by remember { mutableStateOf(false) }
    
    // Get current back stack entry to determine current screen
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    
    // Update isOnHomeScreen based on current route
    LaunchedEffect(currentRoute) {
        val isHomeScreen = currentRoute == Screen.MainContainer.route
        println("backLogging: Current route changed to: $currentRoute")
        println("backLogging: isOnHomeScreen set to: $isHomeScreen")
        onHomeScreenChanged(isHomeScreen)
    }
    
    // Check for updates when app starts
    LaunchedEffect(Unit) {
        try {
            println("DEBUG: Starting update check...")
            val updateService = UpdateService(context)
            val updateResult = updateService.checkForUpdate()
            
            println("DEBUG: Update check result: $updateResult")
            
            if (updateResult.needsUpdate && updateResult.updateConfig != null) {
                println("DEBUG: Update needed! Showing popup...")
                updateConfig = updateResult.updateConfig
                isForceUpdate = updateResult.isForceUpdate
                isSkipable = updateResult.isSkipable
                showUpdateDialog = true
            } else {
                println("DEBUG: No update needed or update config is null")
                println("DEBUG: needsUpdate: ${updateResult.needsUpdate}")
                println("DEBUG: updateConfig: ${updateResult.updateConfig}")
            }
        } catch (e: Exception) {
            println("DEBUG: Error checking for updates: ${e.message}")
            e.printStackTrace()
        }
    }
    
    NoticeBoardNavigation(
        navController = navController, 
        themeViewModel = themeViewModel,
        onBottomNavBarVisibilityChanged = onBottomNavBarVisibilityChanged,
        onHomeTabChanged = onHomeTabChanged
    )
    
    // Show update dialog if needed
    updateConfig?.let { config ->
        UpdateDialog(
            updateConfig = config,
            isForceUpdate = isForceUpdate,
            isSkipable = isSkipable,
            onDismiss = { 
                showUpdateDialog = false
                updateConfig = null
            },
            onUpdate = {
                showUpdateDialog = false
                updateConfig = null
            }
        )
    }
    
    // Debug: Add a test button to manually trigger update check
    // Remove this after debugging
    LaunchedEffect(Unit) {
        delay(2000) // Wait 2 seconds after app start
        println("DEBUG: Manual update check triggered for debugging...")
        
        // Clear update config cache to ensure fresh data
        try {
            val cacheManager = com.notifiy.noticeboard.data.cache.CacheManager(context)
            cacheManager.invalidateUpdateConfig()
            println("DEBUG: Cleared update config cache")
        } catch (e: Exception) {
            println("DEBUG: Error clearing cache: ${e.message}")
        }
        
        try {
            val updateService = UpdateService(context)
            val updateResult = updateService.checkForUpdate()
            println("DEBUG: Manual update check result: $updateResult")
        } catch (e: Exception) {
            println("DEBUG: Manual update check error: ${e.message}")
        }
    }
}
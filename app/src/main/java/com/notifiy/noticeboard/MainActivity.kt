package com.notifiy.noticeboard

import android.os.Bundle
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
import androidx.navigation.compose.rememberNavController
import com.notifiy.noticeboard.navigation.NoticeBoardNavigation
import com.notifiy.noticeboard.ui.theme.NoticeBoardTheme
import com.notifiy.noticeboard.ui.viewmodel.ThemeViewModel

class MainActivity : ComponentActivity() {
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
                        }
                    )
                }
            }
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
fun NoticeBoardApp(themeViewModel: ThemeViewModel, onBottomNavBarVisibilityChanged: (Boolean) -> Unit) {
    val navController = rememberNavController()
    NoticeBoardNavigation(
        navController = navController, 
        themeViewModel = themeViewModel,
        onBottomNavBarVisibilityChanged = onBottomNavBarVisibilityChanged
    )
}
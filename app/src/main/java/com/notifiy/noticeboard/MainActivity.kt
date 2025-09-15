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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NoticeBoardApp(themeViewModel = themeViewModel)
                }
            }
        }
    }
}

@Composable
fun NoticeBoardApp(themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()
    NoticeBoardNavigation(navController = navController, themeViewModel = themeViewModel)
}
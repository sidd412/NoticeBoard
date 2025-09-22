package com.notifiy.noticeboard.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.notifiy.noticeboard.R
import com.notifiy.noticeboard.navigation.Screen
import com.notifiy.noticeboard.ui.viewmodel.AuthViewModel
import com.notifiy.noticeboard.ui.viewmodel.cachedViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController, authViewModel: AuthViewModel = cachedViewModel(AuthViewModel::class.java)
) {
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState.data, authState.isLoading) {
        delay(1000) // Show splash for 2 seconds
        if (authState.data != null) {
            navController.navigate(Screen.MainContainer.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        } else {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.brand_logo_name),
            contentDescription = "NoticeBoard Logo",
            modifier = Modifier.size(270.dp)
        )
//        if (authState.isLoading) { // this is the correct way but to look better i commented it
        Spacer(modifier = Modifier.height(32.dp))
        androidx.compose.material3.CircularProgressIndicator(
            color = Color.White, modifier = Modifier.size(32.dp)
        )
//        }
    }
}

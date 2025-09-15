package com.notifiy.noticeboard.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.notifiy.noticeboard.ui.screens.*

@Composable
fun NoticeBoardNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }
        
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        
        composable(Screen.MainContainer.route) {
            MainContainerScreen(navController = navController)
        }
        
        composable(Screen.SubscribePopup.route) {
            SubscribePopupScreen(navController = navController)
        }
        
        composable(Screen.NoticeViewer.route) { backStackEntry ->
            val boardId = backStackEntry.arguments?.getString("boardId") ?: ""
            NoticeViewerScreen(
                navController = navController,
                boardId = boardId
            )
        }
        
        composable(Screen.CreateBoard.route) {
            CreateBoardScreen(navController = navController)
        }
        
        composable(Screen.UpdateBoard.route) { backStackEntry ->
            val boardId = backStackEntry.arguments?.getString("boardId") ?: ""
            UpdateBoardScreen(
                navController = navController,
                boardId = boardId
            )
        }
        
        composable(Screen.BoardEditor.route) { backStackEntry ->
            val boardId = backStackEntry.arguments?.getString("boardId") ?: ""
            BoardEditorScreen(
                navController = navController,
                boardId = boardId
            )
        }
        
        composable(Screen.BoardDetails.route) { backStackEntry ->
            val boardId = backStackEntry.arguments?.getString("boardId") ?: ""
            BoardDetailsScreen(
                navController = navController,
                boardId = boardId
            )
        }
        
        composable(Screen.Subscription.route) { backStackEntry ->
            val boardId = backStackEntry.arguments?.getString("boardId") ?: ""
            SubscriptionScreen(
                navController = navController,
                boardId = boardId
            )
        }
    }
}

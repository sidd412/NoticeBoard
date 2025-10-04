package com.notifiy.noticeboard.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.notifiy.noticeboard.ui.screens.*
import com.notifiy.noticeboard.ui.viewmodel.ThemeViewModel
import java.net.URLDecoder

@Composable
fun NoticeBoardNavigation(navController: NavHostController, themeViewModel: ThemeViewModel, onBottomNavBarVisibilityChanged: (Boolean) -> Unit = {}, onHomeTabChanged: (Boolean) -> Unit = {}) {
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
            MainContainerScreen(navController = navController, themeViewModel = themeViewModel, onBottomNavBarVisibilityChanged = onBottomNavBarVisibilityChanged, onHomeTabChanged = onHomeTabChanged)
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
        
        composable(Screen.EditNoticeBoard.route) { backStackEntry ->
            val boardId = backStackEntry.arguments?.getString("boardId") ?: ""
            EditNoticeBoardScreen(
                navController = navController,
                boardId = boardId
            )
        }
        
        composable(Screen.Subscription.route) {
            SubscriptionScreen(
                navController = navController
            )
        }
        
        composable(
            route = Screen.Success.route + "?planName={planName}&subscriptionPeriod={subscriptionPeriod}&expiryDate={expiryDate}&purchaseTime={purchaseTime}&orderId={orderId}",
            arguments = listOf()
        ) { backStackEntry ->
            SuccessScreen(
                navController = navController,
                planName = backStackEntry.arguments?.getString("planName") ?: "",
                subscriptionPeriod = backStackEntry.arguments?.getString("subscriptionPeriod") ?: "",
                expiryDate = backStackEntry.arguments?.getString("expiryDate")?.toLongOrNull() ?: 0L,
                purchaseTime = backStackEntry.arguments?.getString("purchaseTime")?.toLongOrNull() ?: 0L,
                orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            )
        }
        
        composable(Screen.Orders.route) {
            OrdersScreen(
                navController = navController
            )
        }
        
        composable(Screen.About.route) {
            AboutScreen(navController = navController)
        }
        
        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(navController = navController)
        }
        
        composable(Screen.PrivacySettings.route) {
            PrivacySettingsScreen(navController = navController)
        }
        
        composable(Screen.HelpSupport.route) {
            HelpSupportScreen(navController = navController)
        }
        
        composable(Screen.Search.route) {
            SearchScreen(navController = navController)
        }
        
        composable(Screen.Orders.route) {
            OrdersScreen(navController = navController)
        }

        composable(Screen.PurchaseDetail.route) { backStackEntry ->
            val purchaseId = backStackEntry.arguments?.getString("purchaseId") ?: ""
            
            PurchaseDetailScreenWrapper(
                navController = navController,
                purchaseId = purchaseId
            )
        }
        
        composable(Screen.MyQueries.route) {
            MyQueriesScreen(navController = navController)
        }
        
        composable(Screen.QueriesToMe.route) { backStackEntry ->
            val orgCode = backStackEntry.arguments?.getString("orgCode") ?: ""
            QueriesToMeScreen(
                navController = navController,
                orgCode = orgCode
            )
        }
    }
}

package com.notifiy.noticeboard.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object MainContainer : Screen("main_container")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object YourBoards : Screen("your_boards")
    object SubscribePopup : Screen("subscribe_popup")
    object NoticeViewer : Screen("notice_viewer/{boardId}") {
        fun createRoute(boardId: String) = "notice_viewer/$boardId"
    }
    object CreateBoard : Screen("create_board")
    object UpdateBoard : Screen("update_board/{boardId}") {
        fun createRoute(boardId: String) = "update_board/$boardId"
    }
    object BoardEditor : Screen("board_editor/{boardId}") {
        fun createRoute(boardId: String) = "board_editor/$boardId"
    }
    object BoardDetails : Screen("board_details/{boardId}") {
        fun createRoute(boardId: String) = "board_details/$boardId"
    }
    object EditNoticeBoard : Screen("edit_notice_board/{boardId}") {
        fun createRoute(boardId: String) = "edit_notice_board/$boardId"
    }
    object Subscription : Screen("subscription")
    object Success : Screen("success") {
        fun createRoute(
            planName: String,
            subscriptionPeriod: String,
            expiryDate: Long,
            purchaseTime: Long,
            orderId: String
        ) = "success?planName=$planName&subscriptionPeriod=$subscriptionPeriod&expiryDate=$expiryDate&purchaseTime=$purchaseTime&orderId=$orderId"
    }
    object About : Screen("about")
    object PrivacyPolicy : Screen("privacy_policy")
    object PrivacySettings : Screen("privacy_settings")
    object HelpSupport : Screen("help_support")
    object Search : Screen("search")
    object Orders : Screen("orders")
    object PurchaseDetail : Screen("purchase_detail/{purchaseId}") {
        fun createRoute(purchaseId: String) = "purchase_detail/$purchaseId"
    }
    object MyQueries : Screen("my_queries")
    object QueriesToMe : Screen("queries_to_me/{orgCode}") {
        fun createRoute(orgCode: String) = "queries_to_me/$orgCode"
    }
    object AllSubscribedBoards : Screen("all_subscribed_boards")
}

sealed class BottomNavScreen(val route: String, val title: String, val icon: String) {
    object Home : BottomNavScreen("home", "Home", "home")
    object Profile : BottomNavScreen("profile", "Profile", "person")
    object YourBoards : BottomNavScreen("your_boards", "My Boards", "settings")
}

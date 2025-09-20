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
    object Subscription : Screen("subscription/{boardId}") {
        fun createRoute(boardId: String) = "subscription/$boardId"
    }
    object About : Screen("about")
    object PrivacySettings : Screen("privacy_settings")
    object HelpSupport : Screen("help_support")
    object Search : Screen("search")
}

sealed class BottomNavScreen(val route: String, val title: String, val icon: String) {
    object Home : BottomNavScreen("home", "Home", "home")
    object Profile : BottomNavScreen("profile", "Profile", "person")
    object YourBoards : BottomNavScreen("your_boards", "My Boards", "settings")
}

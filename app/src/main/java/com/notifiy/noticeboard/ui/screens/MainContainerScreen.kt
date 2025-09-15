package com.notifiy.noticeboard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.notifiy.noticeboard.navigation.BottomNavScreen
import com.notifiy.noticeboard.navigation.Screen
import com.notifiy.noticeboard.ui.viewmodel.AuthViewModel
import com.notifiy.noticeboard.ui.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainerScreen(
    navController: NavController,
    themeViewModel: ThemeViewModel
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Create a shared AuthViewModel instance
    val authViewModel: AuthViewModel = viewModel()
    
    // Hide navigation bar on YourBoards screen
    val showBottomBar = currentDestination?.route != BottomNavScreen.YourBoards.route
    
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    listOf(
                        BottomNavScreen.Home,
                        BottomNavScreen.Profile,
                        BottomNavScreen.YourBoards
                    ).forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = when (screen) {
                                        BottomNavScreen.Home -> Icons.Default.Home
                                        BottomNavScreen.Profile -> Icons.Default.Person
                                        BottomNavScreen.YourBoards -> Icons.Default.Settings
                                    },
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                bottomNavController.navigate(screen.route) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    // on the back stack as users select items
                                    popUpTo(bottomNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination when
                                    // reselecting the same item
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = bottomNavController,
                startDestination = BottomNavScreen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(BottomNavScreen.Home.route) {
                    HomeScreenContent(navController = navController, bottomNavController = bottomNavController, authViewModel = authViewModel)
                }
                composable(BottomNavScreen.Profile.route) {
                    ProfileScreenContent(navController = navController, authViewModel = authViewModel, themeViewModel = themeViewModel)
                }
                composable(BottomNavScreen.YourBoards.route) {
                    YourBoardsScreenContent(navController = bottomNavController, mainNavController = navController, authViewModel = authViewModel)
                }
            }
        }
    }
}

@Composable
fun HomeScreenContent(navController: NavController, bottomNavController: NavController, authViewModel: AuthViewModel) {
    HomeScreen(navController = navController, bottomNavController = bottomNavController, authViewModel = authViewModel)
}

@Composable
fun ProfileScreenContent(navController: NavController, authViewModel: AuthViewModel, themeViewModel: ThemeViewModel) {
    ProfileScreen(navController = navController, authViewModel = authViewModel, themeViewModel = themeViewModel)
}

@Composable
fun YourBoardsScreenContent(navController: NavController, mainNavController: NavController, authViewModel: AuthViewModel) {
    YourBoardsScreen(navController = navController, mainNavController = mainNavController, authViewModel = authViewModel)
}

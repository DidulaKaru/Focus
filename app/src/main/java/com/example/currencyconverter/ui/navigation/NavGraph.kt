package com.example.currencyconverter.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.currencyconverter.ui.screens.AnalyticsScreen
import com.example.currencyconverter.ui.screens.AuthScreen
import com.example.currencyconverter.ui.screens.DashboardScreen
import com.example.currencyconverter.ui.screens.PomodoroTimerScreen
import com.example.currencyconverter.ui.screens.SettingsScreen
import com.example.currencyconverter.ui.theme.FocusFlowTheme
import com.example.currencyconverter.ui.viewmodel.AuthViewModel
import com.example.currencyconverter.ui.viewmodel.PomodoroViewModel
import com.example.currencyconverter.ui.viewmodel.TaskViewModel

object Screen {
    const val AUTH = "auth"
    const val DASHBOARD = "dashboard"
    const val ANALYTICS = "analytics"
    const val POMODORO = "pomodoro"
    const val SETTINGS = "settings"
}

@Composable
fun FocusFlowApp(
    authViewModel: AuthViewModel = viewModel(),
    taskViewModel: TaskViewModel = viewModel(),
    pomodoroViewModel: PomodoroViewModel = viewModel()
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()
    val themeMode by pomodoroViewModel.themeModeStateFlow.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(authState) {
        if (authState) {
            if (currentRoute == Screen.AUTH || currentRoute == null) {
                navController.navigate(Screen.DASHBOARD) {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else {
            navController.navigate(Screen.AUTH) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val showBottomBar = authState && currentRoute != Screen.AUTH

    FocusFlowTheme(themeMode = themeMode) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                            label = { Text("Dashboard") },
                            selected = currentRoute == Screen.DASHBOARD,
                            onClick = {
                                navController.navigate(Screen.DASHBOARD) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.DateRange, contentDescription = "Analytics") },
                            label = { Text("Analytics") },
                            selected = currentRoute == Screen.ANALYTICS,
                            onClick = {
                                navController.navigate(Screen.ANALYTICS) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Timer") },
                            label = { Text("Timer") },
                            selected = currentRoute?.startsWith(Screen.POMODORO) == true,
                            onClick = {
                                navController.navigate("${Screen.POMODORO}/none") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") },
                            selected = currentRoute == Screen.SETTINGS,
                            onClick = {
                                navController.navigate(Screen.SETTINGS) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = if (authState) Screen.DASHBOARD else Screen.AUTH,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(Screen.AUTH) {
                    AuthScreen(
                        authViewModel = authViewModel,
                        onAuthSuccess = {
                            navController.navigate(Screen.DASHBOARD) {
                                popUpTo(Screen.AUTH) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.DASHBOARD) {
                    DashboardScreen(
                        taskViewModel = taskViewModel,
                        authViewModel = authViewModel,
                        pomodoroViewModel = pomodoroViewModel,
                        onNavigateToPomodoro = { taskId ->
                            navController.navigate("${Screen.POMODORO}/$taskId")
                        },
                        onNavigateToSettings = {
                            navController.navigate(Screen.SETTINGS)
                        }
                    )
                }

                composable(Screen.ANALYTICS) {
                    AnalyticsScreen()
                }

                composable(
                    route = "${Screen.POMODORO}/{taskId}",
                    arguments = listOf(navArgument("taskId") {
                        type = NavType.StringType
                        defaultValue = "none"
                    })
                ) { backStackEntry ->
                    val taskIdArg = backStackEntry.arguments?.getString("taskId")
                    val taskId = if (taskIdArg == "none") null else taskIdArg
                    PomodoroTimerScreen(
                        pomodoroViewModel = pomodoroViewModel,
                        taskId = taskId,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.SETTINGS) {
                    SettingsScreen(
                        pomodoroViewModel = pomodoroViewModel,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}

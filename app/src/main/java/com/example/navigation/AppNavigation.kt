package com.example.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.screens.HomeScreen
import com.example.screens.ReportIssueScreen
import com.example.screens.LoginScreen
import com.example.screens.RegisterScreen
import com.example.screens.ForgotPasswordScreen
import com.example.screens.ProfileScreen
import com.example.screens.MyReportsScreen
import com.example.screens.MapScreen
import com.example.screens.ReportDetailsScreen
import com.example.screens.NotificationsScreen
import com.example.viewmodel.AuthState
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.IssueViewModel
import com.example.viewmodel.NotificationViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object Home : Screen("home")
    object Report : Screen("report")
    object Profile : Screen("profile")
    object MyReports : Screen("my_reports")
    object Map : Screen("map")
    object Notifications : Screen("notifications")
    object Settings : Screen("settings")
    object HelpSupport : Screen("help_support")
    object ReportDetails : Screen("details/{issueId}") {
        fun createRoute(issueId: String) = "details/$issueId"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel { AuthViewModel() }
) {
    val authState by authViewModel.authState.collectAsState()
    
    // Instantiate shared ViewModels
    val issueViewModel: IssueViewModel = viewModel { IssueViewModel() }
    val notificationViewModel: NotificationViewModel = viewModel { NotificationViewModel() }

    // Determine initial route
    val startDestination = if (authState is AuthState.Success) {
        Screen.Home.route
    } else {
        Screen.Login.route
    }

    val userId = if (authState is AuthState.Success) (authState as AuthState.Success).uid else ""
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            notificationViewModel.observeNotifications(userId)
        }
    }
    
    val notifications by notificationViewModel.notifications.collectAsState()
    val unreadCount = notifications.count { !it.isRead }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = startDestination) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                    viewModel = authViewModel
                )
            }
            
            composable(Screen.Register.route) {
                RegisterScreen(
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() },
                    viewModel = authViewModel
                )
            }

            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = authViewModel
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToReport = { navController.navigate(Screen.Report.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToDetails = { id -> navController.navigate(Screen.ReportDetails.createRoute(id)) },
                    onNavigateToMap = { navController.navigate(Screen.Map.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    unreadNotificationCount = unreadCount,
                    viewModel = issueViewModel
                )
            }
            
            composable(Screen.Report.route) {
                ReportIssueScreen(
                    onNavigateBack = { navController.popBackStack() },
                    issueViewModel = issueViewModel,
                    authViewModel = authViewModel
                )
            }

            composable(Screen.Map.route) {
                MapScreen(
                    onNavigateToDetails = { id -> navController.navigate(Screen.ReportDetails.createRoute(id)) },
                    onNavigateToHome = { navController.navigate(Screen.Home.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToReport = { navController.navigate(Screen.Report.route) },
                    viewModel = issueViewModel
                )
            }

            composable(
                route = Screen.ReportDetails.route,
                arguments = listOf(navArgument("issueId") { type = NavType.StringType })
            ) { backStackEntry ->
                val issueId = backStackEntry.arguments?.getString("issueId") ?: ""
                ReportDetailsScreen(
                    issueId = issueId,
                    onNavigateBack = { navController.popBackStack() },
                    issueViewModel = issueViewModel,
                    authViewModel = authViewModel
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToMyReports = {
                        navController.navigate(Screen.MyReports.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToHelp = {
                        navController.navigate(Screen.HelpSupport.route)
                    },
                    viewModel = authViewModel,
                    issueViewModel = issueViewModel
                )
            }
            
            composable(Screen.MyReports.route) {
                MyReportsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetails = { id -> navController.navigate(Screen.ReportDetails.createRoute(id)) },
                    issueViewModel = issueViewModel,
                    authViewModel = authViewModel
                )
            }

            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    authViewModel = authViewModel,
                    notificationViewModel = notificationViewModel
                )
            }

            composable(Screen.Settings.route) {
                com.example.screens.SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    authViewModel = authViewModel
                )
            }

            composable(Screen.HelpSupport.route) {
                com.example.screens.HelpSupportScreen(
                    onNavigateBack = { navController.popBackStack() },
                    authViewModel = authViewModel
                )
            }
        }

        // Overlay floating real-time notification banner
        var activeEvent by remember { mutableStateOf<com.example.services.RealtimeEvent?>(null) }
        
        LaunchedEffect(Unit) {
            com.example.services.RealtimeNotificationService.events.collect { event ->
                activeEvent = event
            }
        }
        
        LaunchedEffect(activeEvent) {
            if (activeEvent != null) {
                kotlinx.coroutines.delay(6000)
                activeEvent = null
            }
        }
        
        AnimatedVisibility(
            visible = activeEvent != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .zIndex(99f)
        ) {
            val event = activeEvent
            if (event != null) {
                val issue = when (event) {
                    is com.example.services.RealtimeEvent.Created -> event.issue
                    is com.example.services.RealtimeEvent.Updated -> event.issue
                    is com.example.services.RealtimeEvent.Resolved -> event.issue
                }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                        .clickable {
                            navController.navigate(Screen.ReportDetails.createRoute(issue.id))
                            activeEvent = null
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when (event) {
                            is com.example.services.RealtimeEvent.Created -> Icons.Default.AddAlert
                            is com.example.services.RealtimeEvent.Updated -> Icons.Default.Update
                            is com.example.services.RealtimeEvent.Resolved -> Icons.Default.CheckCircle
                        }
                        
                        val tint = when (event) {
                            is com.example.services.RealtimeEvent.Resolved -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                        
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(36.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            val titleText = when (event) {
                                is com.example.services.RealtimeEvent.Created -> "New Incident Reported!"
                                is com.example.services.RealtimeEvent.Updated -> "Incident Updated"
                                is com.example.services.RealtimeEvent.Resolved -> "Incident Resolved! 🎉"
                            }
                            
                            val subtext = when (event) {
                                is com.example.services.RealtimeEvent.Created -> "${issue.category}: ${issue.title}"
                                is com.example.services.RealtimeEvent.Updated -> "Status changed from ${event.oldStatus} to ${event.newStatus}"
                                is com.example.services.RealtimeEvent.Resolved -> "Fixed: ${issue.title}. Great work team!"
                            }
                            
                            Text(
                                text = titleText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtext,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        IconButton(onClick = { activeEvent = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close alert",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

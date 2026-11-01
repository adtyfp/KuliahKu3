package com.kuliahku.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kuliahku.ui.auth.WelcomeScreen
import com.kuliahku.ui.auth.LoginScreen
import com.kuliahku.ui.auth.RegisterScreen
import com.kuliahku.ui.home.HomeScreen
import com.kuliahku.ui.scan.ScanScreen
import com.kuliahku.ui.reference.ReferenceScreen
import com.kuliahku.ui.profile.ProfileScreen
import com.kuliahku.ui.profile.EditProfileScreen
import com.kuliahku.ui.tasks.TasksScreen
import com.kuliahku.ui.tasks.AddEditTaskScreen
import com.kuliahku.ui.tasks.SubmitTaskScreen
import com.kuliahku.ui.notes.NotesScreen
import com.kuliahku.ui.notes.NoteEditorScreen
import com.kuliahku.ui.calendar.CalendarScreen
import com.kuliahku.ui.calendar.AddEventScreen
import com.kuliahku.ui.finance.FinanceScreen
import com.kuliahku.ui.finance.AddTransactionScreen
import com.kuliahku.ui.finance.TransactionsAllScreen
import com.kuliahku.ui.finance.TransactionDetailScreen
import com.kuliahku.ui.finance.NotificationListenerIntroScreen
import com.kuliahku.ui.scan.RecordAudioScreen
import com.kuliahku.ui.components.FloatingBottomBar
import com.kuliahku.ui.components.bottomNavItems
import com.kuliahku.ui.preview.ComponentPreviewScreen

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String,
    initialDeepLink: String? = null
) {
    LaunchedEffect(initialDeepLink) {
        if (!initialDeepLink.isNullOrBlank()) {
            when {
                initialDeepLink.startsWith("kuliahku://task/") -> {
                    val id = initialDeepLink.removePrefix("kuliahku://task/")
                    navController.navigate(Routes.submitTask(id))
                }
                initialDeepLink.startsWith("kuliahku://event/") -> {
                    val id = initialDeepLink.removePrefix("kuliahku://event/")
                    val today = com.kuliahku.data.repository.EventRepository.keyOf(java.util.Calendar.getInstance())
                    navController.navigate("add_event?editId=$id&date=$today")
                }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                fadeIn(animationSpec = tween(500)) + slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(500, easing = EaseOutQuart)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(500))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(500))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(500)) + slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(500, easing = EaseOutQuart)
                )
            }
        ) {
            // Auth
            composable(Routes.WELCOME) {
                WelcomeScreen(onStart = { navController.navigate(Routes.LOGIN) { popUpTo(Routes.WELCOME) { inclusive = true } } })
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = { navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                    onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegisterSuccess = { navController.navigate(Routes.HOME) { popUpTo(Routes.REGISTER) { inclusive = true } } },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }

            // Main tabs
            composable(Routes.HOME) { HomeScreen(navController) }
            composable(Routes.SCAN) { ScanScreen(navController) }
            composable(Routes.RECORD_AUDIO) { RecordAudioScreen(navController) }
            composable(Routes.REFERENCE) { ReferenceScreen(navController) }
            composable(Routes.PROFILE) { ProfileScreen(navController) }
            composable(Routes.EDIT_PROFILE) { EditProfileScreen(navController) }

            // Detail screens (tanpa bottom bar) - placeholder
            composable(Routes.TASKS) { TasksScreen(navController) }

            composable(Routes.ADD_TASK) { AddEditTaskScreen(navController) }
            composable(
                route = Routes.SUBMIT_TASK,
                arguments = listOf(androidx.navigation.navArgument("taskId") { type = androidx.navigation.NavType.StringType })
            ) { SubmitTaskScreen(navController) }

            composable(Routes.NOTES) { NotesScreen(navController) }
            composable(
                route = "note_edit/{noteId}",
                arguments = listOf(androidx.navigation.navArgument("noteId") { type = androidx.navigation.NavType.StringType })
            ) { NoteEditorScreen(navController) }

            composable(Routes.CALENDAR) { CalendarScreen(navController) }
            composable(
                route = "add_event?editId={editId}&date={date}",
                arguments = listOf(
                    androidx.navigation.navArgument("editId") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = null },
                    androidx.navigation.navArgument("date") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { AddEventScreen(navController) }

            composable(Routes.FINANCE) { FinanceScreen(navController) }
            composable(
                route = "add_transaction?editId={editId}",
                arguments = listOf(androidx.navigation.navArgument("editId") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = null })
            ) { AddTransactionScreen(navController) }
            composable(
                route = Routes.TRANSACTION_DETAIL,
                arguments = listOf(androidx.navigation.navArgument("txId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val txId = backStackEntry.arguments?.getString("txId") ?: ""
                TransactionDetailScreen(navController, txId)
            }
            composable("transactions_all") { TransactionsAllScreen(navController) }
            composable("notification_listener_intro") { NotificationListenerIntroScreen(navController) }

            composable(Routes.REFERENCE) { ReferenceScreen(navController) }
            composable(
                route = Routes.MATERIAL_RESULT,
                arguments = listOf(androidx.navigation.navArgument("materialId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val materialId = backStackEntry.arguments?.getString("materialId") ?: ""
                com.kuliahku.ui.scan.MaterialResultScreen(navController)
            }
            composable(Routes.COMPONENT_PREVIEW) { ComponentPreviewScreen() }


        }

        AnimatedVisibility(
            visible = showBottomBar,
            enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 2 },
            exit = fadeOut(tween(500)) + slideOutVertically(tween(500)) { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            FloatingBottomBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
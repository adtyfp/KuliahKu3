
package com.kuliahku.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

// Introduction
import com.kuliahku.ui.splash.KuliahKuSplashScreen

// Authentication
import com.kuliahku.ui.auth.WelcomeScreen
import com.kuliahku.ui.auth.LoginScreen
import com.kuliahku.ui.auth.RegisterScreen

// Home
import com.kuliahku.ui.home.HomeScreen

// Scan
import com.kuliahku.ui.scan.ScanScreen
import com.kuliahku.ui.scan.RecordAudioScreen
import com.kuliahku.ui.scan.MaterialResultScreen

// Reference
import com.kuliahku.ui.reference.ReferenceScreen

// Profile
import com.kuliahku.ui.profile.ProfileScreen
import com.kuliahku.ui.profile.EditProfileScreen
import com.kuliahku.ui.profile.SettingsScreen
import com.kuliahku.ui.profile.AppearanceSettingsScreen
import com.kuliahku.ui.profile.LanguageSettingsScreen
import com.kuliahku.ui.profile.PrivacySecurityScreen

// Notification
import com.kuliahku.ui.notification.NotificationListScreen

// Tasks
import com.kuliahku.ui.tasks.TasksScreen
import com.kuliahku.ui.tasks.AddEditTaskScreen
import com.kuliahku.ui.tasks.SubmitTaskScreen

// Notes
import com.kuliahku.ui.notes.NotesScreen
import com.kuliahku.ui.notes.NoteEditorScreen

// Calendar
import com.kuliahku.ui.calendar.CalendarScreen
import com.kuliahku.ui.calendar.AddEventScreen

// Finance
import com.kuliahku.ui.finance.FinanceScreen
import com.kuliahku.ui.finance.AddTransactionScreen
import com.kuliahku.ui.finance.TransactionsAllScreen
import com.kuliahku.ui.finance.TransactionDetailScreen
import com.kuliahku.ui.finance.NotificationListenerIntroScreen

// Components
import com.kuliahku.ui.components.FloatingBottomBar
import com.kuliahku.ui.components.bottomNavItems


@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String,
    initialDeepLink: String? = null,
    onDeepLinkHandled: () -> Unit = {},

    // true = tampilkan splash
    // false = langsung ke halaman tujuan
    showSplash: Boolean = false

) {

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentRoute = navBackStackEntry?.destination?.route

    // Handle deep link
    var deepLinkHandled by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(initialDeepLink, currentRoute) {

        if (initialDeepLink == null) {
            deepLinkHandled = false
            return@LaunchedEffect
        }

        if (
            !initialDeepLink.isNullOrBlank() &&
            !deepLinkHandled &&
            currentRoute != Routes.SPLASH &&
            currentRoute != null
        ) {

            when {

                initialDeepLink.startsWith("kuliahku://task/") -> {

                    val id = initialDeepLink.removePrefix(
                        "kuliahku://task/"
                    )

                    navController.navigate(
                        Routes.submitTask(id)
                    )
                }

                initialDeepLink.startsWith("kuliahku://event/") -> {

                    val id = initialDeepLink.removePrefix(
                        "kuliahku://event/"
                    )

                    val today =
                        com.kuliahku.data.repository.EventRepository.keyOf(
                            java.util.Calendar.getInstance()
                        )

                    navController.navigate(
                        "add_event?editId=$id&date=$today"
                    )
                }

                initialDeepLink.startsWith("kuliahku://finance") -> {
                    navController.navigate(Routes.FINANCE)
                }

                initialDeepLink.startsWith("kuliahku://material/") -> {
                    val id = initialDeepLink.removePrefix("kuliahku://material/")
                    navController.navigate(Routes.materialResult(id))
                }
            }

            deepLinkHandled = true
            onDeepLinkHandled()
        }
    }

    // Cek apakah bottom navigation perlu ditampilkan
    val showBottomBar = currentRoute in bottomNavItems.map {
        it.route
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        NavHost(
            navController = navController,

            // PERBAIKAN UTAMA:
            // Jangan selalu memulai dari Routes.SPLASH.
            startDestination = if (showSplash) {
                Routes.SPLASH
            } else {
                startDestination
            },

            modifier = Modifier.fillMaxSize(),

            enterTransition = {
                fadeIn(
                    animationSpec = tween(500)
                ) + slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(
                        500,
                        easing = EaseOutQuart
                    )
                )
            },

            exitTransition = {
                fadeOut(
                    animationSpec = tween(500)
                )
            },

            popEnterTransition = {
                fadeIn(
                    animationSpec = tween(500)
                )
            },

            popExitTransition = {
                fadeOut(
                    animationSpec = tween(500)
                ) + slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(
                        500,
                        easing = EaseOutQuart
                    )
                )
            }

        ) {

            // =========================================
            // SPLASH SCREEN
            // =========================================

            composable(
                route = Routes.SPLASH,
                exitTransition = {
                    fadeOut(
                        animationSpec = tween(800)
                    )
                }
            ) {

                KuliahKuSplashScreen(

                    onFinished = {

                        navController.navigate(startDestination) {

                            popUpTo(Routes.SPLASH) {
                                inclusive = true
                            }

                            launchSingleTop = true
                        }
                    }
                )
            }


            // =========================================
            // AUTHENTICATION
            // =========================================

            composable(Routes.WELCOME) {

                WelcomeScreen(

                    onStart = {

                        navController.navigate(Routes.LOGIN) {

                            popUpTo(Routes.WELCOME) {
                                inclusive = true
                            }
                        }
                    }
                )
            }


            composable(Routes.LOGIN) {

                LoginScreen(

                    onLoginSuccess = {

                        navController.navigate(Routes.HOME) {

                            popUpTo(Routes.LOGIN) {
                                inclusive = true
                            }

                            launchSingleTop = true
                        }
                    },

                    onNavigateToRegister = {

                        navController.navigate(Routes.REGISTER)
                    }
                )
            }


            composable(Routes.REGISTER) {

                RegisterScreen(

                    onRegisterSuccess = {

                        navController.navigate(Routes.HOME) {

                            popUpTo(Routes.REGISTER) {
                                inclusive = true
                            }

                            launchSingleTop = true
                        }
                    },

                    onNavigateToLogin = {

                        navController.popBackStack()
                    }
                )
            }


            // =========================================
            // MAIN TABS
            // =========================================

            composable(Routes.HOME) {

                HomeScreen(navController)
            }


            composable(Routes.SCAN) {

                ScanScreen(navController)
            }


            composable(Routes.RECORD_AUDIO) {

                RecordAudioScreen(navController)
            }


            composable(Routes.REFERENCE) {

                ReferenceScreen(navController)
            }


            composable(Routes.PROFILE) {

                ProfileScreen(navController)
            }


            // =========================================
            // PROFILE AND SETTINGS
            // =========================================

            composable(Routes.EDIT_PROFILE) {

                EditProfileScreen(navController)
            }


            composable(Routes.SETTINGS) {

                SettingsScreen(navController)
            }


            composable(Routes.SETTINGS_APPEARANCE) {

                AppearanceSettingsScreen(navController)
            }


            composable(Routes.SETTINGS_LANGUAGE) {

                LanguageSettingsScreen(navController)
            }


            composable(Routes.SETTINGS_PRIVACY) {

                PrivacySecurityScreen(navController)
            }


            composable(Routes.NOTIFICATIONS_LIST) {

                NotificationListScreen(navController)
            }


            // =========================================
            // TASKS
            // =========================================

            composable(Routes.TASKS) {

                TasksScreen(navController)
            }


            // Tambah tugas
            composable(Routes.ADD_TASK) {

                AddEditTaskScreen(navController)
            }


            // Edit tugas
            composable(
                route = Routes.EDIT_TASK,
                arguments = listOf(
                    navArgument("taskId") {
                        type = NavType.StringType
                    }
                )
            ) {

                AddEditTaskScreen(navController)
            }


            // Submit tugas
            composable(
                route = Routes.SUBMIT_TASK,
                arguments = listOf(
                    navArgument("taskId") {
                        type = NavType.StringType
                    }
                )
            ) {

                SubmitTaskScreen(navController)
            }


            // =========================================
            // NOTES
            // =========================================

            composable(Routes.NOTES) {

                NotesScreen(navController)
            }


            composable(
                route = "note_edit/{noteId}",
                arguments = listOf(
                    navArgument("noteId") {
                        type = NavType.StringType
                    }
                )
            ) {

                NoteEditorScreen(navController)
            }


            // =========================================
            // CALENDAR
            // =========================================

            composable(Routes.CALENDAR) {

                CalendarScreen(navController)
            }


            composable(
                route = "add_event?editId={editId}&date={date}",
                arguments = listOf(

                    navArgument("editId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },

                    navArgument("date") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {

                AddEventScreen(navController)
            }


            // =========================================
            // FINANCE
            // =========================================

            composable(Routes.FINANCE) {

                FinanceScreen(navController)
            }


            composable(
                route = "add_transaction?editId={editId}",
                arguments = listOf(
                    navArgument("editId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {

                AddTransactionScreen(navController)
            }


            composable(
                route = Routes.TRANSACTION_DETAIL,
                arguments = listOf(
                    navArgument("txId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->

                val txId =
                    backStackEntry.arguments?.getString("txId") ?: ""

                TransactionDetailScreen(
                    navController,
                    txId
                )
            }


            composable("transactions_all") {

                TransactionsAllScreen(navController)
            }


            composable("notification_listener_intro") {

                NotificationListenerIntroScreen(navController)
            }


            // =========================================
            // MATERIAL RESULT
            // =========================================

            composable(
                route = Routes.MATERIAL_RESULT,
                arguments = listOf(
                    navArgument("materialId") {
                        type = NavType.StringType
                    }
                )
            ) {

                MaterialResultScreen(navController)
            }
        }


        // =============================================
        // FLOATING BOTTOM NAVIGATION
        // =============================================

        AnimatedVisibility(

            visible = showBottomBar,

            enter = fadeIn(
                tween(500)
            ) + slideInVertically(
                tween(500)
            ) {
                it / 2
            },

            exit = fadeOut(
                tween(500)
            ) + slideOutVertically(
                tween(500)
            ) {
                it / 2
            },

            modifier = Modifier.align(Alignment.BottomCenter)

        ) {

            FloatingBottomBar(

                currentRoute = currentRoute,

                onNavigate = { route ->

                    navController.navigate(route) {

                        popUpTo(Routes.HOME) {
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

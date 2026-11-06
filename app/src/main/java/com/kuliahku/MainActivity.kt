package com.kuliahku

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.kuliahku.data.datastore.AppPreferences
import com.kuliahku.data.repository.AuthRepository
import com.kuliahku.data.repository.EventRepository
import com.kuliahku.data.repository.TaskRepository
import com.kuliahku.notification.NotificationChannels
import com.kuliahku.notification.ReminderScheduler
import com.kuliahku.ui.components.KuliahkuDialog
import com.kuliahku.ui.navigation.AppNavHost
import com.kuliahku.ui.navigation.Routes
import com.kuliahku.ui.theme.KuliahkuTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var authRepo: AuthRepository
    @Inject lateinit var taskRepo: TaskRepository
    @Inject lateinit var eventRepo: EventRepository
    @Inject lateinit var scheduler: ReminderScheduler

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* hasil diabaikan, user bisa mengaktifkan nanti */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationChannels.ensureChannels(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        // Reschedule semua reminder untuk sesi saat ini
        CoroutineScope(Dispatchers.IO).launch {
            taskRepo.observeAllTasks().first().forEach { scheduler.scheduleTaskReminders(it) }
            eventRepo.observeAll().first().forEach { scheduler.scheduleEventReminder(it) }
        }

        val initialDeepLink: String? = intent?.data?.toString()

        setContent {
            val themeMode by appPreferences.themeMode.collectAsState(initial = "system")
            val themeColorHex by appPreferences.themeColor.collectAsState(initial = "#0080FF")
            val fontSize by appPreferences.fontSize.collectAsState(initial = "medium")
            val uiStyle by appPreferences.uiStyle.collectAsState(initial = "soft")
            val appLanguage by appPreferences.appLanguage.collectAsState(initial = "id")

            // Locale handling
            val context = androidx.compose.ui.platform.LocalContext.current
            LaunchedEffect(appLanguage) {
                val locale = java.util.Locale(appLanguage)
                java.util.Locale.setDefault(locale)
                val config = context.resources.configuration
                config.setLocale(locale)
                context.createConfigurationContext(config)
                // Catatan: Untuk penggantian bahasa yang sempurna biasanya butuh recreate activity, 
                // tapi ini dasar untuk compose agar komponen merespon.
            }

            KuliahkuTheme(
                themeMode = themeMode,
                primaryColorHex = themeColorHex,
                fontSizeMode = fontSize,
                uiStyle = uiStyle
            ) {
                var startDestination by remember { mutableStateOf<String?>(null) }
                var showNotifAccessDialog by remember { mutableStateOf(false) }
                
                LaunchedEffect(Unit) {
                    // Cek apakah listener notifikasi sudah aktif
                    val isNotifListenerEnabled = com.kuliahku.notification.listener.FinanceNotificationListener.isEnabled(context)
                    if (!isNotifListenerEnabled) {
                        showNotifAccessDialog = true
                    }

                    val hasSeenWelcome = appPreferences.hasSeenWelcome()
                    val isLoggedIn = appPreferences.isLoggedIn()
                    val firebaseUser = authRepo.currentUser

                    // Sinkronisasi status login antara DataStore dan Firebase
                    if (isLoggedIn && firebaseUser == null) {
                        appPreferences.setLoggedIn(false)
                    } else if (!isLoggedIn && firebaseUser != null) {
                        authRepo.logout()
                    }

                    val finalIsLoggedIn = appPreferences.isLoggedIn()
                    startDestination = when {
                        finalIsLoggedIn -> Routes.HOME
                        hasSeenWelcome -> Routes.LOGIN
                        else -> Routes.WELCOME
                    }
                }

                if (showNotifAccessDialog) {
                    KuliahkuDialog(
                        onDismissRequest = { showNotifAccessDialog = false },
                        title = "Aktifkan Catat Otomatis?",
                        text = "KuliahKu butuh izin 'Akses Notifikasi' untuk mencatat transaksi bank secara otomatis sehingga pengeluaran Anda terpantau lebih mudah. Aktifkan sekarang?",
                        icon = Icons.Outlined.NotificationsActive,
                        confirmText = "Buka Pengaturan",
                        onConfirm = {
                            showNotifAccessDialog = false
                            com.kuliahku.notification.listener.FinanceNotificationListener.openSettings(context)
                        },
                        dismissText = "Nanti Saja"
                    )
                }

                startDestination?.let {
                    AppNavHost(
                        navController = rememberNavController(),
                        startDestination = it,
                        initialDeepLink = initialDeepLink
                    )
                } ?: run {
                    // Tampilkan background splash dasar selagi menghitung startDestination
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isSystemInDarkTheme()) Color(0xFF020617) else Color.White)
                    )
                }
            }
        }
    }
}
package com.kuliahku.notification.listener

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kuliahku.R
import com.kuliahku.data.model.Transaction
import com.kuliahku.data.model.TxType
import com.kuliahku.data.repository.TransactionRepository
import com.kuliahku.data.repository.UserRepository
import com.kuliahku.notification.NotificationChannels
import com.kuliahku.notification.parser.NotificationParserRegistry
import com.kuliahku.util.FinanceStats
import com.kuliahku.util.MonthKey
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FinanceNotificationListener : NotificationListenerService() {

    @Inject lateinit var repo: TransactionRepository
    @Inject lateinit var userRepo: UserRepository
    @Inject lateinit var parserRegistry: NotificationParserRegistry
    @Inject lateinit var auth: com.google.firebase.auth.FirebaseAuth

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val sbnSafe = sbn ?: return
        val pkg = sbnSafe.packageName ?: return
        
        val extras = sbnSafe.notification?.extras ?: return
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        
        // Log detail untuk debugging
        android.util.Log.d("FinanceNotif", "Incoming from $pkg")
        android.util.Log.d("FinanceNotif", "Title: $title")
        android.util.Log.d("FinanceNotif", "Text: $text")
        if (bigText.isNotBlank()) android.util.Log.d("FinanceNotif", "BigText: $bigText")

        val combined = listOf(title, text, bigText).filter { it.isNotBlank() }.joinToString(" | ")

        if (!parserRegistry.isAllowed(pkg)) {
            // Jika mengandung kata kunci uang tapi package belum terdaftar
            if (combined.contains("Rp", true) || combined.contains("IDR", true)) {
                android.util.Log.w("FinanceNotif", "Potensi info keuangan dari $pkg tapi belum didukung.")
            }
            return
        }

        val parsed = parserRegistry.parse(pkg, combined)
        if (parsed == null) {
            android.util.Log.d("FinanceNotif", "Gagal parsing dari $pkg: $combined")
            // Tampilkan toast debug (hanya untuk package yang sudah terdaftar tapi gagal parse)
            scope.launch(Dispatchers.Main) {
                // Toast ini membantu user tahu bahwa notif terdeteksi tapi pola teksnya meleset
                // android.widget.Toast.makeText(this@FinanceNotificationListener, "Format notif $pkg tidak cocok", android.widget.Toast.LENGTH_SHORT).show()
            }
            return
        }
        
        val postTime = if (sbnSafe.postTime > 0) sbnSafe.postTime else System.currentTimeMillis()
        val hash = parsed.dedupHash(pkg, postTime)

        scope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    android.util.Log.e("FinanceNotif", "User tidak login")
                    return@launch
                }

                if (repo.existsByHash(hash)) {
                    android.util.Log.d("FinanceNotif", "Duplikat: $hash")
                    return@launch
                }

                val date = java.util.Date(postTime)
                val tx = Transaction(
                    type = parsed.type.value,
                    category = parsed.category,
                    amount = parsed.amount,
                    description = parsed.merchant ?: "Dari ${parserRegistry.displayName(pkg)}",
                    date = com.google.firebase.Timestamp(date),
                    monthKey = com.kuliahku.util.MonthKey.of(date),
                    yearKey = com.kuliahku.util.MonthKey.yearOf(date),
                    source = com.kuliahku.data.model.TxSource.NOTIFICATION.value,
                    dedupHash = hash
                )
                
                repo.addFromNotification(tx).onSuccess {
                    showSuccessNotification(parsed.amount, parsed.type.value, parsed.merchant ?: parserRegistry.displayName(pkg))
                    checkDailyLimit(parsed.type == com.kuliahku.data.model.TxType.EXPENSE)
                }.onFailure { e ->
                    android.util.Log.e("FinanceNotif", "Gagal simpan: ${e.message}")
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                         android.widget.Toast.makeText(
                            this@FinanceNotificationListener,
                            "Gagal catat: ${e.message}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FinanceNotif", "Error", e)
            }
        }
    }

    private var lastNotifiedKey = ""

    private fun checkDailyLimit(isExpense: Boolean) {
        if (!isExpense) return
        scope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val user = userRepo.getUser(uid) ?: return@launch
            val limit = user.settings.dailyLimit
            if (limit <= 0) return@launch

            val todayKey = MonthKey.of(java.util.Date())
            // Gunakan query yang lebih efisien daripada observeAll().first()
            val monthTransactions = repo.getTransactionsByMonth(todayKey)
            
            val summary = FinanceStats.monthlySummary(monthTransactions)
            val todayDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val key = "$todayDateStr:$limit"
            
            if (summary.todayExpense > limit) {
                if (lastNotifiedKey != key) {
                    lastNotifiedKey = key
                    showLimitNotification(summary.todayExpense, limit)
                }
            } else {
                if (lastNotifiedKey == key) {
                    lastNotifiedKey = ""
                }
            }
        }
    }

    private fun showLimitNotification(todayExpense: Long, limit: Long) {
        val title = "Batas Harian Terlampaui!"
        val body = "Pengeluaran hari ini Rp $todayExpense telah melebihi batas Rp $limit."

        val intent = android.content.Intent(this, com.kuliahku.MainActivity::class.java).apply {
            data = android.net.Uri.parse("kuliahku://finance")
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = android.app.PendingIntent.getActivity(
            this,
            1001,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NotificationChannels.FINANCE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Set high for lockscreen importance
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(1001, notification)
        } catch (_: SecurityException) { }
    }

    private fun showSuccessNotification(amount: Long, type: String, merchant: String) {
        val isIncome = type == com.kuliahku.data.model.TxType.INCOME.value
        val typeLabel = if (isIncome) "Pemasukan" else "Pengeluaran"
        val title = if (isIncome) "Pemasukan Dicatat!" else "Pengeluaran Dicatat!"
        val body = "Berhasil mencatat $typeLabel sebesar Rp $amount dari $merchant."

        val intent = android.content.Intent(this, com.kuliahku.MainActivity::class.java).apply {
            data = android.net.Uri.parse("kuliahku://finance")
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = android.app.PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NotificationChannels.FINANCE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        } catch (_: SecurityException) { }
    }

    companion object {
        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
            return flat.contains(context.packageName)
        }

        fun openSettings(context: Context) {
            val intent = android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}

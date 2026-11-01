package com.kuliahku.notification.listener

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.kuliahku.data.model.Transaction
import com.kuliahku.data.repository.TransactionRepository
import com.kuliahku.notification.parser.NotificationParserRegistry
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FinanceNotificationListener : NotificationListenerService() {

    @Inject lateinit var repo: TransactionRepository
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
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            this@FinanceNotificationListener,
                            "Tercatat: Rp ${parsed.amount} (${parsed.type.value})",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
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

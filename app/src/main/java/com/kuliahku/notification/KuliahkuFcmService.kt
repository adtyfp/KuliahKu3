package com.kuliahku.notification

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kuliahku.MainActivity
import com.kuliahku.R

class KuliahkuFcmService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: "KuliahKu"
            val body = remoteMessage.data["body"] ?: remoteMessage.notification?.body ?: ""
            val deepLink = remoteMessage.data["deep_link"]
            
            // Tentukan channel secara otomatis berdasarkan deep link jika tidak ada channel_id
            val channelId = remoteMessage.data["channel_id"] ?: when {
                deepLink?.contains("material") == true -> NotificationChannels.MATERIAL
                deepLink?.contains("finance") == true -> NotificationChannels.FINANCE
                deepLink?.contains("task") == true -> NotificationChannels.DEADLINE
                else -> NotificationChannels.AGENDA
            }

            showNotification(title, body, channelId, deepLink)
        } else {
            // Handle notification payload
            remoteMessage.notification?.let {
                showNotification(
                    it.title ?: "KuliahKu",
                    it.body ?: "",
                    NotificationChannels.AGENDA,
                    null
                )
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send token to server if needed
    }

    private fun showNotification(title: String, body: String, channelId: String, deepLink: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (deepLink != null) {
                data = android.net.Uri.parse(deepLink)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        } catch (e: SecurityException) {
            // Handle permission error
        }
    }
}

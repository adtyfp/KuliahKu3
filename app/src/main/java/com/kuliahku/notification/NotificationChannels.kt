
package com.kuliahku.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.content.getSystemService

object NotificationChannels {

    const val DEADLINE = "deadline_channel"
    const val AGENDA = "agenda_channel"
    const val FINANCE = "finance_channel"
    const val MATERIAL = "material_channel"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager =
            context.getSystemService<NotificationManager>() ?: return

        val defaultSound = RingtoneManager.getDefaultUri(
            RingtoneManager.TYPE_NOTIFICATION
        )

        val alarmSound = RingtoneManager.getDefaultUri(
            RingtoneManager.TYPE_ALARM
        )

        val notificationAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val alarmAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // Channel deadline tugas.
        val deadline = NotificationChannel(
            DEADLINE,
            "Deadline Tugas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Pengingat deadline tugas"
            enableVibration(true)
            setSound(defaultSound, notificationAttributes)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        // Channel agenda dan spanduk.
        val agenda = NotificationChannel(
            AGENDA,
            "Agenda & Acara",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Pengingat acara, pengingat, dan target"
            enableVibration(true)
            setSound(defaultSound, notificationAttributes)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        // Channel keuangan.
        val finance = NotificationChannel(
            FINANCE,
            "Keuangan",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifikasi transaksi keuangan otomatis"
            enableVibration(true)
            setSound(defaultSound, notificationAttributes)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        // Channel material.
        val material = NotificationChannel(
            MATERIAL,
            "Materi Baru",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifikasi materi baru yang diproses AI"
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        // Channel alarm khusus.
        val alarm = NotificationChannel(
            "$AGENDA.alarm",
            "Alarm Agenda",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alarm untuk acara penting"
            enableVibration(true)
            vibrationPattern = longArrayOf(
                0,
                500,
                200,
                500,
                200,
                500
            )
            setSound(alarmSound, alarmAttributes)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        manager.createNotificationChannels(
            listOf(
                deadline,
                agenda,
                finance,
                material,
                alarm
            )
        )
    }
}

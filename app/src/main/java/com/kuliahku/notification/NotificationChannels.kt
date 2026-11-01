package com.kuliahku.notification

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

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val deadline = NotificationChannel(
            DEADLINE, "Deadline Tugas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Pengingat deadline tugas"
            enableVibration(true)
            setSound(defaultSound, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
        }

        val agenda = NotificationChannel(
            AGENDA, "Agenda & Acara",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Pengingat acara, pengingat, dan target"
            enableVibration(true)
            setSound(defaultSound, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
        }

        val finance = NotificationChannel(
            FINANCE, "Keuangan",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifikasi transaksi keuangan otomatis"
            enableVibration(true)
            setSound(defaultSound, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
        }

        manager.createNotificationChannels(listOf(deadline, agenda, finance))

        // Channel khusus alarm (importance max + suara alarm)
        val alarm = NotificationChannel(
            "$AGENDA.alarm", "Alarm Agenda",
            NotificationManager.IMPORTANCE_MAX
        ).apply {
            description = "Alarm untuk acara penting"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
            setSound(alarmSound, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM).build())
            setBypassDnd(true)
        }
        manager.createNotificationChannel(alarm)
    }
}
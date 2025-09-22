package com.notifiy.noticeboard.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.notifiy.noticeboard.MainActivity
import com.notifiy.noticeboard.R

class LocalNotificationService(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "notice_board_channel"
        private const val CHANNEL_NAME = "Notice Board Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for new notices and updates"
        private const val NOTIFICATION_ID_BASE = 1000
    }

    init {
        createNotificationChannel()
    }

    fun showNotification(title: String, body: String, boardId: String?, boardName: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Add board ID to intent if available
            boardId?.let { putExtra("boardId", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(body))
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = NOTIFICATION_ID_BASE + (boardId?.hashCode() ?: System.currentTimeMillis().toInt())
        notificationManager.notify(notificationId, notification)
        
        println("DEBUG: LocalNotificationService - Notification shown: $title")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

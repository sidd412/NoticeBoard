package com.notifiy.noticeboard.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.notifiy.noticeboard.MainActivity
import com.notifiy.noticeboard.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "firebase_notifications"
        private const val CHANNEL_NAME = "Firebase Push Notifications"
        private const val CHANNEL_DESCRIPTION = "Push notifications from Firebase"
        private const val NOTIFICATION_ID_BASE = 2000
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        println("DEBUG: MyFirebaseMessagingService - Message received from: ${remoteMessage.from}")
        
        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            println("DEBUG: MyFirebaseMessagingService - Message data payload: ${remoteMessage.data}")
            
            val title = remoteMessage.data["title"] ?: "Notice Board"
            val body = remoteMessage.data["body"] ?: "You have a new notification"
            val boardId = remoteMessage.data["boardId"]
            val boardName = remoteMessage.data["boardName"] ?: "Notice Board"
            val notificationType = remoteMessage.data["type"] ?: "general"
            
            showNotification(title, body, boardId, boardName, notificationType)
        }

        // Check if message contains a notification payload
        remoteMessage.notification?.let { notification ->
            println("DEBUG: MyFirebaseMessagingService - Message notification payload: ${notification.title} - ${notification.body}")
            
            val title = notification.title ?: "Notice Board"
            val body = notification.body ?: "You have a new notification"
            val boardId = remoteMessage.data["boardId"]
            val boardName = remoteMessage.data["boardName"] ?: "Notice Board"
            val notificationType = remoteMessage.data["type"] ?: "general"
            
            showNotification(title, body, boardId, boardName, notificationType)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        println("DEBUG: MyFirebaseMessagingService - Refreshed token: $token")
        
        // Send token to your server or store it locally
        // You'll need to implement this to store the FCM token for each user
        sendRegistrationToServer(token)
    }

    private fun showNotification(
        title: String, 
        body: String, 
        boardId: String?, 
        boardName: String,
        notificationType: String
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Add board ID to intent if available
            boardId?.let { putExtra("boardId", it) }
            putExtra("notificationType", notificationType)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = NOTIFICATION_ID_BASE + (boardId?.hashCode() ?: System.currentTimeMillis().toInt())
        notificationManager.notify(notificationId, notification)
        
        println("DEBUG: MyFirebaseMessagingService - Firebase notification shown: $title")
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

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendRegistrationToServer(token: String) {
        // TODO: Implement this to send the FCM token to your server
        // This should be called when the token is refreshed
        println("DEBUG: MyFirebaseMessagingService - Sending token to server: $token")
        
        // For now, we'll store it in SharedPreferences and update it in the repository
        val sharedPref = getSharedPreferences("fcm_token", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("fcm_token", token)
            putLong("token_timestamp", System.currentTimeMillis())
            apply()
        }
    }
}




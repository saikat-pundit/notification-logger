package com.example.notiflogger

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationService : NotificationListenerService() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate() {
        super.onCreate()
        dbHelper = DatabaseHelper(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val packageName = sbn?.packageName ?: return
        val extras = sbn.notification?.extras ?: return

        // Grab the best available title
        val title = extras.getString(Notification.EXTRA_TITLE) 
            ?: extras.getString(Notification.EXTRA_TITLE_BIG) 
            ?: "No Title"

        // Dig deeper to find text from apps like WhatsApp, Telegram, or Shopping Apps
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
            ?: "No Text"

        // Ignore our own app, the Android system UI, or empty notifications
        if (packageName != "com.example.notiflogger" && 
            packageName != "com.android.systemui" && 
            text != "No Text") {
            
            dbHelper.insertLog(packageName, title, text)
        }
    }
}

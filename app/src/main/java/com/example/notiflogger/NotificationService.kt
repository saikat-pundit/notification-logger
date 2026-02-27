package com.example.notiflogger

import android.app.Notification
import android.content.Intent
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

        val title = extras.getString(Notification.EXTRA_TITLE) 
            ?: extras.getString(Notification.EXTRA_TITLE_BIG) 
            ?: "No Title"

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
            ?: "No Text"

        if (packageName != "com.example.notiflogger" && 
            packageName != "com.android.systemui" && 
            text != "No Text") {
            
            // We capture the result (true or false) of the database insertion
            val wasSaved = dbHelper.insertLog(packageName, title, text)
            
            // Only update UI and GitHub if it was a BRAND NEW notification (not a duplicate)
            if (wasSaved) {
                // 1. Update the App Screen
                val updateIntent = Intent("com.example.notiflogger.NEW_NOTIFICATION")
                sendBroadcast(updateIntent)

                // 2. NEW: Backup to GitHub Gist!
                val csvData = dbHelper.getAllLogsAsCSV()
                GistUploader.uploadToGist(csvData)
            }
        }
    }
}

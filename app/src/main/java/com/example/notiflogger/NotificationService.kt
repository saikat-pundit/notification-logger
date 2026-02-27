package com.example.notiflogger

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            
            val wasSaved = dbHelper.insertLog(packageName, title, text)
            
            if (wasSaved) {
                // 1. Update the App Screen
                val updateIntent = Intent("com.example.notiflogger.NEW_NOTIFICATION")
                sendBroadcast(updateIntent)

                // 2. Format the single row for GitHub
                val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                
                // Double-quote any existing quotes so they don't break the CSV columns
                val safeApp = packageName.replace("\"", "\"\"")
                val safeTitle = title.replace("\"", "\"\"")
                val safeText = text.replace("\"", "\"\"")
                
                val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault())
                val time = sdf.format(Date())

                // Create a single comma-separated row wrapped in quotes
                val newCsvRow = "\"$deviceName\",\"$safeApp\",\"$safeTitle\",\"$safeText\",\"$time\""
                
                // 3. Send just this one row to be appended to the cloud
                GistUploader.appendToGist(newCsvRow)
            }
        }
    }
}

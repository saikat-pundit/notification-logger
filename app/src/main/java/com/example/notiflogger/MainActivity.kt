package com.example.notiflogger

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Ask for permission if not granted
        if (!NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)) {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }

        // Display logs from the database
        val dbHelper = DatabaseHelper(this)
        val logTextView = findViewById<TextView>(R.id.logTextView)
        
        val logs = dbHelper.getAllLogs()
        if (logs.isNotEmpty()) {
            logTextView.text = logs
        }
    }
}

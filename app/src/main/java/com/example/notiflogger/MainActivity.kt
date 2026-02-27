package com.example.notiflogger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat

class MainActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var logTextView: TextView

    // NEW: The receiver that listens for the broadcast from the Service
    private val logUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshLogs() // Update the screen instantly when a broadcast is received!
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)
        logTextView = findViewById(R.id.logTextView)
        val btnRefresh = findViewById<Button>(R.id.btnRefresh)
        val btnClear = findViewById<Button>(R.id.btnClear)

        // Ask for permission if not granted
        if (!NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)) {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }

        // Load logs initially
        refreshLogs()

        btnRefresh.setOnClickListener {
            refreshLogs()
        }

        btnClear.setOnClickListener {
            val db = dbHelper.writableDatabase
            db.execSQL("DELETE FROM logs")
            db.close()
            refreshLogs()
        }

        // NEW: Register the receiver to start listening for the "NEW_NOTIFICATION" signal
        val filter = IntentFilter("com.example.notiflogger.NEW_NOTIFICATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(logUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(logUpdateReceiver, filter)
        }
    }

    // NEW: Stop listening when the app is closed to save memory
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(logUpdateReceiver)
    }

    private fun refreshLogs() {
        val logs = dbHelper.getAllLogs()
        if (logs.isNotEmpty()) {
            logTextView.text = logs
        } else {
            logTextView.text = "Waiting for notifications..."
        }
    }
}

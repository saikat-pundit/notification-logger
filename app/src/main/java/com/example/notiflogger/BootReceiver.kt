package com.example.notiflogger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // The phone just turned on. 
            // Waking up this receiver forces Android to launch our app's process in the background.
            // The Android system will now automatically re-bind the NotificationListenerService.
            Log.d("BootReceiver", "Device booted! Waking up NotifLogger...")
        }
    }
}

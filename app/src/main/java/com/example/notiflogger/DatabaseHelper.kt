package com.example.notiflogger

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Notice the version is now 2 to upgrade the database schema
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "Notifs.db", null, 2) {
    
    override fun onCreate(db: SQLiteDatabase) {
        // We added timestampMs to do exact millisecond math, and logTime for readable device time
        db.execSQL("CREATE TABLE logs (id INTEGER PRIMARY KEY AUTOINCREMENT, app TEXT, title TEXT, content TEXT, logTime TEXT, timestampMs INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS logs")
        onCreate(db)
    }

    fun insertLog(app: String, title: String, content: String) {
        val db = this.writableDatabase
        val currentTimeMs = System.currentTimeMillis() // Exact current time in milliseconds

        // 1. DUPLICATE CHECK: Look for this exact notification within the last 2000 milliseconds (2 seconds)
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM logs WHERE app = ? AND title = ? AND content = ? AND (? - timestampMs) <= 2000",
            arrayOf(app, title, content, currentTimeMs.toString())
        )
        
        var isDuplicate = false
        if (cursor.moveToFirst()) {
            if (cursor.getInt(0) > 0) {
                isDuplicate = true // We found a match within the last 2 seconds!
            }
        }
        cursor.close()

        // If it is a duplicate, stop right here and throw it away
        if (isDuplicate) {
            db.close()
            return
        }

        // 2. DEVICE TIME: Format the time using the device's local timezone (IST)
        val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault())
        val formattedLocalTime = sdf.format(Date(currentTimeMs))

        // 3. SAVE TO DATABASE
        val values = ContentValues().apply {
            put("app", app)
            put("title", title)
            put("content", content)
            put("logTime", formattedLocalTime)
            put("timestampMs", currentTimeMs)
        }
        db.insert("logs", null, values)
        db.close()
    }

    fun getAllLogs(): String {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM logs ORDER BY id DESC", null)
        var result = ""
        if (cursor.moveToFirst()) {
            do {
                val app = cursor.getString(cursor.getColumnIndexOrThrow("app"))
                val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                val content = cursor.getString(cursor.getColumnIndexOrThrow("content"))
                val time = cursor.getString(cursor.getColumnIndexOrThrow("logTime"))
                
                result += "[$time]\nApp: $app\nTitle: $title\nText: $content\n\n-----------------\n\n"
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return result
    }
}

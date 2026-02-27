package com.example.notiflogger

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "Notifs.db", null, 2) {
    
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE logs (id INTEGER PRIMARY KEY AUTOINCREMENT, app TEXT, title TEXT, content TEXT, logTime TEXT, timestampMs INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS logs")
        onCreate(db)
    }

    // CHANGED: Now returns a Boolean (true if saved, false if duplicate)
    fun insertLog(app: String, title: String, content: String): Boolean {
        val db = this.writableDatabase
        val currentTimeMs = System.currentTimeMillis()

        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM logs WHERE app = ? AND title = ? AND content = ? AND (? - timestampMs) <= 2000",
            arrayOf(app, title, content, currentTimeMs.toString())
        )
        
        var isDuplicate = false
        if (cursor.moveToFirst()) {
            if (cursor.getInt(0) > 0) {
                isDuplicate = true
            }
        }
        cursor.close()

        if (isDuplicate) {
            db.close()
            return false // Tell the service we did NOT save anything
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault())
        val formattedLocalTime = sdf.format(Date(currentTimeMs))

        val values = ContentValues().apply {
            put("app", app)
            put("title", title)
            put("content", content)
            put("logTime", formattedLocalTime)
            put("timestampMs", currentTimeMs)
        }
        db.insert("logs", null, values)
        db.close()
        return true // Tell the service we successfully saved a new log
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

    // NEW: Function to package everything into CSV format
    fun getAllLogsAsCSV(): String {
        val db = this.readableDatabase
        // Order by ASC so the oldest is at the top of the CSV, newest at the bottom
        val cursor = db.rawQuery("SELECT * FROM logs ORDER BY id ASC", null)
        val csvBuilder = StringBuilder()
        
        // CSV Header
        csvBuilder.append("ID,App,Title,Content,Time\n")
        
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                // We must double-quote any existing quotes in the text so it doesn't break the CSV columns
                val app = cursor.getString(cursor.getColumnIndexOrThrow("app"))?.replace("\"", "\"\"") ?: ""
                val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))?.replace("\"", "\"\"") ?: ""
                val content = cursor.getString(cursor.getColumnIndexOrThrow("content"))?.replace("\"", "\"\"") ?: ""
                val time = cursor.getString(cursor.getColumnIndexOrThrow("logTime")) ?: ""
                
                // Wrap strings in quotes to handle commas inside the notification text
                csvBuilder.append("$id,\"$app\",\"$title\",\"$content\",\"$time\"\n")
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return csvBuilder.toString()
    }
}

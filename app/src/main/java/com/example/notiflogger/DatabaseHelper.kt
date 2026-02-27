package com.example.notiflogger

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "Notifs.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE logs (id INTEGER PRIMARY KEY AUTOINCREMENT, app TEXT, title TEXT, content TEXT, time DATETIME DEFAULT CURRENT_TIMESTAMP)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS logs")
        onCreate(db)
    }

    fun insertLog(app: String, title: String, content: String) {
        val db = this.writableDatabase
        
        // NEW: Check the most recent log in the database to prevent duplicates
        val cursor = db.rawQuery("SELECT app, title, content FROM logs ORDER BY id DESC LIMIT 1", null)
        if (cursor.moveToFirst()) {
            val lastApp = cursor.getString(0)
            val lastTitle = cursor.getString(1)
            val lastContent = cursor.getString(2)
            
            // If the exact same notification was just logged, ignore this update
            if (app == lastApp && title == lastTitle && content == lastContent) {
                cursor.close()
                db.close()
                return
            }
        }
        cursor.close()

        // If it's not a duplicate, save it normally
        val values = ContentValues().apply {
            put("app", app)
            put("title", title)
            put("content", content)
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
                val time = cursor.getString(cursor.getColumnIndexOrThrow("time"))
                result += "[$time]\nApp: $app\nTitle: $title\nText: $content\n\n-----------------\n\n"
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return result
    }
}

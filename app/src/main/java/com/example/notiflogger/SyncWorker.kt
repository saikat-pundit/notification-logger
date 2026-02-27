package com.example.notiflogger

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SyncWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    
    companion object {
        private const val GITHUB_TOKEN = BuildConfig.GITHUB_TOKEN
        // REPLACE WITH YOUR ACTUAL GIST ID
        private const val GIST_ID = "b529558252be113e01993f24429e8556" 
    }

    override fun doWork(): Result {
        val dbHelper = DatabaseHelper(applicationContext)
        val unsyncedLogs = dbHelper.getUnsyncedLogs()

        // If nothing needs to be synced, stop and declare success
        if (unsyncedLogs.isEmpty()) {
            return Result.success()
        }

        try {
            // 1. GET CURRENT GIST DATA
            val getUrl = URL("https://api.github.com/gists/$GIST_ID")
            val getConn = getUrl.openConnection() as HttpURLConnection
            getConn.requestMethod = "GET"
            getConn.setRequestProperty("Authorization", "Bearer $GITHUB_TOKEN")
            getConn.setRequestProperty("Accept", "application/vnd.github.v3+json")

            var currentContent = ""
            if (getConn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(getConn.inputStream))
                val responseStr = reader.readText()
                reader.close()
                val jsonResponse = JSONObject(responseStr)
                val files = jsonResponse.getJSONObject("files")
                if (files.has("notifications.csv")) {
                    currentContent = files.getJSONObject("notifications.csv").getString("content")
                }
            }
            getConn.disconnect()

            if (currentContent.isEmpty()) {
                currentContent = "Device,App,Title,Content,Time\n"
            } else if (!currentContent.endsWith("\n")) {
                currentContent += "\n"
            }

            // 2. APPEND ALL UNSYNCED LOGS
            val syncedIds = mutableListOf<Int>()
            for (log in unsyncedLogs) {
                currentContent += log.second // Append the CSV row
                syncedIds.add(log.first)     // Track the ID to mark as synced later
            }

            // 3. UPLOAD TO GITHUB
            val patchUrl = URL("https://api.github.com/gists/$GIST_ID")
            val patchConn = patchUrl.openConnection() as HttpURLConnection
            patchConn.requestMethod = "PATCH"
            patchConn.setRequestProperty("Authorization", "Bearer $GITHUB_TOKEN")
            patchConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            patchConn.setRequestProperty("Content-Type", "application/json")
            patchConn.doOutput = true

            val escapedCsv = currentContent.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "").replace("\t", "\\t")
            val jsonPayload = "{ \"files\": { \"notifications.csv\": { \"content\": \"$escapedCsv\" } } }"

            val writer = OutputStreamWriter(patchConn.outputStream)
            writer.write(jsonPayload)
            writer.flush()
            writer.close()

            val responseCode = patchConn.responseCode
            patchConn.disconnect()

            // 4. IF SUCCESSFUL, MARK LOCAL DATABASE AS SYNCED
            if (responseCode == 200) {
                dbHelper.markAsSynced(syncedIds)
                return Result.success()
            } else {
                return Result.retry() // If GitHub rejects it, tell Android to try again later
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // If there is NO INTERNET, this exception triggers. 
            // Result.retry() tells Android to automatically run this again when internet returns!
            return Result.retry() 
        }
    }
}

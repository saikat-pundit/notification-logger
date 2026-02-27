package com.example.notiflogger

import android.util.Log
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class GistUploader {
    companion object {
        // REPLACE THESE WITH YOUR ACTUAL TOKEN AND ID
        private const val GITHUB_TOKEN = "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"
        private const val GIST_ID = "b529558252be113e01993f24429e8556"

        fun uploadToGist(csvData: String) {
            // We run this in a background thread so the app doesn't freeze
            thread {
                try {
                    val url = URL("https://api.github.com/gists/$GIST_ID")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "PATCH" // PATCH updates an existing Gist
                    connection.setRequestProperty("Authorization", "Bearer $GITHUB_TOKEN")
                    connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.doOutput = true

                    // We have to heavily escape the CSV text so it doesn't break the JSON structure
                    val escapedCsv = csvData.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "")
                        .replace("\t", "\\t")

                    // This is the format GitHub requires to update a file
                    val jsonPayload = """
                        {
                          "files": {
                            "notifications.csv": {
                              "content": "$escapedCsv"
                            }
                          }
                        }
                    """.trimIndent()

                    val writer = OutputStreamWriter(connection.outputStream)
                    writer.write(jsonPayload)
                    writer.flush()
                    writer.close()

                    val responseCode = connection.responseCode
                    Log.d("GistUploader", "GitHub API Response Code: $responseCode")

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

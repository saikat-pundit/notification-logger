package com.example.notiflogger

import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class GistUploader {
    companion object {
        private const val GITHUB_TOKEN = BuildConfig.GITHUB_TOKEN
        
        // IMPORTANT: Put your Gist ID back in here!
        private const val GIST_ID = "b529558252be113e01993f24429e8556"

        fun appendToGist(newCsvRow: String) {
            thread {
                try {
                    // 1. GET THE CURRENT CLOUD DATA
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

                        // Extract the text from the JSON response
                        val jsonResponse = JSONObject(responseStr)
                        val files = jsonResponse.getJSONObject("files")
                        if (files.has("notifications.csv")) {
                            currentContent = files.getJSONObject("notifications.csv").getString("content")
                        }
                    }
                    getConn.disconnect()

                    // If the Gist is empty, create the header first
                    if (currentContent.isEmpty()) {
                        currentContent = "Device,App,Title,Content,Time\n"
                    } else if (!currentContent.endsWith("\n")) {
                        currentContent += "\n" // Ensure we start on a new line
                    }

                    // 2. APPEND THE NEW NOTIFICATION
                    val updatedContent = currentContent + newCsvRow + "\n"

                    // 3. UPLOAD EVERYTHING BACK TO GITHUB
                    val patchUrl = URL("https://api.github.com/gists/$GIST_ID")
                    val patchConn = patchUrl.openConnection() as HttpURLConnection
                    patchConn.requestMethod = "PATCH"
                    patchConn.setRequestProperty("Authorization", "Bearer $GITHUB_TOKEN")
                    patchConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                    patchConn.setRequestProperty("Content-Type", "application/json")
                    patchConn.doOutput = true

                    // Escape the text so commas and quotes don't break the JSON format
                    val escapedCsv = updatedContent.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "")
                        .replace("\t", "\\t")

                    val jsonPayload = """
                        {
                          "files": {
                            "notifications.csv": {
                              "content": "$escapedCsv"
                            }
                          }
                        }
                    """.trimIndent()

                    val writer = OutputStreamWriter(patchConn.outputStream)
                    writer.write(jsonPayload)
                    writer.flush()
                    writer.close()

                    Log.d("GistUploader", "GitHub API Patch Response: ${patchConn.responseCode}")
                    patchConn.disconnect()

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

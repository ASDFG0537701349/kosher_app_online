package com.example.kosher_app_online

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

@Serializable
data class AppItem(
    val name: String,
    val packageName: String,
    val version: String,
    val versionCode: Int,
    val downloadUrl: String,
    @SerialName("iconUrl")
    val iconUrl: String,
    val category: String = "",
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0
)

class AppRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val TAG = "AppRepository"

    // Use the direct raw GitHub URL (without refs/heads)
    private val jsonUrl = "https://raw.githubusercontent.com/ASDFG0537701349/my-app-store/main/apps.json"
    
    suspend fun fetchApps(): List<AppItem> = withContext(Dispatchers.IO) {
        var connection: HttpsURLConnection? = null
        return@withContext try {
            Log.d(TAG, "Fetching apps from: $jsonUrl")
            val url = URL(jsonUrl)
            connection = url.openConnection() as HttpsURLConnection
            
            // Critical settings to prevent errors
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Connection", "close") // Fixes EOFException
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpsURLConnection.HTTP_OK) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                if (jsonString.isBlank()) {
                    Log.e(TAG, "Empty response body")
                    emptyList()
                } else {
                    Log.d(TAG, "Response body: $jsonString")
                    json.decodeFromString<List<AppItem>>(jsonString)
                }
            } else {
                Log.e(TAG, "Failed to fetch apps: ${connection.responseCode}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching apps", e)
            e.printStackTrace()
            emptyList()
        } finally {
            connection?.disconnect()
        }
    }
}
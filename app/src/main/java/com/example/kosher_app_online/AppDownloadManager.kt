package com.example.kosher_app_online

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast

class AppDownloadManager(private val context: Context) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun enqueueDownload(app: AppItem): Long {
        return try {
            // Validate download URL - check for null/blank and http(s) prefix
            if (app.downloadUrl.isBlank() || !app.downloadUrl.startsWith("http")) {
                android.util.Log.e("AppDownloadManager", "Invalid download URL: ${app.downloadUrl}")
                Toast.makeText(
                    context,
                    "שגיאה: כתובת ההורדה לא תקינה",
                    Toast.LENGTH_SHORT
                ).show()
                return -1
            }
            
            val uri = Uri.parse(app.downloadUrl)
            if (uri == Uri.EMPTY || uri.scheme.isNullOrBlank()) {
                android.util.Log.e("AppDownloadManager", "Invalid URI from URL: ${app.downloadUrl}")
                Toast.makeText(
                    context,
                    "שגיאה: כתובת ה-URL לא תקינה",
                    Toast.LENGTH_SHORT
                ).show()
                return -1
            }
            
            val request = DownloadManager.Request(uri)
                .setTitle("מוריד ${app.name}")
                .setDescription("גרסה ${app.version}")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(
                    context,
                    Environment.DIRECTORY_DOWNLOADS,
                    "${app.packageName}.apk"
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
                .addRequestHeader("User-Agent", "Mozilla/5.0")

            val downloadId = downloadManager.enqueue(request)
            
            Toast.makeText(
                context,
                "ההורדה התחילה: ${app.name}",
                Toast.LENGTH_SHORT
            ).show()
            
            downloadId
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "שגיאה בהפעלת ההורדה: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            -1
        }
    }

    fun getUriForDownloadedFile(downloadId: Long): Uri? {
        return try {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            
            if (cursor.moveToFirst()) {
                val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                if (uriIndex != -1) {
                    val uriString = cursor.getString(uriIndex)
                    Uri.parse(uriString)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
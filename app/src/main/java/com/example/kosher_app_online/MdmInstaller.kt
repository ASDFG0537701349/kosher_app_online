package com.example.kosher_app_online

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import java.io.IOException

class MdmInstaller(private val context: Context) {

    fun installPackage(apkUri: Uri): Boolean {
        return try {
            // Use standard Android installer via Intent
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

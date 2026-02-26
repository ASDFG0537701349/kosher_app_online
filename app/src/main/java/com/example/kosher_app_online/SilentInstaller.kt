package com.example.kosher_app_online

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File

class SilentInstaller(private val context: Context) {

    fun installApk(apkFile: File): Boolean {
        return try {
            // Check if device is rooted
            if (isRooted()) {
                // Use root installation
                installWithRoot(apkFile)
            } else {
                // Fallback to standard package installer
                installWithPackageInstaller(apkFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun isRooted(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            process.outputStream.use { it.write("exit\n".toByteArray()) }
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun installWithRoot(apkFile: File): Boolean {
        return try {
            val command = "pm install -r ${apkFile.absolutePath}"
            val process = Runtime.getRuntime().exec("su -c $command")
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun installWithPackageInstaller(apkFile: File): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.fromFile(apkFile),
                    "application/vnd.android.package-archive"
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
            true // Intent started successfully
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
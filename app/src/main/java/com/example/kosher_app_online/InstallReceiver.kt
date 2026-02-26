package com.example.kosher_app_online

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class InstallReceiver : BroadcastReceiver() {
    
    companion object {
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_STATUS = "status"
        const val EXTRA_PACKAGE_NAME = "package_name"
        
        // Status constants
        const val STATUS_SUCCESS = 1
        const val STATUS_FAILURE = 2
        const val STATUS_PENDING = 3
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getIntExtra(EXTRA_SESSION_ID, -1)
        val status = intent.getIntExtra(EXTRA_STATUS, -1)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        
        when (status) {
            STATUS_SUCCESS -> {
                Toast.makeText(
                    context,
                    "ההתקנה הושלמה בהצלחה: $packageName",
                    Toast.LENGTH_LONG
                ).show()
            }
            STATUS_FAILURE -> {
                val failureReason = when (val reason = intent.getIntExtra("android.content.pm.extra.STATUS", -1)) {
                    1 -> "התקנה נכשלה: כישלון כללי"
                    2 -> "התקנה נכשלה: כישלון באישור"
                    3 -> "התקנה נכשלה: התקנה קיימת"
                    4 -> "התקנה נכשלה: תכנית לא חוקית"
                    else -> "התקנה נכשלה (קוד: $reason)"
                }
                Toast.makeText(
                    context,
                    failureReason,
                    Toast.LENGTH_LONG
                ).show()
            }
            STATUS_PENDING -> {
                // Installation is in progress, no need to show toast
            }
            else -> {
                Toast.makeText(
                    context,
                    "סטטוס התקנה לא ידוע: $status",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
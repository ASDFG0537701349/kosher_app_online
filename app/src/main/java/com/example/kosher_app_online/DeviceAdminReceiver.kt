package com.example.kosher_app_online

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class DeviceAdminReceiver : DeviceAdminReceiver() {
    
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(
            context,
            "Device Admin enabled - Silent install is now available",
            Toast.LENGTH_LONG
        ).show()
    }
    
    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(
            context,
            "Device Admin disabled - Silent install will not work",
            Toast.LENGTH_LONG
        ).show()
    }
}
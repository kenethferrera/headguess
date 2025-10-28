package com.example.headguess.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

object PermissionManager {
    private const val TAG = "PermissionManager"
    
    fun checkNearbyDevicesPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 5–12 require Location permission for NSD/mDNS on many devices
            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            fine || coarse
        }
    }
    
    fun requestNearbyDevicesPermission(context: Context) {
        if (!checkNearbyDevicesPermission(context)) {
            Log.d(TAG, "Discovery permissions not granted, showing notification")
            // Prefer a notification prompt rather than immediately opening Settings
            NotificationHelper.showNearbyPermissionNotification(context)
        }
    }
    
    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Log.d(TAG, "Opened app settings for permission request")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app settings", e)
        }
    }
    
    fun isPermissionGranted(context: Context): Boolean {
        val hasPermission = checkNearbyDevicesPermission(context)
        Log.d(TAG, "Discovery permission status: $hasPermission")
        return hasPermission
    }
}

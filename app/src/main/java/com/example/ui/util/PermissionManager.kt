package com.example.ui.util

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class PermissionManager(private val activity: ComponentActivity) {

    private val sharedPrefs: SharedPreferences =
        activity.getSharedPreferences("app_permissions_prefs", Context.MODE_PRIVATE)

    private var storageCallback: ((Boolean) -> Unit)? = null
    private var notificationCallback: ((Boolean) -> Unit)? = null

    val storagePermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            sharedPrefs.edit().putBoolean("has_requested_storage", true).apply()
            storageCallback?.invoke(isGranted)
        }

    val notificationPermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            sharedPrefs.edit().putBoolean("has_requested_notification", true).apply()
            notificationCallback?.invoke(isGranted)
        }

    val cameraPermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            sharedPrefs.edit().putBoolean("has_requested_camera", true).apply()
            cameraCallback?.invoke(isGranted)
        }

    private var cameraCallback: ((Boolean) -> Unit)? = null

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun shouldShowCameraRationale(): Boolean {
        return !hasCameraPermission() && !sharedPrefs.getBoolean("has_requested_camera", false)
    }

    fun requestCameraPermission(onResult: (Boolean) -> Unit) {
        cameraCallback = onResult
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun shouldShowStorageRationale(): Boolean {
        return !hasStoragePermission() && !sharedPrefs.getBoolean("has_requested_storage", false)
    }

    fun shouldShowNotificationRationale(): Boolean {
        return !hasNotificationPermission() && !sharedPrefs.getBoolean("has_requested_notification", false)
    }

    fun requestStoragePermission(onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            storageCallback = onResult
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            sharedPrefs.edit().putBoolean("has_requested_storage", true).apply()
            onResult(true)
        }
    }

    fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationCallback = onResult
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            sharedPrefs.edit().putBoolean("has_requested_notification", true).apply()
            onResult(true)
        }
    }
}

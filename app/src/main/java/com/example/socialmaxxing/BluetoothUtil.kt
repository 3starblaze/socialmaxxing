package com.example.socialmaxxing

import androidx.core.app.ActivityCompat
import android.app.Activity
import android.Manifest

// NOTE: Bluetooth permission request id
val REQUEST_ENABLE_BLUETOOTH = 1

val requiredPermissions = arrayOf(
    Manifest.permission.BLUETOOTH_CONNECT,
    Manifest.permission.BLUETOOTH_SCAN,
    Manifest.permission.BLUETOOTH_ADVERTISE,
)

fun requestBluetoothPermissions(activity: Activity) {
    ActivityCompat.requestPermissions(activity, requiredPermissions, REQUEST_ENABLE_BLUETOOTH)
}
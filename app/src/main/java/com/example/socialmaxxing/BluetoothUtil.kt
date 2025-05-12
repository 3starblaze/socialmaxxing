package com.example.socialmaxxing

import androidx.core.app.ActivityCompat
import android.app.Activity
import android.Manifest

// NOTE: Bluetooth permission request id
val REQUEST_ENABLE_BLUETOOTH = 1


fun requestBluetoothPermissions(activity: Activity) {
    val permissions = arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
    )

    ActivityCompat.requestPermissions(activity, permissions, REQUEST_ENABLE_BLUETOOTH)
}
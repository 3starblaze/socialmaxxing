package com.example.socialmaxxing

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat

@SuppressLint("MissingPermission")
@Composable
fun MainUIComponent(activity: Activity) {
    val areAllPermissionsAccepted = remember { mutableStateOf(arePermissionsReady(activity)) }
    var singletons: Singletons? = if (areAllPermissionsAccepted.value)
        getSingletons(activity)
        else null

    Column {
        Text(
            text = if (areAllPermissionsAccepted.value)
                "All permissions are granted"
            else "Permissions needed to be granted"
        )

        Button(
            enabled = !areAllPermissionsAccepted.value,
            onClick = {
                requestBluetoothPermissions(activity)

                areAllPermissionsAccepted.value = arePermissionsReady(activity)
                if (areAllPermissionsAccepted.value) {
                    singletons = getSingletons(activity)
                    Toast.makeText(
                        activity,
                        "Everything ok with permissions!",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("tmp_tag", "bluetooth permissions are ok!")
                } else {
                    Log.d("tmp_tag", "couldn't retrieve singletons, handle this gracefully!")
                }
            },
        ) {
            Text(text = "Ask for bluetooth")
        }
    }
}

fun arePermissionsReady(activity: Activity): Boolean {
    return (ContextCompat.checkSelfPermission(
        activity,
        Manifest.permission.BLUETOOTH_CONNECT,
    ) == PackageManager.PERMISSION_GRANTED)
}
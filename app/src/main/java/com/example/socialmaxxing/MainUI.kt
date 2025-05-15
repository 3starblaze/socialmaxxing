package com.example.socialmaxxing

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
                    Log.d(TAG, "bluetooth permissions are ok!")
                } else {
                    Log.d(TAG, "couldn't retrieve singletons, handle this gracefully!")
                }
            },
        ) {
            Text(text = "Ask for bluetooth")
        }

        if (areAllPermissionsAccepted.value and (singletons != null)) {
            AdvertiseButton(singletons as Singletons)
        }

        if (areAllPermissionsAccepted.value) FindDevicesScreen(onConnect = {})
    }
}

@SuppressLint("MissingPermission")
@Composable
fun AdvertiseButton(singletons: Singletons) {
    // FIXME: Populate this payload properly
    val samplePayload = BLEAdvertisementPayload(
        deviceId = arrayOf(1, 1, 1, 1, 1, 1, 1, 1),
        timestamp = arrayOf(2, 2, 2, 2, 2, 2, 2, 2),
    )

    val isBLEAdvertisingOn = remember { mutableStateOf(false) }

    LaunchedEffect(isBLEAdvertisingOn.value) {
        if (isBLEAdvertisingOn.value) {
            startAdvertising(singletons.advertiser, samplePayload)
        } else {
            stopAdvertising(singletons.advertiser)
        }
    }

    Button(
        onClick = {
            isBLEAdvertisingOn.value = !isBLEAdvertisingOn.value
        }
    ) {
        Text(
            text = "${if (isBLEAdvertisingOn.value) "stop" else "start"} sample BLE advertisement"
        )
    }
}

fun arePermissionsReady(activity: Activity): Boolean {
    return requiredPermissions
        .map({ permission -> ContextCompat.checkSelfPermission(activity, permission)})
        .all({ result -> result == PackageManager.PERMISSION_GRANTED })
}
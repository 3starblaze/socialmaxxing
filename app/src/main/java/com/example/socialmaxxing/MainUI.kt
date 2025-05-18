package com.example.socialmaxxing

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.time.LocalTime

@Composable
fun Title(text: String) {
    Text(text, fontSize = 30.sp)
}

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("MissingPermission")
@Composable
fun MainUIComponent(activity: Activity) {
    val areAllPermissionsAccepted = remember { mutableStateOf(arePermissionsReady(activity)) }
    var singletons: Singletons? = if (areAllPermissionsAccepted.value)
        getSingletons(activity)
        else null

    val deviceId = 0xdeadbeef
    val time = LocalTime.now()
    val payload = makeBleAdvertisementPayload(time, deviceId)

    Column {
        PayloadInfo(payload)

        Box(Modifier.padding(0.dp, 8.dp))

        Title("Bluetooth Info")

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
            AdvertiseButton(singletons as Singletons, payload)
        }

        if (areAllPermissionsAccepted.value) FindDevicesScreen(onConnect = {})
    }
}

@OptIn(ExperimentalStdlibApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PayloadInfo(payload: BLEAdvertisementPayload) {
    Column {
        Title("Payload Info")
        Text("id: ${payload.deviceId.toByteArray().toHexString()}")
        Text("Time: ${payload.timestamp}")
    }
}

@SuppressLint("MissingPermission")
@Composable
fun AdvertiseButton(singletons: Singletons, payload: BLEAdvertisementPayload) {
    val isBLEAdvertisingOn = remember { mutableStateOf(false) }

    LaunchedEffect(isBLEAdvertisingOn.value) {
        if (isBLEAdvertisingOn.value) {
            startAdvertising(singletons.advertiser, payload)
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
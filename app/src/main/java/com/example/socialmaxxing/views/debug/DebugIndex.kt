package com.example.socialmaxxing.views.debug

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.socialmaxxing.BLEAdvertisementPayload
import com.example.socialmaxxing.CollectedMessagesView
import com.example.socialmaxxing.FindDevicesScreen
import com.example.socialmaxxing.Singletons
import com.example.socialmaxxing.TAG
import com.example.socialmaxxing.db.AppDatabase
import com.example.socialmaxxing.db.SingletonData
import com.example.socialmaxxing.getSingletons
import com.example.socialmaxxing.makeBleAdvertisementPayload
import com.example.socialmaxxing.requestBluetoothPermissions
import com.example.socialmaxxing.requiredPermissions
import com.example.socialmaxxing.startAdvertising
import com.example.socialmaxxing.stopAdvertising
import com.example.socialmaxxing.ui.theme.Typography
import kotlinx.coroutines.launch
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("MissingPermission")
@Composable
fun DebugIndexView(activity: Activity) {
    val areAllPermissionsAccepted = remember { mutableStateOf(arePermissionsReady(activity)) }
    val singletons = remember { mutableStateOf<Singletons?>(null) }

    // This can be ignored, I'm just swapping the ids out between devices -R
    val deviceId = 0xdeadbeef
//    val deviceId = 0xbeeeeeef
    val time = LocalTime.now()
    val payload = makeBleAdvertisementPayload(time, deviceId)

    val singletonsData = remember { mutableStateOf<SingletonData?>(null)}

    LaunchedEffect(areAllPermissionsAccepted.value) {
        launch() {
            singletons.value = getSingletons(activity)
        }
    }

    LaunchedEffect(singletons.value) {
        launch {
            val db = singletons.value?.database
            if (db !== null) {
                singletonsData.value = getSingletonData(db)
            }
        }
    }

    Column(
        Modifier
            .padding(8.dp, 8.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
        ,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (singletonsData.value !== null) {
            ThisDeviceMessageSection(singletonsData.value as SingletonData)
        }

        PayloadInfo(payload)

        if (singletons.value !== null) {
            LockHeight {
                CollectedMessagesView(singletons.value!!)
            }

            CollectedMessageUtilView(singletons.value!!)
        }

        BluetoothInfo(areAllPermissionsAccepted.value)

        BluetoothButtons(activity, singletons.value, areAllPermissionsAccepted, payload)

        if (areAllPermissionsAccepted.value) {
            LockHeight {
                FindDevicesScreen(onConnect = {}, payload, singletons.value, singletonsData.value)
            }
        }
    }
}

// NOTE: This is needed to prevent the app from crashing because of nested scrollables
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun LockHeight(content: @Composable() () -> Unit) {
    val configuration = LocalConfiguration.current

    // HACK: Hardcoded 0.5 of the screen. The better choice would be something like 90% of the
    // available space but at least this works.
    BoxWithConstraints {
        Box(modifier = Modifier.heightIn(0.dp, configuration.screenHeightDp.dp * 0.5f)) {
            content()
        }
    }
}

@Composable
fun BluetoothInfo(areAllPermissionsAccepted: Boolean) {
    Column {
        Text(
            style = Typography.titleLarge,
            text = "Bluetooth info"
        )

        Text(
            text = if (areAllPermissionsAccepted)
                "All permissions are granted"
            else "Permissions needed to be granted"
        )
    }
}

@Composable
fun BluetoothButtons(
    activity: Activity,
    singletons: Singletons?,
    areAllPermissionsAccepted: MutableState<Boolean>,
    payload: BLEAdvertisementPayload,
) {
    Column {
        Text(
            style = Typography.titleLarge,
            text = "Bluetooth buttons"
        )

        Button(
            enabled = !areAllPermissionsAccepted.value,
            onClick = {
                requestBluetoothPermissions(activity)

                areAllPermissionsAccepted.value = arePermissionsReady(activity)
                if (areAllPermissionsAccepted.value) {
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

        if (singletons !== null) {
            AdvertiseButton(singletons, payload)
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PayloadInfo(payload: BLEAdvertisementPayload) {
    Column {
        Text(
            style = Typography.titleLarge,
            text = "Payload Info",
        )
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

suspend fun getSingletonData(database: AppDatabase): SingletonData {
    return database.singletonDataDao().getData()
}

@Composable
fun ThisDeviceMessageSection(singletonData: SingletonData) {
    val msg = singletonData.currentMessage

    Column() {
        Text(
            style = Typography.titleLarge,
            text = "Device message info",
        )
        Text(buildAnnotatedString {
            append("msg: ")
            if (msg.isEmpty()) {
                withStyle(style = SpanStyle(color = Color.Gray)) {
                    append("<empty>")
                }
            } else {
                append(msg)
            }
        })
        Text(text = "id: ${singletonData.deviceId}")
        Text(text = "updatedAt: ${singletonData.updatedAt}")
    }
}

fun arePermissionsReady(activity: Activity): Boolean {
    return requiredPermissions
        .map({ permission -> ContextCompat.checkSelfPermission(activity, permission)})
        .all({ result -> result == PackageManager.PERMISSION_GRANTED })
}
package com.example.socialmaxxing

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import android.util.SparseArray
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay

/**
 * The vendor id that is used to store our data.
 *
 * This ID shouldn't be assigned to any company (as defined in Bluetooth assigned numbers)
 */
const val FALLBACK_VENDOR_ID = 0xffff

val defaultScanFilter: ScanFilter = ScanFilter.Builder()
    .setManufacturerData(FALLBACK_VENDOR_ID, byteArrayOf(), byteArrayOf())
    .build()

val vendorId = byteArrayOf(
    0xe5.toByte(),
    0x7d.toByte(),
    0x56.toByte(),
    0xbe.toByte(),
    0x1f.toByte(),
    0xd5.toByte(),
    0x0d.toByte(),
    0x3b.toByte(),
)

data class BLEAdvertisementPayload (
    val deviceId: List<Byte>,
    val timestamp: List<Byte>,
)

const val VENDOR_ID_SIZE = 8
const val DEVICE_ID_SIZE = 8
const val TIMESTAMP_SIZE = 8
const val BYTE_PAYLOAD_SIZE = VENDOR_ID_SIZE + DEVICE_ID_SIZE + TIMESTAMP_SIZE

fun decodeBleAdvertisement(bytes: ByteArray): BLEAdvertisementPayload? {
    if (bytes.size !== BYTE_PAYLOAD_SIZE) return null

    // NOTE: Check that vendorId matches
    for (i in vendorId.indices) {
        if (bytes[i] != vendorId[i]) return null;
    }

    val deviceIdIndex = VENDOR_ID_SIZE
    val timestampIndex = deviceIdIndex + DEVICE_ID_SIZE

    return BLEAdvertisementPayload(
        deviceId = bytes.drop(deviceIdIndex).take(DEVICE_ID_SIZE),
        timestamp = bytes.drop(timestampIndex).take(TIMESTAMP_SIZE),
    )
}

fun bleAdvertisementPayloadToBytes(payload: BLEAdvertisementPayload): ByteArray {
    return vendorId + payload.deviceId + payload.timestamp
}

val thisAdvertiseCallback = object : AdvertiseCallback() {
    override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
        Log.d(TAG, "Successfully started advertising")
    }

    override fun onStartFailure(errorCode: Int) {
        val msg = when (errorCode) {
            ADVERTISE_FAILED_DATA_TOO_LARGE -> "advertisement too large"
            ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "too many advertisers"
            ADVERTISE_FAILED_ALREADY_STARTED -> "already started"
            ADVERTISE_FAILED_INTERNAL_ERROR -> "internal error"
            ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "feature unsupported"
            else -> "unknown error"
        }
        Log.d(TAG, "failed to start advertising, error: $msg")
    }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
fun startAdvertising(advertiser: BluetoothLeAdvertiser, payload: BLEAdvertisementPayload) {
    val settings = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        .setConnectable(true)
        .setTimeout(0)
        .build()

    val advertisementBytes = bleAdvertisementPayloadToBytes(payload)

    val data = AdvertiseData.Builder()
        // NOTE: This data bleeds into our 31 byte budget, we don't need this
        .setIncludeDeviceName(false)
        .addManufacturerData(FALLBACK_VENDOR_ID, advertisementBytes)
        .build()

    advertiser.startAdvertising(settings, data, thisAdvertiseCallback)
}

@RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
fun stopAdvertising(advertiser: BluetoothLeAdvertiser) {
    advertiser.stopAdvertising(thisAdvertiseCallback)
}

@RequiresPermission(allOf = [
    Manifest.permission.BLUETOOTH_SCAN,
    Manifest.permission.BLUETOOTH_CONNECT,
])
@Composable
internal fun FindDevicesScreen(onConnect: (BluetoothDevice) -> Unit) {
    var scanning by remember {
        mutableStateOf(true)
    }

    data class DisplayItem(
        val device: BluetoothDevice,
        val manufacturerData: SparseArray<ByteArray>?,
        val appPayload: BLEAdvertisementPayload?,
    )

    val itemsToDisplay = remember {
        mutableStateMapOf<BluetoothDevice, DisplayItem>()
    }

    val scanSettings: ScanSettings = ScanSettings.Builder()
        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .build()

    // This effect will start scanning for devices when the screen is visible
    // If scanning is stop removing the effect will stop the scanning.
    if (scanning) {
        BluetoothScanEffect(
            scanSettings = scanSettings,
            onScanFailed = {
                scanning = false
                Log.w("FindBLEDevicesSample", "Scan failed with error: $it")
            },
            onDeviceFound = { scanResult ->
                Log.d(TAG, "found device")
                val device = scanResult.device

                if (!itemsToDisplay.contains(device)) {
                    val appPayloadBytes: ByteArray? = scanResult
                        .scanRecord
                        ?.manufacturerSpecificData
                        ?.get(FALLBACK_VENDOR_ID)

                    val appPayload = if (appPayloadBytes != null)
                        decodeBleAdvertisement(appPayloadBytes)
                        else null

                    itemsToDisplay.put(device, DisplayItem(
                        device = scanResult.device,
                        manufacturerData = scanResult.scanRecord?.manufacturerSpecificData,
                        appPayload = appPayload
                    ))
                }
            },
        )
        // Stop scanning after a while
        LaunchedEffect(true) {
            delay(15000)
            scanning = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Available devices", style = MaterialTheme.typography.titleSmall)
            if (scanning) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(
                    onClick = {
                        itemsToDisplay.clear()
                        scanning = true
                    },
                ) {
                    Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (itemsToDisplay.isEmpty()) {
                item {
                    Text(text = "No devices found")
                }
            }

            // FIXME: manufacturer data content not really visible
            items(itemsToDisplay.values.toList()) { item ->
                Column() {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth(),

                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            item.device.name ?: "N/A"
                        )
                        Text(item.device.address)
                    }
                    Text(
                        "${item.manufacturerData ?: "no manufacturer data"}",
                        style = TextStyle(fontSize = 12.sp)
                    )
                    if (item.appPayload != null) {
                        Text("deviceId: ${item.appPayload.deviceId}")
                        Text("timestamp: ${item.appPayload.timestamp}")
                    } else {
                        Text("Payload could not be decoded")
                    }
                }
            }
        }
    }

}

@RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
@Composable
private fun BluetoothScanEffect(
    scanSettings: ScanSettings,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onScanFailed: (Int) -> Unit,
    onDeviceFound: (device: ScanResult) -> Unit,
) {
    val context = LocalContext.current
    val adapter = context.getSystemService(BluetoothManager::class.java).adapter

    if (adapter == null) {
        onScanFailed(ScanCallback.SCAN_FAILED_INTERNAL_ERROR)
        return
    }

    val currentOnDeviceFound by rememberUpdatedState(onDeviceFound)

    DisposableEffect(lifecycleOwner, scanSettings) {
        val leScanCallback: ScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                currentOnDeviceFound(result)
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                onScanFailed(errorCode)
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            // Start scanning once the app is in foreground and stop when in background
            if (event == Lifecycle.Event.ON_START) {
                adapter.bluetoothLeScanner.startScan(
                    listOf(defaultScanFilter),
                    scanSettings,
                    leScanCallback
                )
            } else if (event == Lifecycle.Event.ON_STOP) {
                adapter.bluetoothLeScanner.stopScan(leScanCallback)
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer and stop scanning
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adapter.bluetoothLeScanner.stopScan(leScanCallback)
        }
    }
}

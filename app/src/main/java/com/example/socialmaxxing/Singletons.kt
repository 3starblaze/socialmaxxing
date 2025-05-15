package com.example.socialmaxxing

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.content.Intent
import androidx.annotation.RequiresPermission

val TAG = "Socialmaxxing"

data class Singletons (
    val activity: Activity,
    val bluetoothManager: BluetoothManager,
    val bluetoothAdapter: BluetoothAdapter,
    val bluetoothLeScanner: BluetoothLeScanner,
    val advertiser: BluetoothLeAdvertiser,
)

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun getSingletons(activity: Activity): Singletons {
    val context = activity.applicationContext
    val bluetoothManager: BluetoothManager = context.getSystemService(BluetoothManager::class.java)
    val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter

    if (bluetoothAdapter.isEnabled == false) {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

        activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
    }

    val advertiser = bluetoothAdapter.bluetoothLeAdvertiser

    return Singletons(
        activity = activity,
        bluetoothManager = bluetoothManager,
        bluetoothAdapter = bluetoothAdapter,
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner,
        advertiser = advertiser,
    )
}
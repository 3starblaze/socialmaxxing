package com.example.socialmaxxing

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.content.Intent
import androidx.annotation.RequiresPermission
import com.example.socialmaxxing.db.AppDatabase
import com.example.socialmaxxing.db.getDb
import java.util.UUID.nameUUIDFromBytes

val TAG = "Socialmaxxing"

data class Singletons (
    val activity: Activity,
    val bluetoothManager: BluetoothManager,
    val bluetoothAdapter: BluetoothAdapter,
    val bluetoothLeScanner: BluetoothLeScanner,
    val advertiser: BluetoothLeAdvertiser,
    val database: AppDatabase,
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

    val database = getDb(context)

    return Singletons(
        activity = activity,
        bluetoothManager = bluetoothManager,
        bluetoothAdapter = bluetoothAdapter,
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner,
        advertiser = advertiser,
        database = database,
    )
}
package com.example.socialmaxxing

import androidx.core.app.ActivityCompat
import android.app.Activity
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.socialmaxxing.db.CollectedMessage
import com.example.socialmaxxing.db.SingletonData
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import java.util.UUID.nameUUIDFromBytes

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

/**
 * Function for handling message exchange between two devices
*/
@RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
fun handleConnection(
    singletons: Singletons?,
    singletonData: SingletonData?,
    isSender: Boolean,
    uuid: UUID,
    otherDeviceData: DisplayItem
) {
    Log.d(TAG, "We at the start of handleConnection")
    var socket : BluetoothSocket? = null
    try {
        Log.d(TAG, "Current message: ${singletonData!!.currentMessage}")

        if (singletons!!.bluetoothAdapter.isDiscovering) {
            Log.d(TAG, "Stopping Bluetooth discovery...")
            singletons.bluetoothAdapter.cancelDiscovery()
            Thread.sleep(1000) // Give it time to stop
        }

        socket = if (isSender) {
            Thread.sleep(3000)
            val deviceAddress = otherDeviceData.device.getAddress()
            val classicDevice: BluetoothDevice = singletons.bluetoothAdapter.getRemoteDevice(deviceAddress)

            Log.d(TAG, "=== Connection Debug Info ===")
            Log.d(TAG, "Target device address: ${classicDevice.address}")
            Log.d(TAG, "Target device name: ${classicDevice.name}")


            Log.d(TAG, "Target device type: ${classicDevice.type}") // Should be CLASSIC or DUAL
                                                            // TODO: Figure out why it isn't (currently DEVICE_TYPE_UNKNOWN)
                                                            //  gpt says this might be the problem
            Log.d(TAG, "Target device bond state: ${classicDevice.bondState}")
            Log.d(TAG, "UUID: $uuid")
            Log.d(TAG, "Local adapter enabled: ${singletons!!.bluetoothAdapter.isEnabled}")

            Log.d(TAG, "Connecting to (BLE) device: ${otherDeviceData.device.address}")
            Log.d(TAG, "Connecting to (classic) device: ${classicDevice.address}")
            classicDevice.createInsecureRfcommSocketToServiceRecord(uuid).apply {
                connect() // Fails pretty quickly
            }
        } else {
            val listenerSocket = singletons.bluetoothAdapter
                .listenUsingInsecureRfcommWithServiceRecord("Test_name", uuid)

            listenerSocket.accept(30000) // 30s timeout
            // Times out
        }

        Log.d(TAG, "Socket stuff supposedly done")

        val outputStream = socket.outputStream
        val inputStream = socket.inputStream


        outputStream.write(singletonData.currentMessage.toByteArray())
        outputStream.flush()

        Log.d(TAG, "Wrote and flushed")

        val buffer = ByteArray(1024)
        val bytesRead = inputStream.read(buffer)
        val receivedMessage = String(buffer, 0, bytesRead)

        Log.d(TAG,"Received message: $receivedMessage")
        // FIXME: the advertisement itself should be a LocalDateTime instead of just LocalTime
        //      This is a (hopefully) temporary fix just for testing purposes
        val time: LocalTime = payloadTimestampToLocalTime(otherDeviceData.appPayload!!.timestamp.toByteArray())
        val date: LocalDate = LocalDate.now()
        val dateTime: LocalDateTime = LocalDateTime.of(date, time)

        singletons!!.database.collectedMessageDao().insertAll(
            CollectedMessage(
                uid = 0,
                deviceId = bytesToLong(otherDeviceData.appPayload.deviceId.toByteArray()),
                officialDatetime = dateTime,
                actualDatetime = LocalDateTime.now(),
                messageText = receivedMessage,
            )
        )
        Log.d(TAG,"Inserted into database")


    } catch (e: Exception) {
        Log.e(TAG, "Connection failed: ${e.message}", e)
    } finally {
        socket?.close()
    }
}

// TODO:
//  Datubāzes ierakstā jābūt:
//      deviceId, -- ierīces, ar kuru mēs apmainījāmies, id
//      deviceTimestamp, -- ierīces, ar kuru mēs apmainījāmies,
//                          timestamp / kad tajā ierīcē pēdējoreiz updatoja message
//      messageExchangeTimestamp -- kad šīs divas ierīces veica apmaiņu
//      message -- pati ziņa, ko mēs tad ieguvām
@SuppressLint("MissingPermission")
fun swapMessages(
    displayItems: List<DisplayItem>,
    deviceOwnerPayload: BLEAdvertisementPayload,
    singletons: Singletons?,
    singletonData: SingletonData?
) {
    val deviceOwnerTimestamp = payloadTimestampToLocalTime(deviceOwnerPayload.timestamp.toByteArray())

//    val allMessages = singletons!!.database.collectedMessageDao().getAll().first()

    displayItems.forEach { item ->
        val otherDeviceTimestamp = payloadTimestampToLocalTime(item.appPayload!!.timestamp.toByteArray())
        val otherDeviceId = bytesToLong(item.appPayload.deviceId.toByteArray())

        val existingMessage = singletons!!.database.collectedMessageDao()
            .getMessageByDeviceAndTime(otherDeviceId, LocalDateTime.of(LocalDate.now(), otherDeviceTimestamp))

        if (existingMessage != null) {
            // We've already exchanged this message, skip
            return@forEach
        }

        var isSender = deviceOwnerTimestamp < otherDeviceTimestamp
        Log.d(TAG, "Manually setting device to listener")
        isSender = false // This is manually adjusted between installing for each device
        val deviceIds = listOf(bytesToLong(deviceOwnerPayload.deviceId.toByteArray()), otherDeviceId).sorted()
//        val uuid = nameUUIDFromBytes((longToBytes(deviceIds[0]) + longToBytes(deviceIds[1])))
        val test_uuid_string = "test_uuid"
        val uuid = nameUUIDFromBytes(test_uuid_string.toByteArray())

        if (isSender) Log.d(TAG, "This device is sender")
        else Log.d(TAG, "This device is listener")

        handleConnection(singletons, singletonData, isSender, uuid, item)
    }
}

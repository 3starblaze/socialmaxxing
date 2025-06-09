package com.example.socialmaxxing

import androidx.core.app.ActivityCompat
import android.app.Activity
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.Context
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

@SuppressLint("MissingPermission")
class SimpleBLEMessageExchange(private val context: Context) {

    companion object {
        private const val SERVICE_UUID = "12345678-1234-1234-1234-123456789abc"
        private const val MESSAGE_CHAR_UUID = "87654321-4321-4321-4321-cba987654321"
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    // Server variables
    private var gattServer: BluetoothGattServer? = null
    private var serverMessage = ""

    // Client variables
    private var gattClient: BluetoothGatt? = null
    private var messageCharacteristic: BluetoothGattCharacteristic? = null
    private var clientMessage = ""

    // Callbacks
    var onConnectionStateChanged: ((Boolean) -> Unit)? = null
    var onExchangeComplete: ((myMessage: String, theirMessage: String) -> Unit)? = null

    // ======================
    // SERVER IMPLEMENTATION
    // ======================

    fun startServer(message: String) {
        serverMessage = message

        // Create GATT server
        gattServer = bluetoothManager.openGattServer(context, serverCallback)

        // Create service
        val service = BluetoothGattService(
            UUID.fromString(SERVICE_UUID),
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        // Create characteristic
        val characteristic = BluetoothGattCharacteristic(
            UUID.fromString(MESSAGE_CHAR_UUID),
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        service.addCharacteristic(characteristic)
        gattServer?.addService(service)
    }

    private val serverCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            onConnectionStateChanged?.invoke(newState == BluetoothProfile.STATE_CONNECTED)
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice, requestId: Int, offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            gattServer?.sendResponse(
                device, requestId, BluetoothGatt.GATT_SUCCESS,
                offset, serverMessage.toByteArray()
            )
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice, requestId: Int,
            characteristic: BluetoothGattCharacteristic, preparedWrite: Boolean,
            responseNeeded: Boolean, offset: Int, value: ByteArray
        ) {
            val theirMessage = String(value)
            Log.d("BLE", "SERVER: Received client message: $theirMessage")

            // MESSAGE EXCHANGE COMPLETE - Server has their message, client has our message
            onExchangeComplete?.invoke(serverMessage, theirMessage)

            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
            }
        }
    }



    // ======================
    // CLIENT IMPLEMENTATION
    // ======================

    fun connectAsClient(device: BluetoothDevice, messageToSend: String) {
        clientMessage = messageToSend
        gattClient = device.connectGatt(context, false, clientCallback)
    }

    private val clientCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                onConnectionStateChanged?.invoke(true)
                gatt.discoverServices()
            } else {
                onConnectionStateChanged?.invoke(false)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(UUID.fromString(SERVICE_UUID))
                messageCharacteristic = service?.getCharacteristic(UUID.fromString(MESSAGE_CHAR_UUID))

                // First read the server's message
                messageCharacteristic?.let { gatt.readCharacteristic(it) }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val theirMessage = String(characteristic.value)
                serverMessage = theirMessage // Store their message
                Log.d("BLE", "CLIENT: Read server message: $theirMessage")

                // Now send our message back to complete the exchange
                sendClientMessage()
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "CLIENT: Successfully sent our message")

                // MESSAGE EXCHANGE COMPLETE - We sent our message, we already have theirs
                onExchangeComplete?.invoke(clientMessage, serverMessage) // Use stored server message
            }
            // Disconnect after exchange
            disconnect()
        }
    }

    private fun sendClientMessage() {
        messageCharacteristic?.let { char ->
            char.value = clientMessage.toByteArray() // Send our message
            gattClient?.writeCharacteristic(char)
        }
    }

    // ======================
    // CLEANUP
    // ======================

    fun stopServer() {
        gattServer?.close()
        gattServer = null
    }

    fun disconnect() {
        gattClient?.disconnect()
        gattClient?.close()
        gattClient = null
    }
}



/**
 * Function for handling message exchange between two devices
*/
@RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
fun handleConnection(
    singletons: Singletons?,
    singletonData: SingletonData?,
    isSender: Boolean,
    otherDeviceData: DisplayItem
) {
    val bleExchange = SimpleBLEMessageExchange(singletons!!.activity.applicationContext)

    bleExchange.onExchangeComplete = { myMessage, theirMessage ->
        Log.d("BLE", "EXCHANGE COMPLETE!")
        Log.d("BLE", "My message: $myMessage")
        Log.d("BLE", "Their message: $theirMessage")

        val time: LocalTime = payloadTimestampToLocalTime(otherDeviceData.appPayload!!.timestamp.toByteArray())
        val date: LocalDate = LocalDate.now()
        val dateTime: LocalDateTime = LocalDateTime.of(date, time)

        singletons.database.collectedMessageDao().insertAll(
            CollectedMessage(
                uid = 0,
                deviceId = bytesToLong(otherDeviceData.appPayload.deviceId.toByteArray()),
                officialDatetime = dateTime,
                actualDatetime = LocalDateTime.now(),
                messageText = theirMessage,
            )
        )
        Log.d(TAG,"Inserted into database")
    }

    bleExchange.onConnectionStateChanged = { isConnected ->
        Log.d("BLE", "Connection state: $isConnected")
    }

    val myMessage = singletonData!!.currentMessage
    if (isSender) {
        Thread.sleep(3000)
        // Connect as client to get their message and send ours
        val device = otherDeviceData.device // Assuming you have the BluetoothDevice
        bleExchange.connectAsClient(device, myMessage)
    } else {
        // Start server and wait for connection
        bleExchange.startServer(myMessage)
    }
}

@SuppressLint("MissingPermission")
fun swapMessages(
    displayItems: List<DisplayItem>,
    deviceOwnerPayload: BLEAdvertisementPayload,
    singletons: Singletons?,
    singletonData: SingletonData?
) {
    val deviceOwnerTimestamp = payloadTimestampToLocalTime(deviceOwnerPayload.timestamp.toByteArray())

    displayItems.forEach { item ->
        val otherDeviceTimestamp = payloadTimestampToLocalTime(item.appPayload!!.timestamp.toByteArray())
        val otherDeviceId = bytesToLong(item.appPayload.deviceId.toByteArray())

        val existingMessage = singletons!!.database.collectedMessageDao()
            .getMessageByDeviceAndTime(otherDeviceId, LocalDateTime.of(LocalDate.now(), otherDeviceTimestamp))

        if (existingMessage != null) {
            // We've already exchanged this message, skip
            Log.d(TAG, "We already have swapped with $otherDeviceId, skipping")
            return@forEach
        }

        var isSender = deviceOwnerTimestamp < otherDeviceTimestamp

        if (isSender) Log.d(TAG, "This device is sender")
        else Log.d(TAG, "This device is listener")

        handleConnection(singletons, singletonData, isSender, item)
    }
}

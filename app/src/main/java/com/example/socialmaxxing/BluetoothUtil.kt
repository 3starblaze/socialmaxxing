package com.example.socialmaxxing

import androidx.core.app.ActivityCompat
import android.app.Activity
import android.Manifest
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
// TODO: universālā loģika
@OptIn(ExperimentalUuidApi::class)
fun swapMessages(deviceOwnerPayload: BLEAdvertisementPayload,
                 receivedPayload: BLEAdvertisementPayload,
                 isSender: Boolean,
                 uuid: Uuid) {
    // TODO: ziņu sūtīšana
    // TODO: ziņu saņemšana
        // TODO: ziņu pieglabāšana datubāzē
}

/**
 * Function for deciding which device we'll be trying to connect to.
 * Needed for scenarios when more than 2 devices detect each other
*/

// TODO:
//  Datubāzes ierakstā jābūt:
//      deviceId, -- ierīces, ar kuru mēs apmainījāmies, id
//      deviceTimestamp, -- ierīces, ar kuru mēs apmainījāmies,
//                          timestamp / kad tajā ierīcē pēdējoreiz updatoja message
//      messageExchangeTimestamp -- kad šīs divas ierīces veica apmaiņu
//      message -- pati ziņa, ko mēs tad ieguvām
fun decideSwapOrder(sortedDeviceList: List<DisplayItem>, deviceOwnerPayload: BLEAdvertisementPayload) {
    // Atsevišķi maybe pieglabājam savu timestamp

    /**
     for each device in sortedDeviceList:
        ja otras ierīces deviceId ar tādu deviceTimestamp jau ir mūsu datubāzē
        UN
        ja šīs ierīces timestamp IR senāks par tā datubāzes ieraksta messageExchangeTimestamp:
            continue

        val uuid;
        bool firstSender;
        ja šī device timestamp ir senāks nekā atrastajam device:
            uuid = <otra_device_id-mans_timestamp>
            firstSender = true
        ja šī device id ir jaunāks nekā atrastajam device:
            uuid = <šī_device_id-otra_device_timestamp>
            firstSender = false

        ja otras ierīces deviceId ar tādu deviceTimestamp jau ir mūsu datubāzē
        UN
        ja mūsu timestamp ir jaunāks par tā messageExchangeTimestamp:
                                            // mēs esam izmainījuši ziņu kopš pēdējā swap
            atveram listeneri ar iepriekš uzsettoto uuid

        ja otras ierīces deviceId ir mūsu datubāzē
        UN
        ja otras ierīces deviceTimestamp nesakrīt ar mūsu datubāzi
                                        // otrs cilvēks ir izmainījis ziņu kopš pēdējā swap
        UN
        ja mūsu timestamp nav jaunāks par messageExchangeTimestamp:
                                            // mēs neesam neko kopš swap mainījuši
                atveram senderi ar iepriekš uzsettoto uuid

        else:
            // Situācijas, kad vai nu apmaiņa vēl nav bijusi,
            // vai arī abas ierīces ir izmainījušas savu ziņu kopš pēdējās apmaiņas

            if (firstSender):
                uzsetuppojam senderi(createInsecureRfcommSocketToServiceRecord) ar mūsu uuid
                // TODO: vai te nevajadzētu būt like dažu sekunžu delay, lai otrs device paspēj setupu?
                Use BluetoothSocket.connect to initiate the outgoing connection. This will also perform an SDP lookup of the given uuid to determine which channel to connect to.
                Pēc apmaiņas nositam connection
            else:
                uzsetuppojam listeneri(listenUsingInsecureRfcommWithServiceRecord) ar mūsu uuid
                Use BluetoothServerSocket.accept to retrieve incoming connections from a listening BluetoothServerSocket.
                Pēc apmaiņas nositam connection

            if (!firstSender):
                uzsetuppojam senderi(createInsecureRfcommSocketToServiceRecord) ar mūsu uuid
                // TODO: vai te nevajadzētu būt like dažu sekunžu delay, lai otrs device paspēj setupu?
                Use BluetoothSocket.connect to initiate the outgoing connection. This will also perform an SDP lookup of the given uuid to determine which channel to connect to.
                Pēc apmaiņas nositam connection
            else:
                uzsetuppojam listeneri(listenUsingInsecureRfcommWithServiceRecord) ar mūsu uuid
                Use BluetoothServerSocket.accept to retrieve incoming connections from a listening BluetoothServerSocket.
                Pēc apmaiņas nositam connection

    */
}

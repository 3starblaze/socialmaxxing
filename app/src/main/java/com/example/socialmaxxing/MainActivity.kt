package com.example.socialmaxxing

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.socialmaxxing.ui.theme.SocialmaxxingTheme

class MainActivity : ComponentActivity() {
    // NOTE: We have "arePermissionsReady" that does the thing
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val activity = this
        // TODO: Refactor this permission checking logic, it's rather repetitive
        // probably a component with "permissionsReady" flag is a better alternative
        var singletons: Singletons? = if (arePermissionsReady(activity)) getSingletons(activity) else null

        setContent {
            SocialmaxxingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column (modifier = Modifier.padding(innerPadding)) {
                        Text(text = if (singletons == null) "we need your permission" else "everything seems to be good")
                        // TODO: Don't show this when permissions are ok
                        RequestBluetoothButton(
                            onClick = {
                                requestBluetoothPermissions(activity)
                                if (arePermissionsReady(activity)) {
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
                            }
                        )
                    }
                }
            }
        }
    }
}

fun arePermissionsReady(activity: Activity): Boolean {
    return (ContextCompat.checkSelfPermission(
        activity,
        Manifest.permission.BLUETOOTH_CONNECT,
    ) == PackageManager.PERMISSION_GRANTED)
}

@Composable
fun RequestBluetoothButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text = "Ask for Bluetooth")
    }
}
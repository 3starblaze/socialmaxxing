package com.example.socialmaxxing

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.socialmaxxing.ui.theme.SocialmaxxingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val activity = this

        setContent {
            SocialmaxxingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        // TODO: Don't show this when permissions are ok
                        RequestBluetoothButton(
                            onClick = {
                                requestBluetoothPermissions(activity)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RequestBluetoothButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text = "Ask for Bluetooth")
    }
}
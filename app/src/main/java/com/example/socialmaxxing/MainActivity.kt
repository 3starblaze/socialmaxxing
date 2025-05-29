package com.example.socialmaxxing

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import com.example.socialmaxxing.views.IndexView

class MainActivity : ComponentActivity() {
    // NOTE: We have "arePermissionsReady" that does the thing
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val activity = this

        setContent {
            IndexView(activity)
        }
    }
}


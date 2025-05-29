package com.example.socialmaxxing.views

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.socialmaxxing.ui.theme.SocialmaxxingTheme
import com.example.socialmaxxing.views.debug.DebugIndexView
import com.example.socialmaxxing.views.main.MainIndexView

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun IndexView(activity: Activity) {
    val currentTab = remember { mutableStateOf("main") }

    data class PageItem(
        val key: String,
        val displayText: String,
    )

    val pageItems = listOf<PageItem>(
        PageItem(key = "main", displayText = "Main"),
        PageItem(key = "debug", displayText = "Debug"),
    )

    SocialmaxxingTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    pageItems.map({ item ->
                        NavigationBarItem(
                            selected = currentTab.value == item.key,
                            label = { Text(item.displayText) },
                            onClick = { currentTab.value = item.key },
                            // NOTE: First letter as the icon will do for now
                            icon = { Text(item.displayText.slice(0..0)) }
                        )
                    })
                }
            },
        ) { innerPadding ->
            Box (modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                when (currentTab.value) {
                    "main" -> MainIndexView()
                    "debug" -> DebugIndexView(activity)
                }
            }
        }
    }
}
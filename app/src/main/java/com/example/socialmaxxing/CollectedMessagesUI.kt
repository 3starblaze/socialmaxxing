package com.example.socialmaxxing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.socialmaxxing.db.CollectedMessage
import androidx.compose.foundation.lazy.items
import com.example.socialmaxxing.ui.theme.Typography

@Composable
fun CollectedMessagesView(singletons: Singletons) {
    val messages = remember { mutableStateOf<List<CollectedMessage>?>(null) }

    LaunchedEffect(singletons) {
        messages.value = singletons.database.collectedMessageDao().getAll()
    }

    Column {
        Text(
            text = "Collected messages",
            style = Typography.titleLarge,
        )
        LazyColumn {
            if (messages.value === null) {
                item {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            } else if (messages.value!!.isEmpty()) {
                item {
                    Text("You have no messages...")
                }
            } else {
                items(messages.value!!) { item ->
                    Column {
                        Text("${item.deviceId}")
                        Text("${item.messageText}")
                    }
                }
            }
        }
    }
}
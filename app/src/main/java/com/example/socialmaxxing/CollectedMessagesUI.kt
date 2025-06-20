package com.example.socialmaxxing

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.example.socialmaxxing.ui.theme.Typography

@Composable
fun CollectedMessagesView(singletons: Singletons) {
    val messages = remember { mutableStateOf<List<CollectedMessage>?>(null) }

    LaunchedEffect(singletons) {
        singletons.database.collectedMessageDao().getAll().collect {
            value -> messages.value = value
        }
    }

    Column {
        Text(
            text = "Collected messages",
            style = Typography.titleLarge,
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
                        Text(buildAnnotatedString {
                            withStyle(Typography.labelSmall.toSpanStyle()) {
                                append("${item.deviceId}")
                            }
                            append(" said")
                        })
                        Text(buildAnnotatedString {
                            withStyle(Typography.labelSmall.toSpanStyle()) {
                                append("> ")
                            }
                            append(item.messageText)
                        })
                    }
                }
            }
        }
    }
}
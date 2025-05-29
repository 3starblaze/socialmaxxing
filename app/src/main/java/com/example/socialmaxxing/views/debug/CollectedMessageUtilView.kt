package com.example.socialmaxxing.views.debug

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.socialmaxxing.Singletons
import com.example.socialmaxxing.db.CollectedMessage
import com.example.socialmaxxing.ui.theme.Typography
import java.time.LocalDateTime
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random
import kotlin.random.nextInt

val words = listOf<String>(
    "apple",
    "banana",
    "cabbage",
    "door",
    "eel",
    "fridge",
    "good",
)

fun generateText(): String {
    val wordCount = Random.nextInt(2..4)
    return (1..wordCount)
        .map({ _ -> words.get(Random.nextInt(0 until words.size))})
        .joinToString(", ")
}

@RequiresApi(Build.VERSION_CODES.O)
fun randomDatetime(): LocalDateTime {
    val year = Random.nextInt(2024..2025)
    val month = Random.nextInt(1..12)
    // NOTE: Don't want to check the month, this will do for all months
    val day = Random.nextInt(1..28)
    val hour = Random.nextInt(0..23)
    val minute = Random.nextInt(0..59)
    val second = Random.nextInt(0..59)
    val res = LocalDateTime.of(year, month, day, hour, minute, second)
    return res
}

@RequiresApi(Build.VERSION_CODES.O)
fun generateCollectedMessage(): CollectedMessage {
    return CollectedMessage(
        uid = 0,
        deviceId = ThreadLocalRandom.current().nextLong(),
        officialDatetime = randomDatetime(),
        actualDatetime = randomDatetime(),
        messageText = generateText(),
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CollectedMessageUtilView(singletons: Singletons) {
    Column {
        Text(
            style = Typography.titleLarge,
            text = "Collected Message utilities"
        )

        Button(onClick = {
            singletons.database.collectedMessageDao().insertAll(
                generateCollectedMessage()
            )
        }) {
            Text("Add a random collected message")
        }
    }
}
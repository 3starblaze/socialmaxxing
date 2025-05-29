package com.example.socialmaxxing.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import java.time.LocalDateTime

@Entity
data class CollectedMessage(
    @PrimaryKey val uid: Int,
    // NOTE: The device id of the message's owner
    @ColumnInfo(name = "device_id") val deviceId: Long,
    // NOTE: The datetime that this message was published at and this is the time that should show
    // up during the advertisement (if this was the last message)
    @ColumnInfo(name = "official_datetime") val officialDatetime: LocalDateTime,
    // NOTE: The datetime at which this message was taken from source
    @ColumnInfo(name = "actual_datetime") val actualDatetime: LocalDateTime,
    // NOTE: The content of the message
    @ColumnInfo(name = "message_text") val messageText: String,
)

@Dao
interface CollectedMessageDao {
    @Insert
    fun insertAll(vararg collectedMessages: CollectedMessage)

    @Delete
    fun delete(collectedMessage: CollectedMessage)

    @Query("SELECT * from CollectedMessage")
    fun getAll(): List<CollectedMessage>
}
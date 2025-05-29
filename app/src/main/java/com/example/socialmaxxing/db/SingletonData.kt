package com.example.socialmaxxing.db

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
data class SingletonData(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "current_message") val currentMessage: String,
    @ColumnInfo(name = "device_id") val deviceId: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime,
)

@Dao
interface SingletonDataDao {
    @Query("SELECT * FROM singletondata LIMIT 1")
    fun getData(): SingletonData

    @Update
    fun updateData(data: SingletonData)

    @Query("SELECT * FROM singletondata LIMIT 1")
    fun getDataUnsafe(): SingletonData?

    @Insert
    fun insertData(data: SingletonData)
}
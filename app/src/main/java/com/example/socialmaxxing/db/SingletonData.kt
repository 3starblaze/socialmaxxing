package com.example.socialmaxxing.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update

@Entity
data class SingletonData(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "current_message") val currentMessage: String,
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
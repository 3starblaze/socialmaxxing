package com.example.socialmaxxing.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.runBlocking

@Database(entities = [SingletonData::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun singletonDataDao(): SingletonDataDao
}

/**
 * Ensure that the database contains an object for singleton data.
 */
fun ensureSingletonData(db: AppDatabase) {
    val maybeData = db.singletonDataDao().getDataUnsafe()

    if (maybeData === null) {
        val data = SingletonData(
            uid = 0,
            currentMessage = ""
        )

        db.singletonDataDao().insertData(data)
    }
}

fun getDb(context: Context): AppDatabase {
    val db = Room
        .databaseBuilder(context, AppDatabase::class.java, "main-database")
        // HACK: I tried to launch queries through coroutines but I always get the
        // main thread error, so we are allowing this for now.
        .allowMainThreadQueries()
        .build()
    ensureSingletonData(db)
    return db
}
package com.example.socialmaxxing.db

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.socialmaxxing.Singletons
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.util.concurrent.ThreadLocalRandom

class Converters {
    @TypeConverter
    fun serializeLocalDateTime(value: LocalDateTime): String {
        return value.toString()
    }

    @TypeConverter
    @RequiresApi(Build.VERSION_CODES.O)
    fun deserializeLocalDateTime(value: String): LocalDateTime {
        return LocalDateTime.parse(value)
    }
}

@Database(
    entities = [
      SingletonData::class,
      CollectedMessage::class,
    ],
    version = 1,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun singletonDataDao(): SingletonDataDao
    abstract fun collectedMessageDao(): CollectedMessageDao
}

@RequiresApi(Build.VERSION_CODES.O)
fun generateSingletonData(): SingletonData {
    return SingletonData(
        uid = 0,
        currentMessage = "test_message",
        deviceId = ThreadLocalRandom.current().nextLong(),
        updatedAt = LocalDateTime.now(),
    )
}

/**
 * Ensure that the database contains an object for singleton data.
 */
@RequiresApi(Build.VERSION_CODES.O)
fun ensureSingletonData(db: AppDatabase) {
    val maybeData = db.singletonDataDao().getDataUnsafe()

    if (maybeData === null) {
        db.singletonDataDao().insertData(generateSingletonData())
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun getDb(context: Context): AppDatabase {
    val db = Room
        .databaseBuilder(context, AppDatabase::class.java, "main-database")
        // HACK: I tried to launch queries through coroutines but I always get the
        // main thread error, so we are allowing this for now.
        .allowMainThreadQueries()
        // NOTE: This app is a prototype, it's fine to do destructive migration
        .fallbackToDestructiveMigration(true)
        .build()
    ensureSingletonData(db)
    return db
}
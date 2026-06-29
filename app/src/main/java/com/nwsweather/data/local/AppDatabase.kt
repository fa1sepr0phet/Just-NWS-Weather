package com.nwsweather.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SavedLocationEntity::class, PointCacheEntity::class, WeatherSnapshotEntity::class],
    version = 6,
    autoMigrations = [
        AutoMigration(from = 5, to = 6)
    ],
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedLocationDao(): SavedLocationDao
    abstract fun pointCacheDao(): PointCacheDao
    abstract fun weatherSnapshotDao(): WeatherSnapshotDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "just-weather.db"
                )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

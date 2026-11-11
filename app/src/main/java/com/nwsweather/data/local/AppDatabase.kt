package com.nwsweather.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SavedLocationEntity::class, PointCacheEntity::class, WeatherSnapshotEntity::class],
    version = 9,
    autoMigrations = [
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 8, to = 9)
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

        private val MIGRATION_2_8 = object : Migration(2, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `saved_locations` ADD COLUMN `displayOrder` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "UPDATE `saved_locations` SET `displayOrder` = `id` - 1"
                )

                recreatePointCacheTable(db)
                recreateWeatherSnapshotTable(db)
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                recreatePointCacheTable(db)
                recreateWeatherSnapshotTable(db)
            }
        }

        private fun recreatePointCacheTable(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `point_cache`")
            db.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `point_cache` (
                        `key` TEXT NOT NULL,
                        `gridId` TEXT NOT NULL,
                        `gridX` INTEGER NOT NULL,
                        `gridY` INTEGER NOT NULL,
                        `forecastUrl` TEXT NOT NULL,
                        `forecastHourlyUrl` TEXT NOT NULL,
                        `forecastGridDataUrl` TEXT NOT NULL,
                        `observationStations` TEXT,
                        `timeZone` TEXT,
                        `city` TEXT,
                        `state` TEXT,
                        `cachedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`key`)
                    )
                """.trimIndent()
            )
        }

        private fun recreateWeatherSnapshotTable(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `weather_snapshot`")
            db.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `weather_snapshot` (
                        `id` INTEGER NOT NULL,
                        `locationName` TEXT NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `temperature` INTEGER NOT NULL,
                        `temperatureUnit` TEXT NOT NULL,
                        `shortForecast` TEXT NOT NULL,
                        `humidity` INTEGER,
                        `windSpeed` TEXT NOT NULL,
                        `windDirection` TEXT NOT NULL,
                        `uvIndex` INTEGER,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        `isDaytime` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent()
            )
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "just-weather.db"
                )
                    .addMigrations(MIGRATION_2_8, MIGRATION_7_8)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

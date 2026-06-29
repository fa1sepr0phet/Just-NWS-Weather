package com.nwsweather.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedLocationDao {
    @Query("SELECT * FROM saved_locations ORDER BY displayOrder ASC, label ASC")
    fun observeAll(): Flow<List<SavedLocationEntity>>

    @Query("SELECT * FROM saved_locations ORDER BY displayOrder ASC, label ASC")
    suspend fun getAll(): List<SavedLocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SavedLocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<SavedLocationEntity>)

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM saved_locations")
    suspend fun deleteAll()

    @Query("SELECT MAX(displayOrder) FROM saved_locations")
    suspend fun getMaxOrder(): Int?
}

@Dao
interface PointCacheDao {
    @Query("SELECT * FROM point_cache WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): PointCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PointCacheEntity)

    @Query("DELETE FROM point_cache")
    suspend fun deleteAll()
}

@Dao
interface WeatherSnapshotDao {
    @Query("SELECT * FROM weather_snapshot WHERE id = 0 LIMIT 1")
    suspend fun getLatest(): WeatherSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WeatherSnapshotEntity)

    @Query("DELETE FROM weather_snapshot")
    suspend fun deleteAll()
}

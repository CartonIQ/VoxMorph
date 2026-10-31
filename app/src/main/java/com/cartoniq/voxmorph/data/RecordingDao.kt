package com.cartoniq.voxmorph.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE sourceType = :type ORDER BY createdAtEpochMs DESC")
    fun observeByType(type: String): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings")
    suspend fun getAll(): List<RecordingEntity>

    @Insert
    suspend fun insert(entity: RecordingEntity): Long

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recordings")
    suspend fun deleteAll()
}

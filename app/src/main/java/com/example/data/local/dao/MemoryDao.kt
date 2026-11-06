package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AiMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM ai_memories ORDER BY updatedTimestamp DESC")
    fun getAllMemories(): Flow<List<AiMemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: AiMemoryEntity)

    @Query("SELECT value FROM ai_memories WHERE `key` = :key LIMIT 1")
    suspend fun getMemoryValue(key: String): String?

    @Query("DELETE FROM ai_memories WHERE `key` = :key")
    suspend fun deleteMemory(key: String)
}

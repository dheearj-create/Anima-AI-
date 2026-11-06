package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.DeepSearchDossierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeepSearchDao {
    @Query("SELECT * FROM deep_search_dossiers ORDER BY timestamp DESC")
    fun getAllDossiers(): Flow<List<DeepSearchDossierEntity>>

    @Query("SELECT * FROM deep_search_dossiers WHERE id = :id LIMIT 1")
    suspend fun getDossierById(id: Long): DeepSearchDossierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDossier(dossier: DeepSearchDossierEntity): Long

    @Query("DELETE FROM deep_search_dossiers WHERE id = :id")
    suspend fun deleteDossierById(id: Long)

    @Query("DELETE FROM deep_search_dossiers")
    suspend fun clearAllDossiers()
}

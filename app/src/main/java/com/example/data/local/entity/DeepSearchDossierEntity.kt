package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deep_search_dossiers")
data class DeepSearchDossierEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val query: String,
    val summary: String,
    val reportMarkdown: String,
    val stepsJson: String,
    val citationsJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

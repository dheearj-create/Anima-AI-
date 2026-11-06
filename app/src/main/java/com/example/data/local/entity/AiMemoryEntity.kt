package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_memories")
data class AiMemoryEntity(
    @PrimaryKey
    val key: String,
    val value: String,
    val category: String = "general",
    val updatedTimestamp: Long = System.currentTimeMillis()
)

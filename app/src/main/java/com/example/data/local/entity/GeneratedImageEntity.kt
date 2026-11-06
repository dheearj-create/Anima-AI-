package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "generated_images")
data class GeneratedImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val prompt: String,
    val style: String,
    val imagePath: String,
    val width: Int = 512,
    val height: Int = 512,
    val seed: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

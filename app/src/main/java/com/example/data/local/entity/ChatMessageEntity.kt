package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "USER" or "AI"
    val text: String,
    val emotion: String = "CALM",
    val imageUrl: String? = null,
    val isDeepSearch: Boolean = false,
    val deepSearchDossierId: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)

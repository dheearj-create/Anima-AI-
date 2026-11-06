package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AiMemoryEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.DeepSearchDossierEntity
import com.example.data.local.entity.GeneratedImageEntity
import com.example.domain.engine.AiResponse
import com.example.domain.engine.DeepSearchAgent
import com.example.domain.engine.DeepSearchReport
import com.example.domain.engine.GenerativeArtEngine
import com.example.domain.engine.OfflineKnowledgeEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class ChatRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    val allMessages: Flow<List<ChatMessageEntity>> = database.chatDao().getAllMessages()
    val allImages: Flow<List<GeneratedImageEntity>> = database.imageGalleryDao().getAllImages()
    val allDossiers: Flow<List<DeepSearchDossierEntity>> = database.deepSearchDao().getAllDossiers()
    val allMemories: Flow<List<AiMemoryEntity>> = database.memoryDao().getAllMemories()

    suspend fun insertUserMessage(text: String): Long {
        val userMsg = ChatMessageEntity(
            role = "USER",
            text = text,
            emotion = "CALM",
            timestamp = System.currentTimeMillis()
        )
        return database.chatDao().insertMessage(userMsg)
    }

    suspend fun insertAiMessage(
        text: String,
        emotion: String,
        imageUrl: String? = null,
        isDeepSearch: Boolean = false,
        deepSearchDossierId: Long? = null
    ): Long {
        val aiMsg = ChatMessageEntity(
            role = "AI",
            text = text,
            emotion = emotion,
            imageUrl = imageUrl,
            isDeepSearch = isDeepSearch,
            deepSearchDossierId = deepSearchDossierId,
            timestamp = System.currentTimeMillis()
        )
        return database.chatDao().insertMessage(aiMsg)
    }

    suspend fun generateArtwork(
        prompt: String,
        style: GenerativeArtEngine.ArtStyle = GenerativeArtEngine.ArtStyle.CYBERPUNK
    ): GeneratedImageEntity {
        val path = GenerativeArtEngine.generateArtwork(
            context = context,
            prompt = prompt,
            style = style,
            width = 800,
            height = 800
        )
        val imageEntity = GeneratedImageEntity(
            prompt = prompt,
            style = style.label,
            imagePath = path,
            timestamp = System.currentTimeMillis()
        )
        val id = database.imageGalleryDao().insertImage(imageEntity)
        return imageEntity.copy(id = id)
    }

    suspend fun saveDeepSearchDossier(report: DeepSearchReport): Long {
        val stepsJson = report.steps.joinToString(";") { "${it.title}: ${it.detail}" }
        val citationsJson = report.citations.joinToString(";") { "${it.title} (${it.source})" }
        val dossier = DeepSearchDossierEntity(
            query = report.query,
            summary = report.summary,
            reportMarkdown = report.reportMarkdown,
            stepsJson = stepsJson,
            citationsJson = citationsJson,
            timestamp = System.currentTimeMillis()
        )
        return database.deepSearchDao().insertDossier(dossier)
    }

    suspend fun clearHistory() {
        database.chatDao().clearAllMessages()
    }

    suspend fun deleteImage(id: Long) {
        database.imageGalleryDao().deleteImageById(id)
    }

    suspend fun deleteDossier(id: Long) {
        database.deepSearchDao().deleteDossierById(id)
    }
}

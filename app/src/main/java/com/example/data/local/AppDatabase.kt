package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.DeepSearchDao
import com.example.data.local.dao.ImageGalleryDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.entity.AiMemoryEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.DeepSearchDossierEntity
import com.example.data.local.entity.GeneratedImageEntity

@Database(
    entities = [
        ChatMessageEntity::class,
        GeneratedImageEntity::class,
        DeepSearchDossierEntity::class,
        AiMemoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun imageGalleryDao(): ImageGalleryDao
    abstract fun deepSearchDao(): DeepSearchDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "anima_ai_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.luudev.vocalens.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.luudev.vocalens.data.local.dao.*
import com.luudev.vocalens.data.local.entity.*

@Database(entities = [UserEntity::class, UserSettingsEntity::class, VocabularyEntity::class,
    UserVocabularyEntity::class, CollectionEntity::class, CollectionItemEntity::class,
    LessonEntity::class, UserLessonProgressEntity::class, StudySessionEntity::class,
    VocabularyReviewEntity::class], version = 1, exportSchema = true)
abstract class VocabLensDatabase : RoomDatabase() {
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun userDao(): UserDao
    abstract fun collectionDao(): CollectionDao
    abstract fun lessonDao(): LessonDao
    abstract fun studyDao(): StudyDao
    abstract fun statisticsDao(): StatisticsDao

    companion object {
        @Volatile private var instance: VocabLensDatabase? = null

        fun getInstance(context: Context): VocabLensDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, VocabLensDatabase::class.java, "vocab_lens_app.db"
            ).build().also { instance = it }
        }
    }
}

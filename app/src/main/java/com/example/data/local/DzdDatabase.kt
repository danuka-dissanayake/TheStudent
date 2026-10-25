package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SavedPaperEntity::class,
        SavedCourseEntity::class,
        LearningActivityEntity::class,
        UserProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DzdDatabase : RoomDatabase() {
    abstract fun dzdDao(): DzdDao

    companion object {
        @Volatile
        private var INSTANCE: DzdDatabase? = null

        fun getDatabase(context: Context): DzdDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DzdDatabase::class.java,
                    "dzd_learn_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

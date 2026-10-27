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
abstract class TheStudentDatabase : RoomDatabase() {
    abstract fun theStudentDao(): TheStudentDao

    companion object {
        @Volatile
        private var INSTANCE: TheStudentDatabase? = null

        fun getDatabase(context: Context): TheStudentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TheStudentDatabase::class.java,
                    "the_student_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

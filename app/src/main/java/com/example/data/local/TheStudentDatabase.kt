package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SavedPaperEntity::class,
        SavedCourseEntity::class,
        LearningActivityEntity::class,
        UserProfileEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TheStudentDatabase : RoomDatabase() {
    abstract fun theStudentDao(): TheStudentDao

    companion object {
        @Volatile
        private var INSTANCE: TheStudentDatabase? = null

        /** v1 -> v2: phone / avatar / profile-completion flags. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN phone TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN avatarUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN profileCompleted INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN hasPassword INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getDatabase(context: Context): TheStudentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TheStudentDatabase::class.java,
                    "the_student_database"
                )
                    // v2 adds the profile-setup columns. A real migration keeps the
                    // student's saved papers and streak instead of wiping the cache.
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

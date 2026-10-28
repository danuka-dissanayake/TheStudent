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
        UserProfileEntity::class,
        ChatMessageEntity::class,
        QuizResultEntity::class
    ],
    version = 8,
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

        /**
         * v2 -> v3: chat history moved onto the device.
         *
         * The SQL matches the schema Room generates for [ChatMessageEntity]
         * exactly (Room validates it on open), so existing installs keep their
         * saved papers, streak and profile while gaining the chat table.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `chat_messages` (" +
                        "`localId` TEXT NOT NULL, " +
                        "`serverId` INTEGER NOT NULL, " +
                        "`peerId` INTEGER NOT NULL, " +
                        "`senderId` INTEGER NOT NULL, " +
                        "`isMine` INTEGER NOT NULL, " +
                        "`body` TEXT NOT NULL, " +
                        "`sentAtEpoch` INTEGER NOT NULL, " +
                        "`isRead` INTEGER NOT NULL, " +
                        "`isPending` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`localId`))"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_chat_messages_peerId` " +
                        "ON `chat_messages` (`peerId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_chat_messages_sentAtEpoch` " +
                        "ON `chat_messages` (`sentAtEpoch`)"
                )
            }
        }

        /**
         * v3 -> v4: attachments.
         *
         * Only metadata columns are added — the decrypted file itself lives in
         * filesDir, never in the database. Nullable columns need no default;
         * the two NOT NULL ones get one so existing rows stay valid.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN attachmentId TEXT")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN attachmentName TEXT")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN attachmentMime TEXT")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN attachmentSize INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN attachmentKind TEXT")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN attachmentLocal INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v4 -> v5: replies and voice-note length.
         *
         * All four columns are additive with defaults, so existing rows stay
         * valid and old chats simply have no quoted message.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN attachmentDurationMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN replyToServerId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN replyToIsMine INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN replyToPreview TEXT")
            }
        }

        /**
         * v5 -> v6: the structured academic profile (level, stream, exam year,
         * medium and the three subjects).
         *
         * Purely additive with defaults, so an existing install keeps its
         * profile and simply reports "no academic details yet" until the
         * student fills the step in.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN examLevel TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN streamCode TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN examYear INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN medium TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN academicSubjects TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * v6 -> v7: the wider academic stages (Grades 6–9 and university) need a
         * grade / year-of-study, the university and the degree field.
         *
         * Additive with defaults: an existing A/L or O/L profile stays exactly
         * as it is and simply has no university attached.
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN studyYear INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN university TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN degreeProgramme TEXT NOT NULL DEFAULT ''")
            }
        }


        /**
         * v7 -> v8: the offline mirror of quiz results ("Best 82%", "Passed").
         *
         * A brand new table, so an existing install keeps everything it has and
         * simply shows no quiz badges until the first successful load.
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `quiz_results` (" +
                        "`quizId` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`subject` TEXT NOT NULL, " +
                        "`grade` TEXT NOT NULL, " +
                        "`bestPercent` INTEGER NOT NULL, " +
                        "`passed` INTEGER NOT NULL, " +
                        "`attempts` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`quizId`))"
                )
            }
        }

        fun getDatabase(context: Context): TheStudentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TheStudentDatabase::class.java,
                    "the_student_database"
                )
                    // v2 adds the profile-setup columns; v3 the chat history;
                    // v4 attachments; v5 replies and voice notes; v6 the
                    // structured academic profile; v7 Grades 6–9 and university;
                    // v8 the offline quiz-result mirror.
                    // The migrations are explicit so an ordinary update keeps every
                    // saved paper, streak and message.
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8
                    )
                    // Last resort only: if the file on the device cannot be
                    // migrated for any reason (interrupted update, a build from
                    // another branch), recreate it instead of throwing during the
                    // ViewModel constructor. That loses the local cache and the
                    // local chat history, which is still better than an app that
                    // cannot start at all.
                    //
                    // Note: this must be the version-less overload. Passing start
                    // versions here (e.g. fallbackToDestructiveMigrationFrom(1, 2))
                    // throws IllegalArgumentException as soon as a migration for
                    // one of those start versions is also registered above.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

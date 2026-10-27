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
        ChatMessageEntity::class
    ],
    version = 4,
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

        fun getDatabase(context: Context): TheStudentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TheStudentDatabase::class.java,
                    "the_student_database"
                )
                    // v2 adds the profile-setup columns; v3 the chat history;
                    // v4 attachments.
                    // The migrations are explicit so an ordinary update keeps every
                    // saved paper, streak and message.
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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

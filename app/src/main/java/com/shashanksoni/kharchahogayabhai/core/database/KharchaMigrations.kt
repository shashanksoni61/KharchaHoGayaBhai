package com.shashanksoni.kharchahogayabhai.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object KharchaMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS import_batches (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    file_name TEXT NOT NULL,
                    source TEXT NOT NULL,
                    imported_at INTEGER NOT NULL,
                    total_parsed INTEGER NOT NULL,
                    new_count INTEGER NOT NULL,
                    merged_count INTEGER NOT NULL,
                    duplicate_count INTEGER NOT NULL,
                    failed_count INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    error_message TEXT
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_import_batches_imported_at ON import_batches(imported_at)",
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS labels (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    color_hex TEXT NOT NULL,
                    is_system_defined INTEGER NOT NULL,
                    sort_order INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_labels_name ON labels(name)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS transaction_labels (
                    transaction_id INTEGER NOT NULL,
                    label_id INTEGER NOT NULL,
                    PRIMARY KEY(transaction_id, label_id),
                    FOREIGN KEY(transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
                    FOREIGN KEY(label_id) REFERENCES labels(id) ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_transaction_labels_label_id ON transaction_labels(label_id)",
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE transactions ADD COLUMN is_promotional INTEGER NOT NULL DEFAULT 0",
            )
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE transactions ADD COLUMN is_ignored INTEGER NOT NULL DEFAULT 0",
            )
        }
    }
}

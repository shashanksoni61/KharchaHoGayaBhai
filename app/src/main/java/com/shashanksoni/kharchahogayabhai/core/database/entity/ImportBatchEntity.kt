package com.shashanksoni.kharchahogayabhai.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shashanksoni.kharchahogayabhai.domain.model.ImportBatchStatus
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource

@Entity(
    tableName = "import_batches",
    indices = [Index(value = ["imported_at"])],
)
data class ImportBatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "file_name")
    val fileName: String,
    val source: TransactionSource,
    @ColumnInfo(name = "imported_at")
    val importedAtMillis: Long,
    @ColumnInfo(name = "total_parsed")
    val totalParsed: Int,
    @ColumnInfo(name = "new_count")
    val newCount: Int,
    @ColumnInfo(name = "merged_count")
    val mergedCount: Int,
    @ColumnInfo(name = "duplicate_count")
    val duplicateCount: Int,
    @ColumnInfo(name = "failed_count")
    val failedCount: Int,
    val status: ImportBatchStatus,
    @ColumnInfo(name = "error_message")
    val errorMessage: String?,
)

package com.shashanksoni.kharchahogayabhai.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shashanksoni.kharchahogayabhai.core.database.dao.CategoryDao
import com.shashanksoni.kharchahogayabhai.core.database.dao.ImportBatchDao
import com.shashanksoni.kharchahogayabhai.core.database.dao.LabelDao
import com.shashanksoni.kharchahogayabhai.core.database.dao.TransactionDao
import com.shashanksoni.kharchahogayabhai.core.database.dao.TransactionSourceDao
import com.shashanksoni.kharchahogayabhai.core.database.entity.CategoryEntity
import com.shashanksoni.kharchahogayabhai.core.database.entity.ImportBatchEntity
import com.shashanksoni.kharchahogayabhai.core.database.entity.LabelEntity
import com.shashanksoni.kharchahogayabhai.core.database.entity.TransactionEntity
import com.shashanksoni.kharchahogayabhai.core.database.entity.TransactionLabelCrossRef
import com.shashanksoni.kharchahogayabhai.core.database.entity.TransactionSourceRecordEntity

@Database(
    entities = [
        TransactionEntity::class,
        TransactionSourceRecordEntity::class,
        CategoryEntity::class,
        ImportBatchEntity::class,
        LabelEntity::class,
        TransactionLabelCrossRef::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class KharchaDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    abstract fun transactionSourceDao(): TransactionSourceDao

    abstract fun categoryDao(): CategoryDao

    abstract fun importBatchDao(): ImportBatchDao

    abstract fun labelDao(): LabelDao

    companion object {
        const val NAME = "kharcha.db"
    }
}

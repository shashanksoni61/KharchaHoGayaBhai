package com.shashanksoni.kharchahogayabhai.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shashanksoni.kharchahogayabhai.core.database.dao.CategoryDao
import com.shashanksoni.kharchahogayabhai.core.database.dao.TransactionDao
import com.shashanksoni.kharchahogayabhai.core.database.dao.TransactionSourceDao
import com.shashanksoni.kharchahogayabhai.core.database.entity.CategoryEntity
import com.shashanksoni.kharchahogayabhai.core.database.entity.TransactionEntity
import com.shashanksoni.kharchahogayabhai.core.database.entity.TransactionSourceRecordEntity

/**
 * The single local store. All financial data stays on the device; there is no
 * backend and nothing is uploaded.
 *
 * Enums are persisted by name using Room's built-in support, and timestamps as
 * epoch milliseconds, so no type converters are needed. Schemas are exported to
 * `app/schemas` so future migrations are reviewable and testable.
 */
@Database(
    entities = [
        TransactionEntity::class,
        TransactionSourceRecordEntity::class,
        CategoryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class KharchaDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    abstract fun transactionSourceDao(): TransactionSourceDao

    abstract fun categoryDao(): CategoryDao

    companion object {
        const val NAME = "kharcha.db"
    }
}

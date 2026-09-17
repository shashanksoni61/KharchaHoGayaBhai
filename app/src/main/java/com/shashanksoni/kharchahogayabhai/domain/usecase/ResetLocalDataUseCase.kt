package com.shashanksoni.kharchahogayabhai.domain.usecase

import androidx.room.withTransaction
import com.shashanksoni.kharchahogayabhai.core.database.KharchaDatabase
import com.shashanksoni.kharchahogayabhai.core.database.dao.ImportBatchDao
import com.shashanksoni.kharchahogayabhai.core.database.dao.TransactionDao
import com.shashanksoni.kharchahogayabhai.sms.SmsScanPreferences

/**
 * Wipes transactions, sources, and import history so the dashboard starts empty.
 * Categories and labels (including custom ones) are kept — they are definitions,
 * not imported money data. The user can re-import CSV/PDF/SMS afterwards.
 */
class ResetLocalDataUseCase(
    private val database: KharchaDatabase,
    private val transactionDao: TransactionDao,
    private val importBatchDao: ImportBatchDao,
    private val smsScanPreferences: SmsScanPreferences,
) {

    suspend operator fun invoke() {
        database.withTransaction {
            transactionDao.deleteAllTransactions()
            importBatchDao.deleteAllBatches()
        }
        // Next SMS scan should read the full inbox again.
        smsScanPreferences.clearCursor()
    }
}

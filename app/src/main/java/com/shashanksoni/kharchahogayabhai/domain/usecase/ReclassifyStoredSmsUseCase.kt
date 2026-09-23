package com.shashanksoni.kharchahogayabhai.domain.usecase

import com.shashanksoni.kharchahogayabhai.domain.model.Transaction
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSourceRecord
import com.shashanksoni.kharchahogayabhai.domain.repository.TransactionRepository
import com.shashanksoni.kharchahogayabhai.sms.SmsRealityClassifier

data class ReclassifyStoredSmsResult(
    val scannedCount: Int,
    val hiddenCount: Int,
)

/**
 * Runs the on-device SMS model over rows already in the store and hides the
 * ones that look like limits, dues, EMI reminders or offers.
 *
 * Only currently-visible SMS-backed rows are touched. User-ignored rows and
 * already-promotional alerts stay as they are. Real payments are not flipped
 * back; this pass only unmarks junk that slipped through an older parser.
 */
class ReclassifyStoredSmsUseCase(
    private val transactionRepository: TransactionRepository,
    private val isNotAPayment: (body: String, address: String?) -> Boolean =
        SmsRealityClassifier::isNotAPayment,
) {

    suspend operator fun invoke(): ReclassifyStoredSmsResult {
        val transactions = transactionRepository.listVisibleSmsTransactions()
        val sourcesByTransactionId = transactionRepository.listSmsSourceRecords()
            .groupBy { it.transactionId }
        var hidden = 0
        for (transaction in transactions) {
            val smsSources = sourcesByTransactionId[transaction.id].orEmpty()
            val body = bodyToScore(transaction, smsSources)
            if (body.isBlank()) continue
            val address = smsSources.firstNotNullOfOrNull { it.originLabel }
            if (!isNotAPayment(body, address)) continue
            transactionRepository.setPromotional(transaction.id, promotional = true)
            hidden += 1
        }
        return ReclassifyStoredSmsResult(
            scannedCount = transactions.size,
            hiddenCount = hidden,
        )
    }

    private fun bodyToScore(
        transaction: Transaction,
        smsSources: List<TransactionSourceRecord>,
    ): String {
        smsSources.firstNotNullOfOrNull { source ->
            source.rawPayload?.trim()?.takeIf { it.isNotEmpty() }
        }?.let { return it }
        transaction.description?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        return listOfNotNull(
            transaction.merchantName,
            transaction.bankName,
            transaction.notes,
        ).joinToString(" ").trim()
    }
}

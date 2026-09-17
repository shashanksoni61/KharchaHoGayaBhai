package com.shashanksoni.kharchahogayabhai.data.local

import com.shashanksoni.kharchahogayabhai.core.database.dao.TransactionDao
import com.shashanksoni.kharchahogayabhai.core.database.dao.TransactionSourceDao
import com.shashanksoni.kharchahogayabhai.core.deduplication.TransactionMerger
import com.shashanksoni.kharchahogayabhai.core.normalization.TransactionNormalizer
import com.shashanksoni.kharchahogayabhai.data.local.mapper.toEntity
import com.shashanksoni.kharchahogayabhai.domain.model.DefaultCategories
import com.shashanksoni.kharchahogayabhai.domain.model.Money
import com.shashanksoni.kharchahogayabhai.domain.model.ParsedTransaction
import com.shashanksoni.kharchahogayabhai.domain.model.PaymentMethod
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

/**
 * Fills an empty database with believable transactions so the dashboard and list
 * can be built and reviewed before any real importer exists.
 *
 * The sample data is not inserted directly. Each payment is expressed as one
 * [ParsedTransaction] per source that "reported" it, complete with the
 * formatting differences real sources produce — an SMS says `Swiggy` with a time
 * of day, a CSV says `SWIGGY` with a date only, a PDF says
 * `UPI/SWIGGY/123456789012` — and then goes through the same normalise,
 * fingerprint and merge path a real import will use. So the three copies collapse
 * into one transaction backed by three source records, and the pipeline is
 * exercised rather than described.
 *
 * This is temporary scaffolding: it goes away once manual entry and importing can
 * produce real data.
 */
class SampleTransactionSeeder(
    private val transactionDao: TransactionDao,
    private val transactionSourceDao: TransactionSourceDao,
    private val normalizer: TransactionNormalizer,
    private val merger: TransactionMerger,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val clock: Clock = Clock.systemUTC(),
) {

    suspend fun seedIfEmpty() {
        if (transactionDao.countTransactions() > 0) return

        val currentMonth = YearMonth.now(clock.withZone(zone))
        samplePayments().forEachIndexed { index, payment ->
            storeDeduplicated(payment, index, currentMonth)
        }
    }

    private suspend fun storeDeduplicated(
        payment: SamplePayment,
        index: Int,
        currentMonth: YearMonth,
    ) {
        val normalized = payment.reportedBy.map { source ->
            normalizer.normalize(payment.asParsedTransaction(source, index, currentMonth))
        }

        // Copies of the same payment share a fingerprint, so grouping by it is the
        // deduplication decision; each group becomes one transaction that keeps
        // every source record.
        normalized.groupBy { it.transaction.fingerprint }.values.forEach { duplicates ->
            val merged = duplicates
                .map { it.transaction }
                .reduce(merger::merge)
                .copy(categoryId = payment.categoryId)

            val transactionId = transactionDao.insertTransaction(merged.toEntity())
            transactionSourceDao.insertSourceRecords(
                duplicates.map { it.sourceRecord.toEntity(transactionId) },
            )
        }
    }

    /**
     * One payment as a single source would have reported it, mimicking how much
     * each source actually knows and how it words things.
     */
    private fun SamplePayment.asParsedTransaction(
        source: TransactionSource,
        index: Int,
        currentMonth: YearMonth,
    ): ParsedTransaction {
        val month = currentMonth.plusMonths(monthOffset.toLong())
        val date = month.atDay(dayOfMonth.coerceAtMost(month.lengthOfMonth()))
        val sourceIdentifier = "sample-${source.name.lowercase()}-$index"
        val upperMerchant = merchant.uppercase()

        return when (source) {
            // An SMS knows the exact time and the payment method, and abbreviates the account.
            TransactionSource.SMS -> ParsedTransaction(
                amount = Money.fromMajorUnits(BigDecimal(amountMajor)),
                type = type,
                transactionDate = date.atTime(time).atZone(zone).toInstant(),
                merchantName = merchant,
                referenceNumber = reference?.let { "UPI Ref $it" },
                accountIdentifier = accountTail?.let { "XX$it" },
                bankName = bankName,
                paymentMethod = paymentMethod,
                source = source,
                sourceIdentifier = sourceIdentifier,
                originLabel = smsSender,
            )

            // A statement export knows the bank and account but usually only a date.
            TransactionSource.CSV -> ParsedTransaction(
                amount = Money.fromMajorUnits(BigDecimal(amountMajor)),
                type = type,
                transactionDate = date.atStartOfDay(zone).toInstant(),
                description = "UPI-$upperMerchant-${reference.orEmpty()}",
                merchantName = upperMerchant,
                referenceNumber = reference,
                accountIdentifier = accountTail?.let { "XXXXXXXX$it" },
                bankName = bankName,
                source = source,
                sourceIdentifier = sourceIdentifier,
                originLabel = "sample_statement.csv",
            )

            // A PDF statement carries the raw narration line.
            TransactionSource.PDF -> ParsedTransaction(
                amount = Money.fromMajorUnits(BigDecimal(amountMajor)),
                type = type,
                transactionDate = date.atStartOfDay(zone).toInstant(),
                description = "UPI/$upperMerchant/${reference.orEmpty()}/PAYMENT",
                merchantName = "UPI/$upperMerchant/${reference.orEmpty()}",
                referenceNumber = reference?.let { "UPI Ref No $it" },
                accountIdentifier = accountTail,
                bankName = bankName,
                source = source,
                sourceIdentifier = sourceIdentifier,
                originLabel = "Sample Statement.pdf",
            )

            TransactionSource.MANUAL -> ParsedTransaction(
                amount = Money.fromMajorUnits(BigDecimal(amountMajor)),
                type = type,
                transactionDate = date.atTime(time).atZone(zone).toInstant(),
                merchantName = merchant,
                paymentMethod = paymentMethod,
                source = source,
                sourceIdentifier = sourceIdentifier,
            )
        }
    }

    /**
     * A sample payment and which sources reported it. [monthOffset] is relative to
     * the current month, so the dashboard always has a month to compare against.
     */
    private data class SamplePayment(
        val merchant: String,
        val amountMajor: String,
        val type: TransactionType,
        val dayOfMonth: Int,
        val time: LocalTime,
        val categoryId: Long?,
        val paymentMethod: PaymentMethod,
        val reference: String?,
        val accountTail: String?,
        val bankName: String?,
        val smsSender: String?,
        val reportedBy: List<TransactionSource>,
        val monthOffset: Int = 0,
    )

    private fun samplePayments(): List<SamplePayment> = listOf(
        // Seen in all three sources: the case deduplication exists for.
        SamplePayment(
            merchant = "Swiggy",
            amountMajor = "540.00",
            type = TransactionType.DEBIT,
            dayOfMonth = 17,
            time = LocalTime.of(15, 14),
            categoryId = DefaultCategories.FOOD,
            paymentMethod = PaymentMethod.UPI,
            reference = "123456789012",
            accountTail = "1234",
            bankName = "HDFC Bank",
            smsSender = "HDFCBK",
            reportedBy = listOf(TransactionSource.SMS, TransactionSource.CSV, TransactionSource.PDF),
        ),
        SamplePayment(
            merchant = "Amazon",
            amountMajor = "1299.00",
            type = TransactionType.DEBIT,
            dayOfMonth = 16,
            time = LocalTime.of(20, 42),
            categoryId = DefaultCategories.SHOPPING,
            paymentMethod = PaymentMethod.UPI,
            reference = "451236987410",
            accountTail = "1234",
            bankName = "HDFC Bank",
            smsSender = "HDFCBK",
            reportedBy = listOf(TransactionSource.SMS, TransactionSource.CSV),
        ),
        SamplePayment(
            merchant = "Acme Software Salary",
            amountMajor = "95000.00",
            type = TransactionType.CREDIT,
            dayOfMonth = 1,
            time = LocalTime.of(9, 5),
            categoryId = DefaultCategories.SALARY,
            paymentMethod = PaymentMethod.NEFT,
            reference = "AXISN12398745612",
            accountTail = "1234",
            bankName = "HDFC Bank",
            smsSender = "HDFCBK",
            reportedBy = listOf(TransactionSource.SMS, TransactionSource.PDF),
        ),
        // No reference anywhere, so identity falls back to amount, day, merchant and account.
        SamplePayment(
            merchant = "Apollo Pharmacy",
            amountMajor = "1240.50",
            type = TransactionType.DEBIT,
            dayOfMonth = 12,
            time = LocalTime.of(19, 20),
            categoryId = DefaultCategories.HEALTHCARE,
            paymentMethod = PaymentMethod.CARD,
            reference = null,
            accountTail = "4321",
            bankName = "ICICI Bank",
            smsSender = "ICICIB",
            reportedBy = listOf(TransactionSource.SMS, TransactionSource.CSV),
        ),
        SamplePayment(
            merchant = "Landlord Rent",
            amountMajor = "18000.00",
            type = TransactionType.DEBIT,
            dayOfMonth = 3,
            time = LocalTime.of(11, 30),
            categoryId = DefaultCategories.RENT,
            paymentMethod = PaymentMethod.IMPS,
            reference = "IMPS784512369874",
            accountTail = "1234",
            bankName = "HDFC Bank",
            smsSender = "HDFCBK",
            reportedBy = listOf(TransactionSource.SMS, TransactionSource.CSV),
        ),
        SamplePayment(
            merchant = "Netflix",
            amountMajor = "649.00",
            type = TransactionType.DEBIT,
            dayOfMonth = 5,
            time = LocalTime.of(2, 15),
            categoryId = DefaultCategories.ENTERTAINMENT,
            paymentMethod = PaymentMethod.AUTO_DEBIT,
            reference = "329871456320",
            accountTail = "4321",
            bankName = "ICICI Bank",
            smsSender = "ICICIB",
            reportedBy = listOf(TransactionSource.SMS, TransactionSource.CSV),
        ),
        SamplePayment(
            merchant = "Groww SIP",
            amountMajor = "5000.00",
            type = TransactionType.DEBIT,
            dayOfMonth = 7,
            time = LocalTime.of(10, 0),
            categoryId = DefaultCategories.INVESTMENT,
            paymentMethod = PaymentMethod.AUTO_DEBIT,
            reference = "SIP9087654321",
            accountTail = "1234",
            bankName = "HDFC Bank",
            smsSender = "HDFCBK",
            reportedBy = listOf(TransactionSource.SMS, TransactionSource.CSV),
        ),
        SamplePayment(
            merchant = "Decathlon",
            amountMajor = "3499.00",
            type = TransactionType.DEBIT,
            dayOfMonth = 14,
            time = LocalTime.of(17, 55),
            categoryId = DefaultCategories.SHOPPING,
            paymentMethod = PaymentMethod.CARD,
            reference = "556677889900",
            accountTail = "4321",
            bankName = "ICICI Bank",
            smsSender = "ICICIB",
            reportedBy = listOf(TransactionSource.SMS, TransactionSource.PDF),
        ),
        // Single-source entries: the everyday case.
        SamplePayment(
            merchant = "Uber",
            amountMajor = "268.00",
            type = TransactionType.DEBIT,
            dayOfMonth = 17,
            time = LocalTime.of(9, 12),
            categoryId = DefaultCategories.TRANSPORT,
            paymentMethod = PaymentMethod.UPI,
            reference = "778899001122",
            accountTail = "1234",
            bankName = "HDFC Bank",
            smsSender = "HDFCBK",
            reportedBy = listOf(TransactionSource.SMS),
        ),
        SamplePayment(
            merchant = "Zomato",
            amountMajor = "432.00",
            type = TransactionType.DEBIT,
            dayOfMonth = 15,
            time = LocalTime.of(13, 40),
            categoryId = DefaultCategories.FOOD,
            paymentMethod = PaymentMethod.UPI,
            reference = "665544332211",
            accountTail = "1234",
            bankName = "HDFC Bank",
            smsSender = "HDFCBK",
            reportedBy = listOf(TransactionSource.SMS),
        ),
        SamplePayment(
            merchant = "Airtel Postpaid",
            amountMajor = "799.00",
            type = TransactionType.DEBIT,
            dayOfMonth = 9,
            time = LocalTime.of(21, 5),
            categoryId = DefaultCategories.BILLS,
            paymentMethod = PaymentMethod.UPI,
            reference = "334455667788",
            accountTail = "1234",
            bankName = "HDFC Bank",
            smsSender = "HDFCBK",
            reportedBy = listOf(TransactionSource.SMS),
        ),
        SamplePayment(
            merchant = "HDFC ATM Koramangala",
            amountMajor = "5000.00",
            type = TransactionType.DEBIT,
            dayOfMonth = 6,
            time = LocalTime.of(18, 30),
            categoryId = DefaultCategories.CASH_WITHDRAWAL,
            paymentMethod = PaymentMethod.ATM,
            reference = "ATM223344556677",
            accountTail = "1234",
            bankName = "HDFC Bank",
            smsSender = "HDFCBK",
            reportedBy = listOf(TransactionSource.SMS),
        ),
        SamplePayment(
            merchant = "BigBasket",
            amountMajor = "2150.75",
            type = TransactionType.DEBIT,
            dayOfMonth = 8,
            time = LocalTime.of(8, 20),
            categoryId = DefaultCategories.FOOD,
            paymentMethod = PaymentMethod.UPI,
            reference = "990011223344",
            accountTail = "1234",
            bankName = "HDFC Bank",
            smsSender = null,
            reportedBy = listOf(TransactionSource.CSV),
        ),
        SamplePayment(
            merchant = "IRCTC",
            amountMajor = "1885.00",
            type = TransactionType.DEBIT,
            dayOfMonth = 11,
            time = LocalTime.of(12, 0),
            categoryId = DefaultCategories.TRAVEL,
            paymentMethod = PaymentMethod.CARD,
            reference = "112233445566",
            accountTail = "4321",
            bankName = "ICICI Bank",
            smsSender = null,
            reportedBy = listOf(TransactionSource.PDF),
        ),
        SamplePayment(
            merchant = "Blinkit",
            amountMajor = "385.00",
            type = TransactionType.DEBIT,
            dayOfMonth = 13,
            time = LocalTime.of(22, 48),
            categoryId = DefaultCategories.FOOD,
            paymentMethod = PaymentMethod.UPI,
            reference = "445566778899",
            accountTail = "1234",
            bankName = "HDFC Bank",
            smsSender = "HDFCBK",
            reportedBy = listOf(TransactionSource.SMS),
        ),
        SamplePayment(
            merchant = "BESCOM Electricity",
            amountMajor = "2410.00",
            type = TransactionType.DEBIT,
            dayOfMonth = 10,
            time = LocalTime.of(19, 0),
            categoryId = DefaultCategories.BILLS,
            paymentMethod = PaymentMethod.UPI,
            reference = "220033445511",
            accountTail = "1234",
            bankName = "HDFC Bank",
            smsSender = null,
            reportedBy = listOf(TransactionSource.CSV),
        ),
        SamplePayment(
            merchant = "Cult Fit",
            amountMajor = "1499.00",
            type = TransactionType.DEBIT,
            dayOfMonth = 4,
            time = LocalTime.of(7, 15),
            categoryId = DefaultCategories.HEALTHCARE,
            paymentMethod = PaymentMethod.UPI,
            reference = "667788990011",
            accountTail = "1234",
            bankName = "HDFC Bank",
            smsSender = "HDFCBK",
            reportedBy = listOf(TransactionSource.SMS),
        ),
        SamplePayment(
            merchant = "Udemy",
            amountMajor = "649.00",
            type = TransactionType.DEBIT,
            dayOfMonth = 2,
            time = LocalTime.of(23, 5),
            categoryId = DefaultCategories.EDUCATION,
            paymentMethod = PaymentMethod.CARD,
            reference = "889900112233",
            accountTail = "4321",
            bankName = "ICICI Bank",
            smsSender = "ICICIB",
            reportedBy = listOf(TransactionSource.SMS),
        ),
        SamplePayment(
            merchant = "Rajesh Kumar",
            amountMajor = "3500.00",
            type = TransactionType.CREDIT,
            dayOfMonth = 15,
            time = LocalTime.of(16, 22),
            categoryId = DefaultCategories.TRANSFER,
            paymentMethod = PaymentMethod.UPI,
            reference = "121314151617",
            accountTail = "1234",
            bankName = "HDFC Bank",
            smsSender = "HDFCBK",
            reportedBy = listOf(TransactionSource.SMS),
        ),
        // Left uncategorised on purpose, so the dashboard shows that state.
        SamplePayment(
            merchant = "Sharma General Store",
            amountMajor = "620.00",
            type = TransactionType.DEBIT,
            dayOfMonth = 16,
            time = LocalTime.of(19, 48),
            categoryId = null,
            paymentMethod = PaymentMethod.UPI,
            reference = "181920212223",
            accountTail = "1234",
            bankName = "HDFC Bank",
            smsSender = "HDFCBK",
            reportedBy = listOf(TransactionSource.SMS),
        ),

        // Previous month, so month-on-month comparison has something to show.
        SamplePayment(
            merchant = "Acme Software Salary",
            amountMajor = "95000.00",
            type = TransactionType.CREDIT,
            dayOfMonth = 1,
            time = LocalTime.of(9, 8),
            categoryId = DefaultCategories.SALARY,
            paymentMethod = PaymentMethod.NEFT,
            reference = "AXISN99887766554",
            accountTail = "1234",
            bankName = "HDFC Bank",
            smsSender = "HDFCBK",
            reportedBy = listOf(TransactionSource.SMS),
            monthOffset = -1,
        ),
        SamplePayment(
            merchant = "Landlord Rent",
            amountMajor = "18000.00",
            type = TransactionType.DEBIT,
            dayOfMonth = 3,
            time = LocalTime.of(11, 12),
            categoryId = DefaultCategories.RENT,
            paymentMethod = PaymentMethod.IMPS,
            reference = "IMPS553311224466",
            accountTail = "1234",
            bankName = "HDFC Bank",
            smsSender = "HDFCBK",
            reportedBy = listOf(TransactionSource.SMS),
            monthOffset = -1,
        ),
        SamplePayment(
            merchant = "Swiggy",
            amountMajor = "720.00",
            type = TransactionType.DEBIT,
            dayOfMonth = 22,
            time = LocalTime.of(21, 30),
            categoryId = DefaultCategories.FOOD,
            paymentMethod = PaymentMethod.UPI,
            reference = "313233343536",
            accountTail = "1234",
            bankName = "HDFC Bank",
            smsSender = "HDFCBK",
            reportedBy = listOf(TransactionSource.SMS, TransactionSource.CSV),
            monthOffset = -1,
        ),
        SamplePayment(
            merchant = "Amazon",
            amountMajor = "4599.00",
            type = TransactionType.DEBIT,
            dayOfMonth = 18,
            time = LocalTime.of(14, 5),
            categoryId = DefaultCategories.SHOPPING,
            paymentMethod = PaymentMethod.CARD,
            reference = "414243444546",
            accountTail = "4321",
            bankName = "ICICI Bank",
            smsSender = "ICICIB",
            reportedBy = listOf(TransactionSource.SMS),
            monthOffset = -1,
        ),
        SamplePayment(
            merchant = "Ola",
            amountMajor = "310.00",
            type = TransactionType.DEBIT,
            dayOfMonth = 20,
            time = LocalTime.of(8, 45),
            categoryId = DefaultCategories.TRANSPORT,
            paymentMethod = PaymentMethod.UPI,
            reference = "515253545556",
            accountTail = "1234",
            bankName = "HDFC Bank",
            smsSender = "HDFCBK",
            reportedBy = listOf(TransactionSource.SMS),
            monthOffset = -1,
        ),
        SamplePayment(
            merchant = "PVR Cinemas",
            amountMajor = "1180.00",
            type = TransactionType.DEBIT,
            dayOfMonth = 24,
            time = LocalTime.of(18, 10),
            categoryId = DefaultCategories.ENTERTAINMENT,
            paymentMethod = PaymentMethod.CARD,
            reference = "616263646566",
            accountTail = "4321",
            bankName = "ICICI Bank",
            smsSender = "ICICIB",
            reportedBy = listOf(TransactionSource.SMS),
            monthOffset = -1,
        ),
        SamplePayment(
            merchant = "Airtel Postpaid",
            amountMajor = "799.00",
            type = TransactionType.DEBIT,
            dayOfMonth = 9,
            time = LocalTime.of(20, 15),
            categoryId = DefaultCategories.BILLS,
            paymentMethod = PaymentMethod.UPI,
            reference = "717273747576",
            accountTail = "1234",
            bankName = "HDFC Bank",
            smsSender = "HDFCBK",
            reportedBy = listOf(TransactionSource.SMS),
            monthOffset = -1,
        ),
    )
}

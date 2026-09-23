package com.shashanksoni.kharchahogayabhai.di

import android.content.Context
import androidx.room.Room
import com.shashanksoni.kharchahogayabhai.core.database.KharchaDatabase
import com.shashanksoni.kharchahogayabhai.core.database.KharchaMigrations
import com.shashanksoni.kharchahogayabhai.core.deduplication.TransactionIngestor
import com.shashanksoni.kharchahogayabhai.core.deduplication.TransactionMerger
import com.shashanksoni.kharchahogayabhai.core.normalization.TransactionNormalizer
import com.shashanksoni.kharchahogayabhai.csv.CsvTransactionParser
import com.shashanksoni.kharchahogayabhai.data.repository.RoomCategoryRepository
import com.shashanksoni.kharchahogayabhai.data.repository.RoomImportRepository
import com.shashanksoni.kharchahogayabhai.data.repository.RoomLabelRepository
import com.shashanksoni.kharchahogayabhai.data.repository.RoomTransactionRepository
import com.shashanksoni.kharchahogayabhai.domain.repository.CategoryRepository
import com.shashanksoni.kharchahogayabhai.domain.repository.ImportRepository
import com.shashanksoni.kharchahogayabhai.domain.repository.LabelRepository
import com.shashanksoni.kharchahogayabhai.domain.repository.TransactionRepository
import com.shashanksoni.kharchahogayabhai.domain.usecase.GetMonthlyDashboardUseCase
import com.shashanksoni.kharchahogayabhai.domain.usecase.ImportSmsInboxUseCase
import com.shashanksoni.kharchahogayabhai.domain.usecase.ImportStatementFileUseCase
import com.shashanksoni.kharchahogayabhai.domain.usecase.ResetLocalDataUseCase
import com.shashanksoni.kharchahogayabhai.pdf.PdfTextExtractor
import com.shashanksoni.kharchahogayabhai.pdf.PdfTransactionParser
import com.shashanksoni.kharchahogayabhai.sms.SmsInboxReader
import com.shashanksoni.kharchahogayabhai.sms.SmsScanPreferences
import com.shashanksoni.kharchahogayabhai.sms.SmsTransactionParser
import java.time.Clock
import java.time.ZoneId

/**
 * Wires the app together by hand.
 */
class AppContainer(context: Context) {

    private val applicationContext: Context = context.applicationContext
    private val clock: Clock = Clock.systemUTC()
    val zone: ZoneId = ZoneId.systemDefault()

    private val prefs by lazy {
        applicationContext.getSharedPreferences(
            RoomLabelRepository.PREFS_NAME,
            Context.MODE_PRIVATE,
        )
    }

    val smsScanPreferences: SmsScanPreferences by lazy {
        SmsScanPreferences(prefs)
    }

    private val database: KharchaDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            KharchaDatabase::class.java,
            KharchaDatabase.NAME,
        )
            .addMigrations(KharchaMigrations.MIGRATION_1_2, KharchaMigrations.MIGRATION_2_3)
            .build()
    }

    private val transactionNormalizer: TransactionNormalizer by lazy {
        TransactionNormalizer(zone = zone, clock = clock)
    }

    private val transactionMerger: TransactionMerger by lazy {
        TransactionMerger(zone = zone, clock = clock)
    }

    val transactionRepository: TransactionRepository by lazy {
        RoomTransactionRepository(transactionDao = database.transactionDao(), clock = clock)
    }

    val categoryRepository: CategoryRepository by lazy {
        RoomCategoryRepository(categoryDao = database.categoryDao())
    }

    val labelRepository: LabelRepository by lazy {
        RoomLabelRepository(
            labelDao = database.labelDao(),
            prefs = prefs,
        )
    }

    val importRepository: ImportRepository by lazy {
        RoomImportRepository(importBatchDao = database.importBatchDao())
    }

    val getMonthlyDashboard: GetMonthlyDashboardUseCase by lazy {
        GetMonthlyDashboardUseCase(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            zone = zone,
        )
    }

    val resetLocalData: ResetLocalDataUseCase by lazy {
        ResetLocalDataUseCase(
            database = database,
            transactionDao = database.transactionDao(),
            importBatchDao = database.importBatchDao(),
            smsScanPreferences = smsScanPreferences,
        )
    }

    private val transactionIngestor: TransactionIngestor by lazy {
        TransactionIngestor(
            database = database,
            transactionDao = database.transactionDao(),
            transactionSourceDao = database.transactionSourceDao(),
            normalizer = transactionNormalizer,
            merger = transactionMerger,
        )
    }

    val importStatementFile: ImportStatementFileUseCase by lazy {
        ImportStatementFileUseCase(
            appContext = applicationContext,
            csvParser = CsvTransactionParser(zone = zone),
            pdfParser = PdfTransactionParser(zone = zone),
            pdfTextExtractor = PdfTextExtractor(applicationContext),
            ingestor = transactionIngestor,
            importRepository = importRepository,
            transactionRepository = transactionRepository,
            clock = clock,
        )
    }

    val importSmsInbox: ImportSmsInboxUseCase by lazy {
        ImportSmsInboxUseCase(
            inboxReader = SmsInboxReader(applicationContext),
            smsParser = SmsTransactionParser(zone = zone),
            ingestor = transactionIngestor,
            importRepository = importRepository,
            transactionRepository = transactionRepository,
            scanPreferences = smsScanPreferences,
            clock = clock,
        )
    }

    suspend fun prepareLocalData() {
        categoryRepository.ensureDefaultCategoriesExist()
        labelRepository.ensureDefaultLabelsExist()
    }
}

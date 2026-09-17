package com.shashanksoni.kharchahogayabhai.di

import android.content.Context
import androidx.room.Room
import com.shashanksoni.kharchahogayabhai.core.database.KharchaDatabase
import com.shashanksoni.kharchahogayabhai.core.deduplication.TransactionMerger
import com.shashanksoni.kharchahogayabhai.core.normalization.TransactionNormalizer
import com.shashanksoni.kharchahogayabhai.data.local.SampleTransactionSeeder
import com.shashanksoni.kharchahogayabhai.data.repository.RoomCategoryRepository
import com.shashanksoni.kharchahogayabhai.data.repository.RoomTransactionRepository
import com.shashanksoni.kharchahogayabhai.domain.repository.CategoryRepository
import com.shashanksoni.kharchahogayabhai.domain.repository.TransactionRepository
import com.shashanksoni.kharchahogayabhai.domain.usecase.GetMonthlyDashboardUseCase
import java.time.Clock
import java.time.ZoneId

/**
 * Wires the app together by hand.
 *
 * There is no DI framework on purpose: the graph is small, every dependency is
 * constructed here in plain Kotlin, and nothing needs code generation or an extra
 * build plugin. If the graph grows past what is readable in one file, Hilt is the
 * next step.
 */
class AppContainer(context: Context) {

    private val applicationContext: Context = context.applicationContext

    /** UTC clock; local time only enters at display and calendar boundaries. */
    private val clock: Clock = Clock.systemUTC()

    /** Resolved once, so a mid-session timezone change cannot shift existing reports. */
    val zone: ZoneId = ZoneId.systemDefault()

    private val database: KharchaDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            KharchaDatabase::class.java,
            KharchaDatabase.NAME,
        ).build()
    }

    val transactionRepository: TransactionRepository by lazy {
        RoomTransactionRepository(transactionDao = database.transactionDao(), clock = clock)
    }

    val categoryRepository: CategoryRepository by lazy {
        RoomCategoryRepository(categoryDao = database.categoryDao())
    }

    val getMonthlyDashboard: GetMonthlyDashboardUseCase by lazy {
        GetMonthlyDashboardUseCase(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            zone = zone,
        )
    }

    private val sampleTransactionSeeder: SampleTransactionSeeder by lazy {
        SampleTransactionSeeder(
            transactionDao = database.transactionDao(),
            transactionSourceDao = database.transactionSourceDao(),
            normalizer = TransactionNormalizer(zone = zone, clock = clock),
            merger = TransactionMerger(zone = zone, clock = clock),
            zone = zone,
            clock = clock,
        )
    }

    /** Idempotent startup work: built-in categories, plus sample data while there is no importer. */
    suspend fun prepareLocalData() {
        categoryRepository.ensureDefaultCategoriesExist()
        sampleTransactionSeeder.seedIfEmpty()
    }
}

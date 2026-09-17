package com.shashanksoni.kharchahogayabhai.domain.repository

import com.shashanksoni.kharchahogayabhai.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    fun observeCategories(): Flow<List<Category>>

    /** Idempotent: adds any built-in category that is missing, renames nothing. */
    suspend fun ensureDefaultCategoriesExist()
}

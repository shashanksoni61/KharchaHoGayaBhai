package com.shashanksoni.kharchahogayabhai.data.repository

import com.shashanksoni.kharchahogayabhai.core.database.dao.CategoryDao
import com.shashanksoni.kharchahogayabhai.data.local.mapper.toDomain
import com.shashanksoni.kharchahogayabhai.data.local.mapper.toEntity
import com.shashanksoni.kharchahogayabhai.domain.model.Category
import com.shashanksoni.kharchahogayabhai.domain.model.DefaultCategories
import com.shashanksoni.kharchahogayabhai.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCategoryRepository(
    private val categoryDao: CategoryDao,
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeCategories().map { entities -> entities.map { it.toDomain() } }

    override suspend fun ensureDefaultCategoriesExist() {
        categoryDao.insertMissingCategories(DefaultCategories.all.map { it.toEntity() })
    }
}

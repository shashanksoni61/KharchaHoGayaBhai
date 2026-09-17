package com.shashanksoni.kharchahogayabhai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.shashanksoni.kharchahogayabhai.core.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY sort_order ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun findCategory(categoryId: Long): CategoryEntity?

    /** Seeding the built-in categories must not overwrite a user's renames. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissingCategories(categories: List<CategoryEntity>)

    @Upsert
    suspend fun upsertCategory(category: CategoryEntity)
}

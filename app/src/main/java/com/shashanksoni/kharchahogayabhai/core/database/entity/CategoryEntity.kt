package com.shashanksoni.kharchahogayabhai.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shashanksoni.kharchahogayabhai.domain.model.CategoryKind

/**
 * Ids are assigned by us, never auto-generated: the built-in categories have
 * fixed ids that categorisation rules and user data both point at.
 */
@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val kind: CategoryKind,
    @ColumnInfo(name = "color_hex")
    val colorHex: String,
    @ColumnInfo(name = "icon_key")
    val iconKey: String,
    @ColumnInfo(name = "is_system_defined")
    val isSystemDefined: Boolean,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
)

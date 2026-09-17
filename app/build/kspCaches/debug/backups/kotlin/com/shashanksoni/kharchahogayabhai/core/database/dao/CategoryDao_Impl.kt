package com.shashanksoni.kharchahogayabhai.core.database.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.EntityUpsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.shashanksoni.kharchahogayabhai.core.database.entity.CategoryEntity
import com.shashanksoni.kharchahogayabhai.domain.model.CategoryKind
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.IllegalArgumentException
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class CategoryDao_Impl(
  __db: RoomDatabase,
) : CategoryDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfCategoryEntity: EntityInsertAdapter<CategoryEntity>

  private val __upsertAdapterOfCategoryEntity: EntityUpsertAdapter<CategoryEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfCategoryEntity = object : EntityInsertAdapter<CategoryEntity>() {
      protected override fun createQuery(): String = "INSERT OR IGNORE INTO `categories` (`id`,`name`,`kind`,`color_hex`,`icon_key`,`is_system_defined`,`sort_order`) VALUES (?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CategoryEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, __CategoryKind_enumToString(entity.kind))
        statement.bindText(4, entity.colorHex)
        statement.bindText(5, entity.iconKey)
        val _tmp: Int = if (entity.isSystemDefined) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        statement.bindLong(7, entity.sortOrder.toLong())
      }
    }
    this.__upsertAdapterOfCategoryEntity = EntityUpsertAdapter<CategoryEntity>(object : EntityInsertAdapter<CategoryEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `categories` (`id`,`name`,`kind`,`color_hex`,`icon_key`,`is_system_defined`,`sort_order`) VALUES (?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CategoryEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, __CategoryKind_enumToString(entity.kind))
        statement.bindText(4, entity.colorHex)
        statement.bindText(5, entity.iconKey)
        val _tmp: Int = if (entity.isSystemDefined) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        statement.bindLong(7, entity.sortOrder.toLong())
      }
    }, object : EntityDeleteOrUpdateAdapter<CategoryEntity>() {
      protected override fun createQuery(): String = "UPDATE `categories` SET `id` = ?,`name` = ?,`kind` = ?,`color_hex` = ?,`icon_key` = ?,`is_system_defined` = ?,`sort_order` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: CategoryEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, __CategoryKind_enumToString(entity.kind))
        statement.bindText(4, entity.colorHex)
        statement.bindText(5, entity.iconKey)
        val _tmp: Int = if (entity.isSystemDefined) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        statement.bindLong(7, entity.sortOrder.toLong())
        statement.bindLong(8, entity.id)
      }
    })
  }

  public override suspend fun insertMissingCategories(categories: List<CategoryEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfCategoryEntity.insert(_connection, categories)
  }

  public override suspend fun upsertCategory(category: CategoryEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfCategoryEntity.upsert(_connection, category)
  }

  public override fun observeCategories(): Flow<List<CategoryEntity>> {
    val _sql: String = "SELECT * FROM categories ORDER BY sort_order ASC"
    return createFlow(__db, false, arrayOf("categories")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfKind: Int = getColumnIndexOrThrow(_stmt, "kind")
        val _columnIndexOfColorHex: Int = getColumnIndexOrThrow(_stmt, "color_hex")
        val _columnIndexOfIconKey: Int = getColumnIndexOrThrow(_stmt, "icon_key")
        val _columnIndexOfIsSystemDefined: Int = getColumnIndexOrThrow(_stmt, "is_system_defined")
        val _columnIndexOfSortOrder: Int = getColumnIndexOrThrow(_stmt, "sort_order")
        val _result: MutableList<CategoryEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CategoryEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpKind: CategoryKind
          _tmpKind = __CategoryKind_stringToEnum(_stmt.getText(_columnIndexOfKind))
          val _tmpColorHex: String
          _tmpColorHex = _stmt.getText(_columnIndexOfColorHex)
          val _tmpIconKey: String
          _tmpIconKey = _stmt.getText(_columnIndexOfIconKey)
          val _tmpIsSystemDefined: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsSystemDefined).toInt()
          _tmpIsSystemDefined = _tmp != 0
          val _tmpSortOrder: Int
          _tmpSortOrder = _stmt.getLong(_columnIndexOfSortOrder).toInt()
          _item = CategoryEntity(_tmpId,_tmpName,_tmpKind,_tmpColorHex,_tmpIconKey,_tmpIsSystemDefined,_tmpSortOrder)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun findCategory(categoryId: Long): CategoryEntity? {
    val _sql: String = "SELECT * FROM categories WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, categoryId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfKind: Int = getColumnIndexOrThrow(_stmt, "kind")
        val _columnIndexOfColorHex: Int = getColumnIndexOrThrow(_stmt, "color_hex")
        val _columnIndexOfIconKey: Int = getColumnIndexOrThrow(_stmt, "icon_key")
        val _columnIndexOfIsSystemDefined: Int = getColumnIndexOrThrow(_stmt, "is_system_defined")
        val _columnIndexOfSortOrder: Int = getColumnIndexOrThrow(_stmt, "sort_order")
        val _result: CategoryEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpKind: CategoryKind
          _tmpKind = __CategoryKind_stringToEnum(_stmt.getText(_columnIndexOfKind))
          val _tmpColorHex: String
          _tmpColorHex = _stmt.getText(_columnIndexOfColorHex)
          val _tmpIconKey: String
          _tmpIconKey = _stmt.getText(_columnIndexOfIconKey)
          val _tmpIsSystemDefined: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsSystemDefined).toInt()
          _tmpIsSystemDefined = _tmp != 0
          val _tmpSortOrder: Int
          _tmpSortOrder = _stmt.getLong(_columnIndexOfSortOrder).toInt()
          _result = CategoryEntity(_tmpId,_tmpName,_tmpKind,_tmpColorHex,_tmpIconKey,_tmpIsSystemDefined,_tmpSortOrder)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __CategoryKind_enumToString(_value: CategoryKind): String = when (_value) {
    CategoryKind.EXPENSE -> "EXPENSE"
    CategoryKind.INCOME -> "INCOME"
    CategoryKind.TRANSFER -> "TRANSFER"
  }

  private fun __CategoryKind_stringToEnum(_value: String): CategoryKind = when (_value) {
    "EXPENSE" -> CategoryKind.EXPENSE
    "INCOME" -> CategoryKind.INCOME
    "TRANSFER" -> CategoryKind.TRANSFER
    else -> throw IllegalArgumentException("Can't convert value to enum, unknown value: " + _value)
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}

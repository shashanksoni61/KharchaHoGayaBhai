package com.shashanksoni.kharchahogayabhai.core.database

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.shashanksoni.kharchahogayabhai.core.database.dao.CategoryDao
import com.shashanksoni.kharchahogayabhai.core.database.dao.CategoryDao_Impl
import com.shashanksoni.kharchahogayabhai.core.database.dao.TransactionDao
import com.shashanksoni.kharchahogayabhai.core.database.dao.TransactionDao_Impl
import com.shashanksoni.kharchahogayabhai.core.database.dao.TransactionSourceDao
import com.shashanksoni.kharchahogayabhai.core.database.dao.TransactionSourceDao_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class KharchaDatabase_Impl : KharchaDatabase() {
  private val _transactionDao: Lazy<TransactionDao> = lazy {
    TransactionDao_Impl(this)
  }

  private val _transactionSourceDao: Lazy<TransactionSourceDao> = lazy {
    TransactionSourceDao_Impl(this)
  }

  private val _categoryDao: Lazy<CategoryDao> = lazy {
    CategoryDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1, "72c034064098ab599a3af04b8a1d824f", "92abcb4779f6e005b55238c4655ec18e") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount_minor_units` INTEGER NOT NULL, `currency_code` TEXT NOT NULL, `type` TEXT NOT NULL, `transaction_date` INTEGER NOT NULL, `description` TEXT, `merchant_name` TEXT, `reference_number` TEXT, `normalized_reference` TEXT, `account_identifier` TEXT, `bank_name` TEXT, `payment_method` TEXT NOT NULL, `category_id` INTEGER, `primary_source` TEXT NOT NULL, `fingerprint` TEXT NOT NULL, `parse_status` TEXT NOT NULL, `notes` TEXT, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_fingerprint` ON `transactions` (`fingerprint`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_normalized_reference` ON `transactions` (`normalized_reference`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_transaction_date` ON `transactions` (`transaction_date`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_category_id` ON `transactions` (`category_id`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `transaction_sources` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `transaction_id` INTEGER NOT NULL, `source` TEXT NOT NULL, `source_identifier` TEXT, `origin_label` TEXT, `raw_payload` TEXT, `imported_at` INTEGER NOT NULL, FOREIGN KEY(`transaction_id`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_sources_transaction_id` ON `transaction_sources` (`transaction_id`)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transaction_sources_source_source_identifier` ON `transaction_sources` (`source`, `source_identifier`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `kind` TEXT NOT NULL, `color_hex` TEXT NOT NULL, `icon_key` TEXT NOT NULL, `is_system_defined` INTEGER NOT NULL, `sort_order` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '72c034064098ab599a3af04b8a1d824f')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `transactions`")
        connection.execSQL("DROP TABLE IF EXISTS `transaction_sources`")
        connection.execSQL("DROP TABLE IF EXISTS `categories`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON")
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsTransactions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTransactions.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("amount_minor_units", TableInfo.Column("amount_minor_units", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("currency_code", TableInfo.Column("currency_code", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("type", TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("transaction_date", TableInfo.Column("transaction_date", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("description", TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("merchant_name", TableInfo.Column("merchant_name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("reference_number", TableInfo.Column("reference_number", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("normalized_reference", TableInfo.Column("normalized_reference", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("account_identifier", TableInfo.Column("account_identifier", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("bank_name", TableInfo.Column("bank_name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("payment_method", TableInfo.Column("payment_method", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("category_id", TableInfo.Column("category_id", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("primary_source", TableInfo.Column("primary_source", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("fingerprint", TableInfo.Column("fingerprint", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("parse_status", TableInfo.Column("parse_status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("notes", TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("created_at", TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("updated_at", TableInfo.Column("updated_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTransactions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysTransactions.add(TableInfo.ForeignKey("categories", "SET NULL", "NO ACTION", listOf("category_id"), listOf("id")))
        val _indicesTransactions: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesTransactions.add(TableInfo.Index("index_transactions_fingerprint", true, listOf("fingerprint"), listOf("ASC")))
        _indicesTransactions.add(TableInfo.Index("index_transactions_normalized_reference", false, listOf("normalized_reference"), listOf("ASC")))
        _indicesTransactions.add(TableInfo.Index("index_transactions_transaction_date", false, listOf("transaction_date"), listOf("ASC")))
        _indicesTransactions.add(TableInfo.Index("index_transactions_category_id", false, listOf("category_id"), listOf("ASC")))
        val _infoTransactions: TableInfo = TableInfo("transactions", _columnsTransactions, _foreignKeysTransactions, _indicesTransactions)
        val _existingTransactions: TableInfo = read(connection, "transactions")
        if (!_infoTransactions.equals(_existingTransactions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |transactions(com.shashanksoni.kharchahogayabhai.core.database.entity.TransactionEntity).
              | Expected:
              |""".trimMargin() + _infoTransactions + """
              |
              | Found:
              |""".trimMargin() + _existingTransactions)
        }
        val _columnsTransactionSources: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTransactionSources.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactionSources.put("transaction_id", TableInfo.Column("transaction_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactionSources.put("source", TableInfo.Column("source", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactionSources.put("source_identifier", TableInfo.Column("source_identifier", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactionSources.put("origin_label", TableInfo.Column("origin_label", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactionSources.put("raw_payload", TableInfo.Column("raw_payload", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactionSources.put("imported_at", TableInfo.Column("imported_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTransactionSources: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysTransactionSources.add(TableInfo.ForeignKey("transactions", "CASCADE", "NO ACTION", listOf("transaction_id"), listOf("id")))
        val _indicesTransactionSources: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesTransactionSources.add(TableInfo.Index("index_transaction_sources_transaction_id", false, listOf("transaction_id"), listOf("ASC")))
        _indicesTransactionSources.add(TableInfo.Index("index_transaction_sources_source_source_identifier", true, listOf("source", "source_identifier"), listOf("ASC", "ASC")))
        val _infoTransactionSources: TableInfo = TableInfo("transaction_sources", _columnsTransactionSources, _foreignKeysTransactionSources, _indicesTransactionSources)
        val _existingTransactionSources: TableInfo = read(connection, "transaction_sources")
        if (!_infoTransactionSources.equals(_existingTransactionSources)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |transaction_sources(com.shashanksoni.kharchahogayabhai.core.database.entity.TransactionSourceRecordEntity).
              | Expected:
              |""".trimMargin() + _infoTransactionSources + """
              |
              | Found:
              |""".trimMargin() + _existingTransactionSources)
        }
        val _columnsCategories: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCategories.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("kind", TableInfo.Column("kind", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("color_hex", TableInfo.Column("color_hex", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("icon_key", TableInfo.Column("icon_key", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("is_system_defined", TableInfo.Column("is_system_defined", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("sort_order", TableInfo.Column("sort_order", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCategories: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCategories: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesCategories.add(TableInfo.Index("index_categories_name", true, listOf("name"), listOf("ASC")))
        val _infoCategories: TableInfo = TableInfo("categories", _columnsCategories, _foreignKeysCategories, _indicesCategories)
        val _existingCategories: TableInfo = read(connection, "categories")
        if (!_infoCategories.equals(_existingCategories)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |categories(com.shashanksoni.kharchahogayabhai.core.database.entity.CategoryEntity).
              | Expected:
              |""".trimMargin() + _infoCategories + """
              |
              | Found:
              |""".trimMargin() + _existingCategories)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "transactions", "transaction_sources", "categories")
  }

  public override fun clearAllTables() {
    super.performClear(true, "transactions", "transaction_sources", "categories")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(TransactionDao::class, TransactionDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(TransactionSourceDao::class, TransactionSourceDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(CategoryDao::class, CategoryDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun transactionDao(): TransactionDao = _transactionDao.value

  public override fun transactionSourceDao(): TransactionSourceDao = _transactionSourceDao.value

  public override fun categoryDao(): CategoryDao = _categoryDao.value
}

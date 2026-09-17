package com.shashanksoni.kharchahogayabhai.core.database.dao

import androidx.collection.LongSparseArray
import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndex
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.room.util.recursiveFetchLongSparseArray
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import com.shashanksoni.kharchahogayabhai.core.database.entity.CategoryEntity
import com.shashanksoni.kharchahogayabhai.core.database.entity.TransactionEntity
import com.shashanksoni.kharchahogayabhai.core.database.entity.TransactionSourceRecordEntity
import com.shashanksoni.kharchahogayabhai.core.database.projection.TransactionSummaryProjection
import com.shashanksoni.kharchahogayabhai.core.database.relation.TransactionWithDetails
import com.shashanksoni.kharchahogayabhai.domain.model.CategoryKind
import com.shashanksoni.kharchahogayabhai.domain.model.ParseStatus
import com.shashanksoni.kharchahogayabhai.domain.model.PaymentMethod
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
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
import kotlin.text.StringBuilder
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class TransactionDao_Impl(
  __db: RoomDatabase,
) : TransactionDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfTransactionEntity: EntityInsertAdapter<TransactionEntity>

  private val __updateAdapterOfTransactionEntity: EntityDeleteOrUpdateAdapter<TransactionEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfTransactionEntity = object : EntityInsertAdapter<TransactionEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `transactions` (`id`,`amount_minor_units`,`currency_code`,`type`,`transaction_date`,`description`,`merchant_name`,`reference_number`,`normalized_reference`,`account_identifier`,`bank_name`,`payment_method`,`category_id`,`primary_source`,`fingerprint`,`parse_status`,`notes`,`created_at`,`updated_at`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TransactionEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.amountMinorUnits)
        statement.bindText(3, entity.currencyCode)
        statement.bindText(4, __TransactionType_enumToString(entity.type))
        statement.bindLong(5, entity.transactionDateMillis)
        val _tmpDescription: String? = entity.description
        if (_tmpDescription == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpDescription)
        }
        val _tmpMerchantName: String? = entity.merchantName
        if (_tmpMerchantName == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpMerchantName)
        }
        val _tmpReferenceNumber: String? = entity.referenceNumber
        if (_tmpReferenceNumber == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpReferenceNumber)
        }
        val _tmpNormalizedReference: String? = entity.normalizedReference
        if (_tmpNormalizedReference == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpNormalizedReference)
        }
        val _tmpAccountIdentifier: String? = entity.accountIdentifier
        if (_tmpAccountIdentifier == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpAccountIdentifier)
        }
        val _tmpBankName: String? = entity.bankName
        if (_tmpBankName == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpBankName)
        }
        statement.bindText(12, __PaymentMethod_enumToString(entity.paymentMethod))
        val _tmpCategoryId: Long? = entity.categoryId
        if (_tmpCategoryId == null) {
          statement.bindNull(13)
        } else {
          statement.bindLong(13, _tmpCategoryId)
        }
        statement.bindText(14, __TransactionSource_enumToString(entity.primarySource))
        statement.bindText(15, entity.fingerprint)
        statement.bindText(16, __ParseStatus_enumToString(entity.parseStatus))
        val _tmpNotes: String? = entity.notes
        if (_tmpNotes == null) {
          statement.bindNull(17)
        } else {
          statement.bindText(17, _tmpNotes)
        }
        statement.bindLong(18, entity.createdAtMillis)
        statement.bindLong(19, entity.updatedAtMillis)
      }
    }
    this.__updateAdapterOfTransactionEntity = object : EntityDeleteOrUpdateAdapter<TransactionEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `transactions` SET `id` = ?,`amount_minor_units` = ?,`currency_code` = ?,`type` = ?,`transaction_date` = ?,`description` = ?,`merchant_name` = ?,`reference_number` = ?,`normalized_reference` = ?,`account_identifier` = ?,`bank_name` = ?,`payment_method` = ?,`category_id` = ?,`primary_source` = ?,`fingerprint` = ?,`parse_status` = ?,`notes` = ?,`created_at` = ?,`updated_at` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: TransactionEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.amountMinorUnits)
        statement.bindText(3, entity.currencyCode)
        statement.bindText(4, __TransactionType_enumToString(entity.type))
        statement.bindLong(5, entity.transactionDateMillis)
        val _tmpDescription: String? = entity.description
        if (_tmpDescription == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpDescription)
        }
        val _tmpMerchantName: String? = entity.merchantName
        if (_tmpMerchantName == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpMerchantName)
        }
        val _tmpReferenceNumber: String? = entity.referenceNumber
        if (_tmpReferenceNumber == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpReferenceNumber)
        }
        val _tmpNormalizedReference: String? = entity.normalizedReference
        if (_tmpNormalizedReference == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpNormalizedReference)
        }
        val _tmpAccountIdentifier: String? = entity.accountIdentifier
        if (_tmpAccountIdentifier == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpAccountIdentifier)
        }
        val _tmpBankName: String? = entity.bankName
        if (_tmpBankName == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpBankName)
        }
        statement.bindText(12, __PaymentMethod_enumToString(entity.paymentMethod))
        val _tmpCategoryId: Long? = entity.categoryId
        if (_tmpCategoryId == null) {
          statement.bindNull(13)
        } else {
          statement.bindLong(13, _tmpCategoryId)
        }
        statement.bindText(14, __TransactionSource_enumToString(entity.primarySource))
        statement.bindText(15, entity.fingerprint)
        statement.bindText(16, __ParseStatus_enumToString(entity.parseStatus))
        val _tmpNotes: String? = entity.notes
        if (_tmpNotes == null) {
          statement.bindNull(17)
        } else {
          statement.bindText(17, _tmpNotes)
        }
        statement.bindLong(18, entity.createdAtMillis)
        statement.bindLong(19, entity.updatedAtMillis)
        statement.bindLong(20, entity.id)
      }
    }
  }

  public override suspend fun insertTransaction(transaction: TransactionEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfTransactionEntity.insertAndReturnId(_connection, transaction)
    _result
  }

  public override suspend fun updateTransaction(transaction: TransactionEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfTransactionEntity.handle(_connection, transaction)
  }

  public override fun observeTransactions(
    startMillis: Long?,
    endMillisExclusive: Long?,
    type: String?,
    source: String?,
    accountIdentifier: String?,
    filterByCategory: Int,
    categoryIds: List<Long>,
    searchQuery: String?,
  ): Flow<List<TransactionEntity>> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("        SELECT * FROM transactions")
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("        WHERE (")
    _stringBuilder.append("?")
    _stringBuilder.append(" IS NULL OR transaction_date >= ")
    _stringBuilder.append("?")
    _stringBuilder.append(")")
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("          AND (")
    _stringBuilder.append("?")
    _stringBuilder.append(" IS NULL OR transaction_date < ")
    _stringBuilder.append("?")
    _stringBuilder.append(")")
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("          AND (")
    _stringBuilder.append("?")
    _stringBuilder.append(" IS NULL OR type = ")
    _stringBuilder.append("?")
    _stringBuilder.append(")")
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("          AND (")
    _stringBuilder.append("?")
    _stringBuilder.append(" IS NULL OR account_identifier = ")
    _stringBuilder.append("?")
    _stringBuilder.append(")")
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("          AND (")
    _stringBuilder.append("?")
    _stringBuilder.append(" = 0 OR category_id IN (")
    val _inputSize: Int = categoryIds.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append("))")
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("          AND (")
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("            ")
    _stringBuilder.append("?")
    _stringBuilder.append(" IS NULL")
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("            OR merchant_name LIKE '%' || ")
    _stringBuilder.append("?")
    _stringBuilder.append(" || '%'")
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("            OR description LIKE '%' || ")
    _stringBuilder.append("?")
    _stringBuilder.append(" || '%'")
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("            OR notes LIKE '%' || ")
    _stringBuilder.append("?")
    _stringBuilder.append(" || '%'")
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("            OR reference_number LIKE '%' || ")
    _stringBuilder.append("?")
    _stringBuilder.append(" || '%'")
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("            OR bank_name LIKE '%' || ")
    _stringBuilder.append("?")
    _stringBuilder.append(" || '%'")
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("          )")
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("          AND (")
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("            ")
    _stringBuilder.append("?")
    _stringBuilder.append(" IS NULL")
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("            OR EXISTS (")
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("              SELECT 1 FROM transaction_sources")
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("              WHERE transaction_sources.transaction_id = transactions.id")
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("                AND transaction_sources.source = ")
    _stringBuilder.append("?")
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("            )")
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("          )")
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("        ORDER BY transaction_date DESC, id DESC")
    _stringBuilder.append("""
        |
        |""".trimMargin())
    _stringBuilder.append("        ")
    val _sql: String = _stringBuilder.toString()
    return createFlow(__db, false, arrayOf("transactions", "transaction_sources")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        if (startMillis == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindLong(_argIndex, startMillis)
        }
        _argIndex = 2
        if (startMillis == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindLong(_argIndex, startMillis)
        }
        _argIndex = 3
        if (endMillisExclusive == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindLong(_argIndex, endMillisExclusive)
        }
        _argIndex = 4
        if (endMillisExclusive == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindLong(_argIndex, endMillisExclusive)
        }
        _argIndex = 5
        if (type == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, type)
        }
        _argIndex = 6
        if (type == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, type)
        }
        _argIndex = 7
        if (accountIdentifier == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, accountIdentifier)
        }
        _argIndex = 8
        if (accountIdentifier == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, accountIdentifier)
        }
        _argIndex = 9
        _stmt.bindLong(_argIndex, filterByCategory.toLong())
        _argIndex = 10
        for (_item: Long in categoryIds) {
          _stmt.bindLong(_argIndex, _item)
          _argIndex++
        }
        _argIndex = 10 + _inputSize
        if (searchQuery == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, searchQuery)
        }
        _argIndex = 11 + _inputSize
        if (searchQuery == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, searchQuery)
        }
        _argIndex = 12 + _inputSize
        if (searchQuery == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, searchQuery)
        }
        _argIndex = 13 + _inputSize
        if (searchQuery == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, searchQuery)
        }
        _argIndex = 14 + _inputSize
        if (searchQuery == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, searchQuery)
        }
        _argIndex = 15 + _inputSize
        if (searchQuery == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, searchQuery)
        }
        _argIndex = 16 + _inputSize
        if (source == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, source)
        }
        _argIndex = 17 + _inputSize
        if (source == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, source)
        }
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfAmountMinorUnits: Int = getColumnIndexOrThrow(_stmt, "amount_minor_units")
        val _columnIndexOfCurrencyCode: Int = getColumnIndexOrThrow(_stmt, "currency_code")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfTransactionDateMillis: Int = getColumnIndexOrThrow(_stmt, "transaction_date")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfMerchantName: Int = getColumnIndexOrThrow(_stmt, "merchant_name")
        val _columnIndexOfReferenceNumber: Int = getColumnIndexOrThrow(_stmt, "reference_number")
        val _columnIndexOfNormalizedReference: Int = getColumnIndexOrThrow(_stmt, "normalized_reference")
        val _columnIndexOfAccountIdentifier: Int = getColumnIndexOrThrow(_stmt, "account_identifier")
        val _columnIndexOfBankName: Int = getColumnIndexOrThrow(_stmt, "bank_name")
        val _columnIndexOfPaymentMethod: Int = getColumnIndexOrThrow(_stmt, "payment_method")
        val _columnIndexOfCategoryId: Int = getColumnIndexOrThrow(_stmt, "category_id")
        val _columnIndexOfPrimarySource: Int = getColumnIndexOrThrow(_stmt, "primary_source")
        val _columnIndexOfFingerprint: Int = getColumnIndexOrThrow(_stmt, "fingerprint")
        val _columnIndexOfParseStatus: Int = getColumnIndexOrThrow(_stmt, "parse_status")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfCreatedAtMillis: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _columnIndexOfUpdatedAtMillis: Int = getColumnIndexOrThrow(_stmt, "updated_at")
        val _result: MutableList<TransactionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item_1: TransactionEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpAmountMinorUnits: Long
          _tmpAmountMinorUnits = _stmt.getLong(_columnIndexOfAmountMinorUnits)
          val _tmpCurrencyCode: String
          _tmpCurrencyCode = _stmt.getText(_columnIndexOfCurrencyCode)
          val _tmpType: TransactionType
          _tmpType = __TransactionType_stringToEnum(_stmt.getText(_columnIndexOfType))
          val _tmpTransactionDateMillis: Long
          _tmpTransactionDateMillis = _stmt.getLong(_columnIndexOfTransactionDateMillis)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpMerchantName: String?
          if (_stmt.isNull(_columnIndexOfMerchantName)) {
            _tmpMerchantName = null
          } else {
            _tmpMerchantName = _stmt.getText(_columnIndexOfMerchantName)
          }
          val _tmpReferenceNumber: String?
          if (_stmt.isNull(_columnIndexOfReferenceNumber)) {
            _tmpReferenceNumber = null
          } else {
            _tmpReferenceNumber = _stmt.getText(_columnIndexOfReferenceNumber)
          }
          val _tmpNormalizedReference: String?
          if (_stmt.isNull(_columnIndexOfNormalizedReference)) {
            _tmpNormalizedReference = null
          } else {
            _tmpNormalizedReference = _stmt.getText(_columnIndexOfNormalizedReference)
          }
          val _tmpAccountIdentifier: String?
          if (_stmt.isNull(_columnIndexOfAccountIdentifier)) {
            _tmpAccountIdentifier = null
          } else {
            _tmpAccountIdentifier = _stmt.getText(_columnIndexOfAccountIdentifier)
          }
          val _tmpBankName: String?
          if (_stmt.isNull(_columnIndexOfBankName)) {
            _tmpBankName = null
          } else {
            _tmpBankName = _stmt.getText(_columnIndexOfBankName)
          }
          val _tmpPaymentMethod: PaymentMethod
          _tmpPaymentMethod = __PaymentMethod_stringToEnum(_stmt.getText(_columnIndexOfPaymentMethod))
          val _tmpCategoryId: Long?
          if (_stmt.isNull(_columnIndexOfCategoryId)) {
            _tmpCategoryId = null
          } else {
            _tmpCategoryId = _stmt.getLong(_columnIndexOfCategoryId)
          }
          val _tmpPrimarySource: TransactionSource
          _tmpPrimarySource = __TransactionSource_stringToEnum(_stmt.getText(_columnIndexOfPrimarySource))
          val _tmpFingerprint: String
          _tmpFingerprint = _stmt.getText(_columnIndexOfFingerprint)
          val _tmpParseStatus: ParseStatus
          _tmpParseStatus = __ParseStatus_stringToEnum(_stmt.getText(_columnIndexOfParseStatus))
          val _tmpNotes: String?
          if (_stmt.isNull(_columnIndexOfNotes)) {
            _tmpNotes = null
          } else {
            _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          }
          val _tmpCreatedAtMillis: Long
          _tmpCreatedAtMillis = _stmt.getLong(_columnIndexOfCreatedAtMillis)
          val _tmpUpdatedAtMillis: Long
          _tmpUpdatedAtMillis = _stmt.getLong(_columnIndexOfUpdatedAtMillis)
          _item_1 = TransactionEntity(_tmpId,_tmpAmountMinorUnits,_tmpCurrencyCode,_tmpType,_tmpTransactionDateMillis,_tmpDescription,_tmpMerchantName,_tmpReferenceNumber,_tmpNormalizedReference,_tmpAccountIdentifier,_tmpBankName,_tmpPaymentMethod,_tmpCategoryId,_tmpPrimarySource,_tmpFingerprint,_tmpParseStatus,_tmpNotes,_tmpCreatedAtMillis,_tmpUpdatedAtMillis)
          _result.add(_item_1)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeTransactionWithDetails(transactionId: Long): Flow<TransactionWithDetails?> {
    val _sql: String = "SELECT * FROM transactions WHERE id = ?"
    return createFlow(__db, true, arrayOf("categories", "transaction_sources", "transactions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, transactionId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfAmountMinorUnits: Int = getColumnIndexOrThrow(_stmt, "amount_minor_units")
        val _columnIndexOfCurrencyCode: Int = getColumnIndexOrThrow(_stmt, "currency_code")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfTransactionDateMillis: Int = getColumnIndexOrThrow(_stmt, "transaction_date")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfMerchantName: Int = getColumnIndexOrThrow(_stmt, "merchant_name")
        val _columnIndexOfReferenceNumber: Int = getColumnIndexOrThrow(_stmt, "reference_number")
        val _columnIndexOfNormalizedReference: Int = getColumnIndexOrThrow(_stmt, "normalized_reference")
        val _columnIndexOfAccountIdentifier: Int = getColumnIndexOrThrow(_stmt, "account_identifier")
        val _columnIndexOfBankName: Int = getColumnIndexOrThrow(_stmt, "bank_name")
        val _columnIndexOfPaymentMethod: Int = getColumnIndexOrThrow(_stmt, "payment_method")
        val _columnIndexOfCategoryId: Int = getColumnIndexOrThrow(_stmt, "category_id")
        val _columnIndexOfPrimarySource: Int = getColumnIndexOrThrow(_stmt, "primary_source")
        val _columnIndexOfFingerprint: Int = getColumnIndexOrThrow(_stmt, "fingerprint")
        val _columnIndexOfParseStatus: Int = getColumnIndexOrThrow(_stmt, "parse_status")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfCreatedAtMillis: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _columnIndexOfUpdatedAtMillis: Int = getColumnIndexOrThrow(_stmt, "updated_at")
        val _collectionCategory: LongSparseArray<CategoryEntity?> = LongSparseArray<CategoryEntity?>()
        val _collectionSources: LongSparseArray<MutableList<TransactionSourceRecordEntity>> = LongSparseArray<MutableList<TransactionSourceRecordEntity>>()
        while (_stmt.step()) {
          val _tmpKey: Long?
          if (_stmt.isNull(_columnIndexOfCategoryId)) {
            _tmpKey = null
          } else {
            _tmpKey = _stmt.getLong(_columnIndexOfCategoryId)
          }
          if (_tmpKey != null) {
            _collectionCategory.put(_tmpKey, null)
          }
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfId)
          if (!_collectionSources.containsKey(_tmpKey_1)) {
            _collectionSources.put(_tmpKey_1, mutableListOf())
          }
        }
        _stmt.reset()
        __fetchRelationshipcategoriesAscomShashanksoniKharchahogayabhaiCoreDatabaseEntityCategoryEntity(_connection, _collectionCategory)
        __fetchRelationshiptransactionSourcesAscomShashanksoniKharchahogayabhaiCoreDatabaseEntityTransactionSourceRecordEntity(_connection, _collectionSources)
        val _result: TransactionWithDetails?
        if (_stmt.step()) {
          val _tmpTransaction: TransactionEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpAmountMinorUnits: Long
          _tmpAmountMinorUnits = _stmt.getLong(_columnIndexOfAmountMinorUnits)
          val _tmpCurrencyCode: String
          _tmpCurrencyCode = _stmt.getText(_columnIndexOfCurrencyCode)
          val _tmpType: TransactionType
          _tmpType = __TransactionType_stringToEnum(_stmt.getText(_columnIndexOfType))
          val _tmpTransactionDateMillis: Long
          _tmpTransactionDateMillis = _stmt.getLong(_columnIndexOfTransactionDateMillis)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpMerchantName: String?
          if (_stmt.isNull(_columnIndexOfMerchantName)) {
            _tmpMerchantName = null
          } else {
            _tmpMerchantName = _stmt.getText(_columnIndexOfMerchantName)
          }
          val _tmpReferenceNumber: String?
          if (_stmt.isNull(_columnIndexOfReferenceNumber)) {
            _tmpReferenceNumber = null
          } else {
            _tmpReferenceNumber = _stmt.getText(_columnIndexOfReferenceNumber)
          }
          val _tmpNormalizedReference: String?
          if (_stmt.isNull(_columnIndexOfNormalizedReference)) {
            _tmpNormalizedReference = null
          } else {
            _tmpNormalizedReference = _stmt.getText(_columnIndexOfNormalizedReference)
          }
          val _tmpAccountIdentifier: String?
          if (_stmt.isNull(_columnIndexOfAccountIdentifier)) {
            _tmpAccountIdentifier = null
          } else {
            _tmpAccountIdentifier = _stmt.getText(_columnIndexOfAccountIdentifier)
          }
          val _tmpBankName: String?
          if (_stmt.isNull(_columnIndexOfBankName)) {
            _tmpBankName = null
          } else {
            _tmpBankName = _stmt.getText(_columnIndexOfBankName)
          }
          val _tmpPaymentMethod: PaymentMethod
          _tmpPaymentMethod = __PaymentMethod_stringToEnum(_stmt.getText(_columnIndexOfPaymentMethod))
          val _tmpCategoryId: Long?
          if (_stmt.isNull(_columnIndexOfCategoryId)) {
            _tmpCategoryId = null
          } else {
            _tmpCategoryId = _stmt.getLong(_columnIndexOfCategoryId)
          }
          val _tmpPrimarySource: TransactionSource
          _tmpPrimarySource = __TransactionSource_stringToEnum(_stmt.getText(_columnIndexOfPrimarySource))
          val _tmpFingerprint: String
          _tmpFingerprint = _stmt.getText(_columnIndexOfFingerprint)
          val _tmpParseStatus: ParseStatus
          _tmpParseStatus = __ParseStatus_stringToEnum(_stmt.getText(_columnIndexOfParseStatus))
          val _tmpNotes: String?
          if (_stmt.isNull(_columnIndexOfNotes)) {
            _tmpNotes = null
          } else {
            _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          }
          val _tmpCreatedAtMillis: Long
          _tmpCreatedAtMillis = _stmt.getLong(_columnIndexOfCreatedAtMillis)
          val _tmpUpdatedAtMillis: Long
          _tmpUpdatedAtMillis = _stmt.getLong(_columnIndexOfUpdatedAtMillis)
          _tmpTransaction = TransactionEntity(_tmpId,_tmpAmountMinorUnits,_tmpCurrencyCode,_tmpType,_tmpTransactionDateMillis,_tmpDescription,_tmpMerchantName,_tmpReferenceNumber,_tmpNormalizedReference,_tmpAccountIdentifier,_tmpBankName,_tmpPaymentMethod,_tmpCategoryId,_tmpPrimarySource,_tmpFingerprint,_tmpParseStatus,_tmpNotes,_tmpCreatedAtMillis,_tmpUpdatedAtMillis)
          val _tmpCategory: CategoryEntity?
          val _tmpKey_2: Long?
          if (_stmt.isNull(_columnIndexOfCategoryId)) {
            _tmpKey_2 = null
          } else {
            _tmpKey_2 = _stmt.getLong(_columnIndexOfCategoryId)
          }
          if (_tmpKey_2 != null) {
            _tmpCategory = _collectionCategory.get(_tmpKey_2)
          } else {
            _tmpCategory = null
          }
          val _tmpSourcesCollection: MutableList<TransactionSourceRecordEntity>
          val _tmpKey_3: Long
          _tmpKey_3 = _stmt.getLong(_columnIndexOfId)
          _tmpSourcesCollection = checkNotNull(_collectionSources.get(_tmpKey_3))
          _result = TransactionWithDetails(_tmpTransaction,_tmpCategory,_tmpSourcesCollection)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeSummaries(startMillis: Long, endMillisExclusive: Long): Flow<List<TransactionSummaryProjection>> {
    val _sql: String = """
        |
        |        SELECT transaction_date, type, amount_minor_units, currency_code, category_id
        |        FROM transactions
        |        WHERE transaction_date >= ? AND transaction_date < ?
        |        ORDER BY transaction_date ASC
        |        
        """.trimMargin()
    return createFlow(__db, false, arrayOf("transactions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, startMillis)
        _argIndex = 2
        _stmt.bindLong(_argIndex, endMillisExclusive)
        val _columnIndexOfTransactionDateMillis: Int = 0
        val _columnIndexOfType: Int = 1
        val _columnIndexOfAmountMinorUnits: Int = 2
        val _columnIndexOfCurrencyCode: Int = 3
        val _columnIndexOfCategoryId: Int = 4
        val _result: MutableList<TransactionSummaryProjection> = mutableListOf()
        while (_stmt.step()) {
          val _item: TransactionSummaryProjection
          val _tmpTransactionDateMillis: Long
          _tmpTransactionDateMillis = _stmt.getLong(_columnIndexOfTransactionDateMillis)
          val _tmpType: TransactionType
          _tmpType = __TransactionType_stringToEnum(_stmt.getText(_columnIndexOfType))
          val _tmpAmountMinorUnits: Long
          _tmpAmountMinorUnits = _stmt.getLong(_columnIndexOfAmountMinorUnits)
          val _tmpCurrencyCode: String
          _tmpCurrencyCode = _stmt.getText(_columnIndexOfCurrencyCode)
          val _tmpCategoryId: Long?
          if (_stmt.isNull(_columnIndexOfCategoryId)) {
            _tmpCategoryId = null
          } else {
            _tmpCategoryId = _stmt.getLong(_columnIndexOfCategoryId)
          }
          _item = TransactionSummaryProjection(_tmpTransactionDateMillis,_tmpType,_tmpAmountMinorUnits,_tmpCurrencyCode,_tmpCategoryId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeAccountIdentifiers(): Flow<List<String>> {
    val _sql: String = """
        |
        |        SELECT DISTINCT account_identifier FROM transactions
        |        WHERE account_identifier IS NOT NULL
        |        ORDER BY account_identifier ASC
        |        
        """.trimMargin()
    return createFlow(__db, false, arrayOf("transactions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: MutableList<String> = mutableListOf()
        while (_stmt.step()) {
          val _item: String
          _item = _stmt.getText(0)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun countTransactions(): Int {
    val _sql: String = "SELECT COUNT(*) FROM transactions"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun findByFingerprint(fingerprint: String): TransactionEntity? {
    val _sql: String = "SELECT * FROM transactions WHERE fingerprint = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, fingerprint)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfAmountMinorUnits: Int = getColumnIndexOrThrow(_stmt, "amount_minor_units")
        val _columnIndexOfCurrencyCode: Int = getColumnIndexOrThrow(_stmt, "currency_code")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfTransactionDateMillis: Int = getColumnIndexOrThrow(_stmt, "transaction_date")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfMerchantName: Int = getColumnIndexOrThrow(_stmt, "merchant_name")
        val _columnIndexOfReferenceNumber: Int = getColumnIndexOrThrow(_stmt, "reference_number")
        val _columnIndexOfNormalizedReference: Int = getColumnIndexOrThrow(_stmt, "normalized_reference")
        val _columnIndexOfAccountIdentifier: Int = getColumnIndexOrThrow(_stmt, "account_identifier")
        val _columnIndexOfBankName: Int = getColumnIndexOrThrow(_stmt, "bank_name")
        val _columnIndexOfPaymentMethod: Int = getColumnIndexOrThrow(_stmt, "payment_method")
        val _columnIndexOfCategoryId: Int = getColumnIndexOrThrow(_stmt, "category_id")
        val _columnIndexOfPrimarySource: Int = getColumnIndexOrThrow(_stmt, "primary_source")
        val _columnIndexOfFingerprint: Int = getColumnIndexOrThrow(_stmt, "fingerprint")
        val _columnIndexOfParseStatus: Int = getColumnIndexOrThrow(_stmt, "parse_status")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfCreatedAtMillis: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _columnIndexOfUpdatedAtMillis: Int = getColumnIndexOrThrow(_stmt, "updated_at")
        val _result: TransactionEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpAmountMinorUnits: Long
          _tmpAmountMinorUnits = _stmt.getLong(_columnIndexOfAmountMinorUnits)
          val _tmpCurrencyCode: String
          _tmpCurrencyCode = _stmt.getText(_columnIndexOfCurrencyCode)
          val _tmpType: TransactionType
          _tmpType = __TransactionType_stringToEnum(_stmt.getText(_columnIndexOfType))
          val _tmpTransactionDateMillis: Long
          _tmpTransactionDateMillis = _stmt.getLong(_columnIndexOfTransactionDateMillis)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpMerchantName: String?
          if (_stmt.isNull(_columnIndexOfMerchantName)) {
            _tmpMerchantName = null
          } else {
            _tmpMerchantName = _stmt.getText(_columnIndexOfMerchantName)
          }
          val _tmpReferenceNumber: String?
          if (_stmt.isNull(_columnIndexOfReferenceNumber)) {
            _tmpReferenceNumber = null
          } else {
            _tmpReferenceNumber = _stmt.getText(_columnIndexOfReferenceNumber)
          }
          val _tmpNormalizedReference: String?
          if (_stmt.isNull(_columnIndexOfNormalizedReference)) {
            _tmpNormalizedReference = null
          } else {
            _tmpNormalizedReference = _stmt.getText(_columnIndexOfNormalizedReference)
          }
          val _tmpAccountIdentifier: String?
          if (_stmt.isNull(_columnIndexOfAccountIdentifier)) {
            _tmpAccountIdentifier = null
          } else {
            _tmpAccountIdentifier = _stmt.getText(_columnIndexOfAccountIdentifier)
          }
          val _tmpBankName: String?
          if (_stmt.isNull(_columnIndexOfBankName)) {
            _tmpBankName = null
          } else {
            _tmpBankName = _stmt.getText(_columnIndexOfBankName)
          }
          val _tmpPaymentMethod: PaymentMethod
          _tmpPaymentMethod = __PaymentMethod_stringToEnum(_stmt.getText(_columnIndexOfPaymentMethod))
          val _tmpCategoryId: Long?
          if (_stmt.isNull(_columnIndexOfCategoryId)) {
            _tmpCategoryId = null
          } else {
            _tmpCategoryId = _stmt.getLong(_columnIndexOfCategoryId)
          }
          val _tmpPrimarySource: TransactionSource
          _tmpPrimarySource = __TransactionSource_stringToEnum(_stmt.getText(_columnIndexOfPrimarySource))
          val _tmpFingerprint: String
          _tmpFingerprint = _stmt.getText(_columnIndexOfFingerprint)
          val _tmpParseStatus: ParseStatus
          _tmpParseStatus = __ParseStatus_stringToEnum(_stmt.getText(_columnIndexOfParseStatus))
          val _tmpNotes: String?
          if (_stmt.isNull(_columnIndexOfNotes)) {
            _tmpNotes = null
          } else {
            _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          }
          val _tmpCreatedAtMillis: Long
          _tmpCreatedAtMillis = _stmt.getLong(_columnIndexOfCreatedAtMillis)
          val _tmpUpdatedAtMillis: Long
          _tmpUpdatedAtMillis = _stmt.getLong(_columnIndexOfUpdatedAtMillis)
          _result = TransactionEntity(_tmpId,_tmpAmountMinorUnits,_tmpCurrencyCode,_tmpType,_tmpTransactionDateMillis,_tmpDescription,_tmpMerchantName,_tmpReferenceNumber,_tmpNormalizedReference,_tmpAccountIdentifier,_tmpBankName,_tmpPaymentMethod,_tmpCategoryId,_tmpPrimarySource,_tmpFingerprint,_tmpParseStatus,_tmpNotes,_tmpCreatedAtMillis,_tmpUpdatedAtMillis)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun findByNormalizedReference(normalizedReference: String): List<TransactionEntity> {
    val _sql: String = "SELECT * FROM transactions WHERE normalized_reference = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, normalizedReference)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfAmountMinorUnits: Int = getColumnIndexOrThrow(_stmt, "amount_minor_units")
        val _columnIndexOfCurrencyCode: Int = getColumnIndexOrThrow(_stmt, "currency_code")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfTransactionDateMillis: Int = getColumnIndexOrThrow(_stmt, "transaction_date")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfMerchantName: Int = getColumnIndexOrThrow(_stmt, "merchant_name")
        val _columnIndexOfReferenceNumber: Int = getColumnIndexOrThrow(_stmt, "reference_number")
        val _columnIndexOfNormalizedReference: Int = getColumnIndexOrThrow(_stmt, "normalized_reference")
        val _columnIndexOfAccountIdentifier: Int = getColumnIndexOrThrow(_stmt, "account_identifier")
        val _columnIndexOfBankName: Int = getColumnIndexOrThrow(_stmt, "bank_name")
        val _columnIndexOfPaymentMethod: Int = getColumnIndexOrThrow(_stmt, "payment_method")
        val _columnIndexOfCategoryId: Int = getColumnIndexOrThrow(_stmt, "category_id")
        val _columnIndexOfPrimarySource: Int = getColumnIndexOrThrow(_stmt, "primary_source")
        val _columnIndexOfFingerprint: Int = getColumnIndexOrThrow(_stmt, "fingerprint")
        val _columnIndexOfParseStatus: Int = getColumnIndexOrThrow(_stmt, "parse_status")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfCreatedAtMillis: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _columnIndexOfUpdatedAtMillis: Int = getColumnIndexOrThrow(_stmt, "updated_at")
        val _result: MutableList<TransactionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TransactionEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpAmountMinorUnits: Long
          _tmpAmountMinorUnits = _stmt.getLong(_columnIndexOfAmountMinorUnits)
          val _tmpCurrencyCode: String
          _tmpCurrencyCode = _stmt.getText(_columnIndexOfCurrencyCode)
          val _tmpType: TransactionType
          _tmpType = __TransactionType_stringToEnum(_stmt.getText(_columnIndexOfType))
          val _tmpTransactionDateMillis: Long
          _tmpTransactionDateMillis = _stmt.getLong(_columnIndexOfTransactionDateMillis)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpMerchantName: String?
          if (_stmt.isNull(_columnIndexOfMerchantName)) {
            _tmpMerchantName = null
          } else {
            _tmpMerchantName = _stmt.getText(_columnIndexOfMerchantName)
          }
          val _tmpReferenceNumber: String?
          if (_stmt.isNull(_columnIndexOfReferenceNumber)) {
            _tmpReferenceNumber = null
          } else {
            _tmpReferenceNumber = _stmt.getText(_columnIndexOfReferenceNumber)
          }
          val _tmpNormalizedReference: String?
          if (_stmt.isNull(_columnIndexOfNormalizedReference)) {
            _tmpNormalizedReference = null
          } else {
            _tmpNormalizedReference = _stmt.getText(_columnIndexOfNormalizedReference)
          }
          val _tmpAccountIdentifier: String?
          if (_stmt.isNull(_columnIndexOfAccountIdentifier)) {
            _tmpAccountIdentifier = null
          } else {
            _tmpAccountIdentifier = _stmt.getText(_columnIndexOfAccountIdentifier)
          }
          val _tmpBankName: String?
          if (_stmt.isNull(_columnIndexOfBankName)) {
            _tmpBankName = null
          } else {
            _tmpBankName = _stmt.getText(_columnIndexOfBankName)
          }
          val _tmpPaymentMethod: PaymentMethod
          _tmpPaymentMethod = __PaymentMethod_stringToEnum(_stmt.getText(_columnIndexOfPaymentMethod))
          val _tmpCategoryId: Long?
          if (_stmt.isNull(_columnIndexOfCategoryId)) {
            _tmpCategoryId = null
          } else {
            _tmpCategoryId = _stmt.getLong(_columnIndexOfCategoryId)
          }
          val _tmpPrimarySource: TransactionSource
          _tmpPrimarySource = __TransactionSource_stringToEnum(_stmt.getText(_columnIndexOfPrimarySource))
          val _tmpFingerprint: String
          _tmpFingerprint = _stmt.getText(_columnIndexOfFingerprint)
          val _tmpParseStatus: ParseStatus
          _tmpParseStatus = __ParseStatus_stringToEnum(_stmt.getText(_columnIndexOfParseStatus))
          val _tmpNotes: String?
          if (_stmt.isNull(_columnIndexOfNotes)) {
            _tmpNotes = null
          } else {
            _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          }
          val _tmpCreatedAtMillis: Long
          _tmpCreatedAtMillis = _stmt.getLong(_columnIndexOfCreatedAtMillis)
          val _tmpUpdatedAtMillis: Long
          _tmpUpdatedAtMillis = _stmt.getLong(_columnIndexOfUpdatedAtMillis)
          _item = TransactionEntity(_tmpId,_tmpAmountMinorUnits,_tmpCurrencyCode,_tmpType,_tmpTransactionDateMillis,_tmpDescription,_tmpMerchantName,_tmpReferenceNumber,_tmpNormalizedReference,_tmpAccountIdentifier,_tmpBankName,_tmpPaymentMethod,_tmpCategoryId,_tmpPrimarySource,_tmpFingerprint,_tmpParseStatus,_tmpNotes,_tmpCreatedAtMillis,_tmpUpdatedAtMillis)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateCategory(
    transactionId: Long,
    categoryId: Long?,
    updatedAtMillis: Long,
  ) {
    val _sql: String = "UPDATE transactions SET category_id = ?, updated_at = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        if (categoryId == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindLong(_argIndex, categoryId)
        }
        _argIndex = 2
        _stmt.bindLong(_argIndex, updatedAtMillis)
        _argIndex = 3
        _stmt.bindLong(_argIndex, transactionId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __TransactionType_enumToString(_value: TransactionType): String = when (_value) {
    TransactionType.DEBIT -> "DEBIT"
    TransactionType.CREDIT -> "CREDIT"
  }

  private fun __PaymentMethod_enumToString(_value: PaymentMethod): String = when (_value) {
    PaymentMethod.UPI -> "UPI"
    PaymentMethod.CARD -> "CARD"
    PaymentMethod.ATM -> "ATM"
    PaymentMethod.NET_BANKING -> "NET_BANKING"
    PaymentMethod.IMPS -> "IMPS"
    PaymentMethod.NEFT -> "NEFT"
    PaymentMethod.RTGS -> "RTGS"
    PaymentMethod.AUTO_DEBIT -> "AUTO_DEBIT"
    PaymentMethod.WALLET -> "WALLET"
    PaymentMethod.CASH -> "CASH"
    PaymentMethod.CHEQUE -> "CHEQUE"
    PaymentMethod.UNKNOWN -> "UNKNOWN"
  }

  private fun __TransactionSource_enumToString(_value: TransactionSource): String = when (_value) {
    TransactionSource.SMS -> "SMS"
    TransactionSource.CSV -> "CSV"
    TransactionSource.PDF -> "PDF"
    TransactionSource.MANUAL -> "MANUAL"
  }

  private fun __ParseStatus_enumToString(_value: ParseStatus): String = when (_value) {
    ParseStatus.PARSED -> "PARSED"
    ParseStatus.PARTIALLY_PARSED -> "PARTIALLY_PARSED"
    ParseStatus.FAILED -> "FAILED"
  }

  private fun __TransactionType_stringToEnum(_value: String): TransactionType = when (_value) {
    "DEBIT" -> TransactionType.DEBIT
    "CREDIT" -> TransactionType.CREDIT
    else -> throw IllegalArgumentException("Can't convert value to enum, unknown value: " + _value)
  }

  private fun __PaymentMethod_stringToEnum(_value: String): PaymentMethod = when (_value) {
    "UPI" -> PaymentMethod.UPI
    "CARD" -> PaymentMethod.CARD
    "ATM" -> PaymentMethod.ATM
    "NET_BANKING" -> PaymentMethod.NET_BANKING
    "IMPS" -> PaymentMethod.IMPS
    "NEFT" -> PaymentMethod.NEFT
    "RTGS" -> PaymentMethod.RTGS
    "AUTO_DEBIT" -> PaymentMethod.AUTO_DEBIT
    "WALLET" -> PaymentMethod.WALLET
    "CASH" -> PaymentMethod.CASH
    "CHEQUE" -> PaymentMethod.CHEQUE
    "UNKNOWN" -> PaymentMethod.UNKNOWN
    else -> throw IllegalArgumentException("Can't convert value to enum, unknown value: " + _value)
  }

  private fun __TransactionSource_stringToEnum(_value: String): TransactionSource = when (_value) {
    "SMS" -> TransactionSource.SMS
    "CSV" -> TransactionSource.CSV
    "PDF" -> TransactionSource.PDF
    "MANUAL" -> TransactionSource.MANUAL
    else -> throw IllegalArgumentException("Can't convert value to enum, unknown value: " + _value)
  }

  private fun __ParseStatus_stringToEnum(_value: String): ParseStatus = when (_value) {
    "PARSED" -> ParseStatus.PARSED
    "PARTIALLY_PARSED" -> ParseStatus.PARTIALLY_PARSED
    "FAILED" -> ParseStatus.FAILED
    else -> throw IllegalArgumentException("Can't convert value to enum, unknown value: " + _value)
  }

  private fun __CategoryKind_stringToEnum(_value: String): CategoryKind = when (_value) {
    "EXPENSE" -> CategoryKind.EXPENSE
    "INCOME" -> CategoryKind.INCOME
    "TRANSFER" -> CategoryKind.TRANSFER
    else -> throw IllegalArgumentException("Can't convert value to enum, unknown value: " + _value)
  }

  private fun __fetchRelationshipcategoriesAscomShashanksoniKharchahogayabhaiCoreDatabaseEntityCategoryEntity(_connection: SQLiteConnection, _map: LongSparseArray<CategoryEntity?>) {
    if (_map.isEmpty()) {
      return
    }
    if (_map.size() > 999) {
      recursiveFetchLongSparseArray(_map, false) { _tmpMap ->
        __fetchRelationshipcategoriesAscomShashanksoniKharchahogayabhaiCoreDatabaseEntityCategoryEntity(_connection, _tmpMap)
      }
      return
    }
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT `id`,`name`,`kind`,`color_hex`,`icon_key`,`is_system_defined`,`sort_order` FROM `categories` WHERE `id` IN (")
    val _inputSize: Int = _map.size()
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _stmt: SQLiteStatement = _connection.prepare(_sql)
    var _argIndex: Int = 1
    for (i in 0 until _map.size()) {
      val _item: Long = _map.keyAt(i)
      _stmt.bindLong(_argIndex, _item)
      _argIndex++
    }
    try {
      val _itemKeyIndex: Int = getColumnIndex(_stmt, "id")
      if (_itemKeyIndex == -1) {
        return
      }
      val _columnIndexOfId: Int = 0
      val _columnIndexOfName: Int = 1
      val _columnIndexOfKind: Int = 2
      val _columnIndexOfColorHex: Int = 3
      val _columnIndexOfIconKey: Int = 4
      val _columnIndexOfIsSystemDefined: Int = 5
      val _columnIndexOfSortOrder: Int = 6
      while (_stmt.step()) {
        val _tmpKey: Long
        _tmpKey = _stmt.getLong(_itemKeyIndex)
        if (_map.containsKey(_tmpKey)) {
          val _item_1: CategoryEntity?
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
          _item_1 = CategoryEntity(_tmpId,_tmpName,_tmpKind,_tmpColorHex,_tmpIconKey,_tmpIsSystemDefined,_tmpSortOrder)
          _map.put(_tmpKey, _item_1)
        }
      }
    } finally {
      _stmt.close()
    }
  }

  private fun __fetchRelationshiptransactionSourcesAscomShashanksoniKharchahogayabhaiCoreDatabaseEntityTransactionSourceRecordEntity(_connection: SQLiteConnection, _map: LongSparseArray<MutableList<TransactionSourceRecordEntity>>) {
    if (_map.isEmpty()) {
      return
    }
    if (_map.size() > 999) {
      recursiveFetchLongSparseArray(_map, true) { _tmpMap ->
        __fetchRelationshiptransactionSourcesAscomShashanksoniKharchahogayabhaiCoreDatabaseEntityTransactionSourceRecordEntity(_connection, _tmpMap)
      }
      return
    }
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT `id`,`transaction_id`,`source`,`source_identifier`,`origin_label`,`raw_payload`,`imported_at` FROM `transaction_sources` WHERE `transaction_id` IN (")
    val _inputSize: Int = _map.size()
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _stmt: SQLiteStatement = _connection.prepare(_sql)
    var _argIndex: Int = 1
    for (i in 0 until _map.size()) {
      val _item: Long = _map.keyAt(i)
      _stmt.bindLong(_argIndex, _item)
      _argIndex++
    }
    try {
      val _itemKeyIndex: Int = getColumnIndex(_stmt, "transaction_id")
      if (_itemKeyIndex == -1) {
        return
      }
      val _columnIndexOfId: Int = 0
      val _columnIndexOfTransactionId: Int = 1
      val _columnIndexOfSource: Int = 2
      val _columnIndexOfSourceIdentifier: Int = 3
      val _columnIndexOfOriginLabel: Int = 4
      val _columnIndexOfRawPayload: Int = 5
      val _columnIndexOfImportedAtMillis: Int = 6
      while (_stmt.step()) {
        val _tmpKey: Long
        _tmpKey = _stmt.getLong(_itemKeyIndex)
        val _tmpRelation: MutableList<TransactionSourceRecordEntity>? = _map.get(_tmpKey)
        if (_tmpRelation != null) {
          val _item_1: TransactionSourceRecordEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTransactionId: Long
          _tmpTransactionId = _stmt.getLong(_columnIndexOfTransactionId)
          val _tmpSource: TransactionSource
          _tmpSource = __TransactionSource_stringToEnum(_stmt.getText(_columnIndexOfSource))
          val _tmpSourceIdentifier: String?
          if (_stmt.isNull(_columnIndexOfSourceIdentifier)) {
            _tmpSourceIdentifier = null
          } else {
            _tmpSourceIdentifier = _stmt.getText(_columnIndexOfSourceIdentifier)
          }
          val _tmpOriginLabel: String?
          if (_stmt.isNull(_columnIndexOfOriginLabel)) {
            _tmpOriginLabel = null
          } else {
            _tmpOriginLabel = _stmt.getText(_columnIndexOfOriginLabel)
          }
          val _tmpRawPayload: String?
          if (_stmt.isNull(_columnIndexOfRawPayload)) {
            _tmpRawPayload = null
          } else {
            _tmpRawPayload = _stmt.getText(_columnIndexOfRawPayload)
          }
          val _tmpImportedAtMillis: Long
          _tmpImportedAtMillis = _stmt.getLong(_columnIndexOfImportedAtMillis)
          _item_1 = TransactionSourceRecordEntity(_tmpId,_tmpTransactionId,_tmpSource,_tmpSourceIdentifier,_tmpOriginLabel,_tmpRawPayload,_tmpImportedAtMillis)
          _tmpRelation.add(_item_1)
        }
      }
    } finally {
      _stmt.close()
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}

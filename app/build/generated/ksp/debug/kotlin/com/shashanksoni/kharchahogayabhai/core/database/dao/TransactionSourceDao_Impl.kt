package com.shashanksoni.kharchahogayabhai.core.database.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.shashanksoni.kharchahogayabhai.core.database.entity.TransactionSourceRecordEntity
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import javax.`annotation`.processing.Generated
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
public class TransactionSourceDao_Impl(
  __db: RoomDatabase,
) : TransactionSourceDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfTransactionSourceRecordEntity:
      EntityInsertAdapter<TransactionSourceRecordEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfTransactionSourceRecordEntity = object : EntityInsertAdapter<TransactionSourceRecordEntity>() {
      protected override fun createQuery(): String = "INSERT OR IGNORE INTO `transaction_sources` (`id`,`transaction_id`,`source`,`source_identifier`,`origin_label`,`raw_payload`,`imported_at`) VALUES (nullif(?, 0),?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TransactionSourceRecordEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.transactionId)
        statement.bindText(3, __TransactionSource_enumToString(entity.source))
        val _tmpSourceIdentifier: String? = entity.sourceIdentifier
        if (_tmpSourceIdentifier == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpSourceIdentifier)
        }
        val _tmpOriginLabel: String? = entity.originLabel
        if (_tmpOriginLabel == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpOriginLabel)
        }
        val _tmpRawPayload: String? = entity.rawPayload
        if (_tmpRawPayload == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpRawPayload)
        }
        statement.bindLong(7, entity.importedAtMillis)
      }
    }
  }

  public override suspend fun insertSourceRecords(records: List<TransactionSourceRecordEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfTransactionSourceRecordEntity.insert(_connection, records)
  }

  public override fun observeSourcesOf(transactionId: Long): Flow<List<TransactionSourceRecordEntity>> {
    val _sql: String = """
        |
        |        SELECT * FROM transaction_sources
        |        WHERE transaction_id = ?
        |        ORDER BY imported_at ASC
        |        
        """.trimMargin()
    return createFlow(__db, false, arrayOf("transaction_sources")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, transactionId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTransactionId: Int = getColumnIndexOrThrow(_stmt, "transaction_id")
        val _columnIndexOfSource: Int = getColumnIndexOrThrow(_stmt, "source")
        val _columnIndexOfSourceIdentifier: Int = getColumnIndexOrThrow(_stmt, "source_identifier")
        val _columnIndexOfOriginLabel: Int = getColumnIndexOrThrow(_stmt, "origin_label")
        val _columnIndexOfRawPayload: Int = getColumnIndexOrThrow(_stmt, "raw_payload")
        val _columnIndexOfImportedAtMillis: Int = getColumnIndexOrThrow(_stmt, "imported_at")
        val _result: MutableList<TransactionSourceRecordEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TransactionSourceRecordEntity
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
          _item = TransactionSourceRecordEntity(_tmpId,_tmpTransactionId,_tmpSource,_tmpSourceIdentifier,_tmpOriginLabel,_tmpRawPayload,_tmpImportedAtMillis)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun findBySourceIdentifier(source: TransactionSource, sourceIdentifier: String): TransactionSourceRecordEntity? {
    val _sql: String = """
        |
        |        SELECT * FROM transaction_sources
        |        WHERE source = ? AND source_identifier = ?
        |        LIMIT 1
        |        
        """.trimMargin()
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, __TransactionSource_enumToString(source))
        _argIndex = 2
        _stmt.bindText(_argIndex, sourceIdentifier)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTransactionId: Int = getColumnIndexOrThrow(_stmt, "transaction_id")
        val _columnIndexOfSource: Int = getColumnIndexOrThrow(_stmt, "source")
        val _columnIndexOfSourceIdentifier: Int = getColumnIndexOrThrow(_stmt, "source_identifier")
        val _columnIndexOfOriginLabel: Int = getColumnIndexOrThrow(_stmt, "origin_label")
        val _columnIndexOfRawPayload: Int = getColumnIndexOrThrow(_stmt, "raw_payload")
        val _columnIndexOfImportedAtMillis: Int = getColumnIndexOrThrow(_stmt, "imported_at")
        val _result: TransactionSourceRecordEntity?
        if (_stmt.step()) {
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
          _result = TransactionSourceRecordEntity(_tmpId,_tmpTransactionId,_tmpSource,_tmpSourceIdentifier,_tmpOriginLabel,_tmpRawPayload,_tmpImportedAtMillis)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __TransactionSource_enumToString(_value: TransactionSource): String = when (_value) {
    TransactionSource.SMS -> "SMS"
    TransactionSource.CSV -> "CSV"
    TransactionSource.PDF -> "PDF"
    TransactionSource.MANUAL -> "MANUAL"
  }

  private fun __TransactionSource_stringToEnum(_value: String): TransactionSource = when (_value) {
    "SMS" -> TransactionSource.SMS
    "CSV" -> TransactionSource.CSV
    "PDF" -> TransactionSource.PDF
    "MANUAL" -> TransactionSource.MANUAL
    else -> throw IllegalArgumentException("Can't convert value to enum, unknown value: " + _value)
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}

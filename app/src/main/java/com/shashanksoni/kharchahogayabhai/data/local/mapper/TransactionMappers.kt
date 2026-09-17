package com.shashanksoni.kharchahogayabhai.data.local.mapper

import com.shashanksoni.kharchahogayabhai.core.database.entity.CategoryEntity
import com.shashanksoni.kharchahogayabhai.core.database.entity.LabelEntity
import com.shashanksoni.kharchahogayabhai.core.database.entity.TransactionEntity
import com.shashanksoni.kharchahogayabhai.core.database.entity.TransactionSourceRecordEntity
import com.shashanksoni.kharchahogayabhai.core.database.projection.TransactionSummaryProjection
import com.shashanksoni.kharchahogayabhai.core.database.relation.TransactionWithDetails
import com.shashanksoni.kharchahogayabhai.domain.model.Category
import com.shashanksoni.kharchahogayabhai.domain.model.Money
import com.shashanksoni.kharchahogayabhai.domain.model.Transaction
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionDetail
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionLabel
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSourceRecord
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSummary
import java.time.Instant

/**
 * Storage-to-domain translation. Epoch milliseconds become `Instant`s and minor
 * units become [Money] here, so nothing above the data layer deals in raw
 * longs.
 */

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    amount = Money(minorUnits = amountMinorUnits, currencyCode = currencyCode),
    type = type,
    transactionDate = Instant.ofEpochMilli(transactionDateMillis),
    description = description,
    merchantName = merchantName,
    referenceNumber = referenceNumber,
    normalizedReference = normalizedReference,
    accountIdentifier = accountIdentifier,
    bankName = bankName,
    paymentMethod = paymentMethod,
    categoryId = categoryId,
    primarySource = primarySource,
    fingerprint = fingerprint,
    parseStatus = parseStatus,
    notes = notes,
    createdAt = Instant.ofEpochMilli(createdAtMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtMillis),
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    amountMinorUnits = amount.minorUnits,
    currencyCode = amount.currencyCode,
    type = type,
    transactionDateMillis = transactionDate.toEpochMilli(),
    description = description,
    merchantName = merchantName,
    referenceNumber = referenceNumber,
    normalizedReference = normalizedReference,
    accountIdentifier = accountIdentifier,
    bankName = bankName,
    paymentMethod = paymentMethod,
    categoryId = categoryId,
    primarySource = primarySource,
    fingerprint = fingerprint,
    parseStatus = parseStatus,
    notes = notes,
    createdAtMillis = createdAt.toEpochMilli(),
    updatedAtMillis = updatedAt.toEpochMilli(),
)

fun TransactionSourceRecordEntity.toDomain(): TransactionSourceRecord = TransactionSourceRecord(
    id = id,
    transactionId = transactionId,
    source = source,
    sourceIdentifier = sourceIdentifier,
    originLabel = originLabel,
    rawPayload = rawPayload,
    importedAt = Instant.ofEpochMilli(importedAtMillis),
)

fun TransactionSourceRecord.toEntity(transactionId: Long): TransactionSourceRecordEntity =
    TransactionSourceRecordEntity(
        id = id,
        transactionId = transactionId,
        source = source,
        sourceIdentifier = sourceIdentifier,
        originLabel = originLabel,
        rawPayload = rawPayload,
        importedAtMillis = importedAt.toEpochMilli(),
    )

fun TransactionWithDetails.toDomain(): TransactionDetail = TransactionDetail(
    transaction = transaction.toDomain(),
    category = category?.toDomain(),
    sources = sources.map { it.toDomain() },
    labels = labels.map { it.toDomain() },
)

fun LabelEntity.toDomain(): TransactionLabel = TransactionLabel(
    id = id,
    name = name,
    colorHex = colorHex,
    isSystemDefined = isSystemDefined,
    sortOrder = sortOrder,
)

fun TransactionLabel.toEntity(): LabelEntity = LabelEntity(
    id = id,
    name = name,
    colorHex = colorHex,
    isSystemDefined = isSystemDefined,
    sortOrder = sortOrder,
)

fun TransactionSummaryProjection.toDomain(): TransactionSummary = TransactionSummary(
    transactionDate = Instant.ofEpochMilli(transactionDateMillis),
    type = type,
    amount = Money(minorUnits = amountMinorUnits, currencyCode = currencyCode),
    categoryId = categoryId,
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    kind = kind,
    colorHex = colorHex,
    iconKey = iconKey,
    isSystemDefined = isSystemDefined,
    sortOrder = sortOrder,
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    kind = kind,
    colorHex = colorHex,
    iconKey = iconKey,
    isSystemDefined = isSystemDefined,
    sortOrder = sortOrder,
)

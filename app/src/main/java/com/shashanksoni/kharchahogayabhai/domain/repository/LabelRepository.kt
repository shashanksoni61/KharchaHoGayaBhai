package com.shashanksoni.kharchahogayabhai.domain.repository

import com.shashanksoni.kharchahogayabhai.domain.model.TransactionLabel
import kotlinx.coroutines.flow.Flow

interface LabelRepository {

    fun observeLabels(): Flow<List<TransactionLabel>>

    suspend fun ensureDefaultLabelsExist()

    /** Creates a custom label; throws if the name is blank or already used. */
    suspend fun createCustomLabel(name: String, colorHex: String = "90A4AE"): TransactionLabel

    /** System labels cannot be deleted. */
    suspend fun deleteCustomLabel(labelId: Long)

    suspend fun setLabelOnTransaction(transactionId: Long, labelId: Long, attached: Boolean)
}

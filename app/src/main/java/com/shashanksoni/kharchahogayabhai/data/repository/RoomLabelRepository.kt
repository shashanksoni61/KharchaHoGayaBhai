package com.shashanksoni.kharchahogayabhai.data.repository

import android.content.SharedPreferences
import com.shashanksoni.kharchahogayabhai.core.database.dao.LabelDao
import com.shashanksoni.kharchahogayabhai.core.database.entity.LabelEntity
import com.shashanksoni.kharchahogayabhai.core.database.entity.TransactionLabelCrossRef
import com.shashanksoni.kharchahogayabhai.data.local.mapper.toDomain
import com.shashanksoni.kharchahogayabhai.domain.model.DefaultLabels
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionLabel
import com.shashanksoni.kharchahogayabhai.domain.repository.LabelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomLabelRepository(
    private val labelDao: LabelDao,
    private val prefs: SharedPreferences,
) : LabelRepository {

    override fun observeLabels(): Flow<List<TransactionLabel>> =
        labelDao.observeLabels().map { entities -> entities.map { it.toDomain() } }

    override suspend fun ensureDefaultLabelsExist() {
        val storedVersion = prefs.getInt(PREFS_LABELS_VERSION, 0)
        if (storedVersion < LIFESTYLE_LABELS_VERSION) {
            // Drop the previous built-in set (workflow flags → lifestyle tags).
            // Cross-refs cascade-delete with the old system label rows.
            labelDao.deleteSystemLabels()
            prefs.edit().putInt(PREFS_LABELS_VERSION, LIFESTYLE_LABELS_VERSION).apply()
        }
        labelDao.insertMissingLabels(
            DefaultLabels.all.map { label ->
                LabelEntity(
                    id = label.id,
                    name = label.name,
                    colorHex = label.colorHex,
                    isSystemDefined = true,
                    sortOrder = label.sortOrder,
                )
            },
        )
    }

    override suspend fun createCustomLabel(name: String, colorHex: String): TransactionLabel {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Label name cannot be empty" }
        require(labelDao.countByName(trimmed) == 0) { "A label named \"$trimmed\" already exists" }

        val id = labelDao.insertLabel(
            LabelEntity(
                name = trimmed,
                colorHex = colorHex.removePrefix("#").take(6).ifEmpty { "90A4AE" },
                isSystemDefined = false,
                sortOrder = 1_000,
            ),
        )
        return TransactionLabel(
            id = id,
            name = trimmed,
            colorHex = colorHex.removePrefix("#").take(6).ifEmpty { "90A4AE" },
            isSystemDefined = false,
            sortOrder = 1_000,
        )
    }

    override suspend fun deleteCustomLabel(labelId: Long) {
        labelDao.deleteCustomLabel(labelId)
    }

    override suspend fun setLabelOnTransaction(transactionId: Long, labelId: Long, attached: Boolean) {
        if (attached) {
            labelDao.attachLabel(TransactionLabelCrossRef(transactionId, labelId))
        } else {
            labelDao.detachLabel(transactionId, labelId)
        }
    }

    companion object {
        const val PREFS_NAME = "kharcha_prefs"
        private const val PREFS_LABELS_VERSION = "system_labels_version"
        /** Bump when built-in lifestyle tags change enough to replace old system rows. */
        private const val LIFESTYLE_LABELS_VERSION = 2
    }
}

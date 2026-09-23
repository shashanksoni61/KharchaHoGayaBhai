package com.shashanksoni.kharchahogayabhai.feature.transactions.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashanksoni.kharchahogayabhai.R
import com.shashanksoni.kharchahogayabhai.appContainer
import com.shashanksoni.kharchahogayabhai.core.common.DateTimeFormatters
import com.shashanksoni.kharchahogayabhai.core.common.MoneyFormatter
import com.shashanksoni.kharchahogayabhai.core.normalization.AccountIdentifierNormalizer
import com.shashanksoni.kharchahogayabhai.domain.model.Category
import com.shashanksoni.kharchahogayabhai.domain.model.ParseStatus
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionDetail
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionLabel
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSourceRecord
import com.shashanksoni.kharchahogayabhai.ui.component.CategoryAvatar
import com.shashanksoni.kharchahogayabhai.ui.component.SectionCard
import com.shashanksoni.kharchahogayabhai.ui.theme.HeadlineAmountStyle
import com.shashanksoni.kharchahogayabhai.ui.theme.financeColors
import com.shashanksoni.kharchahogayabhai.ui.util.CategoryVisuals
import com.shashanksoni.kharchahogayabhai.ui.util.labelRes
import java.time.ZoneId

@Composable
fun TransactionDetailScreen(
    viewModel: TransactionDetailViewModel,
    zone: ZoneId,
    onHidden: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val container = LocalContext.current.appContainer
    val incomeRevealed by container.securitySession.incomeRevealedFlow
        .collectAsStateWithLifecycle()
    val hideIncome = !incomeRevealed
    var isChoosingCategory by remember { mutableStateOf(false) }
    var isCreatingLabel by remember { mutableStateOf(false) }

    when {
        uiState.isLoading -> CenteredMessage { CircularProgressIndicator() }

        uiState.detail == null -> CenteredMessage {
            Text(
                text = stringResource(R.string.detail_not_found),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }

        else -> {
            val detail = requireNotNull(uiState.detail)
            TransactionDetailContent(
                detail = detail,
                allLabels = uiState.allLabels,
                zone = zone,
                onChangeCategoryClick = { isChoosingCategory = true },
                onToggleLabel = viewModel::toggleLabel,
                onAddCustomLabelClick = { isCreatingLabel = true },
                labelError = uiState.labelError,
                hideIncome = hideIncome,
                onSetIgnored = { ignored ->
                    viewModel.setIgnored(ignored)
                    if (ignored) onHidden()
                },
                modifier = modifier,
                contentPadding = contentPadding,
            )

            if (isChoosingCategory) {
                CategoryPickerDialog(
                    categories = uiState.categories,
                    selectedCategoryId = detail.transaction.categoryId,
                    onCategorySelected = { categoryId ->
                        viewModel.changeCategory(categoryId)
                        isChoosingCategory = false
                    },
                    onDismiss = { isChoosingCategory = false },
                )
            }

            if (isCreatingLabel) {
                CreateLabelOnTransactionDialog(
                    onConfirm = { name ->
                        viewModel.createAndAttachCustomLabel(name)
                        isCreatingLabel = false
                    },
                    onDismiss = { isCreatingLabel = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransactionDetailContent(
    detail: TransactionDetail,
    allLabels: List<TransactionLabel>,
    zone: ZoneId,
    onChangeCategoryClick: () -> Unit,
    onToggleLabel: (labelId: Long, currentlyAttached: Boolean) -> Unit,
    onAddCustomLabelClick: () -> Unit,
    labelError: String?,
    hideIncome: Boolean,
    onSetIgnored: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val transaction = detail.transaction
    val notAvailable = stringResource(R.string.detail_none)
    val attachedIds = detail.labels.map { it.id }.toSet()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = if (hideIncome && !transaction.isDebit) {
                    "••••••"
                } else {
                    MoneyFormatter.formatSigned(transaction.amount, transaction.type)
                },
                style = HeadlineAmountStyle,
                color = if (transaction.isDebit) financeColors.expense else financeColors.income,
            )
            Text(text = transaction.displayTitle, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${DateTimeFormatters.fullDate(transaction.transactionDate, zone)} · " +
                    DateTimeFormatters.timeOfDay(transaction.transactionDate, zone),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (transaction.isPromotional) {
                Text(
                    text = stringResource(R.string.detail_promotional),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            OutlinedButton(onClick = { onSetIgnored(!transaction.isIgnored) }) {
                Text(
                    text = stringResource(
                        if (transaction.isIgnored) {
                            R.string.detail_restore_transaction
                        } else {
                            R.string.detail_mark_not_a_transaction
                        },
                    ),
                )
            }
        }

        SectionCard(
            title = stringResource(R.string.detail_category),
            trailing = {
                TextButton(onClick = onChangeCategoryClick) {
                    Text(stringResource(R.string.detail_change_category))
                }
            },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryAvatar(category = detail.category, size = 32)
                Text(
                    text = detail.category?.name
                        ?: stringResource(R.string.dashboard_uncategorised),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        SectionCard(
            title = stringResource(R.string.detail_flags_title),
            trailing = {
                TextButton(onClick = onAddCustomLabelClick) {
                    Text(stringResource(R.string.detail_add_custom_flag))
                }
            },
        ) {
            Text(
                text = stringResource(R.string.detail_flags_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                allLabels.forEach { label ->
                    val selected = label.id in attachedIds
                    FilterChip(
                        selected = selected,
                        onClick = { onToggleLabel(label.id, selected) },
                        label = { Text(label.name) },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
            labelError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        SectionCard(title = stringResource(R.string.detail_title)) {
            DetailRow(
                label = stringResource(R.string.detail_payment_method),
                value = stringResource(transaction.paymentMethod.labelRes),
            )
            DetailRow(
                label = stringResource(R.string.detail_bank),
                value = transaction.bankName ?: notAvailable,
            )
            DetailRow(
                label = stringResource(R.string.detail_account),
                value = AccountIdentifierNormalizer.toMaskedDisplay(transaction.accountIdentifier)
                    ?: notAvailable,
            )
            DetailRow(
                label = stringResource(R.string.detail_reference_number),
                value = transaction.referenceNumber ?: notAvailable,
            )
            transaction.description?.let { description ->
                DetailRow(
                    label = stringResource(R.string.detail_description),
                    value = description,
                )
            }
            transaction.notes?.let { notes ->
                DetailRow(label = stringResource(R.string.detail_notes), value = notes)
            }
            DetailRow(
                label = stringResource(R.string.detail_parse_status),
                value = stringResource(parseStatusLabelOf(transaction.parseStatus)),
            )
        }

        SectionCard(title = stringResource(R.string.detail_sources)) {
            detail.sources.forEach { sourceRecord ->
                SourceRow(sourceRecord = sourceRecord, zone = zone)
            }
        }
    }
}

@Composable
private fun SourceRow(sourceRecord: TransactionSourceRecord, zone: ZoneId) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = financeColors.income,
            modifier = Modifier.size(18.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(sourceRecord.source.labelRes),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = listOfNotNull(
                    sourceRecord.originLabel,
                    stringResource(
                        R.string.detail_source_imported_on,
                        DateTimeFormatters.fullDate(sourceRecord.importedAt, zone),
                    ),
                ).joinToString(separator = " · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = CategoryVisuals.iconFor(sourceRecord.source),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun CategoryPickerDialog(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.detail_dismiss))
            }
        },
        title = { Text(stringResource(R.string.detail_change_category)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { category ->
                    AssistChip(
                        onClick = { onCategorySelected(category.id) },
                        label = { Text(category.name) },
                        leadingIcon = {
                            Icon(
                                imageVector = CategoryVisuals.iconFor(category.iconKey),
                                contentDescription = null,
                                tint = CategoryVisuals.colorOf(
                                    category.colorHex,
                                    MaterialTheme.colorScheme.primary,
                                ),
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        trailingIcon = {
                            if (category.id == selectedCategoryId) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun CreateLabelOnTransactionDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_add_custom_flag_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.settings_label_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.settings_add_label_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.detail_dismiss))
            }
        },
    )
}

@Composable
private fun CenteredMessage(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private fun parseStatusLabelOf(status: ParseStatus): Int = when (status) {
    ParseStatus.PARSED -> R.string.parse_status_parsed
    ParseStatus.PARTIALLY_PARSED -> R.string.parse_status_partial
    ParseStatus.FAILED -> R.string.parse_status_failed
}

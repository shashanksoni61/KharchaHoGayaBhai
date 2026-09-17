package com.shashanksoni.kharchahogayabhai.feature.transactions.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashanksoni.kharchahogayabhai.R
import com.shashanksoni.kharchahogayabhai.core.common.MoneyFormatter
import com.shashanksoni.kharchahogayabhai.domain.model.Money
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import com.shashanksoni.kharchahogayabhai.ui.component.TransactionRow
import com.shashanksoni.kharchahogayabhai.ui.theme.financeColors
import com.shashanksoni.kharchahogayabhai.ui.util.labelRes

@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var dayInfoDialog by remember { mutableStateOf<TransactionDayGroup?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SearchField(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::search,
                )
                TypeFilterChips(
                    selectedType = uiState.selectedType,
                    onTypeClick = viewModel::toggleTypeFilter,
                )
                SourceFilterChips(
                    selectedSource = uiState.selectedSource,
                    onSourceClick = viewModel::toggleSourceFilter,
                )
                Text(
                    text = stringResource(
                        R.string.transactions_count_summary,
                        uiState.transactionCount,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (uiState.isEmpty) {
            item {
                Text(
                    text = stringResource(R.string.transactions_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                )
            }
        }

        uiState.dayGroups.forEach { dayGroup ->
            val collapsed = uiState.isCollapsed(dayGroup.date)
            item(key = "header-${dayGroup.date}") {
                DayGroupHeader(
                    dayGroup = dayGroup,
                    collapsed = collapsed,
                    onToggleCollapse = { viewModel.toggleDayCollapsed(dayGroup.date) },
                    onInfoClick = { dayInfoDialog = dayGroup },
                )
            }
            if (!collapsed) {
                items(items = dayGroup.entries, key = { it.transaction.id }) { entry ->
                    TransactionRow(
                        transaction = entry.transaction,
                        category = entry.category,
                        onClick = { onTransactionClick(entry.transaction.id) },
                        categories = uiState.categories,
                        onCategorySelected = { categoryId ->
                            viewModel.setCategory(entry.transaction.id, categoryId)
                        },
                    )
                }
                item(key = "divider-${dayGroup.date}") {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    dayInfoDialog?.let { day ->
        DaySummaryDialog(
            dayGroup = day,
            onDismiss = { dayInfoDialog = null },
        )
    }
}

@Composable
private fun DayGroupHeader(
    dayGroup: TransactionDayGroup,
    collapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleCollapse)
            .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = if (collapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
            contentDescription = stringResource(
                if (collapsed) {
                    R.string.transactions_expand_day
                } else {
                    R.string.transactions_collapse_day
                },
            ),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = dayGroup.header,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "+${MoneyFormatter.formatCompact(dayGroup.creditTotal)}",
            style = MaterialTheme.typography.labelMedium,
            color = financeColors.income,
        )
        Text(
            text = "\u2212${MoneyFormatter.formatCompact(dayGroup.debitTotal)}",
            style = MaterialTheme.typography.labelMedium,
            color = financeColors.expense,
        )
        IconButton(
            onClick = onInfoClick,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = stringResource(R.string.transactions_day_info),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun DaySummaryDialog(
    dayGroup: TransactionDayGroup,
    onDismiss: () -> Unit,
) {
    val net = dayGroup.net
    val outcomeRes = when {
        net.minorUnits > 0L -> R.string.transactions_day_profit
        net.minorUnits < 0L -> R.string.transactions_day_loss
        else -> R.string.transactions_day_even
    }
    val outcomeColor = when {
        net.minorUnits > 0L -> financeColors.income
        net.minorUnits < 0L -> financeColors.expense
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dayGroup.header) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(outcomeRes),
                    style = MaterialTheme.typography.titleSmall,
                    color = outcomeColor,
                )
                SummaryLine(
                    label = stringResource(R.string.transactions_day_credit),
                    value = MoneyFormatter.format(dayGroup.creditTotal),
                    valueColor = financeColors.income,
                )
                SummaryLine(
                    label = stringResource(R.string.transactions_day_debit),
                    value = MoneyFormatter.format(dayGroup.debitTotal),
                    valueColor = financeColors.expense,
                )
                SummaryLine(
                    label = stringResource(R.string.transactions_day_net),
                    value = formatNet(net),
                    valueColor = outcomeColor,
                )
                SummaryLine(
                    label = stringResource(R.string.transactions_day_count),
                    value = dayGroup.transactionCount.toString(),
                )
                if (dayGroup.uncategorisedCount > 0) {
                    SummaryLine(
                        label = stringResource(R.string.transactions_day_uncategorised),
                        value = dayGroup.uncategorisedCount.toString(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.detail_dismiss))
            }
        },
    )
}

@Composable
private fun SummaryLine(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
        )
    }
}

private fun formatNet(net: Money): String {
    val abs = MoneyFormatter.format(net.absoluteValue)
    return when {
        net.minorUnits > 0L -> "+$abs"
        net.minorUnits < 0L -> "\u2212$abs"
        else -> abs
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.transactions_search_hint)) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.transactions_clear_search),
                    )
                }
            }
        },
        singleLine = true,
    )
}

@Composable
private fun TypeFilterChips(
    selectedType: TransactionType?,
    onTypeClick: (TransactionType?) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selectedType == null,
            onClick = { onTypeClick(null) },
            label = { Text(stringResource(R.string.transactions_filter_all)) },
        )
        FilterChip(
            selected = selectedType == TransactionType.DEBIT,
            onClick = { onTypeClick(TransactionType.DEBIT) },
            label = { Text(stringResource(R.string.transactions_filter_money_out)) },
        )
        FilterChip(
            selected = selectedType == TransactionType.CREDIT,
            onClick = { onTypeClick(TransactionType.CREDIT) },
            label = { Text(stringResource(R.string.transactions_filter_money_in)) },
        )
    }
}

@Composable
private fun SourceFilterChips(
    selectedSource: TransactionSource?,
    onSourceClick: (TransactionSource?) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TransactionSource.entries.forEach { source ->
            FilterChip(
                selected = selectedSource == source,
                onClick = { onSourceClick(source) },
                label = { Text(stringResource(source.labelRes)) },
            )
        }
    }
}

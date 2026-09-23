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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashanksoni.kharchahogayabhai.R
import com.shashanksoni.kharchahogayabhai.core.common.DateTimeFormatters
import com.shashanksoni.kharchahogayabhai.core.common.MoneyFormatter
import com.shashanksoni.kharchahogayabhai.domain.model.Money
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import com.shashanksoni.kharchahogayabhai.ui.component.TransactionRow
import com.shashanksoni.kharchahogayabhai.ui.theme.financeColors
import com.shashanksoni.kharchahogayabhai.ui.util.labelRes
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var dayInfoDialog by remember { mutableStateOf<TransactionDayGroup?>(null) }
    var monthInfoDialog by remember { mutableStateOf<TransactionMonthGroup?>(null) }
    var filtersOpen by remember { mutableStateOf(false) }

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
                MonthFilterChips(
                    availableMonths = uiState.availableMonths,
                    selectedMonth = uiState.selectedMonth,
                    onMonthClick = viewModel::selectMonth,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FiltersButton(
                        active = uiState.hasSheetFilters,
                        onClick = { filtersOpen = true },
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

        uiState.monthGroups.forEach { monthGroup ->
            val monthCollapsed = uiState.isMonthCollapsed(monthGroup.month)
            item(key = "month-${monthGroup.month}") {
                MonthGroupHeader(
                    monthGroup = monthGroup,
                    collapsed = monthCollapsed,
                    onToggleCollapse = { viewModel.toggleMonthCollapsed(monthGroup.month) },
                    onInfoClick = { monthInfoDialog = monthGroup },
                )
            }
            if (!monthCollapsed) {
                monthGroup.dayGroups.forEach { dayGroup ->
                    val collapsed = uiState.isCollapsed(dayGroup.date)
                    item(key = "header-${dayGroup.date}") {
                        DayGroupHeader(
                            dayGroup = dayGroup,
                            collapsed = collapsed,
                            sort = uiState.sortOf(dayGroup.date),
                            onToggleCollapse = { viewModel.toggleDayCollapsed(dayGroup.date) },
                            onSortHighToLow = { viewModel.sortDayHighToLow(dayGroup.date) },
                            onSortLowToHigh = { viewModel.sortDayLowToHigh(dayGroup.date) },
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
                                onSetIgnored = { ignored ->
                                    viewModel.setIgnored(entry.transaction.id, ignored)
                                },
                            )
                        }
                        item(key = "divider-${dayGroup.date}") {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }

    dayInfoDialog?.let { day ->
        PeriodSummaryDialog(
            title = day.header,
            creditTotal = day.creditTotal,
            debitTotal = day.debitTotal,
            transactionCount = day.transactionCount,
            uncategorisedCount = day.uncategorisedCount,
            profitText = stringResource(R.string.transactions_day_profit),
            lossText = stringResource(R.string.transactions_day_loss),
            evenText = stringResource(R.string.transactions_day_even),
            onDismiss = { dayInfoDialog = null },
        )
    }

    monthInfoDialog?.let { month ->
        PeriodSummaryDialog(
            title = month.header,
            creditTotal = month.creditTotal,
            debitTotal = month.debitTotal,
            transactionCount = month.transactionCount,
            uncategorisedCount = month.uncategorisedCount,
            profitText = stringResource(R.string.transactions_month_profit),
            lossText = stringResource(R.string.transactions_month_loss),
            evenText = stringResource(R.string.transactions_month_even),
            onDismiss = { monthInfoDialog = null },
        )
    }

    if (filtersOpen) {
        FiltersSheet(
            uiState = uiState,
            onDismiss = { filtersOpen = false },
            onTypeClick = viewModel::toggleTypeFilter,
            onSourceClick = viewModel::toggleSourceFilter,
            onAnyAmount = {
                viewModel.setMinAmountInput("")
                viewModel.setMaxAmountInput("")
            },
            onAmountPreset = viewModel::toggleMinAmountPreset,
            onMinAmount = viewModel::setMinAmountInput,
            onMaxAmount = viewModel::setMaxAmountInput,
            onToggleCategory = viewModel::toggleCategoryFilter,
            onToggleAccount = viewModel::toggleAccountFilter,
            onExcludePromotional = viewModel::setExcludePromotional,
            onIncludeIgnored = viewModel::setIncludeIgnored,
            onUncategorisedOnly = viewModel::setUncategorisedOnly,
            onClear = viewModel::clearNonMonthFilters,
        )
    }
}

@Composable
private fun MonthGroupHeader(
    monthGroup: TransactionMonthGroup,
    collapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleCollapse)
            .padding(start = 8.dp, end = 12.dp, top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = if (collapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
            contentDescription = stringResource(
                if (collapsed) {
                    R.string.transactions_expand_month
                } else {
                    R.string.transactions_collapse_month
                },
            ),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(26.dp),
        )
        Text(
            text = monthGroup.header,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "+${MoneyFormatter.formatCompact(monthGroup.creditTotal)}",
            style = MaterialTheme.typography.labelMedium,
            color = financeColors.income,
        )
        Text(
            text = "\u2212${MoneyFormatter.formatCompact(monthGroup.debitTotal)}",
            style = MaterialTheme.typography.labelMedium,
            color = financeColors.expense,
        )
        IconButton(
            onClick = onInfoClick,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = stringResource(R.string.transactions_month_info),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun DayGroupHeader(
    dayGroup: TransactionDayGroup,
    collapsed: Boolean,
    sort: DayAmountSort,
    onToggleCollapse: () -> Unit,
    onSortHighToLow: () -> Unit,
    onSortLowToHigh: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val active = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleCollapse)
            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
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
        IconButton(
            onClick = onSortHighToLow,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowDownward,
                contentDescription = stringResource(R.string.transactions_sort_high_to_low),
                tint = if (sort == DayAmountSort.AMOUNT_HIGH_TO_LOW) active else muted,
                modifier = Modifier.size(16.dp),
            )
        }
        IconButton(
            onClick = onSortLowToHigh,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowUpward,
                contentDescription = stringResource(R.string.transactions_sort_low_to_high),
                tint = if (sort == DayAmountSort.AMOUNT_LOW_TO_HIGH) active else muted,
                modifier = Modifier.size(16.dp),
            )
        }
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
private fun PeriodSummaryDialog(
    title: String,
    creditTotal: Money,
    debitTotal: Money,
    transactionCount: Int,
    uncategorisedCount: Int,
    profitText: String,
    lossText: String,
    evenText: String,
    onDismiss: () -> Unit,
) {
    val net = Money.net(creditTotal, debitTotal)
    val outcomeText = when {
        net.minorUnits > 0L -> profitText
        net.minorUnits < 0L -> lossText
        else -> evenText
    }
    val outcomeColor = when {
        net.minorUnits > 0L -> financeColors.income
        net.minorUnits < 0L -> financeColors.expense
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = outcomeText,
                    style = MaterialTheme.typography.titleSmall,
                    color = outcomeColor,
                )
                SummaryLine(
                    label = stringResource(R.string.transactions_day_credit),
                    value = MoneyFormatter.format(creditTotal),
                    valueColor = financeColors.income,
                )
                SummaryLine(
                    label = stringResource(R.string.transactions_day_debit),
                    value = MoneyFormatter.format(debitTotal),
                    valueColor = financeColors.expense,
                )
                SummaryLine(
                    label = stringResource(R.string.transactions_day_net),
                    value = formatNet(net),
                    valueColor = outcomeColor,
                )
                SummaryLine(
                    label = stringResource(R.string.transactions_day_count),
                    value = transactionCount.toString(),
                )
                if (uncategorisedCount > 0) {
                    SummaryLine(
                        label = stringResource(R.string.transactions_day_uncategorised),
                        value = uncategorisedCount.toString(),
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
private fun AmountPresetChips(
    minAmountMinorUnits: Long?,
    maxAmountInput: String,
    onAnyAmount: () -> Unit,
    onPreset: (Long) -> Unit,
) {
    val customRange = maxAmountInput.isNotBlank()
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = minAmountMinorUnits == null && !customRange,
            onClick = onAnyAmount,
            label = { Text(stringResource(R.string.transactions_amount_any)) },
        )
        listOf(500L, 1_000L, 5_000L).forEach { rupees ->
            FilterChip(
                selected = !customRange && minAmountMinorUnits == rupees * 100L,
                onClick = { onPreset(rupees) },
                label = {
                    Text(stringResource(R.string.transactions_amount_at_least, rupees.toString()))
                },
            )
        }
    }
}

@Composable
private fun FiltersButton(
    active: Boolean,
    onClick: () -> Unit,
) {
    BadgedBox(
        badge = {
            if (active) {
                Badge()
            }
        },
    ) {
        FilterChip(
            selected = active,
            onClick = onClick,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            label = { Text(stringResource(R.string.transactions_filters)) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersSheet(
    uiState: TransactionListUiState,
    onDismiss: () -> Unit,
    onTypeClick: (TransactionType?) -> Unit,
    onSourceClick: (TransactionSource?) -> Unit,
    onAnyAmount: () -> Unit,
    onAmountPreset: (Long) -> Unit,
    onMinAmount: (String) -> Unit,
    onMaxAmount: (String) -> Unit,
    onToggleCategory: (Long) -> Unit,
    onToggleAccount: (String?) -> Unit,
    onExcludePromotional: (Boolean) -> Unit,
    onIncludeIgnored: (Boolean) -> Unit,
    onUncategorisedOnly: (Boolean) -> Unit,
    onClear: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.transactions_filters),
                style = MaterialTheme.typography.titleMedium,
            )
            FilterSectionLabel(stringResource(R.string.transactions_filter_type))
            TypeFilterChips(
                selectedType = uiState.selectedType,
                onTypeClick = onTypeClick,
            )
            FilterSectionLabel(stringResource(R.string.transactions_filter_source))
            SourceFilterChips(
                selectedSource = uiState.selectedSource,
                onSourceClick = onSourceClick,
            )
            FilterSectionLabel(stringResource(R.string.transactions_filter_amount))
            AmountPresetChips(
                minAmountMinorUnits = uiState.minAmountMinorUnits,
                maxAmountInput = uiState.maxAmountInput,
                onAnyAmount = onAnyAmount,
                onPreset = onAmountPreset,
            )
            Text(
                text = stringResource(R.string.transactions_amount_range_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = uiState.minAmountInput,
                    onValueChange = onMinAmount,
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.transactions_amount_min)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = uiState.maxAmountInput,
                    onValueChange = onMaxAmount,
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.transactions_amount_max)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            FilterChip(
                selected = uiState.uncategorisedOnly,
                onClick = { onUncategorisedOnly(!uiState.uncategorisedOnly) },
                label = { Text(stringResource(R.string.transactions_filter_uncategorised)) },
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.transactions_filter_hide_promo),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = uiState.excludePromotional,
                    onCheckedChange = onExcludePromotional,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.transactions_filter_show_hidden),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = uiState.includeIgnored,
                    onCheckedChange = onIncludeIgnored,
                )
            }
            if (uiState.categories.isNotEmpty()) {
                FilterSectionLabel(stringResource(R.string.transactions_filter_category))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.categories.forEach { category ->
                        FilterChip(
                            selected = category.id in uiState.selectedCategoryIds,
                            onClick = { onToggleCategory(category.id) },
                            label = { Text(category.name) },
                        )
                    }
                }
            }
            if (uiState.accounts.isNotEmpty()) {
                FilterSectionLabel(stringResource(R.string.transactions_filter_account))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.accounts.forEach { account ->
                        FilterChip(
                            selected = uiState.selectedAccount == account,
                            onClick = { onToggleAccount(account) },
                            label = { Text("••$account") },
                        )
                    }
                }
            }
            if (uiState.hasSheetFilters) {
                TextButton(onClick = onClear) {
                    Text(stringResource(R.string.transactions_clear_filters))
                }
            }
        }
    }
}

@Composable
private fun FilterSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MonthFilterChips(
    availableMonths: List<YearMonth>,
    selectedMonth: YearMonth?,
    onMonthClick: (YearMonth?) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedMonth == null,
            onClick = { onMonthClick(null) },
            label = { Text(stringResource(R.string.transactions_filter_all_months)) },
        )
        availableMonths.forEach { month ->
            FilterChip(
                selected = selectedMonth == month,
                onClick = { onMonthClick(month) },
                label = { Text(DateTimeFormatters.shortMonthLabel(month)) },
            )
        }
    }
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
        FilterChip(
            selected = selectedSource == null,
            onClick = { onSourceClick(null) },
            label = { Text(stringResource(R.string.transactions_filter_all)) },
        )
        TransactionSource.entries.forEach { source ->
            FilterChip(
                selected = selectedSource == source,
                onClick = { onSourceClick(source) },
                label = { Text(stringResource(source.labelRes)) },
            )
        }
    }
}

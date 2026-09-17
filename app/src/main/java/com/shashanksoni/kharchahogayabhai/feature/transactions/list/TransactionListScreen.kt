package com.shashanksoni.kharchahogayabhai.feature.transactions.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashanksoni.kharchahogayabhai.R
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import com.shashanksoni.kharchahogayabhai.ui.component.TransactionRow
import com.shashanksoni.kharchahogayabhai.ui.util.labelRes

@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
            item(key = "header-${dayGroup.date}") {
                Text(
                    text = dayGroup.header,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                )
            }
            items(items = dayGroup.entries, key = { it.transaction.id }) { entry ->
                TransactionRow(
                    transaction = entry.transaction,
                    category = entry.category,
                    onClick = { onTransactionClick(entry.transaction.id) },
                )
            }
            item(key = "divider-${dayGroup.date}") {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
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

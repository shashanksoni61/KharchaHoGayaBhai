package com.shashanksoni.kharchahogayabhai.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashanksoni.kharchahogayabhai.R
import com.shashanksoni.kharchahogayabhai.core.common.DateTimeFormatters
import com.shashanksoni.kharchahogayabhai.core.common.MoneyFormatter
import com.shashanksoni.kharchahogayabhai.domain.model.CategorySpending
import com.shashanksoni.kharchahogayabhai.domain.model.Money
import com.shashanksoni.kharchahogayabhai.domain.model.MonthTotals
import com.shashanksoni.kharchahogayabhai.domain.model.MonthlyDashboard
import com.shashanksoni.kharchahogayabhai.ui.component.CategoryAvatar
import com.shashanksoni.kharchahogayabhai.ui.component.DailySpendingChart
import com.shashanksoni.kharchahogayabhai.ui.component.MonthNavigator
import com.shashanksoni.kharchahogayabhai.ui.component.SectionCard
import com.shashanksoni.kharchahogayabhai.ui.theme.HeadlineAmountStyle
import com.shashanksoni.kharchahogayabhai.ui.theme.financeColors
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onSeeAllTransactions: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(
        uiState = uiState,
        onPreviousMonth = viewModel::showPreviousMonth,
        onNextMonth = viewModel::showNextMonth,
        onSeeAllTransactions = onSeeAllTransactions,
        modifier = modifier,
        contentPadding = contentPadding,
    )
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSeeAllTransactions: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val dashboard = uiState.dashboard

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MonthNavigator(
                month = uiState.selectedMonth,
                canSelectNextMonth = uiState.canSelectNextMonth,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }

        if (dashboard == null) {
            item { LoadingPlaceholder() }
            return@LazyColumn
        }

        if (!dashboard.hasData) {
            item {
                Text(
                    text = stringResource(R.string.dashboard_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                )
            }
            return@LazyColumn
        }

        item {
            MonthTotalsCard(
                dashboard = dashboard,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item {
            SectionCard(
                title = stringResource(R.string.dashboard_spending_by_category),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                dashboard.categoryBreakdown.forEach { spending ->
                    CategorySpendingRow(spending = spending)
                }
            }
        }

        item {
            SectionCard(
                title = stringResource(R.string.dashboard_daily_spending),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                DailySpendingChart(days = dashboard.dailySpending)
            }
        }

        item {
            SectionCard(
                title = stringResource(R.string.dashboard_transaction_count),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                CountRow(
                    label = stringResource(R.string.dashboard_total_transactions),
                    value = dashboard.totals.transactionCount,
                )
                CountRow(
                    label = stringResource(R.string.dashboard_income),
                    value = dashboard.totals.incomeCount,
                )
                CountRow(
                    label = stringResource(R.string.dashboard_expenses),
                    value = dashboard.totals.expenseCount,
                )
            }
        }

        item {
            TextButton(
                onClick = onSeeAllTransactions,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text(text = stringResource(R.string.dashboard_see_all))
            }
        }
    }
}

@Composable
private fun LoadingPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MonthTotalsCard(
    dashboard: MonthlyDashboard,
    modifier: Modifier = Modifier,
) {
    val totals = dashboard.totals
    SectionCard(
        title = DateTimeFormatters.monthLabel(dashboard.month),
        modifier = modifier,
    ) {
        Text(
            text = MoneyFormatter.format(totals.net.absoluteValue),
            style = HeadlineAmountStyle,
            color = if (totals.net.minorUnits < 0) financeColors.expense else financeColors.income,
        )
        Text(
            text = stringResource(R.string.dashboard_net),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            TotalColumn(
                label = stringResource(R.string.dashboard_income),
                amount = totals.income,
                color = financeColors.income,
                modifier = Modifier.weight(1f),
            )
            TotalColumn(
                label = stringResource(R.string.dashboard_expenses),
                amount = totals.expenses,
                color = financeColors.expense,
                modifier = Modifier.weight(1f),
            )
        }

        PreviousMonthComparison(dashboard = dashboard)
    }
}

@Composable
private fun TotalColumn(
    label: String,
    amount: Money,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = MoneyFormatter.format(amount),
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            color = color,
        )
    }
}

@Composable
private fun PreviousMonthComparison(dashboard: MonthlyDashboard) {
    val previousTotals: MonthTotals = dashboard.previousMonthTotals ?: return
    val changeRatio = dashboard.expenseChangeRatio ?: return
    val percentage = (changeRatio * 100).roundToInt()
    val comparison = if (percentage >= 0) {
        R.string.dashboard_spent_more_than
    } else {
        R.string.dashboard_spent_less_than
    }

    Text(
        text = stringResource(
            comparison,
            abs(percentage),
            DateTimeFormatters.shortMonthLabel(dashboard.previousMonth),
            MoneyFormatter.format(previousTotals.expenses),
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CategorySpendingRow(spending: CategorySpending) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryAvatar(category = spending.category, size = 32)
            Text(
                text = spending.category?.name ?: stringResource(R.string.dashboard_uncategorised),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = MoneyFormatter.format(spending.amount),
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            )
        }
        LinearProgressIndicator(
            progress = { spending.shareOfExpenses },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CountRow(label: String, value: Int) {
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
            text = value.toString(),
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
        )
    }
}

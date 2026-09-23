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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashanksoni.kharchahogayabhai.R
import com.shashanksoni.kharchahogayabhai.appContainer
import com.shashanksoni.kharchahogayabhai.core.common.DateTimeFormatters
import com.shashanksoni.kharchahogayabhai.core.common.MoneyFormatter
import com.shashanksoni.kharchahogayabhai.domain.model.MonthlyDashboard
import com.shashanksoni.kharchahogayabhai.security.rememberSecurityAuth
import com.shashanksoni.kharchahogayabhai.ui.component.CategoryDonutChart
import com.shashanksoni.kharchahogayabhai.ui.component.DailySpendingChart
import com.shashanksoni.kharchahogayabhai.ui.component.DonutSlice
import com.shashanksoni.kharchahogayabhai.ui.component.MonthNavigator
import com.shashanksoni.kharchahogayabhai.ui.component.SectionCard
import com.shashanksoni.kharchahogayabhai.ui.component.SpendingCategoryGrid
import com.shashanksoni.kharchahogayabhai.ui.component.TransactionRow
import com.shashanksoni.kharchahogayabhai.ui.theme.HeadlineAmountStyle
import com.shashanksoni.kharchahogayabhai.ui.theme.chartColorAt

private const val HIDDEN_AMOUNT = "••••••"

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onSeeAllTransactions: (java.time.YearMonth) -> Unit,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val container = LocalContext.current.appContainer
    val incomeRevealed by container.securitySession.incomeRevealedFlow
        .collectAsStateWithLifecycle()
    val incomeHidden = !incomeRevealed
    val auth = rememberSecurityAuth()
    val revealTitle = stringResource(R.string.security_reveal_income_title)
    val revealSubtitle = stringResource(R.string.security_reveal_income_subtitle)
    var authError by remember { mutableStateOf<String?>(null) }

    DashboardContent(
        uiState = uiState,
        incomeHidden = incomeHidden,
        onToggleIncomeVisibility = {
            if (incomeRevealed) {
                container.securitySession.hideIncome()
            } else {
                authError = null
                auth.prompt(
                    title = revealTitle,
                    subtitle = revealSubtitle,
                    onSuccess = { container.securitySession.revealIncome() },
                    onFailed = { message -> authError = message },
                )
            }
        },
        authError = authError,
        onPreviousMonth = viewModel::showPreviousMonth,
        onNextMonth = viewModel::showNextMonth,
        onSeeAllTransactions = { onSeeAllTransactions(uiState.selectedMonth) },
        onTransactionClick = onTransactionClick,
        onSetCategory = viewModel::setCategory,
        modifier = modifier,
        contentPadding = contentPadding,
    )
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    incomeHidden: Boolean,
    onToggleIncomeVisibility: () -> Unit,
    authError: String?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSeeAllTransactions: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onSetCategory: (Long, Long?) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val dashboard = uiState.dashboard

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
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

        item {
            BalanceCard(
                dashboard = dashboard,
                incomeHidden = incomeHidden,
                onToggleIncomeVisibility = onToggleIncomeVisibility,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        authError?.let { message ->
            item {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }

        if (!dashboard.hasData) {
            item {
                Text(
                    text = stringResource(R.string.dashboard_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            return@LazyColumn
        }

        item {
            InsightsCard(
                dashboard = dashboard,
                incomeHidden = incomeHidden,
                onClick = onSeeAllTransactions,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (dashboard.categoryBreakdown.isNotEmpty()) {
            item {
                SectionCard(
                    title = stringResource(R.string.dashboard_spending_categories),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    SpendingCategoryGrid(
                        items = dashboard.categoryBreakdown.take(4),
                        uncategorisedLabel = stringResource(R.string.dashboard_uncategorised),
                    )
                }
            }
        }

        if (dashboard.recentTransactions.isNotEmpty()) {
            item {
                SectionCard(
                    title = stringResource(R.string.dashboard_operations),
                    trailing = {
                        TextButton(onClick = onSeeAllTransactions) {
                            Text(stringResource(R.string.dashboard_see_all))
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    val categoriesById = dashboard.categories.associateBy { it.id }
                    dashboard.recentTransactions.forEach { transaction ->
                        TransactionRow(
                            transaction = transaction,
                            category = transaction.categoryId?.let(categoriesById::get),
                            onClick = { onTransactionClick(transaction.id) },
                            categories = dashboard.categories,
                            onCategorySelected = { categoryId ->
                                onSetCategory(transaction.id, categoryId)
                            },
                            hideIncome = incomeHidden,
                        )
                    }
                }
            }
        }

        item {
            SectionCard(
                title = stringResource(R.string.dashboard_daily_spending),
                onClick = onSeeAllTransactions,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                DailySpendingChart(days = dashboard.dailySpending)
            }
        }
    }
}

@Composable
private fun BalanceCard(
    dashboard: MonthlyDashboard,
    incomeHidden: Boolean,
    onToggleIncomeVisibility: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totals = dashboard.totals
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.dashboard_income),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onToggleIncomeVisibility) {
                    Icon(
                        imageVector = if (incomeHidden) {
                            Icons.Rounded.VisibilityOff
                        } else {
                            Icons.Rounded.Visibility
                        },
                        contentDescription = stringResource(
                            if (incomeHidden) {
                                R.string.dashboard_show_income
                            } else {
                                R.string.dashboard_hide_income
                            },
                        ),
                    )
                }
            }
            Text(
                text = if (incomeHidden) HIDDEN_AMOUNT else MoneyFormatter.format(totals.income),
                style = HeadlineAmountStyle,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.dashboard_spent_this_month),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = MoneyFormatter.format(totals.expenses),
                    style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                )
            }
        }
    }
}

@Composable
private fun InsightsCard(
    dashboard: MonthlyDashboard,
    incomeHidden: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val slices = remember(dashboard.categoryBreakdown) {
        val top = dashboard.categoryBreakdown.take(5)
        val restShare = dashboard.categoryBreakdown.drop(5).sumOf { it.shareOfExpenses.toDouble() }.toFloat()
        buildList {
            top.forEachIndexed { index, spending ->
                add(
                    DonutSlice(
                        label = spending.category?.name.orEmpty(),
                        share = spending.shareOfExpenses,
                        color = chartColorAt(index),
                    ),
                )
            }
            if (restShare > 0.01f) {
                add(DonutSlice(label = "", share = restShare, color = chartColorAt(5)))
            }
        }
    }

    SectionCard(
        title = stringResource(R.string.dashboard_insights),
        onClick = onClick,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            CategoryDonutChart(
                slices = slices,
                centerTitle = stringResource(
                    R.string.dashboard_spent_in,
                    DateTimeFormatters.monthLabel(dashboard.month),
                ),
                centerAmount = MoneyFormatter.format(dashboard.totals.expenses),
            )
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

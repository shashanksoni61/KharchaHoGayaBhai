package com.shashanksoni.kharchahogayabhai.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.shashanksoni.kharchahogayabhai.R
import com.shashanksoni.kharchahogayabhai.core.common.DateTimeFormatters
import java.time.YearMonth

/** Month stepper for the dashboard. Stepping past the current month is blocked. */
@Composable
fun MonthNavigator(
    month: YearMonth,
    canSelectNextMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.Rounded.ChevronLeft,
                contentDescription = stringResource(R.string.dashboard_previous_month),
            )
        }
        Text(
            text = DateTimeFormatters.monthLabel(month),
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(onClick = onNextMonth, enabled = canSelectNextMonth) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = stringResource(R.string.dashboard_next_month),
            )
        }
    }
}

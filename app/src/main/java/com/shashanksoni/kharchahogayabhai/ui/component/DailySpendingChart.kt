package com.shashanksoni.kharchahogayabhai.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.shashanksoni.kharchahogayabhai.core.common.DateTimeFormatters
import com.shashanksoni.kharchahogayabhai.core.common.MoneyFormatter
import com.shashanksoni.kharchahogayabhai.domain.model.DailySpending

/**
 * Spending per day for one month as a bar chart.
 *
 * Drawn directly on a Canvas: a single bar chart is not worth a charting
 * dependency, and this keeps the app free of third-party UI libraries.
 */
@Composable
fun DailySpendingChart(
    days: List<DailySpending>,
    modifier: Modifier = Modifier,
    barHeight: Int = 120,
) {
    if (days.isEmpty()) return

    val peak = days.maxOf { it.amount.minorUnits }
    val barColor = MaterialTheme.colorScheme.primary
    val baselineColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier) {
        Text(
            text = "Busiest day ${MoneyFormatter.formatCompact(days.maxBy { it.amount.minorUnits }.amount)}",
            style = labelStyle,
            color = labelColor,
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight.dp),
        ) {
            val slotWidth = size.width / days.size
            val barWidth = (slotWidth * 0.6f).coerceAtLeast(1.5f)
            val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

            days.forEachIndexed { index, day ->
                val fraction = if (peak == 0L) 0f else day.amount.minorUnits.toFloat() / peak
                val height = (size.height * fraction).coerceAtLeast(if (fraction > 0f) 2f else 0f)
                if (height <= 0f) return@forEachIndexed
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(
                        x = index * slotWidth + (slotWidth - barWidth) / 2f,
                        y = size.height - height,
                    ),
                    size = Size(width = barWidth, height = height),
                    cornerRadius = cornerRadius,
                )
            }

            drawLine(
                color = baselineColor,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = Stroke.HairlineWidth.coerceAtLeast(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf(days.first(), days[days.size / 2], days.last()).forEach { day ->
                Text(
                    text = DateTimeFormatters.dayOfMonth(day.date),
                    style = labelStyle,
                    color = labelColor,
                )
            }
        }
    }
}

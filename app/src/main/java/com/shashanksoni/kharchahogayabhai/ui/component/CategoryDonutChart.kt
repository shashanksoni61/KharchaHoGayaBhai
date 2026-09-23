package com.shashanksoni.kharchahogayabhai.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.shashanksoni.kharchahogayabhai.ui.theme.chartColorAt
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class DonutSlice(
    val label: String,
    val share: Float,
    val color: Color,
)

/**
 * Soft-gapped donut with percentage labels sitting on the ring, matching the
 * money-manager insights mock.
 */
@Composable
fun CategoryDonutChart(
    slices: List<DonutSlice>,
    centerTitle: String,
    centerAmount: String,
    modifier: Modifier = Modifier,
    chartSize: Int = 260,
) {
    val visible = slices.filter { it.share > 0.01f }.ifEmpty { slices }
    val density = LocalDensity.current
    val labelRadiusPx = with(density) { (chartSize / 2f - 18f).dp.toPx() }

    Box(
        modifier = modifier.size(chartSize.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 22.dp.toPx()
            val inset = stroke / 2f + 28.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            val topLeft = Offset(inset, inset)
            val gap = 6f
            var start = -90f
            val totalShare = visible.sumOf { it.share.toDouble() }.toFloat().coerceAtLeast(0.0001f)
            visible.forEach { slice ->
                val sweep = (slice.share / totalShare) * (360f - gap * visible.size)
                drawArc(
                    color = slice.color,
                    startAngle = start + gap / 2f,
                    sweepAngle = sweep.coerceAtLeast(0f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                start += sweep + gap
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerTitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = centerAmount,
                style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                textAlign = TextAlign.Center,
            )
        }

        var start = -90f
        val totalShare = visible.sumOf { it.share.toDouble() }.toFloat().coerceAtLeast(0.0001f)
        visible.forEach { slice ->
            val sweep = (slice.share / totalShare) * (360f - 6f * visible.size)
            val mid = Math.toRadians((start + 3f + sweep / 2f).toDouble())
            val x = (cos(mid) * labelRadiusPx).roundToInt()
            val y = (sin(mid) * labelRadiusPx).roundToInt()
            Text(
                text = "${(slice.share * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = slice.color,
                modifier = Modifier.offset { IntOffset(x, y) },
            )
            start += sweep + 6f
        }
    }
}

fun donutSlicesOf(shares: List<Pair<String, Float>>): List<DonutSlice> =
    shares.mapIndexed { index, (label, share) ->
        DonutSlice(label = label, share = share, color = chartColorAt(index))
    }

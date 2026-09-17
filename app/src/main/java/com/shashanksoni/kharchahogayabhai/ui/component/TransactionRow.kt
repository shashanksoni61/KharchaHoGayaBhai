package com.shashanksoni.kharchahogayabhai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shashanksoni.kharchahogayabhai.domain.model.Category
import com.shashanksoni.kharchahogayabhai.domain.model.Transaction
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import com.shashanksoni.kharchahogayabhai.ui.util.CategoryVisuals

/** One transaction in a list: category icon, title, subtitle, signed amount. */
@Composable
fun TransactionRow(
    transaction: Transaction,
    category: Category?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryAvatar(category = category)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.displayTitle,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = category?.name ?: "Uncategorised",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                SourceBadge(source = transaction.primarySource)
            }
        }

        SignedAmountText(amount = transaction.amount, type = transaction.type)
    }
}

@Composable
fun CategoryAvatar(
    category: Category?,
    modifier: Modifier = Modifier,
    size: Int = 40,
) {
    val tint = CategoryVisuals.colorOf(category?.colorHex, MaterialTheme.colorScheme.primary)
    Box(
        modifier = modifier
            .size(size.dp)
            .background(color = tint.copy(alpha = 0.16f), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = CategoryVisuals.iconFor(category?.iconKey),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size((size * 0.55f).dp),
        )
    }
}

/** Marks which source a transaction came from, so provenance is visible at a glance. */
@Composable
fun SourceBadge(
    source: TransactionSource,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Icon(
        imageVector = CategoryVisuals.iconFor(source),
        contentDescription = source.name,
        tint = tint,
        modifier = modifier
            .size(14.dp)
            .alpha(0.8f),
    )
}

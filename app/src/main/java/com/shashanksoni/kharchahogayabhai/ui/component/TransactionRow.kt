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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shashanksoni.kharchahogayabhai.R
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
    categories: List<Category> = emptyList(),
    onCategorySelected: ((Long?) -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onCategorySelected != null && categories.isNotEmpty()) {
            CategoryQuickPicker(
                category = category,
                categories = categories,
                onCategorySelected = onCategorySelected,
            )
        } else {
            CategoryAvatar(category = category)
        }

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
                    text = category?.name ?: stringResource(R.string.dashboard_uncategorised),
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

/**
 * Category avatar that opens an in-place menu so the user can assign a category
 * without leaving the list (especially useful for the uncategorised "?" icon).
 */
@Composable
fun CategoryQuickPicker(
    category: Category?,
    categories: List<Category>,
    onCategorySelected: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        CategoryAvatar(
            category = category,
            modifier = Modifier.clickable { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.dashboard_uncategorised)) },
                onClick = {
                    expanded = false
                    onCategorySelected(null)
                },
                leadingIcon = {
                    CategoryAvatar(category = null, size = 24)
                },
                trailingIcon = {
                    if (category == null) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                },
            )
            categories.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = {
                        expanded = false
                        onCategorySelected(option.id)
                    },
                    leadingIcon = {
                        CategoryAvatar(category = option, size = 24)
                    },
                    trailingIcon = {
                        if (option.id == category?.id) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                )
            }
        }
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
            contentDescription = category?.name
                ?: stringResource(R.string.transactions_pick_category),
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

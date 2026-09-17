package com.shashanksoni.kharchahogayabhai.ui.component

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.shashanksoni.kharchahogayabhai.core.common.MoneyFormatter
import com.shashanksoni.kharchahogayabhai.domain.model.Money
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import com.shashanksoni.kharchahogayabhai.ui.theme.financeColors

/** An amount with its direction shown by both a sign and a colour. */
@Composable
fun SignedAmountText(
    amount: Money,
    type: TransactionType,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
) {
    val colors = financeColors
    Text(
        text = MoneyFormatter.formatSigned(amount, type),
        modifier = modifier,
        color = if (type == TransactionType.DEBIT) colors.expense else colors.income,
        style = style.copy(fontFeatureSettings = "tnum"),
        fontWeight = FontWeight.Medium,
        maxLines = 1,
    )
}

/** An unsigned amount, for totals whose meaning is already clear from its label. */
@Composable
fun AmountText(
    amount: Money,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    alwaysShowFraction: Boolean = false,
) {
    Text(
        text = MoneyFormatter.format(amount, alwaysShowFraction),
        modifier = modifier,
        color = color,
        style = style.copy(fontFeatureSettings = "tnum"),
        maxLines = 1,
    )
}

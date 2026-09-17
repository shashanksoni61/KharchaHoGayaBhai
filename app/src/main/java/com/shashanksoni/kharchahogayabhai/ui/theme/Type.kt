package com.shashanksoni.kharchahogayabhai.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal val KharchaTypography = Typography()

/**
 * Amounts use tabular figures so digits line up in columns; ragged rupee values
 * in a list are hard to scan.
 */
val TabularNumberStyle = TextStyle(fontFeatureSettings = "tnum")

val HeadlineAmountStyle = TextStyle(
    fontSize = 32.sp,
    lineHeight = 38.sp,
    fontWeight = FontWeight.SemiBold,
    fontFeatureSettings = "tnum",
)

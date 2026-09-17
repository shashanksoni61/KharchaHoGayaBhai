package com.shashanksoni.kharchahogayabhai.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalAtm
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource

/**
 * Maps the domain's icon keys and colour hexes onto drawables and colours.
 *
 * The domain stores keys rather than resources, so categories stay free of
 * Android types and this mapping is the only place that has to change when the
 * icon set does.
 */
object CategoryVisuals {

    fun iconFor(iconKey: String?): ImageVector = when (iconKey) {
        "food" -> Icons.Rounded.Restaurant
        "shopping" -> Icons.Rounded.ShoppingBag
        "transport" -> Icons.Rounded.DirectionsCar
        "bills" -> Icons.Rounded.ReceiptLong
        "entertainment" -> Icons.Rounded.Movie
        "travel" -> Icons.Rounded.Flight
        "healthcare" -> Icons.Rounded.LocalHospital
        "education" -> Icons.Rounded.School
        "rent" -> Icons.Rounded.Home
        "salary" -> Icons.Rounded.Payments
        "investment" -> Icons.Rounded.TrendingUp
        "transfer" -> Icons.Rounded.SwapHoriz
        "cash" -> Icons.Rounded.LocalAtm
        "other" -> Icons.Rounded.Category
        else -> Icons.Rounded.HelpOutline
    }

    fun iconFor(source: TransactionSource): ImageVector = when (source) {
        TransactionSource.SMS -> Icons.Rounded.Sms
        TransactionSource.CSV -> Icons.Rounded.TableChart
        TransactionSource.PDF -> Icons.Rounded.PictureAsPdf
        TransactionSource.MANUAL -> Icons.Rounded.EditNote
    }

    /** Parses an `RRGGBB` hex; falls back to [fallback] for anything unexpected. */
    fun colorOf(colorHex: String?, fallback: Color): Color {
        val hex = colorHex?.removePrefix("#")?.takeIf { it.length == 6 } ?: return fallback
        return hex.toLongOrNull(radix = 16)
            ?.let { rgb -> Color(rgb or 0xFF000000L) }
            ?: fallback
    }
}

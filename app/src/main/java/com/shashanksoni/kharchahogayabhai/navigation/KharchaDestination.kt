package com.shashanksoni.kharchahogayabhai.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.shashanksoni.kharchahogayabhai.R

sealed interface KharchaDestination {
    val route: String

    sealed interface TopLevel : KharchaDestination {
        @get:StringRes
        val labelRes: Int
        val icon: ImageVector
    }

    data object Dashboard : TopLevel {
        override val route = "dashboard"
        override val labelRes = R.string.nav_dashboard
        override val icon = Icons.Rounded.PieChart
    }

    data object Transactions : TopLevel {
        override val route = "transactions"
        override val labelRes = R.string.nav_transactions
        override val icon = Icons.AutoMirrored.Rounded.ReceiptLong
    }

    data object Settings : TopLevel {
        override val route = "settings"
        override val labelRes = R.string.nav_settings
        override val icon = Icons.Rounded.Settings
    }

    /** Nested under Settings — CSV / PDF / SMS import is infrequent. */
    data object Import : KharchaDestination {
        override val route = "settings/import"
    }

    data object TransactionDetail : KharchaDestination {
        const val TRANSACTION_ID_ARG = "transactionId"
        override val route = "transaction/{$TRANSACTION_ID_ARG}"

        fun routeTo(transactionId: Long) = "transaction/$transactionId"
    }

    companion object {
        val topLevelDestinations = listOf(Dashboard, Transactions, Settings)
    }
}

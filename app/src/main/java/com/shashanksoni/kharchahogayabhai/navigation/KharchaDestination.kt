package com.shashanksoni.kharchahogayabhai.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.ui.graphics.vector.ImageVector
import com.shashanksoni.kharchahogayabhai.R

/**
 * Every screen in the app.
 *
 * Import and settings destinations join this list in later phases; screens do not
 * know about each other, only about the callbacks they are given.
 */
sealed interface KharchaDestination {
    val route: String

    /** Reachable from the bottom bar. */
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
        override val icon = Icons.Rounded.ReceiptLong
    }

    data object TransactionDetail : KharchaDestination {
        const val TRANSACTION_ID_ARG = "transactionId"
        override val route = "transaction/{$TRANSACTION_ID_ARG}"

        fun routeTo(transactionId: Long) = "transaction/$transactionId"
    }

    companion object {
        val topLevelDestinations = listOf(Dashboard, Transactions)
    }
}

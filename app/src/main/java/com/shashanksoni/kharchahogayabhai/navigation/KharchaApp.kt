package com.shashanksoni.kharchahogayabhai.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shashanksoni.kharchahogayabhai.R
import com.shashanksoni.kharchahogayabhai.di.AppContainer
import com.shashanksoni.kharchahogayabhai.feature.dashboard.DashboardScreen
import com.shashanksoni.kharchahogayabhai.feature.dashboard.DashboardViewModel
import com.shashanksoni.kharchahogayabhai.feature.lock.AppLockScreen
import com.shashanksoni.kharchahogayabhai.feature.importfile.ImportScreen
import com.shashanksoni.kharchahogayabhai.feature.importfile.ImportViewModel
import com.shashanksoni.kharchahogayabhai.feature.settings.SettingsScreen
import com.shashanksoni.kharchahogayabhai.feature.settings.SettingsViewModel
import com.shashanksoni.kharchahogayabhai.feature.transactions.detail.TransactionDetailScreen
import com.shashanksoni.kharchahogayabhai.feature.transactions.detail.TransactionDetailViewModel
import com.shashanksoni.kharchahogayabhai.feature.transactions.list.TransactionListScreen
import com.shashanksoni.kharchahogayabhai.feature.transactions.list.TransactionListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KharchaApp(container: AppContainer) {
    val appLockEnabled by container.securityPreferences.appLockEnabledFlow
        .collectAsStateWithLifecycle()
    val appUnlocked by container.securitySession.appUnlockedFlow
        .collectAsStateWithLifecycle()
    if (appLockEnabled && !appUnlocked) {
        AppLockScreen(onUnlocked = container.securitySession::unlockApp)
        return
    }

    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val isTopLevel = KharchaDestination.topLevelDestinations.any { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(titleResOf(currentDestination?.route))) },
                navigationIcon = {
                    if (!isTopLevel) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (isTopLevel) {
                BottomNavigation(
                    navController = navController,
                    currentRoute = currentDestination?.route,
                )
            }
        },
    ) { innerPadding ->
        KharchaNavHost(
            navController = navController,
            container = container,
            contentPadding = innerPadding,
        )
    }
}

@Composable
private fun BottomNavigation(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        KharchaDestination.topLevelDestinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { navController.navigateToTopLevel(destination) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.labelRes)) },
            )
        }
    }
}

@Composable
private fun KharchaNavHost(
    navController: NavHostController,
    container: AppContainer,
    contentPadding: PaddingValues,
) {
    NavHost(
        navController = navController,
        startDestination = KharchaDestination.Dashboard.route,
    ) {
        composable(KharchaDestination.Dashboard.route) {
            DashboardScreen(
                viewModel = viewModel(factory = DashboardViewModel.factory(container)),
                onSeeAllTransactions = { month ->
                    container.pendingTransactionMonth.request(month)
                    navController.navigateToTopLevel(KharchaDestination.Transactions)
                },
                onTransactionClick = { transactionId ->
                    navController.navigate(
                        KharchaDestination.TransactionDetail.routeTo(transactionId),
                    )
                },
                contentPadding = contentPadding,
            )
        }

        composable(KharchaDestination.Transactions.route) {
            TransactionListScreen(
                viewModel = viewModel(factory = TransactionListViewModel.factory(container)),
                onTransactionClick = { transactionId ->
                    navController.navigate(
                        KharchaDestination.TransactionDetail.routeTo(transactionId),
                    )
                },
                contentPadding = contentPadding,
            )
        }

        composable(KharchaDestination.Settings.route) {
            SettingsScreen(
                viewModel = viewModel(factory = SettingsViewModel.factory(container)),
                onOpenImport = { navController.navigate(KharchaDestination.Import.route) },
                zone = container.zone,
                contentPadding = contentPadding,
            )
        }

        composable(KharchaDestination.Import.route) {
            ImportScreen(
                viewModel = viewModel(factory = ImportViewModel.factory(container)),
                zone = container.zone,
                contentPadding = contentPadding,
            )
        }

        composable(
            route = KharchaDestination.TransactionDetail.route,
            arguments = listOf(
                navArgument(KharchaDestination.TransactionDetail.TRANSACTION_ID_ARG) {
                    type = NavType.LongType
                },
            ),
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getLong(
                KharchaDestination.TransactionDetail.TRANSACTION_ID_ARG,
            ) ?: return@composable

            TransactionDetailScreen(
                viewModel = viewModel(
                    factory = TransactionDetailViewModel.factory(container, transactionId),
                ),
                zone = container.zone,
                onHidden = { navController.navigateUp() },
                contentPadding = contentPadding,
            )
        }
    }
}

/** Keeps one entry per tab and restores each tab's scroll position. */
private fun NavHostController.navigateToTopLevel(destination: KharchaDestination.TopLevel) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun titleResOf(route: String?): Int = when (route) {
    KharchaDestination.Transactions.route -> R.string.nav_transactions
    KharchaDestination.Import.route -> R.string.nav_import
    KharchaDestination.Settings.route -> R.string.nav_settings
    KharchaDestination.TransactionDetail.route -> R.string.detail_title
    else -> R.string.app_name
}

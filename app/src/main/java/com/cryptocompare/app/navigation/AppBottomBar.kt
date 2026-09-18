package com.cryptocompare.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cryptocompare.app.R
import com.cryptocompare.ui.theme.accentSoft
import com.cryptocompare.ui.theme.bgCard
import com.cryptocompare.ui.theme.textSecondary

/**
 * Две вкладки — «Пары» и «Портфель». Профиль остаётся иконкой в шапке каталога:
 * он про настройки и аккаунт, а не про рынок, и отдельная вкладка под него
 * забирала бы место у того, куда ходят каждый день.
 */
@Composable
internal fun AppBottomBar(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemColors =
        NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.accentSoft,
            unselectedIconColor = MaterialTheme.colorScheme.textSecondary,
            unselectedTextColor = MaterialTheme.colorScheme.textSecondary,
        )

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.bgCard,
    ) {
        NavigationBarItem(
            selected = selected == AppTab.PAIRS,
            onClick = { onSelect(AppTab.PAIRS) },
            icon = { Icon(imageVector = Icons.AutoMirrored.Outlined.ShowChart, contentDescription = null) },
            label = { Text(text = stringResource(R.string.tab_pairs)) },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = selected == AppTab.PORTFOLIO,
            onClick = { onSelect(AppTab.PORTFOLIO) },
            icon = { Icon(imageVector = Icons.Outlined.AccountBalanceWallet, contentDescription = null) },
            label = { Text(text = stringResource(R.string.tab_portfolio)) },
            colors = itemColors,
        )
    }
}

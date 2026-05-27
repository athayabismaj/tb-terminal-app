package com.tbterminal.app.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.dashboard.cashier.CashierDashboardShell
import com.tbterminal.app.ui.dashboard.cashier.CashierDestination

@Composable
fun CashierProfileScreen(
    userName: String,
    role: String,
    isActive: Boolean,
    joinedAt: String,
    lastLoginAt: String?,
    onDashboardClick: () -> Unit = {},
    onPosClick: () -> Unit = {},
    onCashSessionClick: () -> Unit = {},
    onTransactionHistoryClick: () -> Unit = {},
    onStockCheckClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit
) {
    CashierDashboardShell(
        userName = userName,
        role = role,
        activeDestination = CashierDestination.Profile,
        onDashboardClick = onDashboardClick,
        onPosClick = onPosClick,
        onCashSessionClick = onCashSessionClick,
        onTransactionHistoryClick = onTransactionHistoryClick,
        onStockCheckClick = onStockCheckClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        SharedProfileScreen(
            userName = userName,
            role = role,
            isActive = isActive,
            joinedAt = joinedAt,
            lastLoginAt = lastLoginAt,
            modifier = contentModifier
        )
    }
}

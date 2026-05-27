package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.dashboard.DashboardBackground

@Composable
fun OwnerDashboardShell(
    userName: String,
    role: String,
    activeDestination: OwnerDestination,
    onDashboardClick: () -> Unit,
    onUserManagementClick: () -> Unit,
    onSecurityLogClick: () -> Unit = {},
    onLogout: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(DashboardBackground)
    ) {
        OwnerDashboardSidebar(
            activeDestination = activeDestination,
            onDashboardClick = onDashboardClick,
            onUserManagementClick = onUserManagementClick,
            onSecurityLogClick = onSecurityLogClick,
            onLogout = onLogout,
            modifier = Modifier.width(260.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
            .fillMaxHeight()
        ) {
            OwnerDashboardHeader(userName = userName, role = role)
            content(Modifier.weight(1f))
        }
    }
}

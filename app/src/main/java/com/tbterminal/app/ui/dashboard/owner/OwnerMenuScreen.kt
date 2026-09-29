package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.AuthRepository
import com.tbterminal.app.ui.components.TbPageSurface
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.settings.ProfileViewModel

@Composable
fun OwnerMenuScreen(
    name: String,
    role: String,
    authRepository: AuthRepository,
    onDashboardClick: () -> Unit,
    onLogout: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(authRepository)),
) {
    val uiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalAdminDestinationNavigator.current
    val navigate: (AdminDestination) -> Unit = { destination -> navigator?.invoke(destination) }

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.MoreHub,
        onDashboardClick = onDashboardClick,
        onProductsClick = {},
        onProfileClick = { navigate(AdminDestination.Profile) },
        onLogout = onLogout,
    ) { contentModifier ->
        TbPageSurface(modifier = contentModifier, maxContentWidth = 1120.dp) { layout, pageModifier ->
            Column(
                modifier = pageModifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(layout.verticalSpacing),
            ) {
                uiState.error?.let { error ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = profileViewModel::loadProfile, enabled = !uiState.isLoading) {
                            Text("Coba lagi")
                        }
                    }
                }
                OwnerMenuContent(
                    layout = layout,
                    sessionName = name,
                    role = role,
                    profile = uiState.profile,
                    onProfileClick = { navigate(AdminDestination.Profile) },
                    onNavigate = navigate,
                )
            }
        }
    }
}

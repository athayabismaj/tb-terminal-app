package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.AuthRepository
import com.tbterminal.app.ui.components.TbAdaptiveBox
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
    onProfileClick: (() -> Unit)? = null,
    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(authRepository)),
) {
    val uiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalAdminDestinationNavigator.current
    val navigate: (AdminDestination) -> Unit = { destination -> navigator?.invoke(destination) }
    val effectiveOnProfileClick = onProfileClick ?: { navigate(AdminDestination.Profile) }

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.MoreHub,
        onDashboardClick = onDashboardClick,
        onProductsClick = {},
        onProfileClick = effectiveOnProfileClick,
        onLogout = onLogout,
    ) { contentModifier ->
        TbAdaptiveBox(
            modifier = contentModifier
                .fillMaxSize()
                .background(Color(0xFFF2F4F7)),
        ) { layout ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                val containerModifier = if (layout.isCompact) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = 1120.dp)
                        .padding(horizontal = layout.horizontalPadding, vertical = layout.verticalSpacing)
                }
                Column(
                    modifier = containerModifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(if (layout.isCompact) 12.dp else layout.verticalSpacing),
                ) {
                    uiState.error?.let { error ->
                        Column(
                            modifier = if (layout.isCompact) {
                                Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            } else {
                                Modifier
                            },
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
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
                        onProfileClick = effectiveOnProfileClick,
                        onNavigate = navigate,
                        onLogout = onLogout,
                    )
                }
            }
        }
    }
}

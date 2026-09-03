package com.tbterminal.app.ui.managerapproval

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.model.ManagerApprovalContext
import com.tbterminal.app.data.model.ManagerApprovalGrant
import com.tbterminal.app.data.repository.ManagerApprovalRepository

@Composable
fun ManagerApprovalDialog(
    context: ManagerApprovalContext,
    repository: ManagerApprovalRepository,
    onDismiss: () -> Unit,
    onApproved: (ManagerApprovalGrant) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManagerApprovalViewModel = viewModel(
        key = "manager-approval-${context.action}-${context.resourceId}",
        factory = ManagerApprovalViewModel.factory(context, repository),
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val usernameFocus = androidx.compose.runtime.remember { FocusRequester() }
    val pinFocus = androidx.compose.runtime.remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val submitting = state.phase == ManagerApprovalUiPhase.SUBMITTING

    LaunchedEffect(Unit) {
        usernameFocus.requestFocus()
    }

    LaunchedEffect(state.grant?.approvalId) {
        val grant = state.grant ?: return@LaunchedEffect
        viewModel.acknowledgeSuccess()
        onApproved(grant)
        onDismiss()
    }

    fun dismissSafely() {
        if (submitting) return
        focusManager.clearFocus(force = true)
        viewModel.clearSensitiveData()
        onDismiss()
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = ::dismissSafely,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                imageVector = Icons.Outlined.AdminPanelSettings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = "Persetujuan Manager Diperlukan",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Tindakan", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = context.action.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                OutlinedTextField(
                    value = state.approverUsername,
                    onValueChange = viewModel::onUsernameChanged,
                    enabled = !submitting,
                    label = { Text("Akun manager") },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { pinFocus.requestFocus() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(usernameFocus),
                    shape = RoundedCornerShape(14.dp),
                )
                OutlinedTextField(
                    value = state.approverPin,
                    onValueChange = viewModel::onPinChanged,
                    enabled = !submitting,
                    label = { Text("PIN manager") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        viewModel.submit()
                    }),
                    modifier = Modifier.fillMaxWidth().focusRequester(pinFocus),
                    shape = RoundedCornerShape(14.dp),
                )
                state.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = "PIN hanya digunakan untuk verifikasi dan tidak disimpan pada perangkat.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.submit()
                },
                enabled = !submitting,
                shape = RoundedCornerShape(12.dp),
            ) {
                if (submitting) {
                    Row(horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Memverifikasi")
                    }
                } else {
                    Text("Setujui")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = ::dismissSafely, enabled = !submitting) {
                Text("Batal")
            }
        },
    )
}

package com.tbterminal.app.ui.cashier.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.ManagerApprovalContext
import com.tbterminal.app.data.model.ManagerApprovalGrant
import com.tbterminal.app.data.model.RefundDisposition
import com.tbterminal.app.data.repository.ManagerApprovalRepository
import com.tbterminal.app.ui.managerapproval.ManagerApprovalDialog

@Composable
internal fun TransactionActionDialogs(
    state: CashierTransactionHistoryUiState,
    managerApprovalRepository: ManagerApprovalRepository,
    onDismissVoid: () -> Unit,
    onVoidReasonChanged: (String) -> Unit,
    onConfirmVoid: () -> Unit,
    onDismissRefund: () -> Unit,
    onRefundReasonChanged: (String) -> Unit,
    onRefundDispositionChanged: (RefundDisposition) -> Unit,
    onConfirmRefund: () -> Unit,
    onApprovalGranted: (ManagerApprovalGrant) -> Unit,
    onApprovalDismissed: () -> Unit,
) {
    if (state.isVoidDialogOpen) {
        TransactionReasonDialog(
            title = "Void transaksi",
            target = "Transaksi ${state.selectedTransaction?.receiptNumber() ?: "-"}",
            explanation = "Transaksi tidak dihapus. Stok, pembayaran, piutang, dan kas disesuaikan oleh server.",
            reason = state.voidReasonInput,
            error = state.voidErrorMessage,
            submitting = state.isSubmittingVoid,
            ambiguous = state.isVoidOutcomeAmbiguous,
            confirmLabel = "Konfirmasi void",
            onReasonChanged = onVoidReasonChanged,
            onDismiss = onDismissVoid,
            onConfirm = onConfirmVoid,
        )
    }
    if (state.isRefundDialogOpen) {
        RefundTransactionDialog(
            state = state,
            onReasonChanged = onRefundReasonChanged,
            onDispositionChanged = onRefundDispositionChanged,
            onDismiss = onDismissRefund,
            onConfirm = onConfirmRefund,
        )
    }
    val action = state.pendingManagerApprovalAction
    val transaction = state.selectedTransaction
    if (action != null && transaction != null) {
        ManagerApprovalDialog(
            context = ManagerApprovalContext(action = action, resourceId = transaction.id),
            repository = managerApprovalRepository,
            onDismiss = onApprovalDismissed,
            onApproved = onApprovalGranted,
        )
    }
}

@Composable
private fun TransactionReasonDialog(
    title: String,
    target: String,
    explanation: String,
    reason: String,
    error: String?,
    submitting: Boolean,
    ambiguous: Boolean,
    confirmLabel: String,
    onReasonChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(target, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(explanation, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChanged,
                    label = { Text("Alasan wajib") },
                    minLines = 3,
                    enabled = !submitting && !ambiguous,
                    supportingText = error?.let { message ->
                        { Text(message, color = MaterialTheme.colorScheme.error) }
                    },
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (ambiguous) {
                    Text(
                        "Alasan dikunci agar retry memakai request dan idempotency key yang sama.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !submitting && reason.trim().length >= 5,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
            ) {
                if (submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Memproses")
                } else {
                    Text(if (ambiguous) "Periksa status" else confirmLabel)
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !submitting) { Text("Batal") } },
    )
}

@Composable
private fun RefundTransactionDialog(
    state: CashierTransactionHistoryUiState,
    onReasonChanged: (String) -> Unit,
    onDispositionChanged: (RefundDisposition) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Full refund", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Transaksi ${state.selectedTransaction?.receiptNumber() ?: "-"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Nominal refund dihitung oleh server berdasarkan pembayaran transaksi.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text("Kondisi barang", fontWeight = FontWeight.SemiBold)
                RefundDisposition.entries.forEach { disposition ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = !state.isSubmittingRefund && !state.isRefundOutcomeAmbiguous,
                                onClick = { onDispositionChanged(disposition) },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = state.refundDisposition == disposition,
                            onClick = { onDispositionChanged(disposition) },
                            enabled = !state.isSubmittingRefund && !state.isRefundOutcomeAmbiguous,
                        )
                        Text(disposition.displayName)
                    }
                }
                OutlinedTextField(
                    value = state.refundReasonInput,
                    onValueChange = onReasonChanged,
                    label = { Text("Alasan wajib") },
                    minLines = 3,
                    enabled = !state.isSubmittingRefund && !state.isRefundOutcomeAmbiguous,
                    supportingText = state.refundErrorMessage?.let { message ->
                        { Text(message, color = MaterialTheme.colorScheme.error) }
                    },
                    isError = state.refundErrorMessage != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.isRefundOutcomeAmbiguous) {
                    Text(
                        "Form dikunci agar retry memakai request dan idempotency key yang sama.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !state.isSubmittingRefund && state.refundReasonInput.trim().length >= 5,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
            ) {
                if (state.isSubmittingRefund) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Memproses")
                } else {
                    Text(if (state.isRefundOutcomeAmbiguous) "Periksa status" else "Konfirmasi refund")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isSubmittingRefund) { Text("Batal") }
        },
    )
}

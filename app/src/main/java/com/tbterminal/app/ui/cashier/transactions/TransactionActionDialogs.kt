package com.tbterminal.app.ui.cashier.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.ManagerApprovalContext
import com.tbterminal.app.data.model.ManagerApprovalGrant
import com.tbterminal.app.data.model.RefundDisposition
import com.tbterminal.app.data.model.receiptNumber
import com.tbterminal.app.data.model.remainingAmount
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
        VoidTransactionBottomSheet(
            targetReceiptNumber = state.selectedTransaction?.receiptNumber()?.ifBlank { state.selectedTransaction?.id.orEmpty() } ?: "-",
            reason = state.voidReasonInput,
            error = state.voidErrorMessage,
            submitting = state.isSubmittingVoid,
            ambiguous = state.isVoidOutcomeAmbiguous,
            onReasonChanged = onVoidReasonChanged,
            onDismiss = onDismissVoid,
            onConfirm = onConfirmVoid,
        )
    }
    if (state.isRefundDialogOpen) {
        RefundTransactionBottomSheet(
            targetReceiptNumber = state.selectedTransaction?.receiptNumber()?.ifBlank { state.selectedTransaction?.id.orEmpty() } ?: "-",
            disposition = state.refundDisposition,
            reason = state.refundReasonInput,
            error = state.refundErrorMessage,
            submitting = state.isSubmittingRefund,
            ambiguous = state.isRefundOutcomeAmbiguous,
            onDispositionChanged = onRefundDispositionChanged,
            onReasonChanged = onRefundReasonChanged,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoidTransactionBottomSheet(
    targetReceiptNumber: String,
    reason: String,
    error: String?,
    submitting: Boolean,
    ambiguous: Boolean,
    onReasonChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { if (!submitting) onDismiss() },
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 14.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0))
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header Section: Warning Badge, Title & ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFFF1F2))
                        .border(1.dp, Color(0xFFFFE4E6), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = Color(0xFFE11D48),
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Void Transaksi?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.2).sp,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "ID:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8),
                        )
                        Text(
                            text = targetReceiptNumber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569),
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }

            // Informative Helper Alert Box
            TransactionNoticeAlert(
                title = "Ketentuan Pembatalan (Void)",
                message = "Transaksi akan ditandai Batal dan tetap tersimpan sebagai arsip riwayat. Stok barang otomatis dikembalikan ke inventaris toko dan catatan kas disesuaikan.",
                icon = Icons.Outlined.Info,
            )

            // Form Input: Alasan Void
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Alasan Pembatalan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155),
                    )
                    Text(
                        text = " *",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF43F5E),
                    )
                }

                // Quick Preset Chips
                val presets = listOf(
                    "Salah input barang",
                    "Pelanggan batal beli",
                    "Input dobel",
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    presets.forEach { preset ->
                        val isSelected = reason == preset
                        Surface(
                            onClick = {
                                if (!submitting && !ambiguous) {
                                    onReasonChanged(preset)
                                }
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = if (isSelected) Color(0xFFE2EFEA) else Color(0xFFF1F5F9),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFC2DFD6) else Color.Transparent
                            ),
                            modifier = Modifier.height(30.dp),
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = preset,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF164236) else Color(0xFF475569),
                                )
                            }
                        }
                    }
                }

                // Text Input Field
                val isReasonError = error != null
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChanged,
                    placeholder = {
                        Text(
                            text = "Tuliskan alasan detail void transaksi...",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                        )
                    },
                    minLines = 3,
                    maxLines = 4,
                    enabled = !submitting && !ambiguous,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(
                        fontSize = 12.sp,
                        color = Color(0xFF1E293B),
                        lineHeight = 18.sp,
                    ),
                    isError = isReasonError,
                    supportingText = error?.let {
                        { Text(text = it, color = Color(0xFFE11D48), fontSize = 11.sp) }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isReasonError) Color(0xFFE11D48) else Color(0xFF256B57),
                        unfocusedBorderColor = if (isReasonError) Color(0xFFE11D48) else Color(0xFFE2E8F0),
                        focusedContainerColor = if (isReasonError) Color(0xFFFFF1F2) else Color.White,
                        unfocusedContainerColor = if (isReasonError) Color(0xFFFFF1F2) else Color.White,
                        errorBorderColor = Color(0xFFE11D48),
                        errorContainerColor = Color(0xFFFFF1F2),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (ambiguous) {
                    Text(
                        text = "Alasan dikunci agar retry memakai request dan idempotency key yang sama.",
                        color = Color(0xFFE11D48),
                        fontSize = 11.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Dialog Actions (Ergonomic Buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onDismiss,
                    enabled = !submitting,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = Color(0xFF334155),
                        disabledContainerColor = Color(0xFFF8FAFC),
                        disabledContentColor = Color(0xFF94A3B8),
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    Text(
                        text = "Batal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                val canConfirm = !submitting && reason.trim().length >= 5
                Button(
                    onClick = onConfirm,
                    enabled = canConfirm,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE11D48),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFFDA4AF),
                        disabledContentColor = Color.White,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    if (submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Memproses",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    } else {
                        Text(
                            text = if (ambiguous) "Periksa status" else "Konfirmasi",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RefundTransactionBottomSheet(
    targetReceiptNumber: String,
    disposition: RefundDisposition,
    reason: String,
    error: String?,
    submitting: Boolean,
    ambiguous: Boolean,
    onDispositionChanged: (RefundDisposition) -> Unit,
    onReasonChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { if (!submitting) onDismiss() },
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 14.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0))
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header Section: Icon Badge, Title & ID, and Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF0F7F4))
                            .border(1.dp, Color(0xFFDCEEE7), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Undo,
                            contentDescription = null,
                            tint = Color(0xFF256B57),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "Full Refund?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            letterSpacing = (-0.2).sp,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "ID:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF94A3B8),
                            )
                            Text(
                                text = targetReceiptNumber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569),
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable(enabled = !submitting, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup dialog",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Informative Notice Box
            TransactionNoticeAlert(
                title = "Ketentuan Pengembalian Dana",
                message = "Nominal refund dihitung otomatis sesuai total pembayaran transaksi. Stok barang akan disesuaikan berdasarkan kondisi barang yang Anda pilih di bawah.",
                icon = Icons.Outlined.Warning,
            )

            // Kondisi Barang Radio Options
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Kondisi Barang",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155),
                    )
                    Text(
                        text = " *",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF43F5E),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val dispositions = listOf(
                            RefundDisposition.RETURN_TO_STOCK to "Kembali ke stok",
                            RefundDisposition.NOT_RETURNED to "Barang tidak kembali",
                            RefundDisposition.DAMAGED to "Barang rusak / afkir",
                        )
                        dispositions.forEachIndexed { index, (itemDisposition, label) ->
                            if (index > 0) {
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = Color(0xFFF1F5F9),
                                )
                            }
                            val isSelected = disposition == itemDisposition
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) Color(0xFFF0F7F4).copy(alpha = 0.5f) else Color.White)
                                    .clickable(
                                        enabled = !submitting && !ambiguous,
                                        onClick = { onDispositionChanged(itemDisposition) },
                                    )
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(
                                            width = 2.dp,
                                            color = if (isSelected) Color(0xFF256B57) else Color(0xFFCBD5E1),
                                            shape = CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF256B57))
                                        )
                                    }
                                }
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF1E293B) else Color(0xFF334155),
                                )
                            }
                        }
                    }
                }
            }

            // Alasan Refund Input with Quick Preset Chips
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Alasan Refund",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155),
                    )
                    Text(
                        text = " *",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF43F5E),
                    )
                }

                // Quick Preset Chips matching HTML mockup
                val presets = listOf(
                    "Salah beli",
                    "Barang cacat",
                    "Pelanggan batal",
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    presets.forEach { preset ->
                        val isSelected = reason == preset
                        Surface(
                            onClick = {
                                if (!submitting && !ambiguous) {
                                    onReasonChanged(preset)
                                }
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = if (isSelected) Color(0xFFE2EFEA) else Color(0xFFF1F5F9),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFC2DFD6) else Color.Transparent
                            ),
                            modifier = Modifier.height(30.dp),
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = preset,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF164236) else Color(0xFF475569),
                                )
                            }
                        }
                    }
                }

                // Text Input Field
                val isReasonError = error != null
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChanged,
                    placeholder = {
                        Text(
                            text = "Tuliskan alasan detail refund...",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                        )
                    },
                    minLines = 3,
                    maxLines = 4,
                    enabled = !submitting && !ambiguous,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(
                        fontSize = 12.sp,
                        color = Color(0xFF1E293B),
                        lineHeight = 18.sp,
                    ),
                    isError = isReasonError,
                    supportingText = error?.let {
                        { Text(text = it, color = Color(0xFFE11D48), fontSize = 11.sp) }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isReasonError) Color(0xFFE11D48) else Color(0xFF256B57),
                        unfocusedBorderColor = if (isReasonError) Color(0xFFE11D48) else Color(0xFFE2E8F0),
                        focusedContainerColor = if (isReasonError) Color(0xFFFFF1F2) else Color.White,
                        unfocusedContainerColor = if (isReasonError) Color(0xFFFFF1F2) else Color.White,
                        errorBorderColor = Color(0xFFE11D48),
                        errorContainerColor = Color(0xFFFFF1F2),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (ambiguous) {
                    Text(
                        text = "Alasan dikunci agar retry memakai request dan idempotency key yang sama.",
                        color = Color(0xFFE11D48),
                        fontSize = 11.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onDismiss,
                    enabled = !submitting,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = Color(0xFF334155),
                        disabledContainerColor = Color(0xFFF8FAFC),
                        disabledContentColor = Color(0xFF94A3B8),
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    Text(
                        text = "Batal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                val canConfirm = !submitting && reason.trim().length >= 5
                Button(
                    onClick = onConfirm,
                    enabled = canConfirm,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF256B57),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFA7D5C7),
                        disabledContentColor = Color.White,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    if (submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Memproses",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    } else {
                        Text(
                            text = if (ambiguous) "Periksa status" else "Konfirmasi",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CashierPayDebtDialog(
    state: CashierTransactionHistoryUiState,
    onDismiss: () -> Unit,
    onAmountChanged: (String) -> Unit,
    onMethodChanged: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val submitting = state.isSubmittingDebt
    val remaining = state.selectedTransaction?.remainingAmount() ?: BigDecimal.ZERO
    val remainingFormatted = "Rp " + DecimalFormat("#,###", DecimalFormatSymbols(Locale.forLanguageTag("id-ID"))).format(remaining)

    ModalBottomSheet(
        onDismissRequest = { if (!submitting) onDismiss() },
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            // Modal Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Terima Pelunasan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        letterSpacing = (-0.2).sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Masukkan nominal pelunasan sisa tagihan",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .clickable(enabled = !submitting) { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Modal Body Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Sisa Tagihan Minimalist Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, Color(0xFFF1F5F9)), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Sisa tagihan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B),
                    )
                    Text(
                        text = remainingFormatted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                    )
                }

                // Input Field: Nominal bayar
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Nominal bayar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Rp",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B),
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (state.payDebtAmountInput.isEmpty()) {
                                Text(
                                    text = "0",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFCBD5E1),
                                )
                            }
                            BasicTextField(
                                value = state.payDebtAmountInput,
                                onValueChange = onAmountChanged,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                ),
                                textStyle = TextStyle(
                                    color = Color(0xFF0F172A),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (state.payDebtAmountInput.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable { onAmountChanged("") },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Bersihkan",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }

                // Quick Amount Shortcuts (Chips)
                val remainingLong = remaining.toLong()
                val chips = remember(remainingLong) {
                    buildList {
                        add("Bayar penuh" to remaining.stripTrailingZeros().toPlainString())
                        if (remainingLong >= 20000L) {
                            val half = (remainingLong / 2L / 1000L) * 1000L
                            if (half > 0 && half != remainingLong) {
                                val formatted = "Rp " + DecimalFormat("#,###", DecimalFormatSymbols(Locale.forLanguageTag("id-ID"))).format(half)
                                add(formatted to half.toString())
                            }
                        }
                        if (remainingLong >= 50000L) {
                            val smaller = (remainingLong / 5L / 1000L) * 1000L
                            if (smaller > 0 && smaller != remainingLong) {
                                val formatted = "Rp " + DecimalFormat("#,###", DecimalFormatSymbols(Locale.forLanguageTag("id-ID"))).format(smaller)
                                add(formatted to smaller.toString())
                            }
                        } else if (remainingLong > 10000L) {
                            add("Rp 10.000" to "10000")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    chips.forEach { (label, value) ->
                        val isSelected = state.payDebtAmountInput == value
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFF0F7F4) else Color(0xFFF8FAFC))
                                .border(
                                    BorderStroke(1.dp, if (isSelected) Color(0xFFC7E0D6) else Color(0xFFE2E8F0)),
                                    RoundedCornerShape(8.dp),
                                )
                                .clickable { onAmountChanged(value) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF1E5646) else Color(0xFF475569),
                            )
                        }
                    }
                }

                // Metode Penerimaan Segmented Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Metode Penerimaan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Color(0xFFF1F5F9)), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val methods = listOf(
                            Triple("TUNAI", "Tunai", Icons.Outlined.Payments),
                            Triple("TRANSFER", "Transfer", Icons.Outlined.AccountBalance),
                            Triple("QRIS", "QRIS", Icons.Outlined.QrCodeScanner),
                        )
                        methods.forEach { (key, label, icon) ->
                            val isSelected = state.payDebtMethodInput.equals(key, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF256B57) else Color.Transparent)
                                    .clickable { onMethodChanged(key) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF475569),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Action Buttons (Footer)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !submitting,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF475569),
                    ),
                ) {
                    Text(
                        text = "Batal",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                val amountValid = (state.payDebtAmountInput.toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO
                Button(
                    onClick = onConfirm,
                    enabled = !submitting && amountValid,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF256B57),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFA7D5C7),
                        disabledContentColor = Color.White,
                    ),
                ) {
                    if (submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Menyimpan",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    } else {
                        Text(
                            text = "Simpan",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionNoticeAlert(
    title: String,
    message: String,
    icon: ImageVector = Icons.Outlined.Info,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFFBEB),
        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFD97706),
                modifier = Modifier
                    .size(18.dp)
                    .padding(top = 1.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF92400E),
                    letterSpacing = (-0.1).sp,
                )
                Text(
                    text = message,
                    fontSize = 11.5.sp,
                    color = Color(0xFF78350F),
                    lineHeight = 16.5.sp,
                )
            }
        }
    }
}


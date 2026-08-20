package com.tbterminal.app.ui.cash

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun CashHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text("Kas Harian", color = CashText, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Buka sesi, pantau kas sistem, lalu tutup shift dengan kas fisik.",
                color = CashMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        CashHeaderBadge()
    }
}

@Composable
private fun CashHeaderBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(CashPrimarySoft)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = CashPrimary)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Kontrol kas shift", color = CashPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun CashMessage(uiState: CashReconciliationUiState, onDismiss: () -> Unit) {
    val message = uiState.errorMessage ?: uiState.message ?: return
    val isError = uiState.errorMessage != null
    val background = if (isError) CashDanger.copy(alpha = 0.12f) else CashPrimarySoft
    val content = if (isError) CashDanger else CashPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (isError) Icons.Outlined.Lock else Icons.Default.CheckCircle, contentDescription = null, tint = content)
        Spacer(modifier = Modifier.width(12.dp))
        Text(message, modifier = Modifier.weight(1f), color = content, fontWeight = FontWeight.Bold)
        TextButton(onClick = onDismiss) {
            Text("Tutup", color = content, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun CashMetrics(uiState: CashReconciliationUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        CashMetricCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Payments,
            title = "Modal Awal",
            value = uiState.activeSession?.openingCash.money(),
            fallback = "Belum buka sesi",
            color = CashPrimary
        )
        CashMetricCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.AccountBalanceWallet,
            title = "Kas Sistem",
            value = if (uiState.hasActiveSession) uiState.systemCash.money() else null,
            fallback = "-",
            color = CashInfo
        )
        CashMetricCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.PointOfSale,
            title = "Transaksi Sesi",
            value = if (uiState.hasActiveSession) uiState.total.toString() else null,
            fallback = "0",
            color = CashWarning
        )
        CashMetricCard(
            modifier = Modifier.weight(1f),
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            title = "Status",
            value = uiState.activeSession?.status?.uppercase(),
            fallback = "BELUM ADA SESI",
            color = CashPrimary
        )
    }
}

@Composable
private fun CashMetricCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    value: String?,
    fallback: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CashSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CashLine)
    ) {
        Row(modifier = Modifier.padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title.uppercase(), color = CashMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text(value ?: fallback, color = CashText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
internal fun CashSessionPanel(
    uiState: CashReconciliationUiState,
    onOpeningCashChanged: (String) -> Unit,
    onOpenSession: () -> Unit,
    onClosingCashChanged: (String) -> Unit,
    onClosingNotesChanged: (String) -> Unit,
    onCloseSession: () -> Unit,
    onShowExpenseDialog: () -> Unit
) {
    if (uiState.hasActiveSession) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
            ActiveSessionCard(modifier = Modifier.weight(1f), uiState = uiState, onShowExpenseDialog = onShowExpenseDialog)
            CloseSessionCard(
                modifier = Modifier.weight(1f),
                uiState = uiState,
                onClosingCashChanged = onClosingCashChanged,
                onClosingNotesChanged = onClosingNotesChanged,
                onCloseSession = onCloseSession
            )
        }
    } else {
        OpenSessionCard(uiState, onOpeningCashChanged, onOpenSession)
    }
}

@Composable
private fun OpenSessionCard(
    uiState: CashReconciliationUiState,
    onOpeningCashChanged: (String) -> Unit,
    onOpenSession: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CashSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CashLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Buka Sesi Kasir", color = CashText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Masukkan modal awal laci sebelum transaksi. Backend membatasi satu sesi aktif per user.",
                color = CashMuted,
                fontSize = 13.sp
            )
            CashTextField("MODAL AWAL", uiState.openingCashInput, onOpeningCashChanged, "0")
            Button(
                onClick = onOpenSession,
                enabled = !uiState.isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = CashPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(52.dp)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buka Sesi", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ActiveSessionCard(modifier: Modifier, uiState: CashReconciliationUiState, onShowExpenseDialog: () -> Unit) {
    val session = uiState.activeSession
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CashSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CashLine)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                if (uiState.isUsingLocalActiveSession) "Sesi kas aktif dari data lokal" else "Sesi Aktif",
                color = CashText,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            if (uiState.isUsingLocalActiveSession) {
                Text(
                    "Server tidak tersambung. Transaksi akan disimpan lokal.",
                    color = CashMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            CashInfoRow("Sesi Dibuka", session?.openedAt.displayDateTime())
            CashInfoRow("Modal Awal", session?.openingCash.money())
            CashInfoRow("Kas Sistem", uiState.systemCash.money())
            CashInfoRow("Pengeluaran Kasir", session?.totalExpenses.money())
            
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onShowExpenseDialog,
                colors = ButtonDefaults.buttonColors(containerColor = CashDanger),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Outlined.Payments, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Catat Pengeluaran", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CloseSessionCard(
    modifier: Modifier,
    uiState: CashReconciliationUiState,
    onClosingCashChanged: (String) -> Unit,
    onClosingNotesChanged: (String) -> Unit,
    onCloseSession: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CashSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CashLine)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Tutup Sesi", color = CashText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            CashTextField(
                label = "KAS FISIK AKHIR",
                value = uiState.closingCashInput,
                onValueChange = onClosingCashChanged,
                placeholder = "Hitung uang fisik di laci"
            )
            CashNotesField(value = uiState.closingNotesInput, onValueChange = onClosingNotesChanged)
            Button(
                onClick = onCloseSession,
                enabled = !uiState.isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = CashText),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tutup Sesi", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CashInfoRow(label: String, value: String?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = CashMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(value ?: "-", color = CashText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CashTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = CashMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = CashMuted) },
            leadingIcon = { Text("Rp", color = CashMuted, fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = cashFieldColors()
        )
    }
}

@Composable
private fun CashNotesField(value: String, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("CATATAN", color = CashMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Opsional, misal selisih kas atau catatan shift", color = CashMuted) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            shape = RoundedCornerShape(12.dp),
            colors = cashFieldColors()
        )
    }
}

@Composable
private fun cashFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CashPrimary,
    unfocusedBorderColor = CashLine,
    focusedContainerColor = CashSurfaceSoft,
    unfocusedContainerColor = CashSurfaceSoft
)

@Composable
internal fun CashExpenseDialog(
    uiState: CashReconciliationUiState,
    onDismiss: () -> Unit,
    onAmountChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onConfirm: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CashSurface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Catat Pengeluaran",
                    color = CashText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Catat uang yang keluar dari laci (misal: beli perlengkapan) agar saldo sistem tetap akurat.",
                    color = CashMuted,
                    fontSize = 14.sp
                )
                CashTextField(
                    label = "NOMINAL",
                    value = uiState.expenseAmountInput,
                    onValueChange = onAmountChanged,
                    placeholder = "Misal: 15000"
                )
                CashNotesField(
                    value = uiState.expenseDescriptionInput,
                    onValueChange = onDescriptionChanged
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, enabled = !uiState.isSubmitting) {
                        Text("Batal", color = CashMuted, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        enabled = !uiState.isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = CashDanger),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Simpan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

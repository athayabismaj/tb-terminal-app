package com.tbterminal.app.ui.receivables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tbterminal.app.data.model.Receivable

@Composable
internal fun ReceivablePaymentDialog(
    receivable: Receivable,
    uiState: ReceivableUiState,
    onAmountChanged: (String) -> Unit,
    onMethodChanged: (ReceivablePaymentMethod) -> Unit,
    onReferenceChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    val isAmountError = uiState.errorMessage != null

    val slate50 = Color(0xFFF8FAFC)
    val slate100 = Color(0xFFF1F5F9)
    val slate200 = Color(0xFFE2E8F0)
    val slate300 = Color(0xFFCBD5E1)
    val slate400 = Color(0xFF94A3B8)
    val slate500 = Color(0xFF64748B)
    val slate600 = Color(0xFF475569)
    val slate700 = Color(0xFF334155)
    val slate900 = Color(0xFF0F172A)
    val brand50 = Color(0xFFF0FDF4)
    val brand100 = Color(0xFFDCFCE7)
    val brand600 = Color(0xFF16A34A)
    val brand700 = Color(0xFF15803D)
    val rose50 = Color(0xFFFFF1F2)
    val rose600 = Color(0xFFE11D48)

    Dialog(
        onDismissRequest = { if (!uiState.isSubmittingPayment) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header: Icon + Title/Subtitle + Close Button (matching QuickAddCustomerDialog)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(brand100),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = brand700,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Bayar Piutang",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = slate900
                                )
                                Text(
                                    text = "Catat pelunasan nota piutang pelanggan",
                                    fontSize = 11.sp,
                                    color = slate500
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(slate100)
                                .clickable { if (!uiState.isSubmittingPayment) onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = slate500,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Customer & Invoice Summary Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = slate50,
                        border = BorderStroke(1.dp, slate200)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(slate200),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = receivable.customerName.take(1).uppercase(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = slate700
                                        )
                                    }
                                    Text(
                                        text = receivable.customerName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = slate900,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = slate100,
                                    border = BorderStroke(1.dp, slate200)
                                ) {
                                    Text(
                                        text = receivable.mobileReference(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = slate600,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = slate200, thickness = 0.5.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Sisa Piutang",
                                        fontSize = 11.sp,
                                        color = slate500
                                    )
                                    Text(
                                        text = receivable.remainingAmount.currencyText(),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = slate900
                                    )
                                }

                                Surface(
                                    onClick = { onAmountChanged(receivable.remainingAmount.toInputText()) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = brand100,
                                    border = BorderStroke(1.dp, Color(0xFF86EFAC))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = brand700,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Bayar Penuh",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = brand700
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Input Nominal Pembayaran
                    Text(
                        text = "NOMINAL PEMBAYARAN *",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = slate700
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = uiState.paymentAmountInput,
                        onValueChange = onAmountChanged,
                        prefix = {
                            Text(
                                text = "Rp",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAmountError) rose600 else slate700,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        },
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = slate900
                        ),
                        placeholder = { Text("0", fontSize = 16.sp, color = slate400) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        isError = isAmountError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isAmountError) rose600 else brand700,
                            unfocusedBorderColor = if (isAmountError) rose600 else slate300,
                            focusedContainerColor = if (isAmountError) rose50 else slate50,
                            unfocusedContainerColor = if (isAmountError) rose50 else slate50
                        )
                    )
                    if (isAmountError && uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage,
                            color = rose600,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 4.dp, top = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Metode Pembayaran
                    Text(
                        text = "METODE PEMBAYARAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = slate700
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    PaymentMethodRow(
                        selected = uiState.paymentMethod,
                        onSelect = onMethodChanged
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Referensi & Catatan (Optional)
                    Text(
                        text = "REFERENSI (OPSIONAL)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = slate700
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = uiState.referenceInput,
                        onValueChange = onReferenceChanged,
                        placeholder = { Text("Contoh: Bukti Transfer #1234", fontSize = 12.sp, color = slate400) },
                        textStyle = TextStyle(fontSize = 12.sp, color = slate900),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brand700,
                            unfocusedBorderColor = slate300,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "CATATAN (OPSIONAL)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = slate700
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = uiState.notesInput,
                        onValueChange = onNotesChanged,
                        placeholder = { Text("Keterangan pembayaran / catatan kasir", fontSize = 12.sp, color = slate400) },
                        textStyle = TextStyle(fontSize = 12.sp, color = slate900),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brand700,
                            unfocusedBorderColor = slate300,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Footer Action Buttons (Matching QuickAddCustomerDialog)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            enabled = !uiState.isSubmittingPayment,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = slate100,
                                contentColor = slate700
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Text(
                                "Batal",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = slate700
                            )
                        }

                        Button(
                            onClick = onSubmit,
                            enabled = !uiState.isSubmittingPayment,
                            modifier = Modifier
                                .weight(1.5f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = brand700)
                        ) {
                            if (uiState.isSubmittingPayment) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Menyimpan...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simpan Pembayaran", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodRow(
    selected: ReceivablePaymentMethod,
    onSelect: (ReceivablePaymentMethod) -> Unit
) {
    val slate50 = Color(0xFFF8FAFC)
    val slate200 = Color(0xFFE2E8F0)
    val slate500 = Color(0xFF64748B)
    val slate900 = Color(0xFF0F172A)
    val brand50 = Color(0xFFF0FDF4)
    val brand700 = Color(0xFF15803D)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReceivablePaymentMethod.entries.forEach { method ->
            val isSelected = method == selected
            val icon: ImageVector = when (method) {
                ReceivablePaymentMethod.Cash -> Icons.Outlined.Payments
                ReceivablePaymentMethod.Transfer -> Icons.Outlined.AccountBalance
                ReceivablePaymentMethod.Qris -> Icons.Outlined.QrCode
            }

            Surface(
                onClick = { onSelect(method) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) brand50 else slate50,
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) brand700 else slate200
                ),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) brand700 else slate500,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = method.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) brand700 else slate900
                    )
                }
            }
        }
    }
}

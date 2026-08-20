package com.tbterminal.app.ui.cashier.session.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.tbterminal.app.ui.cash.CashReconciliationUiState
import com.tbterminal.app.ui.cash.displayDateTime
import com.tbterminal.app.ui.cash.money
import com.tbterminal.app.ui.cashier.session.BlueAction
import com.tbterminal.app.ui.cashier.session.ErrorColor
import com.tbterminal.app.ui.cashier.session.ErrorLight
import com.tbterminal.app.ui.cashier.session.OnSurface
import com.tbterminal.app.ui.cashier.session.Primary
import com.tbterminal.app.ui.cashier.session.PrimaryDark
import com.tbterminal.app.ui.cashier.session.PrimaryLight
import com.tbterminal.app.ui.cashier.session.PrimaryVariant
import com.tbterminal.app.ui.cashier.session.Slate100
import com.tbterminal.app.ui.cashier.session.Slate300
import com.tbterminal.app.ui.cashier.session.Slate400
import com.tbterminal.app.ui.cashier.session.Slate50
import com.tbterminal.app.ui.cashier.session.Slate500

@Composable
fun LocalCashSessionBadges(
    uiState: CashReconciliationUiState,
    modifier: Modifier = Modifier
) {
    if (!uiState.isUsingLocalActiveSession) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CashSessionBadge(
            text = if (uiState.hasActiveSession) "Sesi Lokal" else "Sesi Ditutup Lokal",
            background = PrimaryLight,
            foreground = Primary
        )
        CashSessionBadge(
            text = "Belum Tersinkron",
            background = Color(0xFFFFF7ED),
            foreground = Color(0xFFEA580C)
        )
    }
}

@Composable
private fun CashSessionBadge(
    text: String,
    background: Color,
    foreground: Color
) {
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = foreground,
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
fun SessionSummarySection(
    modifier: Modifier = Modifier,
    uiState: CashReconciliationUiState,
    onShowExpenseDialog: () -> Unit,
    onTransactionHistoryClick: () -> Unit
) {
    val session = uiState.activeSession
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(32.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Slate100),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(32.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(24.dp)
                                .clip(CircleShape)
                                .background(Primary)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Ringkasan Sesi Aktif", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    }
                    Box(
                        modifier = Modifier
                            .background(PrimaryLight, CircleShape)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "SHIFT BERJALAN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Primary,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoBox(
                        modifier = Modifier.weight(1f),
                        title = "WAKTU BUKA",
                        value = session?.openedAt?.displayDateTime() ?: "-",
                        icon = Icons.Outlined.Schedule
                    )
                    InfoBox(
                        modifier = Modifier.weight(1f),
                        title = "MODAL AWAL",
                        value = session?.openingCash?.money() ?: "Rp 0",
                        icon = null
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CashFlowBox(
                        modifier = Modifier.weight(1f),
                        title = "PENJUALAN TUNAI",
                        value = "+ ${uiState.cashSales.money() ?: "Rp 0"}",
                        color = Primary,
                        icon = Icons.Outlined.ArrowUpward
                    )
                    CashFlowBox(
                        modifier = Modifier.weight(1f),
                        title = "PENGELUARAN KAS",
                        value = "- ${session?.totalExpenses?.money() ?: "Rp 0"}",
                        color = ErrorColor,
                        icon = Icons.Outlined.ArrowDownward
                    )
                }

                HorizontalDivider(color = Slate100, modifier = Modifier.padding(bottom = 24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            "SALDO AKHIR SISTEM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate500,
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 4.dp)) {
                            val sysCashStr = uiState.systemCash.money() ?: "Rp 0"
                            val symbol = if (sysCashStr.startsWith("Rp")) "Rp" else ""
                            val amountStr = sysCashStr.removePrefix("Rp").trim()

                            Text(symbol, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                amountStr,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryDark,
                                letterSpacing = (-1).sp
                            )
                        }
                    }
                    Button(
                        onClick = onShowExpenseDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = BlueAction),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Icon(Icons.Outlined.AddCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Catat\nPengeluaran", fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Slate100),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = Primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Transaksi Terakhir", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    }
                    Text(
                        "LIHAT DETAIL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.clickable { onTransactionHistoryClick() }
                    )
                }

                if (uiState.transactions.isEmpty()) {
                    Text(
                        "Belum ada transaksi di sesi ini.",
                        color = Slate400,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    uiState.transactions.take(3).forEachIndexed { index, tx ->
                        val isIncome = tx.type != "EXPENSE"
                        val title = if (isIncome) "Penjualan #${tx.receiptId}" else tx.receiptId
                        val typeDisplay = if (isIncome) tx.type.uppercase() else "PENGELUARAN"
                        val amountStr = (if (isIncome) "+ " else "- ") + (tx.total.money() ?: "Rp 0")
                        val time = tx.createdAt.take(16).replace("T", " ")
                        TransactionItemRow(
                            title = title,
                            time = time,
                            type = typeDisplay,
                            amount = amountStr,
                            isIncome = isIncome
                        )
                        if (index < minOf(uiState.transactions.size, 3) - 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CloseShiftSection(
    modifier: Modifier = Modifier,
    cashAmount: String,
    notes: String,
    onCashChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isSubmitting: Boolean
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, Slate100),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Primary)
            )

            Column(modifier = Modifier.padding(32.dp)) {
                Text("Tutup Shift", fontSize = 24.sp, fontWeight = FontWeight.Black, color = OnSurface)
                Text(
                    "Verifikasi jumlah uang fisik di laci kasir.",
                    fontSize = 14.sp,
                    color = Slate400,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
                )

                Text(
                    "HITUNG UANG LACI (IDR)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate400,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = cashAmount,
                    onValueChange = onCashChange,
                    placeholder = {
                        Text("0", color = Slate300, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    },
                    leadingIcon = {
                        Text(
                            "Rp",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Primary,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    shape = RoundedCornerShape(16.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurface
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50,
                        unfocusedBorderColor = Slate100,
                        focusedBorderColor = Primary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "CATATAN TAMBAHAN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate400,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    placeholder = { Text("Berikan keterangan jika terdapat selisih...", color = Slate400) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50,
                        unfocusedBorderColor = Slate100,
                        focusedBorderColor = Primary
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onSubmit,
                    enabled = !isSubmitting && cashAmount.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Konfirmasi & Tutup Sesi", fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryLight, RoundedCornerShape(16.dp))
                        .border(1.dp, PrimaryLight.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Menutup sesi akan mengunci transaksi hari ini dan mencetak laporan rekapitulasi akhir secara otomatis.",
                        fontSize = 11.sp,
                        color = PrimaryVariant,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun OpenShiftForm(
    modifier: Modifier = Modifier,
    uiState: CashReconciliationUiState,
    onOpeningCashChanged: (String) -> Unit,
    onOpenSession: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Slate100),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("Buka Sesi Kasir", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = OnSurface)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("MODAL AWAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400)
                OutlinedTextField(
                    value = uiState.openingCashInput,
                    onValueChange = onOpeningCashChanged,
                    placeholder = { Text("0", color = Slate300) },
                    leadingIcon = {
                        Text(
                            "Rp",
                            color = Primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Slate100
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                )
            }

            Button(
                onClick = onOpenSession,
                enabled = !uiState.isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Buka Sesi", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InfoBox(modifier: Modifier = Modifier, title: String, value: String, icon: ImageVector?) {
    Column(
        modifier = modifier
            .background(Slate50, RoundedCornerShape(16.dp))
            .border(1.dp, Slate100, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnSurface)
        }
    }
}

@Composable
private fun CashFlowBox(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color,
    icon: ImageVector
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 1.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = OnSurface)
    }
}

@Composable
private fun TransactionItemRow(
    title: String,
    time: String,
    type: String,
    amount: String,
    isIncome: Boolean
) {
    val bgColor = if (isIncome) PrimaryLight else ErrorLight
    val tintColor = if (isIncome) Primary else ErrorColor
    val icon = if (isIncome) Icons.Outlined.PointOfSale else Icons.Outlined.Payments

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Slate100),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tintColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = OnSurface)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Text(time, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate400)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Slate300)
                    )
                    Text(type.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tintColor)
                }
            }
        }
        Text(amount, fontSize = 18.sp, fontWeight = FontWeight.Black, color = tintColor)
    }
}

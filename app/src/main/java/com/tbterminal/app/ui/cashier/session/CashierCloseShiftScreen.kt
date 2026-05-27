package com.tbterminal.app.ui.cashier.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.ui.cash.CashReconciliationUiState
import com.tbterminal.app.ui.cash.CashReconciliationViewModel
import com.tbterminal.app.ui.cash.CashExpenseDialog
import com.tbterminal.app.ui.cash.displayDateTime
import com.tbterminal.app.ui.cash.money
import com.tbterminal.app.ui.dashboard.cashier.CashierDashboardShell
import com.tbterminal.app.ui.dashboard.cashier.CashierDestination
import com.tbterminal.app.ui.dashboard.DashboardBackground

// ==========================================
// TEMA & WARNA
// ==========================================
val SurfaceBg = Color(0xFFF8FAFB)
val OnSurface = Color(0xFF161D1F)
val Slate50 = Color(0xFFF8FAFC)
val Slate100 = Color(0xFFF1F5F9)
val Slate200 = Color(0xFFE2E8F0)
val Slate300 = Color(0xFFCBD5E1)
val Slate400 = Color(0xFF94A3B8)
val Slate500 = Color(0xFF64748B)
val Primary = Color(0xFF00694C) // Sesuai desain TB Terminal
val PrimaryLight = Color(0xFFE8F5E9) // Emerald 50
val PrimaryVariant = Color(0xFF008560)
val ErrorColor = Color(0xFFBA1A1A)
val ErrorLight = Color(0xFFFFDAD6)
val BlueAction = Color(0xFF1A73E8)
val BlueActionLight = Color(0xFFE8F0FE)
val PrimaryDark = Color(0xFF004D36)

@Composable
fun CashierCloseShiftScreen(
    name: String,
    role: String,
    cashReconciliationRepository: CashReconciliationRepository,
    onDashboardClick: () -> Unit,
    onPosClick: () -> Unit,
    onTransactionHistoryClick: () -> Unit = {},
    onStockCheckClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: CashReconciliationViewModel = viewModel(
        factory = CashReconciliationViewModel.factory(cashReconciliationRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSuccessModal by remember { mutableStateOf(false) }

    // Jika message sukses muncul, kita bisa tangkap dan tampilkan modal
    LaunchedEffect(uiState.message) {
        if (uiState.message != null && uiState.errorMessage == null && !uiState.hasActiveSession) {
            // Sesi berhasil ditutup
            showSuccessModal = true
            viewModel.clearMessage()
        }
    }

    CashierDashboardShell(
        userName = name,
        role = role,
        activeDestination = CashierDestination.CashSession,
        onDashboardClick = onDashboardClick,
        onPosClick = onPosClick,
        onCashSessionClick = {},
        onTransactionHistoryClick = onTransactionHistoryClick,
        onStockCheckClick = onStockCheckClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        BoxWithConstraints(
            modifier = contentModifier
                .fillMaxSize()
                .background(DashboardBackground)
        ) {
            val isDesktop = maxWidth > 900.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ShiftHeader()

                if (uiState.isLoading && !uiState.isSubmitting) {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else if (!uiState.hasActiveSession) {
                    // OPEN SHIFT UI
                    Box(
                        modifier = Modifier.fillMaxWidth().height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        OpenShiftForm(
                            uiState = uiState,
                            onOpeningCashChanged = viewModel::onOpeningCashChanged,
                            onOpenSession = viewModel::openSession
                        )
                    }
                } else {
                    // DAILY CASH CONTAINER (ACTIVE SESSION)
                    if (isDesktop) {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {
                            SessionSummarySection(
                                modifier = Modifier.weight(7f),
                                uiState = uiState,
                                onShowExpenseDialog = viewModel::showExpenseDialog,
                                onTransactionHistoryClick = onTransactionHistoryClick
                            )
                            CloseShiftSection(
                                modifier = Modifier.weight(5f),
                                cashAmount = uiState.closingCashInput,
                                notes = uiState.closingNotesInput,
                                onCashChange = viewModel::onClosingCashChanged,
                                onNotesChange = viewModel::onClosingNotesChanged,
                                onSubmit = viewModel::closeSession,
                                isSubmitting = uiState.isSubmitting
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SessionSummarySection(
                                modifier = Modifier.fillMaxWidth(),
                                uiState = uiState,
                                onShowExpenseDialog = viewModel::showExpenseDialog,
                                onTransactionHistoryClick = onTransactionHistoryClick
                            )
                            CloseShiftSection(
                                modifier = Modifier.fillMaxWidth(),
                                cashAmount = uiState.closingCashInput,
                                notes = uiState.closingNotesInput,
                                onCashChange = viewModel::onClosingCashChanged,
                                onNotesChange = viewModel::onClosingNotesChanged,
                                onSubmit = viewModel::closeSession,
                                isSubmitting = uiState.isSubmitting
                            )
                        }
                    }
                }
            }

            // Error Overlay (jika ada)
            if (uiState.errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(modifier = Modifier.padding(24.dp)) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("Terjadi Kesalahan", color = ErrorColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(uiState.errorMessage ?: "")
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = viewModel::clearMessage) { Text("Tutup") }
                        }
                    }
                }
            }

            // Success Modal Overlay
            if (showSuccessModal) {
                SuccessModalOverlay(
                    onDismiss = { showSuccessModal = false },
                    onPrint = { /* Logika Cetak Ulang Struk Ktor/Printer Bluetooth */ }
                )
            }

            // Expense Dialog
            if (uiState.isExpenseDialogOpen) {
                CashExpenseDialog(
                    uiState = uiState,
                    onDismiss = viewModel::hideExpenseDialog,
                    onAmountChanged = viewModel::onExpenseAmountChanged,
                    onDescriptionChanged = viewModel::onExpenseDescriptionChanged,
                    onConfirm = viewModel::addExpense
                )
            }
        }
    }
}

// ==========================================
// SEGMEN HEADER HALAMAN
// ==========================================
@Composable
private fun ShiftHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(PrimaryLight)
                .padding(14.dp)
        ) {
            Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text("Kas Harian & Pengeluaran", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = OnSurface)
            Text("Pantau aktivitas kas dan buka atau tutup shift kasir Anda.", fontSize = 14.sp, color = Slate400)
        }
    }
}

// ==========================================
// SEGMEN KIRI: RINGKASAN SESI
// ==========================================
@Composable
fun SessionSummarySection(
    modifier: Modifier = Modifier,
    uiState: CashReconciliationUiState,
    onShowExpenseDialog: () -> Unit,
    onTransactionHistoryClick: () -> Unit
) {
    val session = uiState.activeSession
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(32.dp)) {
        // Kartu Ringkasan
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Slate100),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(32.dp)) {
                // Header Kartu
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.width(4.dp).height(24.dp).clip(CircleShape).background(Primary))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Ringkasan Sesi Aktif", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    }
                    Box(modifier = Modifier.background(PrimaryLight, CircleShape).padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Primary))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SHIFT BERJALAN", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Primary, letterSpacing = 1.sp)
                        }
                    }
                }

                // Info Waktu & Modal
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoBox(modifier = Modifier.weight(1f), title = "WAKTU BUKA", value = session?.openedAt?.displayDateTime() ?: "-", icon = Icons.Outlined.Schedule)
                    InfoBox(modifier = Modifier.weight(1f), title = "MODAL AWAL", value = session?.openingCash?.money() ?: "Rp 0", icon = null)
                }

                // Kalkulasi Arus Kas
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CashFlowBox(modifier = Modifier.weight(1f), title = "PENJUALAN TUNAI", value = "+ ${uiState.cashSales.money() ?: "Rp 0"}", color = Primary, icon = Icons.Outlined.ArrowUpward)
                    CashFlowBox(modifier = Modifier.weight(1f), title = "PENGELUARAN KAS", value = "- ${session?.totalExpenses?.money() ?: "Rp 0"}", color = ErrorColor, icon = Icons.Outlined.ArrowDownward)
                }

                HorizontalDivider(color = Slate100, modifier = Modifier.padding(bottom = 24.dp))

                // Saldo Akhir & Action
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column {
                        Text("SALDO AKHIR SISTEM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 1.sp)
                        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 4.dp)) {
                            // Extract Rp and the rest to match the design (e.g. "Rp 1.900.000,00")
                            val sysCashStr = uiState.systemCash.money() ?: "Rp 0"
                            val symbol = if (sysCashStr.startsWith("Rp")) "Rp" else ""
                            val amountStr = sysCashStr.removePrefix("Rp").trim()
                            
                            Text(symbol, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(amountStr, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryDark, letterSpacing = (-1).sp)
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

        // Daftar Transaksi Terakhir
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Slate100),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(PrimaryLight), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = Primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Transaksi Terakhir", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    }
                    Text("LIHAT DETAIL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Primary, letterSpacing = 1.sp, modifier = Modifier.clickable { onTransactionHistoryClick() })
                }

                // List Items (Live Data)
                if (uiState.transactions.isEmpty()) {
                    Text("Belum ada transaksi di sesi ini.", color = Slate400, fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    uiState.transactions.take(3).forEachIndexed { index, tx ->
                        val isIncome = tx.type != "EXPENSE"
                        val title = if (isIncome) "Penjualan #${tx.receiptId}" else tx.receiptId
                        val typeDisplay = if (isIncome) tx.type.uppercase() else "PENGELUARAN"
                        val amountStr = (if (isIncome) "+ " else "- ") + (tx.total.money() ?: "Rp 0")
                        val time = tx.createdAt.take(16).replace("T", " ")
                        TransactionItemRow(title = title, time = time, type = typeDisplay, amount = amountStr, isIncome = isIncome)
                        if (index < minOf(uiState.transactions.size, 3) - 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SEGMEN KANAN: FORM TUTUP SHIFT
// ==========================================
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
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Primary))
            
            Column(modifier = Modifier.padding(32.dp)) {
                Text("Tutup Shift", fontSize = 24.sp, fontWeight = FontWeight.Black, color = OnSurface)
                Text("Verifikasi jumlah uang fisik di laci kasir.", fontSize = 14.sp, color = Slate400, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp, bottom = 32.dp))

                Text("HITUNG UANG LACI (IDR)", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Slate400, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 12.dp))
                OutlinedTextField(
                    value = cashAmount,
                    onValueChange = onCashChange,
                    placeholder = { Text("0", color = Slate300, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold) },
                    leadingIcon = { Text("Rp", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Primary, modifier = Modifier.padding(start = 16.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    shape = RoundedCornerShape(16.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = OnSurface),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50,
                        unfocusedBorderColor = Slate100,
                        focusedBorderColor = Primary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text("CATATAN TAMBAHAN", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Slate400, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 12.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    placeholder = { Text("Berikan keterangan jika terdapat selisih...", color = Slate400) },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
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
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Konfirmasi & Tutup Sesi", fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().background(PrimaryLight, RoundedCornerShape(16.dp)).border(1.dp, PrimaryLight.copy(alpha=0.5f), RoundedCornerShape(16.dp)).padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Menutup sesi akan mengunci transaksi hari ini dan mencetak laporan rekapitulasi akhir secara otomatis.", fontSize = 11.sp, color = PrimaryVariant, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
                }
            }
        }
    }
}

// ==========================================
// FORM BUKA SHIFT (Adapted)
// ==========================================
@Composable
private fun OpenShiftForm(
    uiState: CashReconciliationUiState,
    onOpeningCashChanged: (String) -> Unit,
    onOpenSession: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, Slate100),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.width(500.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Primary))
            Column(modifier = Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("Buka Sesi Kasir", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = OnSurface)
                Text("Masukkan modal awal (uang receh) di dalam laci sebelum mulai melayani pelanggan.", color = Slate500, fontSize = 14.sp)
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MODAL AWAL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate400)
                    OutlinedTextField(
                        value = uiState.openingCashInput,
                        onValueChange = onOpeningCashChanged,
                        placeholder = { Text("0", color = Slate300) },
                        leadingIcon = { Text("Rp", color = Primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, end = 8.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().height(72.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Slate50,
                            unfocusedContainerColor = Slate50,
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Slate100
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = onOpenSession,
                    enabled = !uiState.isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().height(60.dp).padding(top = 12.dp)
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Buka Sesi", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// MODAL & BANTUAN UI
// ==========================================
@Composable
fun SuccessModalOverlay(onDismiss: () -> Unit, onPrint: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(OnSurface.copy(alpha = 0.6f)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.width(400.dp).padding(24.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Column(modifier = Modifier.padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(96.dp).clip(CircleShape).background(PrimaryLight), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(48.dp))
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                Text("Shift Ditutup", fontSize = 28.sp, fontWeight = FontWeight.Black, color = OnSurface)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Laporan akhir berhasil divalidasi. Struk rekap sedang dicetak dan salinan digital telah dikirim.",
                    fontSize = 14.sp, color = Slate400, textAlign = TextAlign.Center, lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("KEMBALI KE BERANDA", fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = onPrint,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, Slate100),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurface)
                ) {
                    Text("CETAK ULANG STRUK", fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
fun InfoBox(modifier: Modifier = Modifier, title: String, value: String, icon: ImageVector?) {
    Column(
        modifier = modifier.background(Slate50, RoundedCornerShape(16.dp)).border(1.dp, Slate100, RoundedCornerShape(16.dp)).padding(16.dp)
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
fun CashFlowBox(modifier: Modifier = Modifier, title: String, value: String, color: Color, icon: ImageVector) {
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
fun TransactionItemRow(title: String, time: String, type: String, amount: String, isIncome: Boolean) {
    val bgColor = if (isIncome) PrimaryLight else ErrorLight
    val tintColor = if (isIncome) Primary else ErrorColor
    val icon = if (isIncome) Icons.Outlined.PointOfSale else Icons.Outlined.Payments

    Row(
        modifier = Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(16.dp)).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).border(1.dp, Slate100), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tintColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = OnSurface)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Text(time, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate400)
                    Box(modifier = Modifier.padding(horizontal = 8.dp).size(4.dp).clip(CircleShape).background(Slate300))
                    Text(type.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tintColor)
                }
            }
        }
        Text(amount, fontSize = 18.sp, fontWeight = FontWeight.Black, color = tintColor)
    }
}

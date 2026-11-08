package com.tbterminal.app.ui.checkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.ui.checkout.components.CustomerCreditInspectorCard
import com.tbterminal.app.ui.checkout.components.DeliverySectionCard
import com.tbterminal.app.ui.checkout.components.QuickAddCustomerSheet
import com.tbterminal.app.ui.dashboard.cashier.CashierDashboardShell
import com.tbterminal.app.ui.dashboard.cashier.CashierDestination
import java.math.BigDecimal

private val PaymentBackground = Color(0xFFF8FAFC)
private val PaymentPrimary = Color(0xFF10533C)
private val PaymentOutlineVariant = Color(0xFFE2E8F0)
private val PaymentOnSurface = Color(0xFF0F172A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierPaymentScreen(
    name: String,
    role: String,
    state: CheckoutUiState,
    snackbarHostState: SnackbarHostState,
    onCustomerSearchChanged: (String) -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onClearCustomer: () -> Unit,
    onUseCustomerName: () -> Unit,
    onSelectPayment: (PaymentMethod) -> Unit,
    onAmountPaidChanged: (String) -> Unit,
    onTransactionDiscountChanged: (CheckoutDiscount?) -> Unit,
    onCheckout: () -> Unit,
    onBackToCart: () -> Unit,
    onToggleDelivery: (Boolean) -> Unit = {},
    onDeliveryFeeChanged: (String) -> Unit = {},
    onDeliveryAddressChanged: (String) -> Unit = {},
    onDeliveryCourierChanged: (String) -> Unit = {},
    onOpenHoldCartDialog: () -> Unit = {},
    onDashboardClick: () -> Unit = {},
    onPosClick: () -> Unit = {},
    onCashSessionClick: () -> Unit = {},
    onTransactionHistoryClick: () -> Unit = {},
    onStockCheckClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    onOpenQrisDialog: () -> Unit = {},
    onOpenQuickAddCustomer: () -> Unit = {},
    onDismissQuickAddCustomer: () -> Unit = {},
    onQuickCreateCustomer: (String, String, String, Boolean) -> Unit = { _, _, _, _ -> },
    onLockScreen: (() -> Unit)? = null
) {
    var showTransactionDiscount by remember { mutableStateOf(false) }
    val totalItems = state.cartItems.sumOf { it.quantity }
    val paymentIsValid = validateCheckoutPaymentInput(
        paymentMethod = state.selectedPaymentMethod,
        amountPaidInput = state.amountPaidInput,
        total = state.finalTotal
    ).error == null
    val creditCustomerIsValid = state.selectedPaymentMethod !in setOf(PaymentMethod.HUTANG, PaymentMethod.DP) ||
            state.selectedCustomer != null

    CashierDashboardShell(
        userName = name,
        role = role,
        activeDestination = CashierDestination.Pos,
        onDashboardClick = onDashboardClick,
        onPosClick = onPosClick,
        onCashSessionClick = onCashSessionClick,
        onTransactionHistoryClick = onTransactionHistoryClick,
        onStockCheckClick = onStockCheckClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout,
        onLockScreen = onLockScreen,
        showHeader = false
    ) { contentModifier ->
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = PaymentBackground,
            modifier = contentModifier,
            contentWindowInsets = WindowInsets(0.dp)
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                // ── TOP BAR PEMBAYARAN BERSIH & MODERN ──
                PaymentTopBar(
                    totalItems = totalItems,
                    onBackToCart = onBackToCart
                )

                // ── KONTEN PEMBAYARAN FOKUS & ERGONOMIS ──
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Hero Card: Total Tagihan
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, PaymentOutlineVariant),
                        shadowElevation = 0.5.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "TOTAL TAGIHAN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = formatRupiah(state.finalTotal),
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PaymentPrimary,
                                    letterSpacing = (-0.5).sp
                                )
                            }

                            if (state.totalDiscount > BigDecimal.ZERO) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFEF3C7),
                                    border = BorderStroke(1.dp, Color(0xFFFCD34D))
                                ) {
                                    Text(
                                        text = "Hemat ${formatRupiah(state.totalDiscount)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = "$totalItems Produk",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Pemilih Metode Pembayaran
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, PaymentOutlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "METODE PEMBAYARAN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B),
                                    letterSpacing = 0.8.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF0FDF4),
                                    border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                                ) {
                                    Text(
                                        text = state.selectedPaymentMethod.displayName(),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PaymentPrimary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Baris 1: Pembayaran Langsung (Tunai, QRIS, Transfer)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(
                                    Triple(PaymentMethod.TUNAI, "Tunai", Icons.Outlined.Payments),
                                    Triple(PaymentMethod.QRIS, "QRIS", Icons.Outlined.QrCode2),
                                    Triple(PaymentMethod.TRANSFER, "Transfer", Icons.Outlined.AccountBalance)
                                ).forEach { (method, label, icon) ->
                                    val isSelected = method == state.selectedPaymentMethod
                                    Surface(
                                        onClick = { onSelectPayment(method) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) PaymentPrimary else Color(0xFFF8FAFC),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) PaymentPrimary else PaymentOutlineVariant
                                        ),
                                        shadowElevation = if (isSelected) 1.dp else 0.dp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = label,
                                                tint = if (isSelected) Color.White else Color(0xFF475569),
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isSelected) Color.White else Color(0xFF334155),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            // Baris 2: Piutang & Bertahap (Hutang / Tempo & DP Proyek)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(
                                    Triple(PaymentMethod.HUTANG, "Hutang / Tempo", Icons.AutoMirrored.Outlined.ReceiptLong),
                                    Triple(PaymentMethod.DP, "DP Proyek", Icons.Outlined.AccountBalanceWallet)
                                ).forEach { (method, label, icon) ->
                                    val isSelected = method == state.selectedPaymentMethod
                                    Surface(
                                        onClick = { onSelectPayment(method) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) PaymentPrimary else Color(0xFFF8FAFC),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) PaymentPrimary else PaymentOutlineVariant
                                        ),
                                        shadowElevation = if (isSelected) 1.dp else 0.dp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = label,
                                                tint = if (isSelected) Color.White else Color(0xFF475569),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isSelected) Color.White else Color(0xFF334155),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Form Input Kontekstual Sesuai Metode
                    when (state.selectedPaymentMethod) {
                        PaymentMethod.TUNAI -> {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, PaymentOutlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Uang Tunai Diterima",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF475569)
                                    )

                                    OutlinedTextField(
                                        value = state.amountPaidInput,
                                        onValueChange = onAmountPaidChanged,
                                        placeholder = { Text("0", fontSize = 17.sp, color = Color(0xFF94A3B8)) },
                                        leadingIcon = {
                                            Text(
                                                "Rp",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PaymentPrimary,
                                                modifier = Modifier.padding(start = 12.dp)
                                            )
                                        },
                                        textStyle = TextStyle(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PaymentOnSurface
                                        ),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedContainerColor = Color(0xFFF8FAFC),
                                            focusedContainerColor = Color.White,
                                            unfocusedBorderColor = PaymentOutlineVariant,
                                            focusedBorderColor = PaymentPrimary
                                        ),
                                        singleLine = true
                                    )

                                    // Quick Cash Presets
                                    if (state.finalTotal > BigDecimal.ZERO) {
                                        val presets = getCashQuickPresets(state.finalTotal)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                onClick = {
                                                    onAmountPaidChanged(state.finalTotal.stripTrailingZeros().toPlainString())
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0xFFF0FDF4),
                                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "Uang Pas",
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = PaymentPrimary
                                                    )
                                                }
                                            }

                                            presets.forEach { preset ->
                                                Surface(
                                                    onClick = {
                                                        onAmountPaidChanged(preset.stripTrailingZeros().toPlainString())
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = Color(0xFFF8FAFC),
                                                    border = BorderStroke(1.dp, PaymentOutlineVariant),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(vertical = 8.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = formatRupiah(preset).replace("Rp ", ""),
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFF334155)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Live Kembalian / Kurang Bayar
                                    if (state.amountPaidInput.isNotBlank()) {
                                        val parsedAmount = state.amountPaidInput.trim().replace(',', '.').toBigDecimalOrNull()
                                        if (parsedAmount != null) {
                                            val diff = parsedAmount.subtract(state.finalTotal)
                                            if (diff >= BigDecimal.ZERO) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = Color(0xFFF0FDF4),
                                                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.CheckCircle,
                                                                contentDescription = null,
                                                                tint = PaymentPrimary,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                            Text(
                                                                text = "Kembalian",
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = PaymentPrimary
                                                            )
                                                        }
                                                        Text(
                                                            text = formatRupiah(diff),
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = PaymentPrimary
                                                        )
                                                    }
                                                }
                                            } else {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = Color(0xFFFEF2F2),
                                                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.WarningAmber,
                                                                contentDescription = null,
                                                                tint = Color(0xFFDC2626),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                            Text(
                                                                text = "Kurang Bayar",
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = Color(0xFFDC2626)
                                                            )
                                                        }
                                                        Text(
                                                            text = formatRupiah(diff.abs()),
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color(0xFFDC2626)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        PaymentMethod.QRIS -> {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, PaymentOutlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Surface(
                                        color = Color(0xFFF0FDF4),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.QrCode2,
                                                contentDescription = null,
                                                tint = PaymentPrimary,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "QRIS Dinamis Otomatis",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PaymentOnSurface
                                        )
                                        Text(
                                            text = "Tekan tombol di bawah untuk menampilkan kode QR ke pembeli",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B),
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        PaymentMethod.TRANSFER -> {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, PaymentOutlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AccountBalance,
                                            contentDescription = null,
                                            tint = PaymentPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Rekening Transfer Toko",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PaymentOnSurface
                                        )
                                    }
                                    Text(
                                        text = "BCA: 0123-456-789 a.n. TB Terminal Barokah Jaya",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF334155)
                                    )
                                    Text(
                                        text = "Pastikan mutasi dana sudah terkonfirmasi sebelum menyelesaikan pembayaran.",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        PaymentMethod.HUTANG -> {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFFFFFBEB),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                            contentDescription = null,
                                            tint = Color(0xFFB45309),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Transaksi Piutang / Tempo",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                    Text(
                                        text = "Total tagihan sebesar ${formatRupiah(state.finalTotal)} akan dicatat sebagai piutang jatuh tempo pada akun pelanggan.",
                                        fontSize = 12.sp,
                                        color = Color(0xFFB45309),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        PaymentMethod.DP -> {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, PaymentOutlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Nominal Uang Muka (DP) Diterima",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF475569)
                                    )

                                    OutlinedTextField(
                                        value = state.amountPaidInput,
                                        onValueChange = onAmountPaidChanged,
                                        placeholder = { Text("0", fontSize = 17.sp, color = Color(0xFF94A3B8)) },
                                        leadingIcon = {
                                            Text(
                                                "Rp",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PaymentPrimary,
                                                modifier = Modifier.padding(start = 12.dp)
                                            )
                                        },
                                        textStyle = TextStyle(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PaymentOnSurface
                                        ),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedContainerColor = Color(0xFFF8FAFC),
                                            focusedContainerColor = Color.White,
                                            unfocusedBorderColor = PaymentOutlineVariant,
                                            focusedBorderColor = PaymentPrimary
                                        ),
                                        singleLine = true
                                    )

                                    val parsedDp = state.amountPaidInput.trim().replace(',', '.').toBigDecimalOrNull()
                                    if (parsedDp != null && parsedDp > BigDecimal.ZERO && parsedDp < state.finalTotal) {
                                        val sisaHutang = state.finalTotal.subtract(parsedDp)
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFFEF3C7),
                                            border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Sisa Tagihan Tempo",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFFB45309)
                                                )
                                                Text(
                                                    text = formatRupiah(sisaHutang),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Pemilih Pelanggan (Wajib bila Hutang / DP, Opsional bila Tunai/Transfer/QRIS)
                    val isCustomerRequired = state.selectedPaymentMethod in setOf(PaymentMethod.HUTANG, PaymentMethod.DP)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DATA PELANGGAN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.8.sp
                            )
                            if (isCustomerRequired) {
                                Text(
                                    text = "* Wajib untuk ${state.selectedPaymentMethod.displayName()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFDC2626)
                                )
                            } else {
                                Text(
                                    text = "Opsional",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        CartCustomerSelector(
                            customerSearchQuery = state.customerSearchQuery,
                            selectedCustomer = state.selectedCustomer,
                            customers = state.customers,
                            isLoading = state.isCustomerLoading,
                            onSearchChanged = onCustomerSearchChanged,
                            onSelectCustomer = onSelectCustomer,
                            onClearCustomer = onClearCustomer,
                            onUseCustomerName = onUseCustomerName,
                            error = state.customerError,
                            onOpenQuickAddCustomer = onOpenQuickAddCustomer
                        )

                        if (state.selectedCustomer != null && isCustomerRequired) {
                            CustomerCreditInspectorCard(
                                customer = state.selectedCustomer,
                                currentBillTotal = state.finalTotal
                            )
                        }
                    }

                    // 5. Pengiriman Armada Toko
                    DeliverySectionCard(
                        isDeliveryEnabled = state.isDeliveryEnabled,
                        deliveryFeeInput = state.deliveryFeeInput,
                        deliveryAddress = state.deliveryAddress,
                        deliveryCourier = state.deliveryCourier,
                        onToggleDelivery = onToggleDelivery,
                        onDeliveryFeeChanged = onDeliveryFeeChanged,
                        onDeliveryAddressChanged = onDeliveryAddressChanged,
                        onDeliveryCourierChanged = onDeliveryCourierChanged,
                    )

                    // 6. Notifikasi Jika Diskon Memerlukan Otorisasi Manajer
                    if (state.checkoutPreview?.approvalRequired == true) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.WarningAmber,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Diskon melebihi limit otoritas. Transaksi memerlukan persetujuan Owner/Manager.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }

                // ── STICKY BOTTOM ACTION BAR (TOMBOL UTAMA DOMINAN) ──
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = Color.White,
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.85f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Drag handle pill
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(width = 40.dp, height = 4.dp)
                                .background(Color(0xFFCBD5E1), RoundedCornerShape(2.dp))
                        )

                        val isSubmitEnabled = !state.isLoading && !state.isPreviewLoading &&
                                state.cartItems.isNotEmpty() && paymentIsValid && creditCustomerIsValid

                        // Tombol Aksi Pembayaran Penuh
                        Button(
                            onClick = {
                                if (state.selectedPaymentMethod == PaymentMethod.QRIS) {
                                    onOpenQrisDialog()
                                } else {
                                    onCheckout()
                                }
                            },
                            enabled = isSubmitEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PaymentPrimary,
                                disabledContainerColor = Color(0xFFE2E8F0),
                                disabledContentColor = Color(0xFF94A3B8)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            if (state.isLoading || state.isPreviewLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                val (buttonIcon, buttonText) = when {
                                    state.hasAmbiguousCheckout -> Pair(Icons.Outlined.Info, "Periksa Ulang Transaksi")
                                    state.checkoutPreview?.approvalRequired == true -> Pair(Icons.Outlined.WarningAmber, "Minta Persetujuan Diskon")
                                    state.selectedPaymentMethod == PaymentMethod.QRIS -> Pair(Icons.Outlined.QrCode2, "Tampilkan Kode QRIS")
                                    state.selectedPaymentMethod == PaymentMethod.TUNAI -> Pair(Icons.Outlined.CheckCircle, "Selesaikan Pembayaran")
                                    state.selectedPaymentMethod == PaymentMethod.TRANSFER -> Pair(Icons.Outlined.CheckCircle, "Konfirmasi Transfer")
                                    state.selectedPaymentMethod == PaymentMethod.HUTANG -> Pair(Icons.AutoMirrored.Outlined.ReceiptLong, "Catat Sebagai Piutang")
                                    state.selectedPaymentMethod == PaymentMethod.DP -> Pair(Icons.Outlined.AccountBalanceWallet, "Simpan Transaksi DP")
                                    else -> Pair(Icons.AutoMirrored.Outlined.ReceiptLong, "Bayar ${formatRupiah(state.finalTotal)}")
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = buttonIcon,
                                        contentDescription = null,
                                        tint = if (isSubmitEnabled) Color.White else Color(0xFF94A3B8),
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Text(
                                        text = buttonText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isSubmitEnabled) Color.White else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }

                        // Home Indicator Pill
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 2.dp)
                                .size(width = 128.dp, height = 4.dp)
                                .background(Color(0xFFCBD5E1), RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }
    }

    if (state.isQuickAddCustomerOpen) {
        QuickAddCustomerSheet(
            initialName = state.customerSearchQuery,
            isSubmitting = state.isQuickCustomerSubmitting,
            errorMessage = state.customerError?.toMessage(),
            onDismiss = onDismissQuickAddCustomer,
            onSubmit = onQuickCreateCustomer
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SUB-KOMPONEN: TOP BAR PEMBAYARAN
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun PaymentTopBar(
    totalItems: Int,
    onBackToCart: () -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBackToCart,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Kembali ke Keranjang",
                            tint = Color(0xFF334155),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Pembayaran",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PaymentOnSurface,
                            letterSpacing = (-0.3).sp
                        )

                        Text(
                            text = "($totalItems item)",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(PaymentPrimary, CircleShape)
                        )
                        Text(
                            text = "POS Terminal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PaymentPrimary
                        )
                    }
                }
            }
            HorizontalDivider(color = PaymentOutlineVariant.copy(alpha = 0.8f))
        }
    }
}

private fun com.tbterminal.app.ui.common.UiText.toMessage(): String {
    return when (this) {
        is com.tbterminal.app.ui.common.UiText.DynamicString -> value
    }
}

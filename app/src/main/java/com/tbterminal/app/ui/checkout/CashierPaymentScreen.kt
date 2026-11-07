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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.ui.checkout.components.CartCalculationRow
import com.tbterminal.app.ui.checkout.components.CustomerCreditInspectorCard
import com.tbterminal.app.ui.checkout.components.DeliverySectionCard
import com.tbterminal.app.ui.checkout.components.PaymentMethodSelector
import com.tbterminal.app.ui.checkout.components.QuickAddCustomerSheet
import com.tbterminal.app.ui.dashboard.cashier.CashierDashboardShell
import com.tbterminal.app.ui.dashboard.cashier.CashierDestination
import java.math.BigDecimal

private val PaymentBackground = Color(0xFFF8FAFC)
private val PaymentSurface = Color.White
private val PaymentPrimary = Color(0xFF004528)
private val PaymentOutlineVariant = Color(0xFFE2E8F0)
private val PaymentOnSurface = Color(0xFF0F172A)
private val PaymentSecondary = Color(0xFFB45309)

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
                // ── TOP BAR PEMBAYARAN ──
                PaymentTopBar(
                    totalItems = totalItems,
                    onBackToCart = onBackToCart
                )

                // ── KONTEN SCROLLABLE PEMBAYARAN ──
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Hero Card: Total Tagihan
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TOTAL TAGIHAN",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF004528),
                                    letterSpacing = 0.8.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                                ) {
                                    Text(
                                        text = "$totalItems Barang Belanja",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF004528),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(
                                text = formatRupiah(state.finalTotal),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF004528)
                            )
                        }
                    }

                    // 2. Pemilih Pelanggan
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

                    if (state.selectedCustomer != null) {
                        CustomerCreditInspectorCard(
                            customer = state.selectedCustomer,
                            currentBillTotal = state.finalTotal
                        )
                    }

                    // 3. Pengiriman Armada Toko
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

                    // 4. Metode Pembayaran
                    PaymentMethodSelector(
                        selectedMethod = state.selectedPaymentMethod,
                        onSelectPayment = onSelectPayment
                    )

                    // 5. Input Nominal Tunai / DP
                    if (state.selectedPaymentMethod == PaymentMethod.DP || state.selectedPaymentMethod == PaymentMethod.TUNAI) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = state.amountPaidInput,
                                onValueChange = onAmountPaidChanged,
                                label = {
                                    Text(if (state.selectedPaymentMethod == PaymentMethod.TUNAI) "Uang Tunai Diterima" else "Nominal DP Proyek")
                                },
                                placeholder = { Text("0") },
                                leadingIcon = {
                                    Text(
                                        "Rp",
                                        fontWeight = FontWeight.Bold,
                                        color = PaymentPrimary,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.White,
                                    focusedContainerColor = Color.White,
                                    unfocusedBorderColor = PaymentOutlineVariant,
                                    focusedBorderColor = PaymentPrimary
                                ),
                                singleLine = true
                            )

                            // Quick Cash Presets untuk Tunai
                            if (state.selectedPaymentMethod == PaymentMethod.TUNAI && state.finalTotal > BigDecimal.ZERO) {
                                val presets = getCashQuickPresets(state.finalTotal)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        onClick = {
                                            onAmountPaidChanged(state.finalTotal.stripTrailingZeros().toPlainString())
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF0FDF4),
                                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 7.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Uang Pas",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF004528)
                                            )
                                        }
                                    }

                                    presets.forEach { preset ->
                                        Surface(
                                            onClick = {
                                                onAmountPaidChanged(preset.stripTrailingZeros().toPlainString())
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 7.dp),
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

                            // Live Kembalian / Kurang Bayar untuk Tunai
                            if (state.selectedPaymentMethod == PaymentMethod.TUNAI && state.amountPaidInput.isNotBlank()) {
                                val parsedAmount = state.amountPaidInput.trim().replace(',', '.').toBigDecimalOrNull()
                                if (parsedAmount != null) {
                                    val diff = parsedAmount.subtract(state.finalTotal)
                                    if (diff >= BigDecimal.ZERO) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFF0FDF4),
                                            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                                                        tint = Color(0xFF004528),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = "Kembalian",
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF004528)
                                                    )
                                                }
                                                Text(
                                                    text = formatRupiah(diff),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF004528)
                                                )
                                            }
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFFEF2F2),
                                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = "Kurang Bayar",
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFFDC2626)
                                                    )
                                                }
                                                Text(
                                                    text = formatRupiah(diff.abs()),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFFDC2626)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // DP Sisa Tagihan Info
                            if (state.selectedPaymentMethod == PaymentMethod.DP && state.amountPaidInput.isNotBlank()) {
                                val parsedDp = state.amountPaidInput.trim().replace(',', '.').toBigDecimalOrNull()
                                if (parsedDp != null && parsedDp > BigDecimal.ZERO && parsedDp < state.finalTotal) {
                                    val sisaHutang = state.finalTotal.subtract(parsedDp)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFFEF3C7),
                                        border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Info,
                                                    contentDescription = null,
                                                    tint = Color(0xFFB45309),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Sisa Tagihan Tempo",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFFB45309)
                                                )
                                            }
                                            Text(
                                                text = formatRupiah(sisaHutang),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFB45309)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. QRIS Shortcut Banner
                    if (state.selectedPaymentMethod == PaymentMethod.QRIS) {
                        Surface(
                            onClick = onOpenQrisDialog,
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color(0xFF10B981),
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.QrCode2,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Tampilkan Kode QRIS",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF065F46)
                                    )
                                    Text(
                                        text = "Tampilkan QR di layar untuk discan pembeli",
                                        fontSize = 11.sp,
                                        color = Color(0xFF047857)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // 7. Rincian Kalkulasi Nota
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, PaymentOutlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "RINCIAN PERHITUNGAN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 0.8.sp
                            )

                            CartCalculationRow("Subtotal Produk", formatRupiah(state.subtotal))

                            if (state.itemDiscountTotal > BigDecimal.ZERO) {
                                CartCalculationRow(
                                    "Diskon Item",
                                    "-${formatRupiah(state.itemDiscountTotal)}",
                                    isDiscount = true
                                )
                            }

                            if (state.transactionDiscountAmount > BigDecimal.ZERO) {
                                CartCalculationRow(
                                    "Diskon Transaksi",
                                    "-${formatRupiah(state.transactionDiscountAmount)}",
                                    isDiscount = true
                                )
                            }

                            if (state.isDeliveryEnabled && state.deliveryFee > BigDecimal.ZERO) {
                                CartCalculationRow(
                                    "Ongkos Kirim Armada",
                                    "+${formatRupiah(state.deliveryFee)}"
                                )
                            }

                            TextButton(
                                onClick = { showTransactionDiscount = true },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = if (state.transactionDiscount == null) "+ Tambah Diskon Nota" else "Ubah Diskon Nota",
                                    color = PaymentPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (state.checkoutPreview?.approvalRequired == true) {
                        Text(
                            text = "Diskon melebihi limit dan memerlukan persetujuan Owner/Admin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PaymentSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // ── STICKY BOTTOM ACTION BAR (SELESAIKAN PEMBAYARAN) ──
                Surface(
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, PaymentOutlineVariant),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onOpenHoldCartDialog,
                            modifier = Modifier
                                .height(50.dp)
                                .weight(0.7f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFFEF3C7),
                                contentColor = Color(0xFFB45309)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BookmarkAdd,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Parkir", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (state.selectedPaymentMethod == PaymentMethod.QRIS) {
                                    onOpenQrisDialog()
                                } else {
                                    onCheckout()
                                }
                            },
                            enabled = !state.isLoading && !state.isPreviewLoading && state.cartItems.isNotEmpty() && paymentIsValid && creditCustomerIsValid,
                            modifier = Modifier
                                .height(50.dp)
                                .weight(1.6f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF004528),
                                disabledContainerColor = Color(0xFFE2E8F0),
                                disabledContentColor = Color(0xFF94A3B8)
                            )
                        ) {
                            if (state.isLoading || state.isPreviewLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (state.selectedPaymentMethod == PaymentMethod.QRIS) Icons.Outlined.QrCode2 else Icons.AutoMirrored.Outlined.ReceiptLong,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when {
                                        state.hasAmbiguousCheckout -> "Periksa"
                                        state.checkoutPreview?.approvalRequired == true -> "Persetujuan"
                                        state.selectedPaymentMethod == PaymentMethod.QRIS -> "QRIS"
                                        else -> "Bayar ${formatRupiah(state.finalTotal)}"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            }
                        }
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

    if (showTransactionDiscount) {
        val afterItemDiscount = state.subtotal.subtract(state.itemDiscountTotal)
        DiscountInputDialog(
            title = "Diskon transaksi",
            baseAmount = afterItemDiscount,
            initialValue = state.transactionDiscount,
            onDismiss = { showTransactionDiscount = false },
            onApply = { discount ->
                onTransactionDiscountChanged(discount)
                showTransactionDiscount = false
            },
        )
    }
}

@Composable
private fun PaymentTopBar(
    totalItems: Int,
    onBackToCart: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(BorderStroke(1.dp, PaymentOutlineVariant))
            .padding(horizontal = 14.dp, vertical = 10.dp),
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
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, PaymentOutlineVariant, RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Kembali ke Keranjang",
                    tint = PaymentOnSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Pembayaran Transaksi",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PaymentOnSurface
                )
                Text(
                    text = "$totalItems Barang • Terminal Barokah Jaya",
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, Color(0xFFBBF7D0))
        ) {
            Text(
                text = "Langkah 2/2",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF004528),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

private fun com.tbterminal.app.ui.common.UiText.toMessage(): String {
    return when (this) {
        is com.tbterminal.app.ui.common.UiText.DynamicString -> value
    }
}

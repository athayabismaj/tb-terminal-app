package com.tbterminal.app.ui.checkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.ui.common.UiText
import com.tbterminal.app.ui.checkout.components.CartCalculationRow
import com.tbterminal.app.ui.checkout.components.CartEmptyState
import com.tbterminal.app.ui.checkout.components.CartItemCard
import com.tbterminal.app.ui.checkout.components.PaymentMethodSelector
import com.tbterminal.app.ui.dashboard.cashier.CashierDashboardShell
import com.tbterminal.app.ui.dashboard.cashier.CashierDestination
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

// ==========================================
// TEMA & WARNA KERANJANG
// ==========================================
private val CartBackground = Color.White
private val CartSurface = Color(0xFFFFFFFF)
private val CartSurfaceContainerLow = Color(0xFFEFF4FF)
private val CartOnSurface = Color(0xFF121C2A)
private val CartOnSurfaceVariant = Color(0xFF3D4A42)
private val CartOutlineVariant = Color(0xFFBCCAC0)
private val CartOutline = Color(0xFF6D7A72)

private val CartPrimary = Color(0xFF006948)
private val CartOnPrimary = Color(0xFFFFFFFF)
private val CartSecondary = Color(0xFF855300)
private val CartSecondaryContainer = Color(0xFFFEA619)

private val CartError = Color(0xFFBA1A1A)

// ==========================================
// SCREEN UTAMA
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierCartScreen(
    name: String,
    role: String,
    state: CheckoutUiState,
    snackbarHostState: SnackbarHostState,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onRemove: (String) -> Unit,
    onCustomerSearchChanged: (String) -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onClearCustomer: () -> Unit,
    onUseCustomerName: () -> Unit,
    onSelectPayment: (PaymentMethod) -> Unit,
    onAmountPaidChanged: (String) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: () -> Unit,
    onBackToPos: () -> Unit,
    onDashboardClick: () -> Unit = {},
    onPosClick: () -> Unit = {},
    onCashSessionClick: () -> Unit = {},
    onTransactionHistoryClick: () -> Unit = {},
    onStockCheckClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
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
        showHeader = false
    ) { contentModifier ->
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = CartBackground,
            modifier = contentModifier,
            contentWindowInsets = WindowInsets(0.dp)
        ) { contentPadding ->
            if (state.cartItems.isEmpty()) {
                CartEmptyState(
                    onBackToPos = onBackToPos,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                )
            } else {
                CartContent(
                    state = state,
                    onIncrease = onIncrease,
                    onDecrease = onDecrease,
                    onRemove = onRemove,
                    onCustomerSearchChanged = onCustomerSearchChanged,
                    onSelectCustomer = onSelectCustomer,
                    onClearCustomer = onClearCustomer,
                    onUseCustomerName = onUseCustomerName,
                    onSelectPayment = onSelectPayment,
                    onAmountPaidChanged = onAmountPaidChanged,
                    onClearCart = onClearCart,
                    onCheckout = onCheckout,
                    onBackToPos = onBackToPos,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                )
            }
        }
    }
}

// ==========================================
// KONTEN KERANJANG (2-Panel Layout)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartContent(
    state: CheckoutUiState,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onRemove: (String) -> Unit,
    onCustomerSearchChanged: (String) -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onClearCustomer: () -> Unit,
    onUseCustomerName: () -> Unit,
    onSelectPayment: (PaymentMethod) -> Unit,
    onAmountPaidChanged: (String) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: () -> Unit,
    onBackToPos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalItems = state.cartItems.sumOf { it.quantity }
    val paymentIsValid = validateCheckoutPaymentInput(
        paymentMethod = state.selectedPaymentMethod,
        amountPaidInput = state.amountPaidInput,
        total = state.finalTotal
    ).error == null
    val creditCustomerIsValid = state.selectedPaymentMethod !in setOf(PaymentMethod.HUTANG, PaymentMethod.DP) ||
        state.selectedCustomer != null

    Row(modifier = modifier.fillMaxSize()) {
        // BAGIAN KIRI: Daftar Item (2/3 Lebar)
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackToPos,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CartSurface)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Kembali",
                            tint = CartOnSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        Icons.Outlined.ShoppingCart,
                        contentDescription = null,
                        tint = CartPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Keranjang Belanja",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = CartOnSurface
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = CartPrimary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "$totalItems item",
                                    color = CartPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Text(
                            "Daftar Barang",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CartOnSurfaceVariant
                        )
                    }
                }
                TextButton(onClick = onBackToPos) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = null,
                        tint = CartPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Tambah Barang",
                        color = CartPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(state.cartItems, key = { it.cartItemId }) { item ->
                    val product = state.products.firstOrNull { it.productId == item.productId }
                    CartItemCard(
                        item = item,
                        sku = item.sku.ifBlank { product?.sku ?: "" },
                        unitName = item.unitName.ifBlank { product?.unitName ?: "" },
                        onIncrease = { onIncrease(item.cartItemId) },
                        onDecrease = { onDecrease(item.cartItemId) },
                        onRemove = { onRemove(item.cartItemId) }
                    )
                }
            }
        }

        // BAGIAN KANAN: Ringkasan Pesanan (1/3 Lebar)
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            color = CartSurfaceContainerLow,
            border = BorderStroke(1.dp, CartPrimary.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxHeight()
            ) {
                Text(
                    "Ringkasan Pesanan",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = CartOnSurface,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Kalkulasi
                CartCalculationRow(
                    "Subtotal",
                    formatRupiah(state.subtotal)
                )
                if (state.totalDiscount > BigDecimal.ZERO) {
                    CartCalculationRow(
                        "Diskon",
                        "-${formatRupiah(state.totalDiscount)}",
                        isDiscount = true
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = CartOutlineVariant
                )

                // Total
                Text(
                    "Total Bayar",
                    fontSize = 13.sp,
                    color = CartOnSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    formatRupiah(state.finalTotal),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CartPrimary,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Pemilih Pelanggan
                CartCustomerSelector(
                    customerSearchQuery = state.customerSearchQuery,
                    selectedCustomer = state.selectedCustomer,
                    customers = state.customers,
                    isLoading = state.isCustomerLoading,
                    onSearchChanged = onCustomerSearchChanged,
                    onSelectCustomer = onSelectCustomer,
                    onClearCustomer = onClearCustomer,
                    onUseCustomerName = onUseCustomerName,
                    error = state.customerError
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Metode Pembayaran
                PaymentMethodSelector(
                    selectedMethod = state.selectedPaymentMethod,
                    onSelectPayment = onSelectPayment
                )

                // Input uang diterima/DP
                if (state.selectedPaymentMethod == PaymentMethod.DP || state.selectedPaymentMethod == PaymentMethod.TUNAI) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.amountPaidInput,
                        onValueChange = onAmountPaidChanged,
                        placeholder = {
                            Text(
                                if (state.selectedPaymentMethod == PaymentMethod.TUNAI) "Uang diterima" else "Nominal DP",
                                color = CartOutline
                            )
                        },
                        leadingIcon = {
                            Text(
                                "Rp",
                                color = CartPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = CartSurface,
                            focusedContainerColor = CartSurface,
                            unfocusedBorderColor = CartOutlineVariant,
                            focusedBorderColor = CartPrimary
                        ),
                        singleLine = true
                    )
                    if (state.selectedPaymentMethod == PaymentMethod.TUNAI) {
                        Text(
                            "Kembalian: ${checkoutChange(state.amountPaidInput, state.finalTotal)}",
                            color = CartPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Tombol Aksi
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onClearCart,
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, CartError),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CartError
                        )
                    ) {
                        Icon(
                            Icons.Outlined.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Batal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Button(
                        onClick = onCheckout,
                        enabled = !state.isLoading && state.cartItems.isNotEmpty() && paymentIsValid && creditCustomerIsValid,
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CartPrimary
                        )
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = CartOnPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Outlined.Payments,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Bayar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// PEMILIH PELANGGAN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartCustomerSelector(
    customerSearchQuery: String,
    selectedCustomer: Customer?,
    customers: List<Customer>,
    isLoading: Boolean,
    onSearchChanged: (String) -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onClearCustomer: () -> Unit,
    onUseCustomerName: () -> Unit,
    error: UiText?
) {
    var isExpanded by remember { mutableStateOf(false) }
    val typedName = customerSearchQuery.trim()
    val hasQuickName = selectedCustomer == null && typedName.isNotBlank()
    val exactMatch = customers.any { customer ->
        customer.name.equals(typedName, ignoreCase = true)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CartSurface,
        border = BorderStroke(
            1.dp,
            if (selectedCustomer != null || hasQuickName) CartPrimary.copy(alpha = 0.3f)
            else CartOutlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (selectedCustomer == null) {
                            isExpanded = !isExpanded
                        }
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (selectedCustomer != null || hasQuickName) CartPrimary.copy(alpha = 0.1f)
                            else CartSecondaryContainer.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (selectedCustomer != null) Icons.Outlined.Person else Icons.Outlined.EditNote,
                        contentDescription = null,
                        tint = if (selectedCustomer != null || hasQuickName) CartPrimary else CartSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        selectedCustomer?.name ?: typedName.ifBlank { "Pilih Pelanggan" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CartOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        when {
                            selectedCustomer != null -> "Pelanggan terdaftar"
                            hasQuickName -> "Dipakai di nota, tidak disimpan"
                            else -> "Opsional untuk transaksi tunai"
                        }, /*
                        else "Opsional • Loyalty",
                        */
                        fontSize = 12.sp,
                        color = CartOnSurfaceVariant
                    )
                }
                if (selectedCustomer != null || hasQuickName) {
                    IconButton(
                        onClick = {
                            onClearCustomer()
                            isExpanded = false
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Hapus pelanggan",
                            tint = CartOnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Icon(
                        if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = CartOnSurfaceVariant
                    )
                }
            }

            // Dropdown Pencarian Pelanggan
            if (isExpanded && selectedCustomer == null) {
                HorizontalDivider(color = CartOutlineVariant.copy(alpha = 0.3f))
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = customerSearchQuery,
                        onValueChange = onSearchChanged,
                        placeholder = {
                            Text(
                                "Cari nama pelanggan...",
                                color = CartOutline,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = null,
                                tint = CartOutline,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = CartSurfaceContainerLow,
                            focusedContainerColor = CartSurfaceContainerLow,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = CartPrimary
                        ),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                    )

                    error?.let {
                        Text(
                            text = it.toMessage(),
                            color = CartError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (typedName.isNotBlank() && !exactMatch) {
                        Button(
                            onClick = {
                                onUseCustomerName()
                                isExpanded = false
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CartPrimary)
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Pakai \"$typedName\" di nota",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            "Nama ini hanya masuk catatan nota dan tidak dibuat sebagai pelanggan baru.",
                            color = CartOnSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = CartPrimary
                            )
                        }
                    } else {
                        customers.take(4).forEach { customer ->
                            Surface(
                                onClick = {
                                    onSelectCustomer(customer)
                                    isExpanded = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = CartSurfaceContainerLow.copy(alpha = 0.45f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                CartPrimary.copy(alpha = 0.08f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            customer.name.take(1).uppercase(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CartPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            customer.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = CartOnSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!customer.phone.isNullOrBlank()) {
                                            Text(
                                                customer.phone!!,
                                                fontSize = 12.sp,
                                                color = CartOnSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (customers.isEmpty() && customerSearchQuery.isNotBlank()) {
                            Text(
                                "Tidak ada pelanggan terdaftar yang cocok.",
                                fontSize = 13.sp,
                                color = CartOnSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// FORMAT HELPER
// ==========================================
private fun formatRupiah(amount: BigDecimal): String {
    val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
    return "Rp ${formatter.format(amount)}"
}

private fun UiText.toMessage(): String {
    return when (this) {
        is UiText.DynamicString -> value
    }
}

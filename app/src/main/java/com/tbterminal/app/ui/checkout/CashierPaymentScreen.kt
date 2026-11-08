package com.tbterminal.app.ui.checkout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.ui.receivables.CustomerSelectionDialog
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tbterminal.app.data.repository.RepositoryResult
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.ui.checkout.components.CustomerCreditInspectorCard
import com.tbterminal.app.ui.checkout.components.QuickAddCustomerSheet
import com.tbterminal.app.ui.dashboard.cashier.CashierDashboardShell
import com.tbterminal.app.ui.dashboard.cashier.CashierDestination
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val PaymentBackground = Color(0xFFF8FAFC)
private val PaymentBrand = Color(0xFF10533C)
private val PaymentOutline = Color(0xFFE2E8F0)
private val PaymentTextPrimary = Color(0xFF0F172A)
private val PaymentTextMuted = Color(0xFF64748B)

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
    customerRepository: CustomerRepository? = null,
    onToggleDelivery: (Boolean) -> Unit = {},
    onDeliveryFeeChanged: (String) -> Unit = {},
    onDeliveryAddressChanged: (String) -> Unit = {},
    onDeliveryCourierChanged: (String) -> Unit = {},
    onDeliveryFleetChanged: (String) -> Unit = {},
    onDeliveryRecipientPhoneChanged: (String) -> Unit = {},
    onDeliveryVehiclePlateChanged: (String) -> Unit = {},
    onDeliverySpecialInstructionsChanged: (String) -> Unit = {},
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
    val totalItems = state.cartItems.sumOf { it.quantity }
    val paymentIsValid = validateCheckoutPaymentInput(
        paymentMethod = state.selectedPaymentMethod,
        amountPaidInput = state.amountPaidInput,
        total = state.finalTotal
    ).error == null
    val isCustomerRequired = state.selectedPaymentMethod in setOf(PaymentMethod.HUTANG, PaymentMethod.DP)
    val creditCustomerIsValid = !isCustomerRequired || state.selectedCustomer != null

    var showCustomerPickerDialog by remember { mutableStateOf(false) }
    val isAnyPopupOpen = showCustomerPickerDialog || state.isQuickAddCustomerOpen
    val popupBlurRadius by animateDpAsState(
        targetValue = if (isAnyPopupOpen) 16.dp else 0.dp,
        label = "popup_blur_animation"
    )

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
                    .then(if (popupBlurRadius > 0.dp) Modifier.blur(popupBlurRadius) else Modifier)
            ) {
                // ── 1. TOP BAR SESUAI MOCKUP HTML ──
                PaymentTopBar(
                    totalItems = totalItems,
                    onBackToCart = onBackToCart
                )

                // ── 2. MAIN SCROLLABLE CONTENT ──
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // A. TotalBillingCard
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, PaymentOutline.copy(alpha = 0.8f)),
                        shadowElevation = 0.5.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TOTAL TAGIHAN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8),
                                    letterSpacing = 1.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, PaymentOutline.copy(alpha = 0.8f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ShoppingBag,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "$totalItems Item",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = formatRupiah(state.finalTotal),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PaymentTextPrimary,
                                    letterSpacing = (-0.5).sp
                                )

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
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // B. PaymentMethodsSection (Grid 2 Kolom Persis Mockup HTML)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, PaymentOutline.copy(alpha = 0.8f)),
                        shadowElevation = 0.5.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    color = Color(0xFF94A3B8),
                                    letterSpacing = 1.sp
                                )

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, PaymentOutline.copy(alpha = 0.8f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color(0xFF10B981), CircleShape)
                                        )
                                        Text(
                                            text = when (state.selectedPaymentMethod) {
                                                PaymentMethod.TUNAI -> "Tunai"
                                                PaymentMethod.QRIS -> "QRIS"
                                                PaymentMethod.TRANSFER -> "Transfer Bank"
                                                PaymentMethod.HUTANG -> "Hutang / Tempo"
                                                PaymentMethod.DP -> "DP / Bertahap"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                }
                            }

                            // Grid 2-Kolom: Baris 1 (Tunai & QRIS)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                PaymentMethodGridCard(
                                    title = "Tunai",
                                    subtitle = "Cash on Hand",
                                    icon = Icons.Outlined.Payments,
                                    isSelected = state.selectedPaymentMethod == PaymentMethod.TUNAI,
                                    onClick = { onSelectPayment(PaymentMethod.TUNAI) },
                                    modifier = Modifier.weight(1f)
                                )

                                PaymentMethodGridCard(
                                    title = "QRIS",
                                    subtitle = "Scan Statis/Dinamis",
                                    icon = Icons.Outlined.QrCode2,
                                    isSelected = state.selectedPaymentMethod == PaymentMethod.QRIS,
                                    onClick = { onSelectPayment(PaymentMethod.QRIS) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Grid 2-Kolom: Baris 2 (Transfer Bank & Hutang / Tempo)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                PaymentMethodGridCard(
                                    title = "Transfer Bank",
                                    subtitle = "BCA, Mandiri, BRI",
                                    icon = Icons.Outlined.AccountBalance,
                                    isSelected = state.selectedPaymentMethod == PaymentMethod.TRANSFER,
                                    onClick = { onSelectPayment(PaymentMethod.TRANSFER) },
                                    modifier = Modifier.weight(1f)
                                )

                                PaymentMethodGridCard(
                                    title = "Hutang / Tempo",
                                    subtitle = "Jatuh tempo nota",
                                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                                    isSelected = state.selectedPaymentMethod == PaymentMethod.HUTANG,
                                    onClick = { onSelectPayment(PaymentMethod.HUTANG) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Grid 2-Kolom: Baris 3 (DP / Bertahap)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                PaymentMethodGridCard(
                                    title = "DP / Bertahap",
                                    subtitle = "Uang muka termin",
                                    icon = Icons.Outlined.AccountBalanceWallet,
                                    isSelected = state.selectedPaymentMethod == PaymentMethod.DP,
                                    onClick = { onSelectPayment(PaymentMethod.DP) },
                                    modifier = Modifier.fillMaxWidth(0.485f)
                                )
                            }
                        }
                    }

                    // C. CashPaymentInputSection (Bila Tunai Terpilih)
                    if (state.selectedPaymentMethod == PaymentMethod.TUNAI) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, PaymentOutline.copy(alpha = 0.8f)),
                            shadowElevation = 0.5.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Uang Tunai Diterima",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF334155)
                                    )
                                    Text(
                                        text = "Ketik atau pilih nominal",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                // Input Field Bersih & Besar
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, PaymentOutline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Rp",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF94A3B8),
                                            modifier = Modifier.padding(end = 10.dp)
                                        )

                                        OutlinedTextField(
                                            value = state.amountPaidInput,
                                            onValueChange = onAmountPaidChanged,
                                            placeholder = {
                                                Text("0", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                            },
                                            textStyle = TextStyle(
                                                fontSize = 19.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PaymentTextPrimary,
                                                letterSpacing = (-0.3).sp
                                            ),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedBorderColor = Color.Transparent,
                                                unfocusedBorderColor = Color.Transparent
                                            )
                                        )

                                        if (state.amountPaidInput.isNotEmpty()) {
                                            IconButton(
                                                onClick = { onAmountPaidChanged("") },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = "Hapus nominal",
                                                    tint = Color(0xFF94A3B8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Quick Cash Preset Chips (Uang Pas, 50.000, 100.000, dsb)
                                if (state.finalTotal > BigDecimal.ZERO) {
                                    val presets = getCashQuickPresets(state.finalTotal)
                                    val parsedInput = state.amountPaidInput.trim().replace(',', '.').toBigDecimalOrNull()
                                    val isUangPasSelected = parsedInput != null && parsedInput.compareTo(state.finalTotal) == 0

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Chip Uang Pas
                                        Surface(
                                            onClick = {
                                                onAmountPaidChanged(state.finalTotal.stripTrailingZeros().toPlainString())
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.White,
                                            border = BorderStroke(
                                                if (isUangPasSelected) 2.dp else 1.dp,
                                                if (isUangPasSelected) Color(0xFF10B981) else PaymentOutline
                                            ),
                                            shadowElevation = 0.5.dp,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Uang Pas",
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isUangPasSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = if (isUangPasSelected) Color(0xFF0F172A) else Color(0xFF334155)
                                                )
                                            }
                                        }

                                        presets.take(2).forEach { preset ->
                                            val isPresetSelected = parsedInput != null && parsedInput.compareTo(preset) == 0
                                            Surface(
                                                onClick = {
                                                    onAmountPaidChanged(preset.stripTrailingZeros().toPlainString())
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color.White,
                                                border = BorderStroke(
                                                    if (isPresetSelected) 2.dp else 1.dp,
                                                    if (isPresetSelected) Color(0xFF10B981) else PaymentOutline
                                                ),
                                                shadowElevation = 0.5.dp,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(vertical = 10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = formatRupiah(preset).replace("Rp ", ""),
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isPresetSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                        color = if (isPresetSelected) Color(0xFF0F172A) else Color(0xFF334155)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Change/Kembalian Result Banner
                                val parsedAmount = state.amountPaidInput.trim().replace(',', '.').toBigDecimalOrNull()
                                val diff = if (parsedAmount != null) parsedAmount.subtract(state.finalTotal) else BigDecimal.ZERO
                                val isOver = diff >= BigDecimal.ZERO

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (state.amountPaidInput.isNotEmpty() && !isOver) Color(0xFFFEF2F2) else Color.White,
                                    border = BorderStroke(1.dp, if (state.amountPaidInput.isNotEmpty() && !isOver) Color(0xFFFECACA) else PaymentOutline),
                                    shadowElevation = 0.5.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (state.amountPaidInput.isNotEmpty() && !isOver) Color(0xFFFEE2E2) else Color(0xFFF1F5F9),
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (state.amountPaidInput.isNotEmpty() && !isOver) Icons.Outlined.WarningAmber else Icons.Outlined.Check,
                                                        contentDescription = null,
                                                        tint = if (state.amountPaidInput.isNotEmpty() && !isOver) Color(0xFFDC2626) else Color(0xFF64748B),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = if (state.amountPaidInput.isNotEmpty() && !isOver) "Kurang Bayar" else "Kembalian",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (state.amountPaidInput.isNotEmpty() && !isOver) Color(0xFFDC2626) else Color(0xFF334155)
                                            )
                                        }

                                        Text(
                                            text = formatRupiah(diff.abs()),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (state.amountPaidInput.isNotEmpty() && !isOver) Color(0xFFDC2626) else PaymentTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // D. Konteks Khusus Metode Lainnya (QRIS, Transfer, Hutang, DP)
                    when (state.selectedPaymentMethod) {
                        PaymentMethod.QRIS -> {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, PaymentOutline.copy(alpha = 0.8f)),
                                shadowElevation = 0.5.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Surface(
                                        color = Color(0xFFDCFCE7),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.QrCode2,
                                                contentDescription = null,
                                                tint = Color(0xFF047857),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "QRIS Dinamis Otomatis",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PaymentTextPrimary
                                        )
                                        Text(
                                            text = "Tekan tombol di bawah untuk membuka kode QR pembayaran ke pembeli",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF64748B),
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }

                        PaymentMethod.TRANSFER -> {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, PaymentOutline.copy(alpha = 0.8f)),
                                shadowElevation = 0.5.dp,
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
                                            tint = PaymentBrand,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Rekening Transfer Toko",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PaymentTextPrimary
                                        )
                                    }
                                    Text(
                                        text = "BCA: 0123-456-789 a.n. TB Terminal Barokah Jaya",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF334155)
                                    )
                                    Text(
                                        text = "Pastikan dana telah masuk ke mutasi rekening sebelum menyelesaikan pembayaran.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        PaymentMethod.HUTANG -> {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFFFFBEB),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                shadowElevation = 0.5.dp,
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
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Transaksi Hutang / Tempo (Piutang)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                    Text(
                                        text = "Total ${formatRupiah(state.finalTotal)} akan dicatat sebagai piutang jatuh tempo pada akun pelanggan yang dipilih.",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFFB45309),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }

                        PaymentMethod.DP -> {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, PaymentOutline.copy(alpha = 0.8f)),
                                shadowElevation = 0.5.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Nominal Uang Muka (DP) Diterima",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF334155)
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
                                                color = PaymentBrand,
                                                modifier = Modifier.padding(start = 12.dp)
                                            )
                                        },
                                        textStyle = TextStyle(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PaymentTextPrimary
                                        ),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedContainerColor = Color(0xFFF8FAFC),
                                            focusedContainerColor = Color.White,
                                            unfocusedBorderColor = PaymentOutline,
                                            focusedBorderColor = PaymentBrand
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
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFFB45309)
                                                )
                                                Text(
                                                    text = formatRupiah(sisaHutang),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> Unit
                    }

                    // E. Data Pelanggan (Card Selector Persis Mockup HTML)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DATA PELANGGAN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCustomerRequired && state.selectedCustomer == null) Color(0xFFDC2626) else Color(0xFF94A3B8),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (isCustomerRequired) "* Wajib untuk ${state.selectedPaymentMethod.displayName()}" else "Opsional",
                                fontSize = 11.sp,
                                fontWeight = if (isCustomerRequired) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isCustomerRequired) Color(0xFFDC2626) else Color(0xFF94A3B8)
                            )
                        }

                        // Card Pelanggan (Persis Mockup HTML: bg-white rounded-2xl border border-slate-200)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(
                                1.dp,
                                if (isCustomerRequired && state.selectedCustomer == null) Color(0xFFFCA5A5) else PaymentOutline
                            ),
                            shadowElevation = 0.5.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { showCustomerPickerDialog = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Avatar: 40x40 rounded-xl bg-slate-100 border border-slate-200/80
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (state.selectedCustomer != null) {
                                                Text(
                                                    text = state.selectedCustomer.name.take(2).uppercase(),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp,
                                                    color = Color(0xFF334155),
                                                    letterSpacing = 0.5.sp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Outlined.Person,
                                                    contentDescription = null,
                                                    tint = if (isCustomerRequired) Color(0xFFDC2626) else Color(0xFF64748B),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = state.selectedCustomer?.name ?: "Pilih Pelanggan",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (state.selectedCustomer != null) {
                                                val cat = if (state.selectedCustomer.isContractor) "Kontraktor" else "Pelanggan Biasa"
                                                val phone = state.selectedCustomer.phone?.ifBlank { "Tanpa nomor telepon" } ?: "Tanpa nomor telepon"
                                                "$cat • $phone"
                                            } else {
                                                if (isCustomerRequired) "Wajib untuk ${state.selectedPaymentMethod.displayName()}"
                                                else "Pelanggan Biasa • Transaksi tanpa member"
                                            },
                                            fontSize = 12.sp,
                                            color = if (isCustomerRequired && state.selectedCustomer == null) Color(0xFFDC2626) else Color(0xFF94A3B8),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (isCustomerRequired && state.selectedCustomer != null) {
                            CustomerCreditInspectorCard(
                                customer = state.selectedCustomer,
                                currentBillTotal = state.finalTotal
                            )
                        }
                    }

                    // F. FleetDeliverySection (Pengiriman Armada Persis Mockup HTML)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PENGIRIMAN ARMADA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Opsional",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, PaymentOutline.copy(alpha = 0.9f)),
                            shadowElevation = 0.5.dp,
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (state.isDeliveryEnabled) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Outlined.LocalShipping,
                                                    contentDescription = null,
                                                    tint = if (state.isDeliveryEnabled) PaymentBrand else Color(0xFF64748B),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = "Pengiriman Armada",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PaymentTextPrimary
                                            )
                                            Text(
                                                text = if (state.isDeliveryEnabled) "Diantar ke alamat pembeli" else "Bawa pulang sendiri",
                                                fontSize = 11.5.sp,
                                                fontWeight = if (state.isDeliveryEnabled) FontWeight.Medium else FontWeight.Normal,
                                                color = if (state.isDeliveryEnabled) PaymentBrand else Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = state.isDeliveryEnabled,
                                        onCheckedChange = onToggleDelivery,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = PaymentBrand,
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = Color(0xFFE2E8F0)
                                        )
                                    )
                                }

                                AnimatedVisibility(
                                    visible = state.isDeliveryEnabled,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // 1. Jenis Armada (3 Chips Cepat Sesuai Mockup HTML)
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Jenis Armada",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF64748B)
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                listOf("Pick-up", "Truk Engkel", "Motor Viar").forEach { fleet ->
                                                    val isSelected = state.deliveryFleet.equals(fleet, ignoreCase = true)
                                                    Surface(
                                                        onClick = { onDeliveryFleetChanged(if (isSelected) "" else fleet) },
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = Color.White,
                                                        border = BorderStroke(
                                                            if (isSelected) 2.dp else 1.dp,
                                                            if (isSelected) Color(0xFF10B981) else PaymentOutline
                                                        ),
                                                        shadowElevation = 0.5.dp,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.Center
                                                        ) {
                                                            if (isSelected) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(6.dp)
                                                                        .background(Color(0xFF10B981), CircleShape)
                                                                )
                                                                Spacer(Modifier.width(6.dp))
                                                            }
                                                            Text(
                                                                text = fleet,
                                                                fontSize = 12.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                                color = if (isSelected) Color(0xFF065F46) else Color(0xFF334155),
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // 2. Biaya Pengiriman (Ongkir)
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, PaymentOutline),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Rp",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF94A3B8),
                                                    modifier = Modifier.padding(end = 10.dp)
                                                )
                                                OutlinedTextField(
                                                    value = state.deliveryFeeInput,
                                                    onValueChange = onDeliveryFeeChanged,
                                                    placeholder = {
                                                        Text("Biaya Pengiriman (Ongkir)", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                                    },
                                                    textStyle = TextStyle(fontSize = 12.sp, color = PaymentTextPrimary),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedContainerColor = Color.Transparent,
                                                        unfocusedContainerColor = Color.Transparent,
                                                        focusedBorderColor = Color.Transparent,
                                                        unfocusedBorderColor = Color.Transparent
                                                    )
                                                )
                                            }
                                        }

                                        // 3. Alamat Pengiriman
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, PaymentOutline),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.LocationOn,
                                                    contentDescription = null,
                                                    tint = Color(0xFF94A3B8),
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .padding(end = 2.dp)
                                                )
                                                OutlinedTextField(
                                                    value = state.deliveryAddress,
                                                    onValueChange = onDeliveryAddressChanged,
                                                    placeholder = {
                                                        Text("Alamat Pengiriman & Patokan Proyek", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                                    },
                                                    textStyle = TextStyle(fontSize = 12.sp, color = PaymentTextPrimary),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedContainerColor = Color.Transparent,
                                                        unfocusedContainerColor = Color.Transparent,
                                                        focusedBorderColor = Color.Transparent,
                                                        unfocusedBorderColor = Color.Transparent
                                                    )
                                                )
                                            }
                                        }

                                        // 4. No. Telepon / WhatsApp Penerima
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, PaymentOutline),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Phone,
                                                    contentDescription = null,
                                                    tint = Color(0xFF94A3B8),
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .padding(end = 2.dp)
                                                )
                                                OutlinedTextField(
                                                    value = state.deliveryRecipientPhone,
                                                    onValueChange = onDeliveryRecipientPhoneChanged,
                                                    placeholder = {
                                                        Text("No. Telepon / WhatsApp Penerima", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                                    },
                                                    textStyle = TextStyle(fontSize = 12.sp, color = PaymentTextPrimary),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedContainerColor = Color.Transparent,
                                                        unfocusedContainerColor = Color.Transparent,
                                                        focusedBorderColor = Color.Transparent,
                                                        unfocusedBorderColor = Color.Transparent
                                                    )
                                                )
                                            }
                                        }

                                        // 5. Grid 2 Kolom: Nama Supir + No. Plat / Nopol
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Nama Supir (Hanya Nama Supir, Teks Armada Dihilangkan Sesuai Request)
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color.White,
                                                border = BorderStroke(1.dp, PaymentOutline),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Person,
                                                        contentDescription = null,
                                                        tint = Color(0xFF94A3B8),
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .padding(end = 2.dp)
                                                    )
                                                    OutlinedTextField(
                                                        value = state.deliveryCourier,
                                                        onValueChange = onDeliveryCourierChanged,
                                                        placeholder = {
                                                            Text("Nama Supir", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                                        },
                                                        textStyle = TextStyle(fontSize = 12.sp, color = PaymentTextPrimary),
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedContainerColor = Color.Transparent,
                                                            unfocusedContainerColor = Color.Transparent,
                                                            focusedBorderColor = Color.Transparent,
                                                            unfocusedBorderColor = Color.Transparent
                                                        )
                                                    )
                                                }
                                            }

                                            // No. Plat / Nopol
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color.White,
                                                border = BorderStroke(1.dp, PaymentOutline),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Description,
                                                        contentDescription = null,
                                                        tint = Color(0xFF94A3B8),
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .padding(end = 2.dp)
                                                    )
                                                    OutlinedTextField(
                                                        value = state.deliveryVehiclePlate,
                                                        onValueChange = onDeliveryVehiclePlateChanged,
                                                        placeholder = {
                                                            Text("No. Plat / Nopol", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                                        },
                                                        textStyle = TextStyle(fontSize = 12.sp, color = PaymentTextPrimary),
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedContainerColor = Color.Transparent,
                                                            unfocusedContainerColor = Color.Transparent,
                                                            focusedBorderColor = Color.Transparent,
                                                            unfocusedBorderColor = Color.Transparent
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        // 6. Catatan Khusus / Instruksi Lapangan
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, PaymentOutline),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Edit,
                                                    contentDescription = null,
                                                    tint = Color(0xFF94A3B8),
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .padding(end = 2.dp)
                                                )
                                                OutlinedTextField(
                                                    value = state.deliverySpecialInstructions,
                                                    onValueChange = onDeliverySpecialInstructionsChanged,
                                                    placeholder = {
                                                        Text("Instruksi Lapangan / Catatan Supir", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                                    },
                                                    textStyle = TextStyle(fontSize = 12.sp, color = PaymentTextPrimary),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedContainerColor = Color.Transparent,
                                                        unfocusedContainerColor = Color.Transparent,
                                                        focusedBorderColor = Color.Transparent,
                                                        unfocusedBorderColor = Color.Transparent
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // G. Otorisasi Manager Alert (Bila Diperlukan)
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

                // ── 3. STICKY BOTTOM ACTION BAR (PERSIS MOCKUP HTML) ──
                Surface(
                    color = Color.White.copy(alpha = 0.95f),
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sheet Handle Hint
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(width = 40.dp, height = 4.dp)
                                .background(Color(0xFFE2E8F0), RoundedCornerShape(2.dp))
                        )

                        val isSubmitEnabled = !state.isLoading && !state.isPreviewLoading &&
                                state.cartItems.isNotEmpty() && paymentIsValid && creditCustomerIsValid

                        // Big Thumb-Friendly Checkout Button
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
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PaymentBrand,
                                disabledContainerColor = Color(0xFFE2E8F0),
                                disabledContentColor = Color(0xFF94A3B8)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                        ) {
                            if (state.isLoading || state.isPreviewLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSubmitEnabled) Color(0xFF0F172A).copy(alpha = 0.35f) else Color(0xFF94A3B8),
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = when {
                                                state.hasAmbiguousCheckout -> "Periksa Transaksi"
                                                state.checkoutPreview?.approvalRequired == true -> "Persetujuan Diskon"
                                                state.selectedPaymentMethod == PaymentMethod.QRIS -> "Tampilkan Kode QRIS"
                                                state.selectedPaymentMethod == PaymentMethod.HUTANG -> "Catat Piutang"
                                                state.selectedPaymentMethod == PaymentMethod.DP -> "Simpan Transaksi DP"
                                                else -> "Selesaikan Pembayaran"
                                            },
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = if (isSubmitEnabled) Color.White else Color(0xFF94A3B8)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = formatRupiah(state.finalTotal),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSubmitEnabled) Color(0xFFD1FAE5) else Color(0xFF94A3B8),
                                            letterSpacing = (-0.3).sp
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                            contentDescription = null,
                                            tint = if (isSubmitEnabled) Color(0xFF86EFAC) else Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // iOS Home Indicator bar
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 4.dp)
                                .size(width = 128.dp, height = 4.dp)
                                .background(Color(0xFF0F172A).copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }
    }

    if (showCustomerPickerDialog) {
        CustomerModalBottomSheet(
            customerRepository = customerRepository,
            initialCustomers = state.customers,
            currentSelectedCustomer = state.selectedCustomer,
            onCustomerSelected = { customer ->
                if (customer != null) {
                    onSelectCustomer(customer)
                } else {
                    onClearCustomer()
                }
                showCustomerPickerDialog = false
            },
            onAddNewCustomerClick = {
                showCustomerPickerDialog = false
                onOpenQuickAddCustomer()
            },
            onDismiss = { showCustomerPickerDialog = false }
        )
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
// SUB-KOMPONEN: MODAL BOTTOM SHEET PILIH PELANGGAN (PERSIS MOCKUP HTML)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun CustomerModalBottomSheet(
    customerRepository: CustomerRepository?,
    initialCustomers: List<Customer>,
    currentSelectedCustomer: Customer?,
    onCustomerSelected: (Customer?) -> Unit,
    onAddNewCustomerClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var customersList by remember { mutableStateOf(initialCustomers) }
    var tempSelectedCustomer by remember { mutableStateOf<Customer?>(currentSelectedCustomer) }
    var isGeneralSelected by remember { mutableStateOf(currentSelectedCustomer == null) }

    LaunchedEffect(searchQuery, customerRepository) {
        if (customerRepository != null) {
            if (searchQuery.isNotBlank()) delay(250)
            when (val result = customerRepository.getCustomers(
                page = 1,
                limit = 50,
                search = searchQuery.trim().takeIf(String::isNotBlank)
            )) {
                is RepositoryResult.Success -> {
                    customersList = result.data.data
                }
                else -> Unit
            }
        } else {
            customersList = if (searchQuery.isBlank()) {
                initialCustomers
            } else {
                initialCustomers.filter {
                    it.name.contains(searchQuery.trim(), ignoreCase = true) ||
                    (it.phone?.contains(searchQuery.trim(), ignoreCase = true) == true) ||
                    (it.address?.contains(searchQuery.trim(), ignoreCase = true) == true)
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A).copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* prevent backdrop dismissal */ }
                    .heightIn(max = 680.dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 1. Sheet Header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        // Drag Handle Hint
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(width = 36.dp, height = 4.dp)
                                .background(Color(0xFFE2E8F0), RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "Pilih Pelanggan",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    letterSpacing = (-0.3).sp
                                )
                                Text(
                                    text = "Pilih pelanggan untuk mencatat piutang",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Search Toggle Button
                                Surface(
                                    onClick = { isSearchVisible = !isSearchVisible },
                                    shape = CircleShape,
                                    color = if (isSearchVisible) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.Search,
                                            contentDescription = "Buka Pencarian",
                                            tint = if (isSearchVisible) PaymentBrand else Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Close Button
                                Surface(
                                    onClick = onDismiss,
                                    shape = CircleShape,
                                    color = Color(0xFFF1F5F9),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Tutup",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Collapsible Search Field
                        AnimatedVisibility(visible = isSearchVisible) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Search,
                                            contentDescription = null,
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        OutlinedTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            placeholder = {
                                                Text(
                                                    text = "Cari nama, telepon, atau alamat...",
                                                    fontSize = 12.5.sp,
                                                    color = Color(0xFF94A3B8)
                                                )
                                            },
                                            textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF0F172A)),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedBorderColor = Color.Transparent,
                                                unfocusedBorderColor = Color.Transparent
                                            )
                                        )
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(
                                                onClick = { searchQuery = "" },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Hapus",
                                                    tint = Color(0xFF94A3B8),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // 2. List Header Info
                    val totalDataCount = customersList.size + (if (searchQuery.isBlank()) 1 else 0)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daftar Pelanggan",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "$totalDataCount Data",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // 3. Scrollable Customer Options List
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Option 1: Pelanggan Umum (Tanpa Member)
                        if (searchQuery.isBlank()) {
                            CustomerOptionCard(
                                initials = "PU",
                                name = "Pelanggan Umum",
                                category = "Biasa",
                                phoneOrDesc = "• Transaksi Tunai Tanpa Member",
                                address = null,
                                isSelected = isGeneralSelected,
                                onClick = {
                                    isGeneralSelected = true
                                    tempSelectedCustomer = null
                                }
                            )
                        }

                        // Option 2..N: Registered Customers
                        customersList.forEach { customer ->
                            val isSelected = !isGeneralSelected && tempSelectedCustomer?.id == customer.id
                            CustomerOptionCard(
                                initials = customer.name.take(2).uppercase(),
                                name = customer.name,
                                category = if (customer.isContractor) "Kontraktor" else "Biasa",
                                phoneOrDesc = customer.phone?.takeIf { it.isNotBlank() }?.let { "• $it" } ?: "• Tanpa telepon",
                                address = customer.address?.takeIf { it.isNotBlank() },
                                isSelected = isSelected,
                                onClick = {
                                    isGeneralSelected = false
                                    tempSelectedCustomer = customer
                                }
                            )
                        }

                        // Option 3: Add New Customer Action Button (Dashed Border Card)
                        Surface(
                            onClick = onAddNewCustomerClick,
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 13.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Tambah Pelanggan Baru",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155)
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                    }

                    // 4. Bottom Action CTA Buttons
                    Surface(
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (isGeneralSelected || tempSelectedCustomer == null) {
                                        onCustomerSelected(null)
                                    } else {
                                        onCustomerSelected(tempSelectedCustomer)
                                    }
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10533C),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Text("Pilih", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            TextButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                            ) {
                                Text(
                                    text = "Batal",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerOptionCard(
    initials: String,
    name: String,
    category: String,
    phoneOrDesc: String,
    address: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) Color(0xFF10B981) else Color(0xFFE2E8F0)
        ),
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar: w-10 h-10 rounded-xl bg-slate-100 text-slate-700
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Info Column
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (category.equals("Kontraktor", ignoreCase = true)) Color(0xFF059669) else Color(0xFF475569)
                        )
                        Text(
                            text = phoneOrDesc,
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!address.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = address,
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Radio Circle
            Surface(
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(
                    2.dp,
                    if (isSelected) Color(0xFF059669) else Color(0xFFCBD5E1)
                ),
                modifier = Modifier.size(20.dp)
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .background(Color(0xFF059669), CircleShape)
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SUB-KOMPONEN: TOP BAR PEMBAYARAN (PERSIS MOCKUP HTML)
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
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Kembali ke Keranjang",
                            tint = Color(0xFF334155),
                            modifier = Modifier.size(20.dp)
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
                            color = PaymentTextPrimary,
                            letterSpacing = (-0.3).sp
                        )

                        Text(
                            text = "($totalItems item)",
                            color = Color(0xFF64748B),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, PaymentOutline),
                    shadowElevation = 0.5.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF10B981), CircleShape)
                        )
                        Text(
                            text = "POS-01",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                    }
                }
            }
            HorizontalDivider(color = Color(0xFFF1F5F9))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SUB-KOMPONEN: KARTU METODE PEMBAYARAN GRID (2 KOLOM PERSIS MOCKUP HTML)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun PaymentMethodGridCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) Color(0xFF10B981) else PaymentOutline
        ),
        shadowElevation = if (isSelected) 0.5.dp else 0.dp,
        modifier = modifier.height(58.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Icon Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isSelected) Color(0xFF047857) else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Title & Subtitle
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) Color(0xFF065F46) else PaymentTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isSelected) Color(0xFF059669) else Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Top-Right Check Badge
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .background(Color(0xFF10B981), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}

private fun com.tbterminal.app.ui.common.UiText.toMessage(): String {
    return when (this) {
        is com.tbterminal.app.ui.common.UiText.DynamicString -> value
    }
}

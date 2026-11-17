package com.tbterminal.app.ui.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.model.DiscountType
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal

private val sampleCustomer = Customer(
    id = "CUST-001",
    name = "Budi Santoso",
    phone = "08123456789",
    address = "Jl. Merdeka Barat No. 12, Surabaya",
    isContractor = false,
    creditLimit = BigDecimal("5000000"),
    paymentTermDays = 30,
    isActive = true,
    createdAt = "2026-01-01T00:00:00Z",
    updatedAt = "2026-01-01T00:00:00Z"
)

private val sampleCartItemsFase3 = listOf(
    CartItem(
        productId = "P01",
        productName = "Semen Gresik PPC 40kg",
        unitPrice = BigDecimal("65000"),
        quantity = 2,
        unitName = "Sak"
    ),
    CartItem(
        productId = "P05",
        productName = "Paku Kayu 5cm (2 Inci) SNI Super Kuat",
        unitPrice = BigDecimal("18000"),
        quantity = 2,
        unitName = "Kg"
    )
)

// Subtotal: (65,000 * 2) + (18,000 * 2) = 130,000 + 36,000 = 166,000
// Diskon Nota 5%: 166,000 * 5% = 8,300
// Total: 166,000 - 8,300 = 157,700
// Pembulatan: +300 -> Final Total: 158,000

private val samplePopulatedCartState = CheckoutUiState(
    cartItems = sampleCartItemsFase3,
    selectedCustomer = sampleCustomer,
    transactionDiscount = CheckoutDiscount(
        type = DiscountType.PERCENTAGE,
        value = BigDecimal("5")
    ),
    subtotal = BigDecimal("166000"),
    itemDiscountTotal = BigDecimal.ZERO,
    transactionDiscountAmount = BigDecimal("8300"),
    totalDiscount = BigDecimal("8300"),
    cashRoundingUnit = 500,
    roundingAmount = BigDecimal("300"),
    finalTotal = BigDecimal("158000"),
    selectedPaymentMethod = PaymentMethod.TUNAI
)

@Preview(name = "Fase 3 - Cart Receipt Sheet Empty", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PreviewCartReceiptSheetEmpty() {
    TbterminalappTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8FAFC)
        ) {
            CashierCartScreen(
                name = "Ahmad Kasir",
                role = "KASIR",
                state = CheckoutUiState(),
                snackbarHostState = remember { SnackbarHostState() },
                onIncrease = {},
                onDecrease = {},
                onRemove = {},
                onCustomerSearchChanged = {},
                onSelectCustomer = {},
                onClearCustomer = {},
                onUseCustomerName = {},
                onSelectPayment = {},
                onAmountPaidChanged = {},
                onClearCart = {},
                onItemDiscountChanged = { _, _ -> },
                onTransactionDiscountChanged = {},
                onCheckout = {},
                onBackToPos = {}
            )
        }
    }
}

@Preview(name = "Fase 3 - Cart Unified Receipt Populated", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PreviewCartReceiptSheetPopulated() {
    TbterminalappTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8FAFC)
        ) {
            CashierCartScreen(
                name = "Ahmad Kasir",
                role = "KASIR",
                state = samplePopulatedCartState,
                snackbarHostState = remember { SnackbarHostState() },
                onIncrease = {},
                onDecrease = {},
                onRemove = {},
                onCustomerSearchChanged = {},
                onSelectCustomer = {},
                onClearCustomer = {},
                onUseCustomerName = {},
                onSelectPayment = {},
                onAmountPaidChanged = {},
                onClearCart = {},
                onItemDiscountChanged = { _, _ -> },
                onTransactionDiscountChanged = {},
                onCheckout = {},
                onBackToPos = {}
            )
        }
    }
}

@Preview(name = "Fase 3 - Payment Cash Initial (Uang Pas & Presets)", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PreviewPaymentCashInitial() {
    val state = samplePopulatedCartState.copy(
        selectedPaymentMethod = PaymentMethod.TUNAI,
        amountPaidInput = ""
    )
    TbterminalappTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8FAFC)
        ) {
            CashierPaymentScreen(
                name = "Ahmad Kasir",
                role = "KASIR",
                state = state,
                snackbarHostState = remember { SnackbarHostState() },
                onCustomerSearchChanged = {},
                onSelectCustomer = {},
                onClearCustomer = {},
                onUseCustomerName = {},
                onSelectPayment = {},
                onAmountPaidChanged = {},
                onTransactionDiscountChanged = {},
                onCheckout = {},
                onBackToCart = {}
            )
        }
    }
}

@Preview(name = "Fase 3 - Payment Cash Sufficient (Kembalian Hijau)", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PreviewPaymentCashSufficient() {
    val state = samplePopulatedCartState.copy(
        selectedPaymentMethod = PaymentMethod.TUNAI,
        amountPaidInput = "200000"
    )
    TbterminalappTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8FAFC)
        ) {
            CashierPaymentScreen(
                name = "Ahmad Kasir",
                role = "KASIR",
                state = state,
                snackbarHostState = remember { SnackbarHostState() },
                onCustomerSearchChanged = {},
                onSelectCustomer = {},
                onClearCustomer = {},
                onUseCustomerName = {},
                onSelectPayment = {},
                onAmountPaidChanged = {},
                onTransactionDiscountChanged = {},
                onCheckout = {},
                onBackToCart = {}
            )
        }
    }
}

@Preview(name = "Fase 3 - Payment Cash Shortage (Kurang Merah)", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PreviewPaymentCashShortage() {
    val state = samplePopulatedCartState.copy(
        selectedPaymentMethod = PaymentMethod.TUNAI,
        amountPaidInput = "100000"
    )
    TbterminalappTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8FAFC)
        ) {
            CashierPaymentScreen(
                name = "Ahmad Kasir",
                role = "KASIR",
                state = state,
                snackbarHostState = remember { SnackbarHostState() },
                onCustomerSearchChanged = {},
                onSelectCustomer = {},
                onClearCustomer = {},
                onUseCustomerName = {},
                onSelectPayment = {},
                onAmountPaidChanged = {},
                onTransactionDiscountChanged = {},
                onCheckout = {},
                onBackToCart = {}
            )
        }
    }
}

@Preview(name = "Fase 3 - Payment Method Tempo (HUTANG)", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PreviewPaymentMethodTempo() {
    val state = samplePopulatedCartState.copy(
        selectedPaymentMethod = PaymentMethod.HUTANG,
        amountPaidInput = ""
    )
    TbterminalappTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8FAFC)
        ) {
            CashierPaymentScreen(
                name = "Ahmad Kasir",
                role = "KASIR",
                state = state,
                snackbarHostState = remember { SnackbarHostState() },
                onCustomerSearchChanged = {},
                onSelectCustomer = {},
                onClearCustomer = {},
                onUseCustomerName = {},
                onSelectPayment = {},
                onAmountPaidChanged = {},
                onTransactionDiscountChanged = {},
                onCheckout = {},
                onBackToCart = {}
            )
        }
    }
}

package com.tbterminal.app.ui.salestransactions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.local.dao.TransactionDao
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.sync.OfflineCheckoutSyncService
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.dashboard.DashboardBrandGreenDark
import com.tbterminal.app.ui.dashboard.DashboardTextSecondary
import com.tbterminal.app.ui.dashboard.DashboardWarningOrange
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AdminTransactionHistoryScreen(
    name: String,
    role: String,
    cashReconciliationRepository: CashReconciliationRepository,
    transactionDao: TransactionDao? = null,
    offlineCheckoutSyncService: OfflineCheckoutSyncService? = null,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onProductCategoriesClick: () -> Unit,
    onProductUnitsClick: () -> Unit,
    onCashReconciliationClick: () -> Unit,
    onSalesTransactionsClick: () -> Unit,
    onReportsClick: () -> Unit,
    onPriceManagementClick: () -> Unit,
    onStockOpnameClick: () -> Unit,
    onStockOpnameFormClick: () -> Unit,
    onIncomingGoodsClick: () -> Unit,
    onIncomingGoodsFormClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onOperationalAuditClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onReceiptClick: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminTransactionHistoryViewModel = viewModel(
        factory = AdminTransactionHistoryViewModel.factory(
            repository = cashReconciliationRepository,
            transactionDao = transactionDao,
            offlineCheckoutSyncService = offlineCheckoutSyncService,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadTransactions(page = 1) }

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.SalesTransactions,
        pageTitle = "Riwayat Penjualan",
        onDashboardClick = onDashboardClick,
        onProductsClick = onProductsClick,
        onAddProductClick = onAddProductClick,
        onProductCategoriesClick = onProductCategoriesClick,
        onProductUnitsClick = onProductUnitsClick,
        onCashReconciliationClick = onCashReconciliationClick,
        onSalesTransactionsClick = onSalesTransactionsClick,
        onReportsClick = onReportsClick,
        onPriceManagementClick = onPriceManagementClick,
        onStockOpnameClick = onStockOpnameClick,
        onStockOpnameFormClick = onStockOpnameFormClick,
        onIncomingGoodsClick = onIncomingGoodsClick,
        onIncomingGoodsFormClick = onIncomingGoodsFormClick,
        onSupplierDebtsClick = onSupplierDebtsClick,
        onReceivablesClick = onReceivablesClick,
        onCustomersClick = onCustomersClick,
        onOperationalAuditClick = onOperationalAuditClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout,
    ) { contentModifier ->
        RefreshableContent(
            isRefreshing = uiState.isLoading && uiState.transactions.isNotEmpty(),
            onRefresh = viewModel::refresh,
            modifier = contentModifier,
        ) {
            TransactionHistoryContent(
                state = uiState,
                onSearchChanged = viewModel::updateSearchQuery,
                onStatusChanged = viewModel::updateStatusFilter,
                onPaymentMethodChanged = viewModel::updatePaymentMethodFilter,
                onDatePresetSelected = viewModel::setDatePreset,
                onDateSelected = viewModel::setDate,
                onSyncClick = viewModel::syncLocalTransaction,
                onSyncAllClick = viewModel::syncAllLocalTransactions,
                onRetry = viewModel::refresh,
                onPreviousPage = viewModel::previousPage,
                onNextPage = viewModel::nextPage,
                onReceiptClick = onReceiptClick,
                modifier = Modifier,
            )
        }
    }
}

internal fun CashTransaction.receiptNumber(): String = receiptId

internal fun BigDecimal.moneyText(): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(this)

internal fun String.displayDateTime(): String = runCatching {
    OffsetDateTime.parse(this).format(
        DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.forLanguageTag("id-ID")),
    )
}.getOrDefault(this)

internal fun String.displayDate(): String = runCatching {
    OffsetDateTime.parse(this).format(
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")),
    )
}.getOrDefault("-")

internal fun String.displayTime(): String = runCatching {
    OffsetDateTime.parse(this).format(DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("id-ID")))
}.getOrDefault("-")

internal fun String.statusColor(): Color = when (lowercase()) {
    "lunas", "paid", "success", "completed" -> DashboardBrandGreenDark
    "dp", "partial" -> DashboardWarningOrange
    "hutang", "unpaid" -> Color(0xFFEF4444)
    "voided" -> Color(0xFF7F1D1D)
    else -> DashboardTextSecondary
}

internal fun String.formatDisplayDate(): String = runCatching {
    LocalDate.parse(this).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}.getOrDefault(this)

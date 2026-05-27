package com.tbterminal.app.ui.payables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.model.SupplierPayable
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun AdminSupplierDebtScreen(
    name: String,
    role: String,
    purchasingRepository: PurchasingRepository,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onProductCategoriesClick: () -> Unit,
    onProductUnitsClick: () -> Unit,
    onCashReconciliationClick: () -> Unit = {},
    onSalesTransactionsClick: () -> Unit = {},
    onReportsClick: () -> Unit = {},
    onPriceManagementClick: () -> Unit = {},
    onStockOpnameClick: () -> Unit,
    onStockOpnameFormClick: () -> Unit,
    onIncomingGoodsClick: () -> Unit,
    onIncomingGoodsFormClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onReceivablesClick: () -> Unit = {},
    onCustomersClick: () -> Unit = {},
    onOperationalAuditClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: SupplierDebtViewModel = viewModel(
        factory = SupplierDebtViewModel.factory(purchasingRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.SupplierDebts,
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
        onLogout = onLogout
    ) { contentModifier ->
        SupplierDebtContent(
            modifier = contentModifier,
            uiState = uiState,
            onSearchChanged = viewModel::onSearchChanged,
            onStatusFilterChanged = viewModel::onStatusFilterChanged,
            onRefresh = { viewModel.loadPayables() },
            onPayClick = viewModel::openPayment,
            onPreviousPage = viewModel::previousPage,
            onNextPage = viewModel::nextPage,
            onDismissMessage = viewModel::clearMessage
        )
    }

    uiState.selectedPayable?.let { payable ->
        SupplierPaymentDialog(
            payable = payable,
            uiState = uiState,
            onAmountChanged = viewModel::onPaymentAmountChanged,
            onMethodChanged = viewModel::onPaymentMethodChanged,
            onReferenceChanged = viewModel::onReferenceChanged,
            onNotesChanged = viewModel::onNotesChanged,
            onDismiss = viewModel::closePayment,
            onSubmit = viewModel::submitPayment
        )
    }
}

@Composable
private fun SupplierDebtContent(
    modifier: Modifier,
    uiState: SupplierDebtUiState,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (SupplierDebtStatusFilter) -> Unit,
    onRefresh: () -> Unit,
    onPayClick: (SupplierPayable) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onDismissMessage: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DebtBackground)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SupplierDebtHeader()
        SupplierDebtMessage(uiState, onDismissMessage)
        SupplierDebtMetrics(uiState)
        SupplierDebtTableCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            uiState = uiState,
            onSearchChanged = onSearchChanged,
            onStatusFilterChanged = onStatusFilterChanged,
            onRefresh = onRefresh,
            onPayClick = onPayClick,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage
        )
    }
}

@Composable
private fun SupplierDebtHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text("Utang Supplier", color = DebtText, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Pantau tagihan supplier dari barang masuk dan catat pembayaran secara terkontrol.",
                color = DebtMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(DebtPrimary.copy(alpha = 0.1f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null, tint = DebtPrimaryDark)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Kontrol utang supplier", color = DebtPrimaryDark, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SupplierDebtMessage(
    uiState: SupplierDebtUiState,
    onDismiss: () -> Unit
) {
    val message = uiState.errorMessage ?: uiState.message ?: return
    val isError = uiState.errorMessage != null
    val tint = if (isError) DebtDanger else DebtPrimaryDark
    val background = if (isError) DebtDanger.copy(alpha = 0.1f) else DebtPrimary.copy(alpha = 0.1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (isError) Icons.Outlined.Warning else Icons.Default.CheckCircle, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.width(12.dp))
        Text(message, modifier = Modifier.weight(1f), color = tint, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onDismiss) { Text("Tutup", color = tint) }
    }
}

@Composable
private fun SupplierDebtMetrics(uiState: SupplierDebtUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        DebtMetricCard(
            modifier = Modifier.weight(1f),
            title = "TOTAL DATA",
            value = "${uiState.total}",
            subtitle = "Tagihan pada filter aktif",
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            tint = DebtPrimaryDark
        )
        DebtMetricCard(
            modifier = Modifier.weight(1f),
            title = "SISA HALAMAN INI",
            value = uiState.pageRemainingTotal.currencyText(),
            subtitle = "Akumulasi data yang tampil",
            icon = Icons.Outlined.AccountBalanceWallet,
            tint = DebtInfo
        )
        DebtMetricCard(
            modifier = Modifier.weight(1f),
            title = "BELUM LUNAS",
            value = "${uiState.unpaidCount}",
            subtitle = "Butuh tindak lanjut",
            icon = Icons.Outlined.Payments,
            tint = DebtWarning
        )
    }
}

@Composable
private fun DebtMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(132.dp),
        colors = CardDefaults.cardColors(containerColor = DebtSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DebtLine)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = DebtMuted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                Text(value, color = DebtText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text(subtitle, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

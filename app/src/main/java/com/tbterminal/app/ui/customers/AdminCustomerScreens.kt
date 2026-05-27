package com.tbterminal.app.ui.customers

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AdminCustomerListScreen(
    name: String,
    role: String,
    customerRepository: CustomerRepository,
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
    onCustomersClick: () -> Unit,
    onOperationalAuditClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onAddCustomerClick: () -> Unit,
    onEditCustomerClick: (String) -> Unit,
    onCustomerDetailClick: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: CustomerListViewModel = viewModel(
        factory = CustomerListViewModel.factory(customerRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var customerToDeactivate by remember { mutableStateOf<Customer?>(null) }

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.Customers,
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
        CustomerListContent(
            modifier = contentModifier,
            uiState = uiState,
            onSearchChanged = viewModel::onSearchChanged,
            onRefresh = { viewModel.loadCustomers() },
            onAddCustomerClick = onAddCustomerClick,
            onEditCustomerClick = onEditCustomerClick,
            onCustomerDetailClick = onCustomerDetailClick,
            onDeactivateClick = { customerToDeactivate = it },
            onPreviousPage = viewModel::previousPage,
            onNextPage = viewModel::nextPage,
            onDismissMessage = viewModel::clearMessage
        )
    }

    customerToDeactivate?.let { customer ->
        AlertDialog(
            onDismissRequest = { customerToDeactivate = null },
            title = { Text("Nonaktifkan Pelanggan") },
            text = { Text("Pelanggan ${customer.name} tidak akan tampil pada daftar aktif dan pilihan transaksi kredit.") },
            confirmButton = {
                Button(
                    onClick = {
                        customerToDeactivate = null
                        viewModel.deactivate(customer)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CustomerDanger)
                ) {
                    Text("Nonaktifkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDeactivate = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun AdminCustomerFormScreen(
    name: String,
    role: String,
    customerRepository: CustomerRepository,
    customerId: String?,
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
    onCustomersClick: () -> Unit,
    onOperationalAuditClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onBackToCustomers: () -> Unit,
    onLogout: () -> Unit,
    viewModel: CustomerFormViewModel = viewModel(
        factory = CustomerFormViewModel.factory(customerRepository, customerId)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            viewModel.resetSavedState()
            onBackToCustomers()
        }
    }

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.CustomerForm,
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
        CustomerFormContent(
            modifier = contentModifier,
            uiState = uiState,
            onInputChanged = viewModel::onInputChanged,
            onSave = viewModel::save,
            onCancel = onBackToCustomers
        )
    }
}

@Composable
fun AdminCustomerDetailScreen(
    name: String,
    role: String,
    customerRepository: CustomerRepository,
    customerId: String,
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
    onCustomersClick: () -> Unit,
    onOperationalAuditClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onEditCustomerClick: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: CustomerDetailViewModel = viewModel(
        factory = CustomerDetailViewModel.factory(customerRepository, customerId)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.Customers,
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
        CustomerDetailContent(
            modifier = contentModifier,
            uiState = uiState,
            onRetry = viewModel::loadDetail,
            onBack = onCustomersClick,
            onEdit = onEditCustomerClick
        )
    }
}

@Composable
private fun CustomerListContent(
    modifier: Modifier,
    uiState: CustomerListUiState,
    onSearchChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onAddCustomerClick: () -> Unit,
    onEditCustomerClick: (String) -> Unit,
    onCustomerDetailClick: (String) -> Unit,
    onDeactivateClick: (Customer) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onDismissMessage: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CustomerBackground)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        CustomerHeader(
            title = "Pelanggan",
            subtitle = "Kelola data pelanggan, limit kredit, dan termin pembayaran.",
            action = {
                Button(
                    onClick = onAddCustomerClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CustomerPrimary),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tambah Pelanggan", fontWeight = FontWeight.Bold)
                }
            }
        )
        CustomerMessage(uiState.message ?: uiState.errorMessage, uiState.errorMessage != null, onDismissMessage)
        CustomerMetrics(uiState)
        CustomerTableCard(
            modifier = Modifier.weight(1f),
            uiState = uiState,
            onSearchChanged = onSearchChanged,
            onRefresh = onRefresh,
            onEditCustomerClick = onEditCustomerClick,
            onCustomerDetailClick = onCustomerDetailClick,
            onDeactivateClick = onDeactivateClick,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage
        )
    }
}

@Composable
private fun CustomerFormContent(
    modifier: Modifier,
    uiState: CustomerFormUiState,
    onInputChanged: (CustomerFormInput) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val input = uiState.input
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CustomerBackground)
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        CustomerHeader(
            title = if (uiState.isEditMode) "Edit Pelanggan" else "Tambah Pelanggan",
            subtitle = "Lengkapi identitas dan kebijakan kredit pelanggan."
        )
        CustomerMessage(uiState.errorMessage, isError = true, onDismiss = {})
        Card(
            colors = CardDefaults.cardColors(containerColor = CustomerSurface),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, CustomerLine),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (uiState.isLoading) {
                    CustomerLoading()
                } else {
                    CustomerField("NAMA PELANGGAN", input.name, { onInputChanged(input.copy(name = it)) }, "Contoh: CV Perkasa Mulia")
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CustomerField("NOMOR HP", input.phone, { onInputChanged(input.copy(phone = it)) }, "08xxxxxxxxxx", Modifier.weight(1f))
                        CustomerField("LIMIT KREDIT", input.creditLimit, { onInputChanged(input.copy(creditLimit = it.numericInput())) }, "0", Modifier.weight(1f))
                        CustomerField("TERMIN HARI", input.paymentTermDays, { onInputChanged(input.copy(paymentTermDays = it.filter(Char::isDigit))) }, "0", Modifier.weight(1f))
                    }
                    CustomerField("ALAMAT", input.address, { onInputChanged(input.copy(address = it)) }, "Alamat pelanggan", minLines = 3)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CustomerSoft)
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pelanggan Kontraktor", color = CustomerText, fontWeight = FontWeight.Bold)
                            Text("Aktifkan jika pelanggan memakai harga kontraktor dan limit kredit khusus.", color = CustomerMuted, fontSize = 12.sp)
                        }
                        Switch(
                            checked = input.isContractor,
                            onCheckedChange = { onInputChanged(input.copy(isContractor = it)) },
                            colors = SwitchDefaults.colors(checkedTrackColor = CustomerPrimary)
                        )
                    }
                    HorizontalDivider(color = CustomerLine)
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = onCancel) { Text("Batal", color = CustomerMuted, fontWeight = FontWeight.Bold) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = onSave,
                            enabled = !uiState.isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = CustomerPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (uiState.isSaving) "Menyimpan..." else "Simpan Pelanggan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerDetailContent(
    modifier: Modifier,
    uiState: CustomerDetailUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onEdit: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CustomerBackground)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        CustomerHeader(
            title = "Detail Pelanggan",
            subtitle = "Ringkasan profil, kebijakan kredit, dan status pelanggan.",
            action = { TextButton(onClick = onBack) { Text("Kembali", color = CustomerPrimaryDark, fontWeight = FontWeight.Bold) } }
        )
        when {
            uiState.isLoading -> CustomerLoading()
            uiState.errorMessage != null -> CustomerCenteredError(uiState.errorMessage, onRetry)
            uiState.customer != null -> CustomerDetailCard(uiState.customer, onEdit)
        }
    }
}

@Composable
private fun CustomerTableCard(
    modifier: Modifier,
    uiState: CustomerListUiState,
    onSearchChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onEditCustomerClick: (String) -> Unit,
    onCustomerDetailClick: (String) -> Unit,
    onDeactivateClick: (Customer) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CustomerSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CustomerLine)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChanged,
                    placeholder = { Text("Cari nama atau nomor HP...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = customerTextFieldColors()
                )
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, CustomerLine)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Muat Ulang", fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = CustomerLine)
            CustomerTableHeader()
            HorizontalDivider(color = CustomerLine)
            when {
                uiState.isLoading -> CustomerLoading()
                uiState.errorMessage != null -> CustomerCenteredError(uiState.errorMessage, onRefresh)
                uiState.customers.isEmpty() -> CustomerEmpty()
                else -> LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.customers, key = Customer::id) { customer ->
                        CustomerTableRow(customer, onCustomerDetailClick, onEditCustomerClick, onDeactivateClick)
                        HorizontalDivider(color = CustomerLine.copy(alpha = 0.7f))
                    }
                }
            }
            CustomerPagination(uiState, onPreviousPage, onNextPage)
        }
    }
}

@Composable
private fun CustomerTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().background(CustomerSoft.copy(alpha = 0.72f)).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderCell("PELANGGAN", Modifier.weight(2.5f))
        HeaderCell("KONTAK", Modifier.weight(1.5f))
        HeaderCell("TIPE", Modifier.weight(1.3f))
        HeaderCell("LIMIT KREDIT", Modifier.weight(1.6f), Alignment.End)
        HeaderCell("TERMIN", Modifier.weight(1f), Alignment.CenterHorizontally)
        HeaderCell("AKSI", Modifier.weight(1.2f), Alignment.End)
    }
}

@Composable
private fun CustomerTableRow(
    customer: Customer,
    onDetail: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDeactivate: (Customer) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onDetail(customer.id) }.padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(2.5f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(CustomerPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(customer.name.initialText(), color = CustomerPrimaryDark, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(customer.name, color = CustomerText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(customer.address ?: "Alamat belum diisi", color = CustomerMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(customer.phone ?: "-", modifier = Modifier.weight(1.5f), color = CustomerMuted, fontWeight = FontWeight.SemiBold)
        Box(modifier = Modifier.weight(1.3f)) {
            CustomerChip(if (customer.isContractor) "KONTRAKTOR" else "UMUM", if (customer.isContractor) CustomerInfo else CustomerPrimaryDark)
        }
        Text(customer.creditLimit.currencyText(), modifier = Modifier.weight(1.6f), color = CustomerText, fontWeight = FontWeight.Bold, maxLines = 1)
        Text("${customer.paymentTermDays} hari", modifier = Modifier.weight(1f), color = CustomerMuted, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.weight(1.2f), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = { onEdit(customer.id) }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = CustomerMuted) }
            IconButton(onClick = { onDeactivate(customer) }) { Icon(Icons.Default.Delete, contentDescription = "Nonaktifkan", tint = CustomerDanger.copy(alpha = 0.8f)) }
        }
    }
}

@Composable
private fun CustomerHeader(title: String, subtitle: String, action: @Composable (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Column {
            Text(title, color = CustomerText, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = CustomerMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        action?.invoke()
    }
}

@Composable
private fun CustomerMetrics(uiState: CustomerListUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        CustomerMetric("TOTAL PELANGGAN", uiState.totalCustomers.toString(), "Data aktif", Icons.Outlined.Group, CustomerPrimaryDark, Modifier.weight(1f))
        CustomerMetric("KONTRAKTOR", uiState.contractorCount.toString(), "Pada halaman ini", Icons.Outlined.Badge, CustomerInfo, Modifier.weight(1f))
        CustomerMetric("LIMIT HALAMAN", uiState.totalCreditLimit.currencyText(), "Akumulasi data tampil", Icons.Outlined.AccountBalanceWallet, CustomerWarning, Modifier.weight(1f))
    }
}

@Composable
private fun CustomerMetric(title: String, value: String, subtitle: String, icon: ImageVector, tint: Color, modifier: Modifier) {
    Card(modifier = modifier.height(124.dp), colors = CardDefaults.cardColors(containerColor = CustomerSurface), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, CustomerLine)) {
        Row(modifier = Modifier.fillMaxSize().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, color = CustomerMuted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                Text(value, color = CustomerText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text(subtitle, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CustomerDetailCard(customer: Customer, onEdit: (String) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = CustomerSurface), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, CustomerLine)) {
        Column(modifier = Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(58.dp).clip(CircleShape).background(CustomerPrimary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = CustomerPrimaryDark)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(customer.name, color = CustomerText, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text(if (customer.isContractor) "Pelanggan kontraktor" else "Pelanggan umum", color = CustomerMuted)
                }
                Button(onClick = { onEdit(customer.id) }, colors = ButtonDefaults.buttonColors(containerColor = CustomerPrimary), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit")
                }
            }
            HorizontalDivider(color = CustomerLine)
            DetailRow("Nomor HP", customer.phone ?: "-")
            DetailRow("Alamat", customer.address ?: "-")
            DetailRow("Limit Kredit", customer.creditLimit.currencyText())
            DetailRow("Termin Pembayaran", "${customer.paymentTermDays} hari")
            DetailRow("Status", if (customer.isActive) "Aktif" else "Nonaktif")
        }
    }
}

@Composable
private fun CustomerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1
) {
    Column(modifier = modifier) {
        Text(label, color = CustomerMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = CustomerMuted.copy(alpha = 0.65f)) },
            minLines = minLines,
            singleLine = minLines == 1,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = customerTextFieldColors()
        )
    }
}

@Composable
private fun CustomerMessage(message: String?, isError: Boolean, onDismiss: () -> Unit) {
    if (message == null) return
    val tint = if (isError) CustomerDanger else CustomerPrimaryDark
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.11f)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (isError) Icons.Outlined.Warning else Icons.Default.CheckCircle, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.width(12.dp))
        Text(message, modifier = Modifier.weight(1f), color = tint, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onDismiss) { Text("Tutup", color = tint) }
    }
}

@Composable
private fun CustomerPagination(uiState: CustomerListUiState, onPreviousPage: () -> Unit, onNextPage: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(CustomerSoft.copy(alpha = 0.6f)).padding(horizontal = 24.dp, vertical = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Menampilkan ${uiState.currentStart}-${uiState.currentEnd} dari ${uiState.totalCustomers} pelanggan", color = CustomerMuted, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PaginationButton(Icons.Default.ChevronLeft, enabled = uiState.page > 1, onClick = onPreviousPage)
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(CustomerPrimary), contentAlignment = Alignment.Center) {
                Text(uiState.page.toString(), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text("/ ${uiState.totalPages}", color = CustomerMuted, fontWeight = FontWeight.Bold)
            PaginationButton(Icons.Default.ChevronRight, enabled = uiState.page < uiState.totalPages, onClick = onNextPage)
        }
    }
}

@Composable
private fun PaginationButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(44.dp), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(0.dp)) {
        Icon(icon, contentDescription = null)
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier, align: Alignment.Horizontal = Alignment.Start) {
    Column(modifier = modifier, horizontalAlignment = align) {
        Text(text, color = CustomerMuted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun CustomerChip(text: String, tint: Color) {
    Box(modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 5.dp)) {
        Text(text, color = tint, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = CustomerMuted, fontWeight = FontWeight.SemiBold)
        Text(value, color = CustomerText, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CustomerLoading() {
    Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = CustomerPrimary)
    }
}

@Composable
private fun CustomerCenteredError(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = CustomerDanger, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onRetry) { Text("Muat Ulang") }
        }
    }
}

@Composable
private fun CustomerEmpty() {
    Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
        Text("Belum ada pelanggan yang cocok.", color = CustomerMuted, fontSize = 16.sp)
    }
}

@Composable
private fun customerTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CustomerPrimary,
    unfocusedBorderColor = CustomerLine,
    focusedContainerColor = CustomerSoft.copy(alpha = 0.72f),
    unfocusedContainerColor = CustomerSoft.copy(alpha = 0.72f)
)

private fun String.initialText(): String {
    return trim().firstOrNull()?.uppercase() ?: "P"
}

private fun BigDecimal.currencyText(): String {
    return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(this)
}

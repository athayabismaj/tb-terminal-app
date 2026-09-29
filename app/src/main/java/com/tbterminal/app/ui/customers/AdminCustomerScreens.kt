package com.tbterminal.app.ui.customers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.isOwnerPersona
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.components.AppConfirmationSpec
import com.tbterminal.app.ui.components.AppConfirmDialog
import com.tbterminal.app.ui.components.AppEmptyState
import com.tbterminal.app.ui.components.AppErrorState
import com.tbterminal.app.ui.components.AppSnackbar
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.components.TbMobileControlSheet
import com.tbterminal.app.ui.components.TbMobileFilterButton
import com.tbterminal.app.ui.components.TbMobileSheetDoneButton
import com.tbterminal.app.ui.components.TbPagination
import com.tbterminal.app.navigation.AppAccessPolicy
import com.tbterminal.app.navigation.AppCapability
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
    onBackToPrevious: (() -> Unit)? = null,
    onLogout: () -> Unit,
    viewModel: CustomerListViewModel = viewModel(
        factory = CustomerListViewModel.factory(customerRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var customerToDeactivate by remember { mutableStateOf<Customer?>(null) }
    var wasDisplayed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(viewModel) {
        // Owner navigation reuses the list entry; show edits made in a child page on return.
        if (isOwnerPersona(role) && wasDisplayed) viewModel.refresh()
        wasDisplayed = true
    }

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.Customers,
        pageTitle = "Pelanggan",
        onBack = onBackToPrevious,
        showPageHeader = true,
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
        RefreshableContent(
            isRefreshing = uiState.isLoading && uiState.customers.isNotEmpty(),
            onRefresh = viewModel::refresh,
            modifier = contentModifier,
        ) {
            CustomerListContent(
            modifier = Modifier,
            uiState = uiState,
            canManage = AppAccessPolicy.can(role, AppCapability.MANAGE_CUSTOMERS),
            onSearchChanged = viewModel::onSearchChanged,
            onCategoryFilterChanged = viewModel::onCategoryFilterChanged,
            onAddCustomerClick = onAddCustomerClick,
            onEditCustomerClick = onEditCustomerClick,
            onCustomerDetailClick = onCustomerDetailClick,
            onDeactivateClick = { customerToDeactivate = it },
            onPreviousPage = viewModel::previousPage,
            onNextPage = viewModel::nextPage,
            onDismissMessage = viewModel::clearMessage
            )
        }
    }

    customerToDeactivate?.let { customer ->
        AppConfirmDialog(
            spec = AppConfirmationSpec(
                title = "Nonaktifkan pelanggan",
                target = customer.name,
                consequence = "Pelanggan tidak lagi tampil pada daftar aktif atau pilihan transaksi kredit.",
                confirmLabel = "Nonaktifkan",
            ),
            onDismiss = { customerToDeactivate = null },
            onConfirm = {
                viewModel.deactivate(customer)
                customerToDeactivate = null
            },
            isLoading = uiState.isMutating,
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
        pageTitle = if (customerId == null) "Tambah Pelanggan" else "Edit Pelanggan",
        onBack = onBackToCustomers,
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
        pageTitle = "Detail Pelanggan",
        onBack = onCustomersClick,
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
            onEdit = onEditCustomerClick.takeIf {
                AppAccessPolicy.can(role, AppCapability.MANAGE_CUSTOMERS)
            }
        )
    }
}

@Composable
internal fun CustomerListContent(
    modifier: Modifier,
    uiState: CustomerListUiState,
    canManage: Boolean,
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (CustomerCategoryFilter) -> Unit,
    onAddCustomerClick: () -> Unit,
    onEditCustomerClick: (String) -> Unit,
    onCustomerDetailClick: (String) -> Unit,
    onDeactivateClick: (Customer) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onDismissMessage: () -> Unit
) {
    BoxWithConstraints(modifier.fillMaxSize().background(CustomerBackground)) {
        val compact = maxWidth < 700.dp
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 1120.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 14.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 18.dp)
        ) {
        CustomerMessage(uiState.errorMessage, isError = true, onDismissMessage)
        CustomerTableCard(
            modifier = Modifier.fillMaxWidth(),
            uiState = uiState,
            onSearchChanged = onSearchChanged,
            onCategoryFilterChanged = onCategoryFilterChanged,
            canManage = canManage,
            onEditCustomerClick = onEditCustomerClick,
            onCustomerDetailClick = onCustomerDetailClick,
            onDeactivateClick = onDeactivateClick,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage,
            onAddCustomerClick = onAddCustomerClick,
            compact = compact
            )
        }
        AppSnackbar(
            message = uiState.message,
            onDismiss = onDismissMessage,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
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
    BoxWithConstraints(modifier.fillMaxSize().background(CustomerBackground)) {
        val compact = maxWidth < 700.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 14.dp else 24.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
        CustomerMessage(uiState.errorMessage, isError = true, onDismiss = {})
        Card(
            colors = CardDefaults.cardColors(containerColor = CustomerSurface),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, CustomerLine),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(if (compact) 16.dp else 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (uiState.isLoading) {
                    CustomerLoading()
                } else {
                    CustomerField(
                        label = "NAMA PELANGGAN",
                        value = input.name,
                        onValueChange = { onInputChanged(input.copy(name = it)) },
                        placeholder = "Contoh: CV Perkasa Mulia",
                        error = uiState.fieldErrors[CUSTOMER_FIELD_NAME],
                        initialFocus = true,
                    )
                    if (compact) Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        CustomerField("NOMOR HP", input.phone, { onInputChanged(input.copy(phone = it)) }, "08xxxxxxxxxx", error = uiState.fieldErrors[CUSTOMER_FIELD_PHONE], keyboardType = KeyboardType.Phone)
                        CustomerField("LIMIT KREDIT", input.creditLimit, { onInputChanged(input.copy(creditLimit = it.numericInput())) }, "0", error = uiState.fieldErrors[CUSTOMER_FIELD_CREDIT_LIMIT], keyboardType = KeyboardType.Decimal)
                        CustomerField("TERMIN HARI", input.paymentTermDays, { onInputChanged(input.copy(paymentTermDays = it.filter(Char::isDigit))) }, "0", error = uiState.fieldErrors[CUSTOMER_FIELD_PAYMENT_TERM], keyboardType = KeyboardType.Number)
                    } else Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CustomerField("NOMOR HP", input.phone, { onInputChanged(input.copy(phone = it)) }, "08xxxxxxxxxx", Modifier.weight(1f), error = uiState.fieldErrors[CUSTOMER_FIELD_PHONE], keyboardType = KeyboardType.Phone)
                        CustomerField("LIMIT KREDIT", input.creditLimit, { onInputChanged(input.copy(creditLimit = it.numericInput())) }, "0", Modifier.weight(1f), error = uiState.fieldErrors[CUSTOMER_FIELD_CREDIT_LIMIT], keyboardType = KeyboardType.Decimal)
                        CustomerField("TERMIN HARI", input.paymentTermDays, { onInputChanged(input.copy(paymentTermDays = it.filter(Char::isDigit))) }, "0", Modifier.weight(1f), error = uiState.fieldErrors[CUSTOMER_FIELD_PAYMENT_TERM], keyboardType = KeyboardType.Number)
                    }
                    CustomerField(
                        label = "ALAMAT",
                        value = input.address,
                        onValueChange = { onInputChanged(input.copy(address = it)) },
                        placeholder = "Alamat pelanggan",
                        minLines = 3,
                        error = uiState.fieldErrors[CUSTOMER_FIELD_ADDRESS],
                        imeAction = ImeAction.Done,
                    )
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
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (uiState.isSaving) "Menyimpan..." else "Simpan Pelanggan", fontWeight = FontWeight.Bold)
                        }
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
    onEdit: ((String) -> Unit)?
) {
    BoxWithConstraints(modifier.fillMaxSize().background(CustomerBackground)) {
        val compact = maxWidth < 700.dp
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 14.dp else 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        when {
            uiState.isLoading -> CustomerLoading()
            uiState.errorMessage != null -> CustomerCenteredError(uiState.errorMessage, onRetry)
            uiState.customer != null -> CustomerDetailCard(uiState.customer, onEdit)
        }
        }
    }
}

@Composable
private fun CustomerTableCard(
    modifier: Modifier,
    uiState: CustomerListUiState,
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (CustomerCategoryFilter) -> Unit,
    canManage: Boolean,
    onEditCustomerClick: (String) -> Unit,
    onCustomerDetailClick: (String) -> Unit,
    onDeactivateClick: (Customer) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onAddCustomerClick: () -> Unit,
    compact: Boolean
) {
    Column(modifier = modifier.fillMaxWidth()) {
        CustomerTableToolbar(
            uiState = uiState,
            onSearchChanged = onSearchChanged,
            onCategoryFilterChanged = onCategoryFilterChanged,
            canManage = canManage,
            onAddCustomerClick = onAddCustomerClick,
        )
        Spacer(modifier = Modifier.height(if (compact) 14.dp else 18.dp))
        when {
            uiState.isLoading && uiState.customers.isEmpty() -> CustomerListLoading(compact = compact)
            uiState.errorMessage != null && uiState.customers.isEmpty() -> CustomerEmpty("Pelanggan gagal dimuat.")
            uiState.visibleCustomers.isEmpty() -> CustomerEmpty()
            compact -> Column(
                modifier = Modifier.fillMaxWidth().testTag("customer-card-list"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                uiState.visibleCustomers.forEach { customer ->
                    CustomerMobileRow(customer, onCustomerDetailClick, onEditCustomerClick, onDeactivateClick, canManage)
                }
            }
            else -> Surface(
                modifier = Modifier.fillMaxWidth().testTag("customer-table"),
                color = CustomerSurface,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, CustomerLine.copy(alpha = 0.8f)),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    CustomerTableHeader()
                    uiState.visibleCustomers.forEachIndexed { index, customer ->
                        CustomerTableRow(customer, onCustomerDetailClick, onEditCustomerClick, onDeactivateClick, canManage)
                        if (index < uiState.visibleCustomers.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 24.dp),
                                color = CustomerLine.copy(alpha = 0.55f),
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        CustomerPagination(uiState, compact, onPreviousPage, onNextPage)
    }
}

@Composable
private fun CustomerTableToolbar(
    uiState: CustomerListUiState,
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (CustomerCategoryFilter) -> Unit,
    canManage: Boolean,
    onAddCustomerClick: () -> Unit,
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val search: @Composable (Modifier) -> Unit = { fieldModifier ->
        androidx.compose.material3.Surface(
            modifier = fieldModifier.height(52.dp).testTag("customer-search"),
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, com.tbterminal.app.ui.theme.TbOutline.copy(alpha = 0.7f)),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Icon(Icons.Outlined.Search, "Cari pelanggan", tint = CustomerMuted, modifier = Modifier.size(20.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChanged,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    singleLine = true,
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(color = CustomerText),
                    decorationBox = @Composable { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (uiState.searchQuery.isBlank()) {
                                Text("Cari pelanggan", color = CustomerMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("customer-toolbar")) {
        val compactActions = maxWidth < 840.dp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (compactActions) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            search(Modifier.weight(1f))
            TbMobileFilterButton(
                onClick = { showFilters = true },
                active = uiState.categoryFilter != CustomerCategoryFilter.ALL,
                contentDescription = "Filter pelanggan",
                testTag = "customer-open-filter",
            )
            if (canManage) CustomerAddButton(compact = compactActions, onClick = onAddCustomerClick)
        }

        if (showFilters) {
            TbMobileControlSheet(
                title = "Filter pelanggan",
                subtitle = "Pilih jenis pelanggan yang ditampilkan",
                onDismiss = { showFilters = false },
                testTag = "customer-filter-sheet",
            ) {
                CustomerFilterChips(
                    selectedFilter = uiState.categoryFilter,
                    onCategoryFilterChanged = onCategoryFilterChanged,
                )
                TbMobileSheetDoneButton(
                    onClick = { showFilters = false },
                    testTag = "customer-filter-done",
                )
            }
        }
    }
}

@Composable
private fun CustomerAddButton(compact: Boolean, onClick: () -> Unit) {
    if (compact) {
        androidx.compose.material3.Surface(
            modifier = Modifier.size(48.dp).testTag("customer-add").clickable { onClick() },
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            contentColor = com.tbterminal.app.ui.theme.TbGreenDark,
            border = BorderStroke(1.dp, com.tbterminal.app.ui.theme.TbOutline.copy(alpha = 0.7f)),
            shadowElevation = 1.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Add, contentDescription = "Tambah pelanggan", modifier = Modifier.size(24.dp))
            }
        }
        return
    }
    Button(
        onClick = onClick,
        modifier = Modifier.height(52.dp).testTag("customer-add"),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CustomerPrimary),
        contentPadding = PaddingValues(horizontal = if (compact) 14.dp else 18.dp),
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text("Tambah pelanggan", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CustomerFilterChips(
    selectedFilter: CustomerCategoryFilter,
    onCategoryFilterChanged: (CustomerCategoryFilter) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag("customer-filter"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CustomerCategoryFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onCategoryFilterChanged(filter) },
                label = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            when (filter) {
                                CustomerCategoryFilter.ALL -> "Semua"
                                CustomerCategoryFilter.GENERAL -> "Umum"
                                CustomerCategoryFilter.CONTRACTOR -> "Kontraktor"
                            },
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("customer-filter-${filter.name}"),
                shape = RoundedCornerShape(14.dp),
                border = null,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = CustomerSurface,
                    labelColor = CustomerMuted,
                    selectedContainerColor = CustomerSoft,
                    selectedLabelColor = CustomerPrimaryDark,
                ),
            )
        }
    }
}

@Composable
private fun CustomerTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().background(CustomerSoft.copy(alpha = 0.55f)).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderCell("Pelanggan", Modifier.weight(2.5f))
        HeaderCell("Kontak", Modifier.weight(1.5f))
        HeaderCell("Tipe", Modifier.weight(1.3f))
        HeaderCell("Limit kredit", Modifier.weight(1.6f), Alignment.End)
        HeaderCell("Termin", Modifier.weight(1f), Alignment.CenterHorizontally)
        HeaderCell("Aksi", Modifier.weight(1.2f), Alignment.End)
    }
}

@Composable
private fun CustomerTableRow(
    customer: Customer,
    onDetail: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDeactivate: (Customer) -> Unit,
    canManage: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CustomerSurface)
            .clickable { onDetail(customer.id) }
            .padding(horizontal = 24.dp, vertical = 18.dp),
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
        Box(modifier = Modifier.weight(1.3f)) { CustomerTypeBadge(customer.isContractor) }
        Text(customer.creditLimit.currencyText(), modifier = Modifier.weight(1.6f), color = CustomerText, fontWeight = FontWeight.Bold, maxLines = 1)
        Text("${customer.paymentTermDays} hari", modifier = Modifier.weight(1f), color = CustomerMuted, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.weight(1.2f), horizontalArrangement = Arrangement.End) {
            if (canManage) CustomerActionsMenu(customer, onEdit, onDeactivate)
        }
    }
}

@Composable
private fun CustomerDetailCard(customer: Customer, onEdit: ((String) -> Unit)?) {
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
                if (onEdit != null) {
                    Button(onClick = { onEdit(customer.id) }, colors = ButtonDefaults.buttonColors(containerColor = CustomerPrimary), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit")
                    }
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
    minLines: Int = 1,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    initialFocus: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(initialFocus) {
        if (initialFocus) focusRequester.requestFocus()
    }
    Column(modifier = modifier) {
        Text(label, color = CustomerMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = CustomerMuted.copy(alpha = 0.65f)) },
            minLines = minLines,
            singleLine = minLines == 1,
            isError = error != null,
            supportingText = error?.let { message -> { Text(message) } },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = { focusManager.clearFocus() },
            ),
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
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
private fun CustomerPagination(uiState: CustomerListUiState, compact: Boolean, onPreviousPage: () -> Unit, onNextPage: () -> Unit) {
    TbPagination(
        currentPage = uiState.page,
        totalPages = uiState.totalPages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        supportingText = "${uiState.currentStart}-${uiState.currentEnd} dari ${uiState.totalCustomers} pelanggan",
        isLoading = uiState.isLoading,
        testTag = "customer-pagination",
    )
}

@Composable
private fun CustomerMobileRow(
    customer: Customer,
    onDetail: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDeactivate: (Customer) -> Unit,
    canManage: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("customer-card-${customer.id}"),
        onClick = { onDetail(customer.id) },
        color = CustomerSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, com.tbterminal.app.ui.theme.TbOutline.copy(alpha = 0.7f)),
        shadowElevation = 1.dp
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(com.tbterminal.app.ui.theme.TbGreenLight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(customer.name.initialText(), color = com.tbterminal.app.ui.theme.TbGreenDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    customer.name,
                    modifier = Modifier.weight(1f),
                    color = CustomerText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                CustomerTypeBadge(
                    isContractor = customer.isContractor,
                    modifier = Modifier.testTag("customer-type-${customer.id}"),
                )
                if (canManage) {
                    Spacer(Modifier.width(6.dp))
                    CustomerActionsMenu(customer, onEdit, onDeactivate)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                CustomerInfoRow(
                    icon = Icons.Outlined.Phone,
                    label = "Nomor HP",
                    value = customer.phone ?: "Belum diisi",
                    modifier = Modifier.weight(1f),
                    isEmpty = customer.phone.isNullOrBlank()
                )
                CustomerInfoRow(
                    icon = Icons.Outlined.Place,
                    label = "Alamat",
                    value = customer.address ?: "Belum diisi",
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    isEmpty = customer.address.isNullOrBlank()
                )
            }

            HorizontalDivider(color = CustomerLine.copy(alpha = 0.55f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Limit kredit", color = CustomerMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Text(
                        customer.creditLimit.currencyText(),
                        color = CustomerText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Termin pembayaran", color = CustomerMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "${customer.paymentTermDays} hari",
                        color = CustomerText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    isEmpty: Boolean = false,
) {
    Row(
        modifier = modifier.heightIn(min = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(com.tbterminal.app.ui.theme.TbGreenLight.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = com.tbterminal.app.ui.theme.TbGreenDark,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = CustomerMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(
                value,
                modifier = Modifier.padding(top = 4.dp),
                color = if (isEmpty) CustomerMuted else CustomerText,
                fontSize = 12.sp,
                fontWeight = if (isEmpty) FontWeight.Normal else FontWeight.SemiBold,
                fontStyle = if (isEmpty) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CustomerActionsMenu(
    customer: Customer,
    onEdit: (String) -> Unit,
    onDeactivate: (Customer) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(32.dp).testTag("customer-actions-${customer.id}"),
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Aksi pelanggan",
                modifier = Modifier.size(20.dp),
                tint = CustomerMuted,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CustomerSurface, RoundedCornerShape(14.dp)),
        ) {
            DropdownMenuItem(
                text = { Text("Edit pelanggan") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                        tint = CustomerText,
                    )
                },
                onClick = {
                    expanded = false
                    onEdit(customer.id)
                },
                modifier = Modifier.heightIn(min = 48.dp),
            )
            if (customer.isActive) {
                DropdownMenuItem(
                    text = { Text("Nonaktifkan", color = CustomerDanger) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                            tint = CustomerDanger,
                        )
                    },
                    onClick = {
                        expanded = false
                        onDeactivate(customer)
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier, align: Alignment.Horizontal = Alignment.Start) {
    Column(modifier = modifier, horizontalAlignment = align) {
        Text(text, color = CustomerMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CustomerTypeBadge(isContractor: Boolean, modifier: Modifier = Modifier) {
    val tint = CustomerPrimaryDark
    val label = if (isContractor) "Kontraktor" else "Umum"
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tint.copy(alpha = 0.11f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(tint))
        Text(label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
private fun CustomerListLoading(compact: Boolean) {
    if (compact) {
        Column(
            modifier = Modifier.fillMaxWidth().testTag("customer-card-skeleton-list"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(4) { CustomerCardSkeleton() }
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("customer-table-skeleton"),
            color = CustomerSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, CustomerLine.copy(alpha = 0.8f)),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                SkeletonBox(Modifier.fillMaxWidth(0.32f).height(14.dp))
                Spacer(Modifier.height(14.dp))
                repeat(5) { index ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SkeletonBox(Modifier.weight(2.5f).height(16.dp))
                        SkeletonBox(Modifier.weight(1.5f).height(14.dp))
                        SkeletonBox(Modifier.weight(1.3f).height(28.dp))
                        SkeletonBox(Modifier.weight(1.6f).height(14.dp))
                        SkeletonBox(Modifier.weight(1f).height(14.dp))
                    }
                    if (index < 4) HorizontalDivider(color = CustomerLine.copy(alpha = 0.45f))
                }
            }
        }
    }
}

@Composable
private fun CustomerCardSkeleton() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CustomerSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CustomerLine.copy(alpha = 0.85f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SkeletonBox(Modifier.size(44.dp))
                SkeletonBox(Modifier.weight(1f).height(15.dp))
                SkeletonBox(Modifier.width(70.dp).height(24.dp))
                SkeletonBox(Modifier.size(40.dp))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) { index ->
                    Column(modifier = Modifier.weight(1f).heightIn(min = 64.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            SkeletonBox(Modifier.size(30.dp))
                            SkeletonBox(Modifier.fillMaxWidth(0.48f).height(9.dp))
                        }
                        SkeletonBox(
                            Modifier
                                .padding(start = 38.dp, top = 5.dp)
                                .fillMaxWidth(if (index == 0) 0.72f else 1f)
                                .height(12.dp),
                        )
                    }
                }
            }
            HorizontalDivider(color = CustomerLine.copy(alpha = 0.45f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SkeletonBox(Modifier.weight(1f).height(28.dp))
                SkeletonBox(Modifier.weight(1f).height(28.dp))
            }
        }
    }
}

@Composable
private fun CustomerLoading() {
    com.tbterminal.app.ui.components.SkeletonList(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        itemCount = 4,
    )
}

@Composable
private fun CustomerCenteredError(message: String, onRetry: () -> Unit) {
    AppErrorState(message = message, onRetry = onRetry, modifier = Modifier.height(220.dp))
}

@Composable
private fun CustomerEmpty(message: String = "Belum ada pelanggan yang cocok.") {
    AppEmptyState(message = message, modifier = Modifier.height(220.dp))
}

@Composable
private fun customerTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CustomerPrimary,
    unfocusedBorderColor = CustomerLine,
    focusedContainerColor = CustomerSoft.copy(alpha = 0.72f),
    unfocusedContainerColor = CustomerSoft.copy(alpha = 0.72f)
)

@Composable
private fun customerToolbarTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = CustomerSurface,
    unfocusedContainerColor = CustomerSurface,
    disabledContainerColor = CustomerSurface,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    cursorColor = CustomerPrimaryDark,
)

private fun String.initialText(): String {
    return trim().firstOrNull()?.uppercase() ?: "P"
}

private fun BigDecimal.currencyText(): String {
    return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(this)
}

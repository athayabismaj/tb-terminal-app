package com.tbterminal.app.ui.customers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.zIndex
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
import com.tbterminal.app.ui.components.TopFloatingCapsule
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
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var showFilterPopup by rememberSaveable { mutableStateOf(false) }
    var showSummaryPopup by rememberSaveable { mutableStateOf(false) }
    val canManage = AppAccessPolicy.can(role, AppCapability.MANAGE_CUSTOMERS)
    val isFiltered = uiState.categoryFilter != CustomerCategoryFilter.ALL || uiState.statusFilter != CustomerStatusFilter.ALL

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
        topBarActions = {
            // Button 1: Search [ 🔍 ]
            IconButton(
                onClick = { isSearchExpanded = !isSearchExpanded },
                modifier = Modifier.size(36.dp).testTag("customer-toggle-search")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Cari pelanggan",
                    tint = if (isSearchExpanded || uiState.searchQuery.isNotEmpty()) Color(0xFF256B57) else Color(0xFF475569),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Button 2: Filter [ 🎛️ ]
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { showFilterPopup = true },
                    modifier = Modifier.size(36.dp).testTag("customer-open-filters")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "Filter pelanggan",
                        tint = if (isFiltered) Color(0xFF256B57) else Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (isFiltered) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .size(8.dp)
                            .background(Color(0xFFE5A93C), CircleShape)
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                }
            }

            // Button 3: Ringkasan [ 📑 ]
            IconButton(
                onClick = { showSummaryPopup = true },
                modifier = Modifier.size(36.dp).testTag("customer-summary")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = "Tampilkan ringkasan pelanggan",
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(20.dp)
                )
            }
        },
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
                canManage = canManage,
                isSearchExpanded = isSearchExpanded,
                onToggleSearch = { isSearchExpanded = !isSearchExpanded },
                showFilterPopup = showFilterPopup,
                onDismissFilter = { showFilterPopup = false },
                showSummaryPopup = showSummaryPopup,
                onDismissSummary = { showSummaryPopup = false },
                onSearchChanged = viewModel::onSearchChanged,
                onCategoryFilterChanged = viewModel::onCategoryFilterChanged,
                onFilterApplied = viewModel::onFilterApplied,
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
    isSearchExpanded: Boolean = false,
    onToggleSearch: () -> Unit = {},
    showFilterPopup: Boolean = false,
    onDismissFilter: () -> Unit = {},
    showSummaryPopup: Boolean = false,
    onDismissSummary: () -> Unit = {},
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (CustomerCategoryFilter) -> Unit,
    onFilterApplied: (CustomerCategoryFilter, CustomerStatusFilter) -> Unit = { _, _ -> },
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
            CustomerTableCard(
                modifier = Modifier.fillMaxWidth(),
                uiState = uiState,
                isSearchExpanded = isSearchExpanded,
                onToggleSearch = onToggleSearch,
                onSearchChanged = onSearchChanged,
                onCategoryFilterChanged = onCategoryFilterChanged,
                onFilterApplied = onFilterApplied,
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
        TopFloatingCapsule(
            message = uiState.message,
            error = uiState.errorMessage,
            onDismiss = onDismissMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                .zIndex(99f),
        )

        if (showFilterPopup) {
            CustomerFilterBottomSheet(
                uiState = uiState,
                onDismiss = onDismissFilter,
                onApply = onFilterApplied
            )
        }

        if (showSummaryPopup) {
            CustomerSummaryBottomSheet(
                uiState = uiState,
                onDismiss = onDismissSummary
            )
        }
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

    TopFloatingCapsule(
        message = null,
        error = uiState.errorMessage,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 16.dp, start = 20.dp, end = 20.dp)
            .zIndex(99f),
    )
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
    isSearchExpanded: Boolean = false,
    onToggleSearch: () -> Unit = {},
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (CustomerCategoryFilter) -> Unit,
    onFilterApplied: (CustomerCategoryFilter, CustomerStatusFilter) -> Unit = { _, _ -> },
    canManage: Boolean,
    onEditCustomerClick: (String) -> Unit,
    onCustomerDetailClick: (String) -> Unit,
    onDeactivateClick: (Customer) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onAddCustomerClick: () -> Unit,
    compact: Boolean
) {
    val isFiltered = uiState.categoryFilter != CustomerCategoryFilter.ALL || uiState.statusFilter != CustomerStatusFilter.ALL
    val isSearchActive = isSearchExpanded || uiState.searchQuery.isNotEmpty()

    Column(modifier = modifier.fillMaxWidth()) {
        if (compact) {
            // Expandable Search Bar (Riwayat Pembelian style)
            AnimatedVisibility(
                visible = isSearchActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF256B57).copy(alpha = 0.5f)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = Color(0xFF256B57),
                            modifier = Modifier.size(18.dp)
                        )
                        androidx.compose.foundation.text.BasicTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchChanged,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("customer-search-input"),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color(0xFF1E293B),
                                fontSize = 13.sp
                            ),
                            decorationBox = { innerTextField ->
                                if (uiState.searchQuery.isEmpty()) {
                                    Text(
                                        text = "Cari nama, kontak, atau alamat...",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchChanged("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Hapus",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Active Filter Chips
            if (isFiltered) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Filter:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )

                    if (uiState.categoryFilter != CustomerCategoryFilter.ALL) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEAF3EF),
                            border = BorderStroke(1.dp, Color(0xFF256B57).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = when (uiState.categoryFilter) {
                                        CustomerCategoryFilter.GENERAL -> "Umum"
                                        CustomerCategoryFilter.CONTRACTOR -> "Kontraktor"
                                        else -> ""
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF256B57)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .clickable { onCategoryFilterChanged(CustomerCategoryFilter.ALL) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Close, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(11.dp))
                                }
                            }
                        }
                    }

                    if (uiState.statusFilter != CustomerStatusFilter.ALL) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEAF3EF),
                            border = BorderStroke(1.dp, Color(0xFF256B57).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = when (uiState.statusFilter) {
                                        CustomerStatusFilter.ACTIVE -> "Aktif"
                                        CustomerStatusFilter.INACTIVE -> "Nonaktif"
                                        else -> ""
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF256B57)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .clickable { onFilterApplied(uiState.categoryFilter, CustomerStatusFilter.ALL) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Close, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(11.dp))
                                }
                            }
                        }
                    }

                    Text(
                        text = "Reset",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFEF4444),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onFilterApplied(CustomerCategoryFilter.ALL, CustomerStatusFilter.ALL) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
            if (isSearchActive || isFiltered) {
                Spacer(modifier = Modifier.height(10.dp))
            }
        } else {
            CustomerTableToolbar(
                uiState = uiState,
                onSearchChanged = onSearchChanged,
                onCategoryFilterChanged = onCategoryFilterChanged,
                canManage = canManage,
                onAddCustomerClick = onAddCustomerClick,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        CustomerSectionHeader(
            canManage = canManage,
            onAddCustomerClick = onAddCustomerClick,
        )
        Spacer(modifier = Modifier.height(if (compact) 8.dp else 12.dp))
        when {
            uiState.isLoading && uiState.customers.isEmpty() -> CustomerListLoading(compact = compact)
            uiState.errorMessage != null && uiState.customers.isEmpty() -> CustomerEmpty("Pelanggan gagal dimuat.")
            uiState.visibleCustomers.isEmpty() -> CustomerEmpty()
            compact -> Surface(
                modifier = Modifier.fillMaxWidth().testTag("customer-card-list"),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    uiState.visibleCustomers.forEachIndexed { index, customer ->
                        CustomerMobileCard(customer, onCustomerDetailClick, onEditCustomerClick, onDeactivateClick, canManage)
                        if (index < uiState.visibleCustomers.lastIndex) {
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                        }
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomerFilterBottomSheet(
    uiState: CustomerListUiState,
    onDismiss: () -> Unit,
    onApply: (CustomerCategoryFilter, CustomerStatusFilter) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedCategory by remember(uiState.categoryFilter) { mutableStateOf(uiState.categoryFilter) }
    var selectedStatus by remember(uiState.statusFilter) { mutableStateOf(uiState.statusFilter) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .background(Color(0xFFCBD5E1), RoundedCornerShape(2.dp))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Filter Pelanggan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Atur tipe dan status pelanggan",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup filter",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Section 1: TIPE PELANGGAN
            Text(
                text = "TIPE PELANGGAN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = Color(0xFF64748B)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CustomerFilterOptionCard(
                    label = "Semua Tipe",
                    isSelected = selectedCategory == CustomerCategoryFilter.ALL,
                    onClick = { selectedCategory = CustomerCategoryFilter.ALL },
                    modifier = Modifier.weight(1f)
                )
                CustomerFilterOptionCard(
                    label = "Umum",
                    isSelected = selectedCategory == CustomerCategoryFilter.GENERAL,
                    onClick = { selectedCategory = CustomerCategoryFilter.GENERAL },
                    modifier = Modifier.weight(1f)
                )
                CustomerFilterOptionCard(
                    label = "Kontraktor",
                    isSelected = selectedCategory == CustomerCategoryFilter.CONTRACTOR,
                    onClick = { selectedCategory = CustomerCategoryFilter.CONTRACTOR },
                    modifier = Modifier.weight(1f)
                )
            }

            // Section 2: STATUS PELANGGAN
            Text(
                text = "STATUS PELANGGAN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = Color(0xFF64748B)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CustomerFilterOptionCard(
                    label = "Semua Status",
                    isSelected = selectedStatus == CustomerStatusFilter.ALL,
                    onClick = { selectedStatus = CustomerStatusFilter.ALL },
                    modifier = Modifier.weight(1f)
                )
                CustomerFilterOptionCard(
                    label = "Aktif",
                    isSelected = selectedStatus == CustomerStatusFilter.ACTIVE,
                    onClick = { selectedStatus = CustomerStatusFilter.ACTIVE },
                    modifier = Modifier.weight(1f)
                )
                CustomerFilterOptionCard(
                    label = "Nonaktif",
                    isSelected = selectedStatus == CustomerStatusFilter.INACTIVE,
                    onClick = { selectedStatus = CustomerStatusFilter.INACTIVE },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Footer Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        selectedCategory = CustomerCategoryFilter.ALL
                        selectedStatus = CustomerStatusFilter.ALL
                        onApply(CustomerCategoryFilter.ALL, CustomerStatusFilter.ALL)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF256B57)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF256B57))
                ) {
                    Text("Reset", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        onApply(selectedCategory, selectedStatus)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF256B57),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text("Terapkan", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CustomerFilterOptionCard(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFFEAF3EF) else Color.White,
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF256B57) else Color(0xFFE2E8F0)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFF256B57) else Color(0xFF1E293B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isSelected) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color(0xFF256B57),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomerSummaryBottomSheet(
    uiState: CustomerListUiState,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val totalCount = uiState.totalCustomers.coerceAtLeast(uiState.customers.size.toLong())
    val generalCount = uiState.customers.count { !it.isContractor }
    val contractorCount = uiState.contractorCount

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .background(Color(0xFFCBD5E1), RoundedCornerShape(2.dp))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ringkasan Pelanggan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup ringkasan",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Hero Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "TOTAL PELANGGAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "$totalCount Pelanggan",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF256B57)
                    )
                }
            }

            // Metrics rows
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pelanggan Umum", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                Text("$generalCount Pelanggan", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
            }
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pelanggan Kontraktor", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                Text("$contractorCount Pelanggan", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
            }
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Limit Kredit", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                Text(uiState.totalCreditLimit.currencyText(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF256B57))
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF256B57),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text("Selesai", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
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
private fun CustomerSectionHeader(
    canManage: Boolean,
    onAddCustomerClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Daftar pelanggan",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
        )
        if (canManage) {
            Surface(
                onClick = onAddCustomerClick,
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFF256B57)),
                contentColor = Color(0xFF256B57),
                modifier = Modifier
                    .size(32.dp)
                    .testTag("customer-add"),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah pelanggan",
                        tint = Color(0xFF256B57),
                        modifier = Modifier.size(18.dp),
                    )
                }
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
private fun CustomerMobileCard(
    customer: Customer,
    onDetail: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDeactivate: (Customer) -> Unit,
    canManage: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer-card-${customer.id}")
            .clickable { onDetail(customer.id) }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Item Header: Avatar, Name & Status Inline, More Menu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CustomerAvatar(customer.name)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = customer.name,
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CustomerStatusInline(
                            isActive = customer.isActive,
                            modifier = Modifier.testTag("customer-status-${customer.id}"),
                        )
                        CustomerTypeBadge(
                            isContractor = customer.isContractor,
                            modifier = Modifier.testTag("customer-type-${customer.id}"),
                        )
                    }
                }
            }
            if (canManage) {
                CustomerActionsMenu(customer, onEdit, onDeactivate)
            }
        }

        // Contact & Address Clean List
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Telepon Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF256B57),
                )
                Text(
                    text = "Telepon:",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Normal,
                )
                if (customer.phone.isNullOrBlank()) {
                    Text(
                        text = "Belum diisi",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    )
                } else {
                    Text(
                        text = customer.phone,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Alamat Row
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).padding(top = 1.dp),
                    tint = Color(0xFF256B57),
                )
                Text(
                    text = "Alamat:",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Normal,
                )
                if (customer.address.isNullOrBlank()) {
                    Text(
                        text = "Belum diisi",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    )
                } else {
                    Text(
                        text = customer.address,
                        fontSize = 12.sp,
                        color = Color(0xFF334155),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Divider & Credit Info (Clean inline text without container)
        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Limit Kredit",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Normal,
                )
                Text(
                    text = customer.creditLimit.currencyText(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Termin Pembayaran",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Normal,
                )
                Text(
                    text = "${customer.paymentTermDays} hari",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                )
            }
        }
    }
}

@Composable
private fun CustomerAvatar(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0xFFEAF3EF)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().firstOrNull()?.uppercase() ?: "P",
            color = Color(0xFF256B57),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CustomerStatusInline(isActive: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isActive) Color(0xFF256B57) else Color(0xFF94A3B8)),
        )
        Text(
            text = if (isActive) "Aktif" else "Nonaktif",
            color = if (isActive) Color(0xFF256B57) else Color(0xFF64748B),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
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

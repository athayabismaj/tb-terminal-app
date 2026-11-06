package com.tbterminal.app.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.local.checkout.LocalCheckoutCommand
import com.tbterminal.app.data.local.checkout.LocalCheckoutItemCommand
import com.tbterminal.app.data.local.checkout.LocalCheckoutLookupService
import com.tbterminal.app.data.local.checkout.LocalCheckoutResult
import com.tbterminal.app.data.local.checkout.LocalCheckoutService
import com.tbterminal.app.data.local.database.CashSessionLocalDataSource
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.model.AppDataMode
import com.tbterminal.app.data.model.CheckoutSubmitCommand
import com.tbterminal.app.data.model.CheckoutSubmitItem
import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.data.model.CheckoutPreview
import com.tbterminal.app.data.model.CheckoutPreviewCommand
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.repository.CheckoutRepository
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.sync.BackendStatus
import com.tbterminal.app.data.sync.InternetStatus
import com.tbterminal.app.data.sync.OfflineSyncScheduler
import com.tbterminal.app.data.sync.OfflineStatus
import com.tbterminal.app.data.sync.OfflineStatusRepository
import com.tbterminal.app.ui.common.UiText
import com.tbterminal.app.ui.common.viewModelFactory
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CheckoutEvent {
    data class NavigateToReceipt(val transactionId: String) : CheckoutEvent
}

class CheckoutViewModel(
    private val checkoutRepository: CheckoutRepository,
    private val inventoryRepository: InventoryRepository,
    private val customerRepository: CustomerRepository,
    private val cashReconciliationRepository: CashReconciliationRepository,
    private val cashSessionLocalDataSource: CashSessionLocalDataSource? = null,
    private val localCashExpenseService: com.tbterminal.app.data.local.cashexpense.LocalCashExpenseService? = null,
    private val localCheckoutLookupService: LocalCheckoutLookupService? = null,
    private val localCheckoutService: LocalCheckoutService? = null,
    private val offlineStatusRepository: OfflineStatusRepository? = null,
    private val offlineSyncScheduler: OfflineSyncScheduler? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private val _checkoutEvents = MutableSharedFlow<CheckoutEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val checkoutEvents: SharedFlow<CheckoutEvent> = _checkoutEvents.asSharedFlow()
    val errorEvent: Flow<UiText?> = uiState
        .map { state -> state.errorEvent }
        .distinctUntilChanged()
    private var searchJob: Job? = null
    private var customerSearchJob: Job? = null
    private var latestOfflineStatus: OfflineStatus = OfflineStatus()
    private var pendingOnlineCheckout: PendingOnlineCheckout? = null
    private var ambiguousOnlineCheckout: CheckoutSubmitCommand? = null

    init {
        observeOfflineStatus()
        loadCashSession()
        loadProducts()
        loadCustomers()
    }

    private fun observeOfflineStatus() {
        val repository = offlineStatusRepository ?: return
        viewModelScope.launch {
            repository.status.collect { status ->
                latestOfflineStatus = status
            }
        }
    }

    fun loadCashSession() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isCashSessionLoading = true, cashSessionError = null)
            }

            when (val result = cashReconciliationRepository.getActiveSession()) {
                is RepositoryResult.Success -> {
                    val activeCashSession = result.data?.toMirroredActiveCashSessionUi()
                    _uiState.update { state ->
                        state.copy(
                            activeCashSession = activeCashSession,
                            hasActiveCashSession = activeCashSession != null,
                            isCashSessionLoading = false,
                            cashSessionError = null
                        )
                    }
                }
                is RepositoryResult.Error -> {
                    if (result.isAuthFailure() || !result.isServerFailure()) {
                        _uiState.update { state ->
                            state.copy(
                                activeCashSession = null,
                                hasActiveCashSession = false,
                                isCashSessionLoading = false,
                                cashSessionError = UiText.DynamicString(result.message)
                            )
                        }
                    } else if (!restoreLocalOpenCashSessionIfAvailable()) {
                        _uiState.update { state ->
                            state.copy(
                                activeCashSession = null,
                                hasActiveCashSession = false,
                                isCashSessionLoading = false,
                                cashSessionError = UiText.DynamicString(result.message)
                            )
                        }
                    }
                }
                is RepositoryResult.Exception -> {
                    if (!restoreLocalOpenCashSessionIfAvailable()) {
                        _uiState.update { state ->
                            state.copy(
                                activeCashSession = null,
                                hasActiveCashSession = false,
                                isCashSessionLoading = false,
                                cashSessionError = UiText.DynamicString("Sesi kasir gagal dicek.")
                            )
                        }
                    }
                }
            }
        }
    }

    fun onStartingCashChanged(value: String) {
        val digitsOnly = value.filter(Char::isDigit).take(MAX_CASH_INPUT_LENGTH)
        _uiState.update { state ->
            state.copy(startingCashInput = digitsOnly, cashSessionError = null)
        }
    }

    fun openCashSession() {
        val startingCash = _uiState.value.startingCashInput.toBigDecimalOrNull()
        if (startingCash == null || startingCash < BigDecimal.ZERO) {
            _uiState.update { state ->
                state.copy(cashSessionError = UiText.DynamicString("Modal awal kas belum valid."))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isOpeningCashSession = true, cashSessionError = null)
            }

            when (val result = cashReconciliationRepository.openSession(startingCash)) {
                is RepositoryResult.Success -> {
                    val activeCashSession = result.data.toMirroredActiveCashSessionUi()
                    _uiState.update { state ->
                        state.copy(
                            activeCashSession = activeCashSession,
                            hasActiveCashSession = true,
                            isOpeningCashSession = false,
                            startingCashInput = "",
                            cashSessionError = null,
                            errorEvent = UiText.DynamicString("Sesi kasir berhasil dibuka.")
                        )
                    }
                }
                is RepositoryResult.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            isOpeningCashSession = false,
                            cashSessionError = UiText.DynamicString(result.message)
                        )
                    }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update { state ->
                        state.copy(
                            isOpeningCashSession = false,
                            cashSessionError = UiText.DynamicString("Koneksi bermasalah. Sesi kasir gagal dibuka.")
                        )
                    }
                }
            }
        }
    }

    fun replaceProducts(products: List<Product>) {
        _uiState.update { state ->
            state.copy(products = products)
        }
    }

    fun loadProducts() {
        viewModelScope.launch {
            val page = _uiState.value.productPage.coerceAtLeast(1)
            val search = _uiState.value.searchQuery.trim().takeIf(String::isNotBlank)
            _uiState.update { state ->
                state.copy(isProductLoading = true, productError = null)
            }

            when (
                val result = inventoryRepository.getProductStocks(
                    page = page,
                    limit = PRODUCT_PAGE_LIMIT,
                    search = search
                )
            ) {
                is RepositoryResult.Success -> {
                    val productPage = result.data
                    _uiState.update { state ->
                        state.copy(
                            products = productPage.data
                                .filter { product -> product.isActive }
                                .map { product ->
                                    Product(
                                        productId = product.productId,
                                        name = product.productName,
                                        unitPrice = product.priceRetail,
                                        sku = product.sku,
                                        barcode = product.sku,
                                        unitName = product.unitName,
                                        categoryName = product.categoryName,
                                        stockQty = product.quantity,
                                        secondaryUnitId = product.secondaryUnitId,
                                        secondaryUnitName = product.secondaryUnitName,
                                        secondaryUnitFactor = product.secondaryUnitFactor
                                    )
                                },
                            productPage = productPage.page.coerceAtLeast(1),
                            productLimit = productPage.limit,
                            productTotal = productPage.total,
                            productTotalPages = productPage.totalPages.coerceAtLeast(1),
                            isProductLoading = false,
                            productError = null
                        )
                    }
                }
                is RepositoryResult.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            isProductLoading = false,
                            productError = UiText.DynamicString(result.message)
                        )
                    }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update { state ->
                        state.copy(
                            isProductLoading = false,
                            productError = UiText.DynamicString("Koneksi bermasalah. Produk gagal dimuat.")
                        )
                    }
                }
            }
        }
    }

    fun onSearchChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                productPage = 1
            )
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            loadProducts()
        }
    }

    fun onProductPageChanged(page: Int) {
        val totalPages = _uiState.value.productTotalPages.coerceAtLeast(1)
        val targetPage = page.coerceIn(1, totalPages)
        if (targetPage == _uiState.value.productPage || _uiState.value.isProductLoading) {
            return
        }

        _uiState.update { state ->
            state.copy(productPage = targetPage)
        }
        loadProducts()
    }

    fun loadCustomers() {
        viewModelScope.launch {
            val search = _uiState.value.customerSearchQuery.trim().takeIf(String::isNotBlank)
            _uiState.update { state ->
                state.copy(isCustomerLoading = true, customerError = null)
            }

            when (
                val result = customerRepository.getCustomers(
                    page = 1,
                    limit = CUSTOMER_PAGE_LIMIT,
                    search = search
                )
            ) {
                is RepositoryResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            customers = result.data.data.filter(Customer::isActive),
                            isCustomerLoading = false,
                            customerError = null
                        )
                    }
                }
                is RepositoryResult.Error -> {
                    _uiState.update { state ->
                        if (result.isCustomerLookupAccessDenied()) {
                            state.copy(
                                customers = emptyList(),
                                isCustomerLoading = false,
                                customerError = null
                            )
                        } else {
                            state.copy(
                                isCustomerLoading = false,
                                customerError = UiText.DynamicString(result.message)
                            )
                        }
                    }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update { state ->
                        state.copy(
                            isCustomerLoading = false,
                            customerError = UiText.DynamicString("Koneksi bermasalah. Pelanggan gagal dimuat.")
                        )
                    }
                }
            }
        }
    }

    fun onCustomerSearchChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                customerSearchQuery = query,
                selectedCustomer = null,
                customerError = null
            )
        }
        customerSearchJob?.cancel()
        customerSearchJob = viewModelScope.launch {
            delay(300)
            loadCustomers()
        }
    }

    fun selectCustomer(customer: Customer) {
        _uiState.update { state ->
            state.copy(
                selectedCustomer = customer,
                customerSearchQuery = customer.name,
                customerError = null
            )
        }
    }

    fun clearSelectedCustomer() {
        _uiState.update { state ->
            state.copy(
                selectedCustomer = null,
                customerSearchQuery = ""
            )
        }
        loadCustomers()
    }

    fun useQuickCustomerName() {
        val name = _uiState.value.customerSearchQuery.trim()
        if (name.isBlank()) {
            setError(UiText.DynamicString("Nama pelanggan belum diisi."))
            return
        }

        _uiState.update { state ->
            state.copy(
                customerSearchQuery = name,
                selectedCustomer = null,
                customerError = null
            )
        }
    }

    fun openQuickAddCustomer() {
        _uiState.update { it.copy(isQuickAddCustomerOpen = true) }
    }

    fun dismissQuickAddCustomer() {
        _uiState.update { it.copy(isQuickAddCustomerOpen = false, isQuickCustomerSubmitting = false) }
    }

    fun quickCreateCustomer(
        name: String,
        phone: String,
        address: String,
        isContractor: Boolean
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            _uiState.update { it.copy(customerError = UiText.DynamicString("Nama pelanggan wajib diisi.")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isQuickCustomerSubmitting = true) }
            val command = com.tbterminal.app.data.model.CustomerCommand(
                name = cleanName,
                phone = phone.trim().takeIf(String::isNotBlank),
                address = address.trim().takeIf(String::isNotBlank),
                isContractor = isContractor,
                creditLimit = BigDecimal.ZERO,
                paymentTermDays = 0
            )

            when (val result = customerRepository.createCustomer(command)) {
                is RepositoryResult.Success -> {
                    val created = result.data
                    val updatedCustomers = listOf(created) + _uiState.value.customers.filter { it.id != created.id }
                    _uiState.update {
                        it.copy(
                            customers = updatedCustomers,
                            selectedCustomer = created,
                            customerSearchQuery = created.name,
                            isQuickAddCustomerOpen = false,
                            isQuickCustomerSubmitting = false,
                            customerError = null,
                            quickFeedbackMessage = "Pelanggan \"${created.name}\" berhasil didaftarkan!"
                        )
                    }
                }
                is RepositoryResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isQuickCustomerSubmitting = false,
                            customerError = UiText.DynamicString(result.message)
                        )
                    }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isQuickCustomerSubmitting = false,
                            customerError = UiText.DynamicString(result.throwable.message ?: "Gagal mendaftarkan pelanggan.")
                        )
                    }
                }
            }
        }
    }

    fun replaceCart(items: List<CartItem>) {
        if (!allowCheckoutIntentMutation()) return
        _uiState.update { state ->
            state.withCart(items)
        }
    }

    fun addProduct(product: Product, isSecondary: Boolean = false) {
        if (product.stockQty <= BigDecimal.ZERO) {
            com.tbterminal.app.ui.barcode.BarcodeFeedbackHelper.playErrorBeep()
            setError(UiText.DynamicString("Stok ${product.name} kosong."))
            return
        }

        val factor = if (isSecondary && product.secondaryUnitFactor != null) product.secondaryUnitFactor else BigDecimal.ONE
        val targetCartItemId = if (isSecondary) "${product.productId}__sec" else product.productId

        val currentConsumedBaseQty = _uiState.value.cartItems
            .filter { it.productId == product.productId }
            .fold(BigDecimal.ZERO) { acc, item -> acc.add(item.equivalentBaseQuantityBigDecimal) }

        val neededBaseQty = currentConsumedBaseQty.add(factor)
        if (neededBaseQty > product.stockQty) {
            val sisa = product.stockQty.subtract(currentConsumedBaseQty).coerceAtLeast(BigDecimal.ZERO)
            com.tbterminal.app.ui.barcode.BarcodeFeedbackHelper.playErrorBeep()
            setError(
                UiText.DynamicString(
                    "Stok ${product.name} tidak cukup (tersedia ${sisa.stripTrailingZeros().toPlainString()} ${product.unitName})."
                )
            )
            return
        }

        if (!allowCheckoutIntentMutation()) return
        _uiState.update { state ->
            val existingItem = state.cartItems.firstOrNull { item ->
                item.cartItemId == targetCartItemId
            }
            val updatedItems = if (existingItem == null) {
                val effectiveUnitPrice = if (isSecondary && product.secondaryUnitFactor != null) {
                    product.unitPrice.multiply(product.secondaryUnitFactor)
                } else {
                    product.unitPrice
                }
                val effectiveUnitName = if (isSecondary) {
                    product.secondaryUnitName?.ifBlank { "Satuan 2" } ?: "Satuan 2"
                } else {
                    product.unitName
                }
                state.cartItems + CartItem(
                    productId = product.productId,
                    productName = product.name,
                    unitPrice = effectiveUnitPrice,
                    quantity = 1,
                    sku = product.sku,
                    unitName = effectiveUnitName,
                    cartItemId = targetCartItemId,
                    baseUnitPrice = product.unitPrice,
                    baseUnitName = product.unitName,
                    secondaryUnitId = product.secondaryUnitId,
                    secondaryUnitName = product.secondaryUnitName,
                    secondaryUnitFactor = product.secondaryUnitFactor,
                    isSecondaryUnit = isSecondary
                )
            } else {
                state.cartItems.map { item ->
                    if (item.cartItemId == existingItem.cartItemId) {
                        item.copy(quantity = item.quantity + 1)
                    } else {
                        item
                    }
                }
            }

            state.withCart(updatedItems)
        }
    }

    fun onScanBarcode(barcode: String, context: android.content.Context? = null) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank()) return

        // 1. Cek produk di katalog memori aktif
        val matchedProduct = _uiState.value.products.firstOrNull { product ->
            product.sku.equals(cleanBarcode, ignoreCase = true) ||
            product.barcode.equals(cleanBarcode, ignoreCase = true) ||
            product.productId.equals(cleanBarcode, ignoreCase = true)
        }

        if (matchedProduct != null) {
            if (matchedProduct.stockQty <= BigDecimal.ZERO) {
                com.tbterminal.app.ui.barcode.BarcodeFeedbackHelper.playErrorBeep()
                setError(UiText.DynamicString("Stok ${matchedProduct.name} kosong."))
                return
            }
            addProduct(matchedProduct)
            com.tbterminal.app.ui.barcode.BarcodeFeedbackHelper.playSuccessBeep()
            context?.let { ctx ->
                com.tbterminal.app.ui.barcode.BarcodeFeedbackHelper.vibrate(ctx)
            }
            _uiState.update { it.copy(scanNotification = "Ditambahkan: ${matchedProduct.name} (+1)") }
            return
        }

        // 2. Jika tidak ada di memori aktif, cari ke repository
        viewModelScope.launch {
            when (val result = inventoryRepository.getProductStocks(page = 1, limit = 5, search = cleanBarcode)) {
                is RepositoryResult.Success -> {
                    val found = result.data.data.firstOrNull { p ->
                        p.isActive && (p.sku.equals(cleanBarcode, ignoreCase = true) || p.productId.equals(cleanBarcode, ignoreCase = true))
                    } ?: result.data.data.firstOrNull { it.isActive }

                    if (found != null) {
                        if (found.quantity <= BigDecimal.ZERO) {
                            com.tbterminal.app.ui.barcode.BarcodeFeedbackHelper.playErrorBeep()
                            setError(UiText.DynamicString("Stok ${found.productName} kosong."))
                            return@launch
                        }
                        val uiProduct = Product(
                            productId = found.productId,
                            name = found.productName,
                            unitPrice = found.priceRetail,
                            sku = found.sku,
                            barcode = found.sku,
                            unitName = found.unitName,
                            categoryName = found.categoryName,
                            stockQty = found.quantity,
                            secondaryUnitId = found.secondaryUnitId,
                            secondaryUnitName = found.secondaryUnitName,
                            secondaryUnitFactor = found.secondaryUnitFactor
                        )
                        addProduct(uiProduct)
                        com.tbterminal.app.ui.barcode.BarcodeFeedbackHelper.playSuccessBeep()
                        context?.let { ctx ->
                            com.tbterminal.app.ui.barcode.BarcodeFeedbackHelper.vibrate(ctx)
                        }
                        _uiState.update { it.copy(scanNotification = "Ditambahkan: ${found.productName} (+1)") }
                    } else {
                        com.tbterminal.app.ui.barcode.BarcodeFeedbackHelper.playErrorBeep()
                        setError(UiText.DynamicString("Barcode \"$cleanBarcode\" tidak ditemukan."))
                    }
                }
                else -> {
                    com.tbterminal.app.ui.barcode.BarcodeFeedbackHelper.playErrorBeep()
                    setError(UiText.DynamicString("Barcode \"$cleanBarcode\" tidak ditemukan."))
                }
            }
        }
    }

    fun clearScanNotification() {
        _uiState.update { it.copy(scanNotification = null) }
    }

    fun updateCartQuantity(
        cartItemId: String,
        quantity: Int
    ) {
        val currentItem = _uiState.value.cartItems.firstOrNull { it.cartItemId == cartItemId }
            ?: return
        val product = _uiState.value.products.firstOrNull { it.productId == currentItem.productId }

        if (quantity > currentItem.quantity && product != null) {
            val otherItemsConsumedBaseQty = _uiState.value.cartItems
                .filter { it.productId == currentItem.productId && it.cartItemId != cartItemId }
                .fold(BigDecimal.ZERO) { acc, item -> acc.add(item.equivalentBaseQuantityBigDecimal) }
            val thisItemFactor = if (currentItem.isSecondaryUnit && currentItem.secondaryUnitFactor != null) {
                currentItem.secondaryUnitFactor
            } else {
                BigDecimal.ONE
            }
            val newThisItemBaseQty = quantity.toBigDecimal().multiply(thisItemFactor)
            val totalNeededBaseQty = otherItemsConsumedBaseQty.add(newThisItemBaseQty)

            if (totalNeededBaseQty > product.stockQty) {
                val sisa = product.stockQty.subtract(otherItemsConsumedBaseQty).coerceAtLeast(BigDecimal.ZERO)
                setError(
                    UiText.DynamicString(
                        "Stok ${product.name} tidak cukup (tersedia ${sisa.stripTrailingZeros().toPlainString()} ${product.unitName})."
                    )
                )
                return
            }
        }

        if (!allowCheckoutIntentMutation()) return
        _uiState.update { state ->
            val updatedItems = if (quantity <= 0) {
                state.cartItems.filterNot { item ->
                    item.cartItemId == cartItemId
                }
            } else {
                state.cartItems.map { item ->
                    if (item.cartItemId == cartItemId) {
                        item.copy(quantity = quantity)
                    } else {
                        item
                    }
                }
            }

            state.withCart(updatedItems)
        }
    }

    fun toggleCartItemUnit(cartItemId: String, toSecondary: Boolean) {
        val currentItem = _uiState.value.cartItems.firstOrNull { it.cartItemId == cartItemId }
            ?: return
        if (currentItem.isSecondaryUnit == toSecondary) return
        val factor = currentItem.secondaryUnitFactor
        if (toSecondary && (factor == null || factor <= BigDecimal.ONE)) return

        val product = _uiState.value.products.firstOrNull { it.productId == currentItem.productId }
        val newFactor = if (toSecondary) (factor ?: BigDecimal.ONE) else BigDecimal.ONE
        val otherItemsConsumedBaseQty = _uiState.value.cartItems
            .filter { it.productId == currentItem.productId && it.cartItemId != cartItemId }
            .fold(BigDecimal.ZERO) { acc, item -> acc.add(item.equivalentBaseQuantityBigDecimal) }
        val newThisItemBaseQty = currentItem.quantity.toBigDecimal().multiply(newFactor)
        val totalNeeded = otherItemsConsumedBaseQty.add(newThisItemBaseQty)

        if (product != null && totalNeeded > product.stockQty) {
            val sisa = product.stockQty.subtract(otherItemsConsumedBaseQty).coerceAtLeast(BigDecimal.ZERO)
            setError(
                UiText.DynamicString(
                    "Stok ${currentItem.productName} tidak cukup untuk beralih satuan (tersedia ${sisa.stripTrailingZeros().toPlainString()} ${currentItem.baseUnitName})."
                )
            )
            return
        }

        if (!allowCheckoutIntentMutation()) return

        val targetCartItemId = if (toSecondary) "${currentItem.productId}__sec" else currentItem.productId
        val newUnitPrice = if (toSecondary) {
            currentItem.baseUnitPrice.multiply(factor ?: BigDecimal.ONE)
        } else {
            currentItem.baseUnitPrice
        }
        val newUnitName = if (toSecondary) {
            currentItem.secondaryUnitName?.ifBlank { "Satuan 2" } ?: "Satuan 2"
        } else {
            currentItem.baseUnitName
        }

        _uiState.update { state ->
            val existingTarget = state.cartItems.firstOrNull { it.cartItemId == targetCartItemId }
            val updatedItems = if (existingTarget != null) {
                state.cartItems
                    .filterNot { it.cartItemId == cartItemId }
                    .map { item ->
                        if (item.cartItemId == targetCartItemId) {
                            item.copy(quantity = item.quantity + currentItem.quantity)
                        } else {
                            item
                        }
                    }
            } else {
                state.cartItems.map { item ->
                    if (item.cartItemId == cartItemId) {
                        item.copy(
                            cartItemId = targetCartItemId,
                            unitPrice = newUnitPrice,
                            unitName = newUnitName,
                            isSecondaryUnit = toSecondary,
                            discountRequest = null
                        )
                    } else {
                        item
                    }
                }
            }

            state.withCart(updatedItems)
        }
    }

    fun setItemDiscount(cartItemId: String, discount: CheckoutDiscount?) {
        val item = _uiState.value.cartItems.firstOrNull { it.cartItemId == cartItemId }
            ?: return
        val lineGross = item.unitPrice.multiply(item.quantity.toBigDecimal())
        validateCheckoutDiscount(lineGross, discount)?.let { message ->
            setError(UiText.DynamicString(message))
            return
        }
        if (!allowCheckoutIntentMutation()) return
        _uiState.update { state ->
            state.withCart(
                state.cartItems.map { cartItem ->
                    if (cartItem.cartItemId == cartItemId) {
                        cartItem.copy(discountRequest = discount)
                    } else {
                        cartItem
                    }
                },
            )
        }
    }

    fun setTransactionDiscount(discount: CheckoutDiscount?) {
        val itemTotals = estimateCheckoutTotals(_uiState.value.cartItems, null)
        val afterItem = itemTotals.grossSubtotal.subtract(itemTotals.itemDiscountTotal)
        validateCheckoutDiscount(afterItem, discount)?.let { message ->
            setError(UiText.DynamicString(message))
            return
        }
        if (!allowCheckoutIntentMutation()) return
        _uiState.update { state -> state.withTransactionDiscount(discount) }
    }

    fun clearErrorEvent() {
        _uiState.update { state ->
            state.copy(errorEvent = null)
        }
    }

    fun openHoldCartDialog() {
        if (_uiState.value.cartItems.isEmpty()) {
            setError(UiText.DynamicString("Keranjang kosong, tidak ada pesanan untuk diparkir."))
            return
        }
        _uiState.update { it.copy(isHoldCartDialogOpen = true) }
    }

    fun dismissHoldCartDialog() {
        _uiState.update { it.copy(isHoldCartDialogOpen = false) }
    }

    fun holdCurrentCart(note: String) {
        val state = _uiState.value
        if (state.cartItems.isEmpty()) return
        val resolvedNote = note.trim().ifBlank {
            state.selectedCustomer?.name?.let { "Pesanan $it" } ?: "Pesanan #${state.heldCarts.size + 1}"
        }
        val heldCart = HeldCart(
            note = resolvedNote,
            customer = state.selectedCustomer,
            items = state.cartItems,
            transactionDiscount = state.transactionDiscount,
            deliveryFee = state.deliveryFee,
            deliveryAddress = state.deliveryAddress,
            deliveryCourier = state.deliveryCourier,
            isDeliveryEnabled = state.isDeliveryEnabled,
            subtotal = state.subtotal,
            finalTotal = state.finalTotal
        )
        _uiState.update { current ->
            current.copy(
                heldCarts = current.heldCarts + heldCart,
                isHoldCartDialogOpen = false,
                quickFeedbackMessage = "Pesanan \"$resolvedNote\" berhasil diparkir."
            ).recalculateCheckoutState(
                items = emptyList(),
                discount = null,
                deliveryFeeAmount = BigDecimal.ZERO
            ).copy(
                selectedCustomer = null,
                customerSearchQuery = "",
                isDeliveryEnabled = false,
                deliveryFee = BigDecimal.ZERO,
                deliveryFeeInput = "",
                deliveryAddress = "",
                deliveryCourier = "",
            )
        }
    }

    fun openHeldCartsSheet() {
        _uiState.update { it.copy(isHeldCartsSheetOpen = true) }
    }

    fun dismissHeldCartsSheet() {
        _uiState.update { it.copy(isHeldCartsSheetOpen = false) }
    }

    fun openPettyCashDialog(type: com.tbterminal.app.data.local.cashexpense.CashMovementType = com.tbterminal.app.data.local.cashexpense.CashMovementType.CASH_OUT) {
        _uiState.update {
            it.copy(
                isPettyCashDialogOpen = true,
                pettyCashType = type,
                pettyCashAmountInput = "",
                pettyCashCategoryInput = if (type == com.tbterminal.app.data.local.cashexpense.CashMovementType.CASH_IN) "Tambah Modal Kembalian" else "Bensin Armada",
                pettyCashDescriptionInput = "",
                pettyCashError = null
            )
        }
    }

    fun dismissPettyCashDialog() {
        _uiState.update { it.copy(isPettyCashDialogOpen = false, pettyCashError = null) }
    }

    fun onPettyCashTypeChanged(type: com.tbterminal.app.data.local.cashexpense.CashMovementType) {
        _uiState.update {
            it.copy(
                pettyCashType = type,
                pettyCashCategoryInput = if (type == com.tbterminal.app.data.local.cashexpense.CashMovementType.CASH_IN) "Tambah Modal Kembalian" else "Bensin Armada"
            )
        }
    }

    fun onPettyCashAmountChanged(amount: String) {
        _uiState.update { it.copy(pettyCashAmountInput = amount.filter(Char::isDigit), pettyCashError = null) }
    }

    fun onPettyCashCategoryChanged(category: String) {
        _uiState.update { it.copy(pettyCashCategoryInput = category, pettyCashError = null) }
    }

    fun onPettyCashDescriptionChanged(description: String) {
        _uiState.update { it.copy(pettyCashDescriptionInput = description, pettyCashError = null) }
    }

    fun submitPettyCash() {
        val state = _uiState.value
        val amount = state.pettyCashAmountInput.toBigDecimalOrNull()
            ?: return _uiState.update { it.copy(pettyCashError = "Nominal wajib diisi angka valid.") }
        if (amount <= BigDecimal.ZERO) return _uiState.update { it.copy(pettyCashError = "Nominal harus lebih dari nol.") }
        val type = state.pettyCashType
        val cat = state.pettyCashCategoryInput.trim()
        val desc = state.pettyCashDescriptionInput.trim().ifBlank { cat }
        val actionLabel = if (type == com.tbterminal.app.data.local.cashexpense.CashMovementType.CASH_IN) "Kas masuk" else "Pengeluaran"

        viewModelScope.launch {
            _uiState.update { it.copy(isPettyCashSubmitting = true, pettyCashError = null) }
            val localSession = state.activeCashSession?.localId
            if (localSession != null && localCashExpenseService != null) {
                when (val res = localCashExpenseService.addLocalCashMovement(localSession, type, amount, desc, cat)) {
                    is com.tbterminal.app.data.local.cashexpense.LocalCashExpenseResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isPettyCashDialogOpen = false,
                                isPettyCashSubmitting = false,
                                quickFeedbackMessage = "$actionLabel Rp $amount berhasil dicatat."
                            )
                        }
                    }
                    is com.tbterminal.app.data.local.cashexpense.LocalCashExpenseResult.Failed -> {
                        _uiState.update { it.copy(isPettyCashSubmitting = false, pettyCashError = res.message) }
                    }
                }
            } else {
                when (val res = if (type == com.tbterminal.app.data.local.cashexpense.CashMovementType.CASH_IN) {
                    cashReconciliationRepository.addExpense(amount, "KAS MASUK: $desc ($cat)")
                } else {
                    cashReconciliationRepository.addExpense(amount, "$desc ($cat)")
                }) {
                    is RepositoryResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isPettyCashDialogOpen = false,
                                isPettyCashSubmitting = false,
                                quickFeedbackMessage = "$actionLabel Rp $amount berhasil dicatat."
                            )
                        }
                    }
                    is RepositoryResult.Error -> {
                        _uiState.update { it.copy(isPettyCashSubmitting = false, pettyCashError = res.message) }
                    }
                    is RepositoryResult.Exception -> {
                        _uiState.update { it.copy(isPettyCashSubmitting = false, pettyCashError = res.throwable.message ?: "Gagal mencatat mutasi kas.") }
                    }
                }
            }
        }
    }

    fun resumeHeldCart(heldCartId: String) {
        val state = _uiState.value
        val target = state.heldCarts.firstOrNull { it.id == heldCartId } ?: return

        // Jika keranjang aktif saat ini memiliki item, simpan otomatis dulu agar tidak hilang
        val updatedHeldCarts = state.heldCarts.filterNot { it.id == heldCartId }.toMutableList()
        if (state.cartItems.isNotEmpty()) {
            val autoSaveNote = state.selectedCustomer?.name?.let { "Tukar ($it)" } ?: "Tukar Pesanan #${updatedHeldCarts.size + 1}"
            updatedHeldCarts.add(
                HeldCart(
                    note = autoSaveNote,
                    customer = state.selectedCustomer,
                    items = state.cartItems,
                    transactionDiscount = state.transactionDiscount,
                    deliveryFee = state.deliveryFee,
                    deliveryAddress = state.deliveryAddress,
                    deliveryCourier = state.deliveryCourier,
                    isDeliveryEnabled = state.isDeliveryEnabled,
                    subtotal = state.subtotal,
                    finalTotal = state.finalTotal
                )
            )
        }

        _uiState.update { current ->
            current.copy(
                heldCarts = updatedHeldCarts,
                isHeldCartsSheetOpen = false,
                selectedCustomer = target.customer,
                customerSearchQuery = target.customer?.name.orEmpty(),
                isDeliveryEnabled = target.isDeliveryEnabled,
                deliveryFee = target.deliveryFee,
                deliveryFeeInput = if (target.deliveryFee > BigDecimal.ZERO) target.deliveryFee.toPlainString() else "",
                deliveryAddress = target.deliveryAddress,
                deliveryCourier = target.deliveryCourier,
                quickFeedbackMessage = "Pesanan \"${target.note}\" dimuat ke keranjang."
            ).recalculateCheckoutState(
                items = target.items,
                discount = target.transactionDiscount,
                deliveryFeeAmount = if (target.isDeliveryEnabled) target.deliveryFee else BigDecimal.ZERO
            )
        }
    }

    fun deleteHeldCart(heldCartId: String) {
        _uiState.update { current ->
            current.copy(
                heldCarts = current.heldCarts.filterNot { it.id == heldCartId },
                quickFeedbackMessage = "Pesanan terparkir dihapus."
            )
        }
    }

    fun toggleDelivery(enabled: Boolean) {
        _uiState.update { state ->
            val fee = if (enabled) state.deliveryFee else BigDecimal.ZERO
            state.copy(isDeliveryEnabled = enabled).recalculateCheckoutState(
                items = state.cartItems,
                discount = state.transactionDiscount,
                deliveryFeeAmount = fee
            )
        }
    }

    fun updateDeliveryFee(feeInput: String) {
        val sanitized = feeInput.filter(Char::isDigit).trimStart('0').ifBlank { "0" }
        val feeAmount = BigDecimal(sanitized)
        _uiState.update { state ->
            state.copy(
                deliveryFeeInput = feeInput,
                deliveryFee = feeAmount,
            ).recalculateCheckoutState(
                items = state.cartItems,
                discount = state.transactionDiscount,
                deliveryFeeAmount = if (state.isDeliveryEnabled) feeAmount else BigDecimal.ZERO
            )
        }
    }

    fun updateDeliveryAddress(address: String) {
        _uiState.update { it.copy(deliveryAddress = address) }
    }

    fun updateDeliveryCourier(courier: String) {
        _uiState.update { it.copy(deliveryCourier = courier) }
    }

    fun lockScreen() {
        _uiState.update { it.copy(isScreenLocked = true, lockPinInput = "", lockPinError = null) }
    }

    fun updateLockPinInput(pin: String) {
        val cleanPin = pin.filter(Char::isDigit).take(6)
        _uiState.update { it.copy(lockPinInput = cleanPin, lockPinError = null) }
    }

    fun unlockScreen(validPin: String? = null) {
        val input = _uiState.value.lockPinInput
        if (input.length < 4) {
            _uiState.update { it.copy(lockPinError = "PIN minimal 4 angka.") }
            return
        }
        if (validPin != null && input != validPin) {
            _uiState.update { it.copy(lockPinError = "PIN salah. Coba lagi.") }
            return
        }
        _uiState.update { it.copy(isScreenLocked = false, lockPinInput = "", lockPinError = null) }
    }

    fun clearQuickFeedback() {
        _uiState.update { it.copy(quickFeedbackMessage = null) }
    }

    fun openQrisDialog() {
        if (_uiState.value.cartItems.isEmpty()) {
            setError(UiText.DynamicString("Keranjang belanja kosong."))
            return
        }
        _uiState.update { it.copy(isQrisDialogOpen = true, selectedPaymentMethod = PaymentMethod.QRIS) }
    }

    fun dismissQrisDialog() {
        _uiState.update { it.copy(isQrisDialogOpen = false) }
    }

    fun confirmQrisPayment() {
        val total = _uiState.value.finalTotal
        _uiState.update { it.copy(isQrisDialogOpen = false, selectedPaymentMethod = PaymentMethod.QRIS) }
        prepareCheckout(PaymentMethod.QRIS, total)
    }

    fun openCameraScanner() {
        _uiState.update { it.copy(isCameraScannerOpen = true) }
    }

    fun dismissCameraScanner() {
        _uiState.update { it.copy(isCameraScannerOpen = false) }
    }

    fun selectPaymentMethod(paymentMethod: PaymentMethod) {
        _uiState.update { state ->
            state.copy(
                selectedPaymentMethod = paymentMethod,
                amountPaidInput = when (paymentMethod) {
                    PaymentMethod.HUTANG -> "0"
                    PaymentMethod.DP -> {
                        if (state.selectedPaymentMethod == PaymentMethod.DP) state.amountPaidInput else ""
                    }
                    else -> state.finalTotal.toPlainString()
                }
            )
        }
    }

    fun onAmountPaidChanged(value: String) {
        _uiState.update { state ->
            state.copy(amountPaidInput = value.moneyInput(), errorEvent = null)
        }
    }

    fun submitCheckout(paymentMethod: PaymentMethod, amountPaid: BigDecimal) {
        prepareCheckout(paymentMethod, amountPaid)
    }

    fun prepareCheckout(paymentMethod: PaymentMethod, amountPaid: BigDecimal) {
        val initialState = _uiState.value
        if (initialState.cartItems.isEmpty()) {
            setError(UiText.DynamicString("Keranjang masih kosong."))
            return
        }
        if (!initialState.canStartCheckout()) return
        if (!initialState.hasActiveCashSession) {
            setError(UiText.DynamicString("Buka sesi kasir untuk akun ini terlebih dahulu sebelum bertransaksi."))
            return
        }

        // Set before launching so two taps in the same frame cannot start two requests.
        _uiState.update { it.copy(isLoading = true, isPreviewLoading = true, errorEvent = null) }
        viewModelScope.launch {
            ambiguousOnlineCheckout?.let { command ->
                _uiState.update { it.copy(isPreviewLoading = false) }
                executeOnlineCheckout(command)
                return@launch
            }
            if (shouldUseLocalCheckout()) {
                if (_uiState.value.hasDiscountRequest()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPreviewLoading = false,
                            errorEvent = UiText.DynamicString(
                                "Diskon memerlukan koneksi ke server dan tidak tersedia saat offline.",
                            ),
                        )
                    }
                } else {
                    _uiState.update { it.copy(isPreviewLoading = false) }
                    submitLocalCheckout()
                }
                return@launch
            }

            val cartSnapshot = _uiState.value.cartItems
            val previewCommand = CheckoutPreviewCommand(
                items = cartSnapshot.map { item ->
                    val effectiveQty = if (item.isSecondaryUnit && item.secondaryUnitFactor != null) {
                        (item.quantity.toBigDecimal() * item.secondaryUnitFactor).toInt()
                    } else {
                        item.quantity
                    }
                    CheckoutSubmitItem(
                        productId = item.productId,
                        quantity = effectiveQty,
                        discountRequest = item.discountRequest,
                    )
                },
                transactionDiscount = _uiState.value.transactionDiscount,
            )
            when (val result = checkoutRepository.previewCheckout(previewCommand)) {
                is RepositoryResult.Success -> {
                    applyAuthoritativePreview(result.data)
                    if (result.data.nextStep() == CheckoutPreviewDecision.MANAGER_APPROVAL) {
                        _uiState.update { it.copy(isLoading = false, isPreviewLoading = false) }
                    } else {
                        submitOnlineCheckout(
                            paymentMethod = paymentMethod,
                            fallbackAmount = amountPaid,
                            preview = result.data,
                            managerApprovalId = null,
                        )
                    }
                }
                is RepositoryResult.Error -> {
                    invalidatePendingCheckout()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPreviewLoading = false,
                            errorEvent = result.toUiText(),
                        )
                    }
                }
                is RepositoryResult.Exception -> {
                    invalidatePendingCheckout()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPreviewLoading = false,
                            errorEvent = UiText.DynamicString(
                                "Preview checkout gagal karena koneksi bermasalah. Coba lagi.",
                            ),
                        )
                    }
                }
            }
        }
    }

    fun continueCheckoutWithApproval(
        approvalId: String,
        checkoutAttemptId: String,
    ) {
        val state = _uiState.value
        val preview = state.checkoutPreview
        if (state.isLoading || state.isPreviewLoading) return
        if (preview == null || !preview.approvalRequired ||
            preview.checkoutAttemptId != checkoutAttemptId || approvalId.isBlank()
        ) {
            invalidatePendingCheckout()
            _uiState.update {
                it.copy(
                    checkoutPreview = null,
                    errorEvent = UiText.DynamicString(
                        "Persetujuan tidak cocok dengan preview diskon. Buat preview ulang.",
                    ),
                )
            }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorEvent = null) }
        _uiState.update { it.copy(approvedManagerApprovalId = approvalId) }
        viewModelScope.launch {
            submitOnlineCheckout(
                paymentMethod = _uiState.value.selectedPaymentMethod,
                fallbackAmount = preview.netTotal,
                preview = preview,
                managerApprovalId = approvalId,
            )
        }
    }

    private suspend fun submitOnlineCheckout(
        paymentMethod: PaymentMethod,
        fallbackAmount: BigDecimal,
        preview: CheckoutPreview,
        managerApprovalId: String?,
    ) {
        val state = _uiState.value
        if (state.checkoutPreview?.checkoutAttemptId != preview.checkoutAttemptId) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isPreviewLoading = false,
                    errorEvent = UiText.DynamicString("Keranjang berubah. Silakan lakukan checkout ulang."),
                )
            }
            return
        }
        val customerId = resolveCustomerIdBeforeCheckout()
        val typedCustomerName = _uiState.value.customerSearchQuery.trim()
        if (customerId == null && paymentMethod.requiresRegisteredCustomer()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorEvent = UiText.DynamicString(
                        "Transaksi hutang/DP wajib memakai pelanggan terdaftar.",
                    ),
                )
            }
            return
        }
        val effectiveAmountPaid = resolveAmountPaid(paymentMethod, fallbackAmount) ?: run {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        val cartSnapshot = _uiState.value.cartItems
        val notes = checkoutCustomerNote(typedCustomerName)
        val approvalAttemptId = preview.checkoutAttemptId.takeIf { preview.approvalRequired }
        val command = CheckoutSubmitCommand(
            idempotencyKey = resolveIdempotencyKey(
                items = cartSnapshot,
                paymentMethod = paymentMethod,
                amountPaid = effectiveAmountPaid,
                customerId = customerId,
                notes = notes,
                transactionDiscount = state.transactionDiscount,
                checkoutAttemptId = approvalAttemptId,
                managerApprovalId = managerApprovalId,
            ),
            items = cartSnapshot.map { item ->
                val effectiveQty = if (item.isSecondaryUnit && item.secondaryUnitFactor != null) {
                    (item.quantity.toBigDecimal() * item.secondaryUnitFactor).toInt()
                } else {
                    item.quantity
                }
                CheckoutSubmitItem(
                    productId = item.productId,
                    quantity = effectiveQty,
                    discountRequest = item.discountRequest,
                )
            },
            paymentMethod = paymentMethod.apiValue(),
            amountPaid = effectiveAmountPaid.toPlainString(),
            customerId = customerId,
            notes = notes,
            transactionDiscount = state.transactionDiscount,
            checkoutAttemptId = approvalAttemptId,
            managerApprovalId = managerApprovalId,
        )

        ambiguousOnlineCheckout = command
        executeOnlineCheckout(command)
    }

    private suspend fun executeOnlineCheckout(command: CheckoutSubmitCommand) {
        when (val result = checkoutRepository.submitCheckout(command)) {
            is RepositoryResult.Success -> {
                pendingOnlineCheckout = null
                ambiguousOnlineCheckout = null
                _checkoutEvents.tryEmit(CheckoutEvent.NavigateToReceipt(result.data.transactionId))
                _uiState.update { current -> current.resetAfterSuccessfulCheckout() }
            }
            is RepositoryResult.Error -> {
                val ambiguous = result.isAmbiguousCheckoutFailure()
                if (!ambiguous) {
                    pendingOnlineCheckout = null
                    ambiguousOnlineCheckout = null
                }
                val refreshRequired = result.requiresFreshDiscountPreview()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isPreviewLoading = false,
                        checkoutPreview = if (refreshRequired) null else it.checkoutPreview,
                        approvedManagerApprovalId = if (refreshRequired) null else it.approvedManagerApprovalId,
                        hasAmbiguousCheckout = ambiguous,
                        errorEvent = result.toUiText(),
                    )
                }
            }
            is RepositoryResult.Exception -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isPreviewLoading = false,
                        hasAmbiguousCheckout = true,
                        errorEvent = UiText.DynamicString(
                            "Hasil checkout belum dapat dipastikan. Tekan checkout lagi untuk memeriksa transaksi yang sama.",
                        ),
                    )
                }
            }
        }
        _uiState.update { it.copy(isLoading = false, isPreviewLoading = false) }
    }

    private suspend fun submitLocalCheckout() {
        when (val buildResult = buildLocalCheckoutCommand()) {
            is LocalCheckoutCommandBuildResult.Failed -> {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorEvent = UiText.DynamicString(buildResult.reason)
                    )
                }
            }
            is LocalCheckoutCommandBuildResult.Success -> {
                val service = localCheckoutService
                if (service == null) {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorEvent = UiText.DynamicString("Penyimpanan checkout lokal belum tersedia.")
                        )
                    }
                    return
                }

                when (val result = service.saveCheckout(buildResult.command)) {
                    is LocalCheckoutResult.Success -> {
                        offlineSyncScheduler?.scheduleIfEnabled()
                        _uiState.update { state ->
                            CheckoutUiState(
                                products = state.products,
                                productPage = state.productPage,
                                productLimit = state.productLimit,
                                productTotal = state.productTotal,
                                productTotalPages = state.productTotalPages,
                                customers = state.customers,
                                activeCashSession = state.activeCashSession,
                                hasActiveCashSession = state.hasActiveCashSession,
                                errorEvent = UiText.DynamicString(
                                    "Transaksi disimpan lokal. Data akan disinkronkan saat server tersedia."
                                )
                            )
                        }
                    }
                    is LocalCheckoutResult.Failed -> {
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                errorEvent = UiText.DynamicString(result.reason)
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun shouldUseLocalCheckout(): Boolean {
        val repository = offlineStatusRepository ?: return false

        if (!repository.isOnline()) {
            return true
        }

        val status = latestOfflineStatus
        if (status.appDataMode == AppDataMode.OFFLINE_ONLY) {
            return true
        }
        if (!status.isOnline || !status.isInternetOnline || status.internetStatus == InternetStatus.OFFLINE) {
            return true
        }

        return when (status.backendStatus) {
            BackendStatus.CONNECTED -> false
            BackendStatus.UNREACHABLE -> true
            BackendStatus.UNKNOWN -> {
                val refreshedStatus = repository.refreshBackendHealth(force = true)
                latestOfflineStatus = status.copy(
                    backendStatus = refreshedStatus,
                    isServerReachable = refreshedStatus == BackendStatus.CONNECTED
                )
                refreshedStatus != BackendStatus.CONNECTED
            }
        }
    }

    private fun resolveCustomerIdBeforeCheckout(): String? {
        _uiState.value.selectedCustomer?.let { customer -> return customer.id }

        val name = _uiState.value.customerSearchQuery.trim()
        if (name.isBlank()) {
            return null
        }

        _uiState.value.customers.firstOrNull { customer ->
            customer.isActive && customer.name.equals(name, ignoreCase = true)
        }?.let { customer ->
            selectCustomer(customer)
            return customer.id
        }

        return null
    }

    private fun checkoutCustomerNote(typedCustomerName: String): String? {
        val state = _uiState.value
        val customerName = state.selectedCustomer?.name
            ?: typedCustomerName.takeIf(String::isNotBlank)

        val parts = mutableListOf<String>()
        customerName?.let { parts.add("Pelanggan: $it") }
        if (state.isDeliveryEnabled) {
            val deliveryInfo = buildString {
                append("[PENGIRIMAN] Ongkir: Rp ${state.deliveryFee.toPlainString()}")
                if (state.deliveryAddress.isNotBlank()) append(" | Alamat: ${state.deliveryAddress.trim()}")
                if (state.deliveryCourier.isNotBlank()) append(" | Armada: ${state.deliveryCourier.trim()}")
            }
            parts.add(deliveryInfo)
        }
        return if (parts.isNotEmpty()) parts.joinToString(" • ") else null
    }

    private fun resolveAmountPaid(
        paymentMethod: PaymentMethod,
        fallbackAmount: BigDecimal
    ): BigDecimal? {
        val input = when (paymentMethod) {
            PaymentMethod.TRANSFER, PaymentMethod.QRIS -> fallbackAmount.toPlainString()
            PaymentMethod.HUTANG -> "0"
            PaymentMethod.TUNAI, PaymentMethod.DP -> _uiState.value.amountPaidInput
        }
        val result = validateCheckoutPaymentInput(paymentMethod, input, _uiState.value.finalTotal)
        if (result.error != null) setError(UiText.DynamicString(result.error))
        return result.amountPaid
    }

    private fun resolveIdempotencyKey(
        items: List<CartItem>,
        paymentMethod: PaymentMethod,
        amountPaid: BigDecimal,
        customerId: String?,
        notes: String?,
        transactionDiscount: CheckoutDiscount?,
        checkoutAttemptId: String?,
        managerApprovalId: String?,
    ): String {
        val fingerprint = buildString {
            append(paymentMethod.apiValue()).append('|').append(amountPaid.toPlainString()).append('|')
            append(customerId.orEmpty()).append('|').append(notes.orEmpty()).append('|')
            append(checkoutAttemptId.orEmpty()).append('|').append(managerApprovalId.orEmpty()).append('|')
            append(transactionDiscount.fingerprintPart()).append('|')
            items.sortedBy(CartItem::cartItemId).forEach { item ->
                append(item.cartItemId).append(':').append(item.quantity).append(':')
                    .append(item.discountRequest.fingerprintPart()).append(':')
                    .append(item.isSecondaryUnit).append(';')
            }
        }
        val existing = pendingOnlineCheckout
        if (existing?.fingerprint == fingerprint) return existing.idempotencyKey
        return UUID.randomUUID().toString().also { key ->
            pendingOnlineCheckout = PendingOnlineCheckout(key, fingerprint)
        }
    }

    private fun setError(error: UiText) {
        _uiState.update { state ->
            state.copy(errorEvent = error)
        }
    }

    private fun invalidatePendingCheckout() {
        pendingOnlineCheckout = null
        ambiguousOnlineCheckout = null
    }

    private fun allowCheckoutIntentMutation(): Boolean {
        if (ambiguousOnlineCheckout != null) {
            setError(
                UiText.DynamicString(
                    "Checkout sebelumnya belum pasti. Tekan checkout lagi atau periksa histori sebelum mengubah keranjang.",
                ),
            )
            return false
        }
        pendingOnlineCheckout = null
        return true
    }

    private fun applyAuthoritativePreview(preview: CheckoutPreview) {
        _uiState.update { state -> state.withAuthoritativePreview(preview) }
    }

    private fun CheckoutUiState.resetAfterSuccessfulCheckout(): CheckoutUiState = CheckoutUiState(
        products = products,
        productPage = productPage,
        productLimit = productLimit,
        productTotal = productTotal,
        productTotalPages = productTotalPages,
        customers = customers,
        activeCashSession = activeCashSession,
        hasActiveCashSession = hasActiveCashSession,
        heldCarts = heldCarts,
    )

    private fun RepositoryResult.Error.toUiText(): UiText {
        val message = when (code) {
            "CREDIT_LIMIT_EXCEEDED" -> "Limit kredit pelanggan terlampaui."
            "INVALID_DISCOUNT" -> "Diskon tidak valid. Periksa jenis dan nilainya."
            "DISCOUNT_EXCEEDS_AMOUNT" -> "Diskon melebihi nilai barang atau transaksi."
            "DISCOUNT_OVERRIDE_REQUIRED", "MANAGER_APPROVAL_REQUIRED" -> {
                "Diskon ini memerlukan persetujuan owner atau admin."
            }
            "DISCOUNT_APPROVAL_SCOPE_MISMATCH", "MANAGER_APPROVAL_SCOPE_MISMATCH" -> {
                "Persetujuan tidak lagi cocok karena keranjang atau harga berubah. Buat preview ulang."
            }
            "MANAGER_APPROVAL_EXPIRED" -> "Persetujuan telah kedaluwarsa. Minta persetujuan baru."
            "MANAGER_APPROVAL_ALREADY_USED" -> "Persetujuan sudah digunakan. Minta persetujuan baru."
            "FINANCIAL_CONSTRAINT_VIOLATION" -> {
                "Checkout ditolak karena melanggar aturan finansial."
            }
            else -> message
        }

        return UiText.DynamicString(message)
    }

    private fun RepositoryResult.Error.requiresFreshDiscountPreview(): Boolean =
        requiresFreshDiscountPreview(code)

    private fun RepositoryResult.Error.isCustomerLookupAccessDenied(): Boolean {
        val normalizedMessage = message.lowercase()
        val normalizedCode = code.lowercase()
        return normalizedCode.contains("forbidden") ||
            normalizedCode.contains("unauthorized") ||
            normalizedMessage.contains("akses ditolak") ||
            normalizedMessage.contains("admin atau owner")
    }

    private fun RepositoryResult.Error.isAuthFailure(): Boolean {
        val normalizedCode = code.lowercase()
        val normalizedMessage = message.lowercase()
        return normalizedCode == "http_401" ||
            normalizedCode == "http_403" ||
            normalizedCode.contains("unauthorized") ||
            normalizedCode.contains("forbidden") ||
            normalizedMessage.contains("unauthorized") ||
            normalizedMessage.contains("forbidden")
    }

    private fun RepositoryResult.Error.isServerFailure(): Boolean {
        val normalizedCode = code.lowercase()
        val normalizedMessage = message.lowercase()
        return normalizedCode.startsWith("http_5") ||
            normalizedCode.contains("internal_server_error") ||
            normalizedCode.contains("service_unavailable") ||
            normalizedCode.contains("gateway") ||
            normalizedCode.contains("timeout") ||
            normalizedMessage.contains("server tidak tersambung") ||
            normalizedMessage.contains("sesi kasir gagal dicek")
    }

    private fun RepositoryResult.Error.isAmbiguousCheckoutFailure(): Boolean {
        val normalized = code.uppercase()
        return normalized.startsWith("HTTP_5") || normalized in setOf(
            "NETWORK_TIMEOUT", "NETWORK_UNAVAILABLE", "CONNECTION_FAILED",
            "CONNECTION_INTERRUPTED", "INVALID_RESPONSE", "EMPTY_BODY"
        )
    }

    private suspend fun restoreLocalOpenCashSessionIfAvailable(): Boolean {
        val dataSource = cashSessionLocalDataSource ?: return false
        val currentCashierUserId = _uiState.value.activeCashSession
            ?.cashierUserId
            ?.takeIf(String::isNotBlank)
        val localSession = runCatching {
            dataSource.getLatestOpenSession(currentCashierUserId)
        }.getOrNull() ?: return false

        val activeCashSession = localSession.toActiveCashSessionUi()
        _uiState.update { state ->
            state.copy(
                activeCashSession = activeCashSession,
                hasActiveCashSession = true,
                isCashSessionLoading = false,
                cashSessionError = null,
                errorEvent = UiText.DynamicString(
                    "Sesi kas aktif dari data lokal. Server tidak tersambung; transaksi akan disimpan lokal."
                )
            )
        }
        return true
    }

    private suspend fun buildLocalCheckoutCommand(): LocalCheckoutCommandBuildResult {
        val state = _uiState.value
        if (state.cartItems.isEmpty()) {
            return LocalCheckoutCommandBuildResult.Failed("Keranjang masih kosong.")
        }

        val lookupService = localCheckoutLookupService
            ?: return LocalCheckoutCommandBuildResult.Failed("Lookup data lokal belum tersedia.")

        val activeSession = state.activeCashSession
            ?: return LocalCheckoutCommandBuildResult.Failed("Sesi kasir aktif belum tersedia.")

        if (activeSession.cashierUserId.isBlank()) {
            return LocalCheckoutCommandBuildResult.Failed("Kasir sesi aktif belum valid.")
        }

        val cashSessionLocalId = activeSession.localId
            ?: lookupService.findCashSessionByServerId(activeSession.serverId)?.localId
            ?: return LocalCheckoutCommandBuildResult.Failed("Sesi kasir lokal belum tersedia.")

        val subtotal = state.subtotal.toSafeDouble()
        val discount = state.totalDiscount.toSafeDouble()
        val total = state.finalTotal.toSafeDouble()
        val paidAmount = resolveLocalCheckoutPaidAmount(state)
            ?: return LocalCheckoutCommandBuildResult.Failed("Nominal bayar belum valid.")

        if (subtotal < 0.0 || discount < 0.0 || total < 0.0) {
            return LocalCheckoutCommandBuildResult.Failed("Nilai transaksi lokal belum valid.")
        }
        if (paidAmount < 0.0) {
            return LocalCheckoutCommandBuildResult.Failed("Nominal bayar tidak boleh negatif.")
        }
        if (paidAmount > total) {
            return LocalCheckoutCommandBuildResult.Failed("Nominal bayar melebihi total transaksi.")
        }

        val selectedCustomer = state.selectedCustomer
        if (state.selectedPaymentMethod.requiresRegisteredCustomer() && selectedCustomer == null) {
            return LocalCheckoutCommandBuildResult.Failed("Hutang/DP wajib memakai pelanggan terdaftar.")
        }

        val customerLookup = selectedCustomer?.let { customer ->
            lookupService.findCustomerByServerId(customer.id)
        }
        val typedCustomerName = state.customerSearchQuery.trim()
        val items = mutableListOf<LocalCheckoutItemCommand>()

        for (cartItem in state.cartItems) {
            if (cartItem.quantity <= 0) {
                return LocalCheckoutCommandBuildResult.Failed("Qty ${cartItem.productName} belum valid.")
            }

            val productLookup = lookupService.findProductByServerId(cartItem.productId)
                ?: return LocalCheckoutCommandBuildResult.Failed(
                    "Data lokal produk ${cartItem.productName} belum tersedia."
                )
            val quantity = cartItem.quantity.toDouble()
            val baseQuantity = cartItem.equivalentBaseQuantity
            val factor = if (cartItem.isSecondaryUnit && cartItem.secondaryUnitFactor != null) {
                cartItem.secondaryUnitFactor.toDouble()
            } else {
                1.0
            }
            val totalProductBaseQty = state.cartItems
                .filter { it.productId == cartItem.productId }
                .sumOf { it.equivalentBaseQuantity }
            if (productLookup.stock < totalProductBaseQty) {
                return LocalCheckoutCommandBuildResult.Failed(
                    "Stok lokal ${cartItem.productName} tidak cukup."
                )
            }

            val grossSubtotal = cartItem.unitPrice.multiply(cartItem.quantity.toBigDecimal())
            val lineSubtotal = grossSubtotal
            items += LocalCheckoutItemCommand(
                productLocalId = productLookup.localId,
                productServerId = cartItem.productId,
                productNameSnapshot = cartItem.productName,
                skuSnapshot = cartItem.sku.takeIf(String::isNotBlank),
                unitNameSnapshot = cartItem.unitName.takeIf(String::isNotBlank),
                quantity = quantity,
                baseQuantity = baseQuantity,
                conversionFactor = factor,
                priceAtTransaction = cartItem.unitPrice.toSafeDouble(),
                cogsAtTransaction = productLookup.cogsAtTransaction * factor,
                discount = 0.0,
                subtotal = lineSubtotal.toSafeDouble()
            )
        }

        val remainingAmount = (total - paidAmount).coerceAtLeast(0.0)
        return LocalCheckoutCommandBuildResult.Success(
            LocalCheckoutCommand(
                cashierUserId = activeSession.cashierUserId,
                cashSessionLocalId = cashSessionLocalId,
                cashSessionServerId = activeSession.serverId,
                customerLocalId = customerLookup?.localId,
                customerServerId = selectedCustomer?.id,
                customerName = selectedCustomer?.name ?: typedCustomerName.takeIf(String::isNotBlank),
                paymentMethod = state.selectedPaymentMethod.apiValue(),
                subtotal = subtotal,
                discount = discount,
                total = total,
                paidAmount = paidAmount,
                remainingAmount = remainingAmount,
                items = items,
                note = checkoutCustomerNote(typedCustomerName)
            )
        )
    }

    private fun resolveLocalCheckoutPaidAmount(state: CheckoutUiState): Double? {
        return when (state.selectedPaymentMethod) {
            PaymentMethod.HUTANG -> BigDecimal.ZERO
            PaymentMethod.DP -> state.amountPaidInput.toBigDecimalOrNull()
            else -> state.finalTotal
        }?.toSafeDouble()
    }

    companion object {
        private const val PRODUCT_PAGE_LIMIT = 24
        private const val CUSTOMER_PAGE_LIMIT = 12

        fun factory(
            checkoutRepository: CheckoutRepository,
            inventoryRepository: InventoryRepository,
            customerRepository: CustomerRepository,
            cashReconciliationRepository: CashReconciliationRepository,
            cashSessionLocalDataSource: CashSessionLocalDataSource? = null,
            localCashExpenseService: com.tbterminal.app.data.local.cashexpense.LocalCashExpenseService? = null,
            localCheckoutLookupService: LocalCheckoutLookupService? = null,
            localCheckoutService: LocalCheckoutService? = null,
            offlineStatusRepository: OfflineStatusRepository? = null,
            offlineSyncScheduler: OfflineSyncScheduler? = null
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                CheckoutViewModel(
                    checkoutRepository = checkoutRepository,
                    inventoryRepository = inventoryRepository,
                    customerRepository = customerRepository,
                    cashReconciliationRepository = cashReconciliationRepository,
                    cashSessionLocalDataSource = cashSessionLocalDataSource,
                    localCashExpenseService = localCashExpenseService,
                    localCheckoutLookupService = localCheckoutLookupService,
                    localCheckoutService = localCheckoutService,
                    offlineStatusRepository = offlineStatusRepository,
                    offlineSyncScheduler = offlineSyncScheduler
                )
            }
        }

        private const val MAX_CASH_INPUT_LENGTH = 12
    }

    private fun CheckoutUiState.withCart(items: List<CartItem>): CheckoutUiState =
        recalculateCheckoutState(items, transactionDiscount)

    private fun CheckoutUiState.withTransactionDiscount(discount: CheckoutDiscount?): CheckoutUiState =
        recalculateCheckoutState(cartItems, discount)

    private fun String.moneyInput(): String {
        return filter(Char::isDigit).trimStart('0').ifBlank { "0" }.take(MAX_CASH_INPUT_LENGTH)
    }

    private fun PaymentMethod.requiresRegisteredCustomer(): Boolean {
        return this == PaymentMethod.HUTANG || this == PaymentMethod.DP
    }

    private suspend fun CashSession.toMirroredActiveCashSessionUi(): ActiveCashSessionUi {
        val localId = mirrorCashSessionSafely(this)
        return toActiveCashSessionUi(localId = localId)
    }

    private suspend fun mirrorCashSessionSafely(session: CashSession): Long? {
        return try {
            cashSessionLocalDataSource?.mirrorRemoteSession(session)?.localId
        } catch (_: Exception) {
            null
        }
    }
}

private sealed interface LocalCheckoutCommandBuildResult {
    data class Success(val command: LocalCheckoutCommand) : LocalCheckoutCommandBuildResult
    data class Failed(val reason: String) : LocalCheckoutCommandBuildResult
}

private data class PendingOnlineCheckout(
    val idempotencyKey: String,
    val fingerprint: String
)

private fun CheckoutDiscount?.fingerprintPart(): String = this?.let { discount ->
    "${discount.type.name}:${discount.value.stripTrailingZeros().toPlainString()}"
}.orEmpty()

private fun CheckoutUiState.hasDiscountRequest(): Boolean =
    transactionDiscount != null || cartItems.any { it.discountRequest != null }

internal fun CheckoutUiState.recalculateCheckoutState(
    items: List<CartItem>,
    discount: CheckoutDiscount?,
    deliveryFeeAmount: BigDecimal = if (isDeliveryEnabled) deliveryFee else BigDecimal.ZERO,
): CheckoutUiState {
    val totals = estimateCheckoutTotals(items, discount)
    val calculatedFinalTotal = totals.netTotal.add(deliveryFeeAmount).coerceAtLeast(BigDecimal.ZERO)
    return copy(
        cartItems = items,
        subtotal = totals.grossSubtotal,
        itemDiscountTotal = totals.itemDiscountTotal,
        transactionDiscount = discount,
        transactionDiscountAmount = totals.transactionDiscountAmount,
        totalDiscount = totals.totalDiscountAmount,
        finalTotal = calculatedFinalTotal,
        checkoutPreview = null,
        approvedManagerApprovalId = null,
        hasAmbiguousCheckout = false,
        isPreviewLoading = false,
        amountPaidInput = when (selectedPaymentMethod) {
            PaymentMethod.HUTANG -> "0"
            PaymentMethod.DP -> amountPaidInput
            else -> calculatedFinalTotal.toPlainString()
        },
        errorEvent = null,
    )
}

private fun CashSession.toActiveCashSessionUi(localId: Long? = null): ActiveCashSessionUi {
    return ActiveCashSessionUi(
        localId = localId,
        serverId = id,
        cashierUserId = userId,
        openedAt = openedAt,
        startingCash = openingCash,
        status = status
    )
}

private fun LocalCashSessionEntity.toActiveCashSessionUi(): ActiveCashSessionUi {
    return ActiveCashSessionUi(
        localId = localId,
        serverId = serverId.orEmpty(),
        cashierUserId = cashierUserId,
        openedAt = openedAt.toOffsetDateTimeString(),
        startingCash = BigDecimal.valueOf(startingCash),
        status = status
    )
}

private fun Long.toOffsetDateTimeString(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.of("Asia/Jakarta"))
        .toOffsetDateTime()
        .toString()
}

private fun BigDecimal.toSafeDouble(): Double = runCatching { toDouble() }.getOrDefault(0.0)

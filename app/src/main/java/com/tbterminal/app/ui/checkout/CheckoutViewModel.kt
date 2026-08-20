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
                                        unitName = product.unitName,
                                        categoryName = product.categoryName,
                                        stockQty = product.quantity
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

    fun replaceCart(items: List<CartItem>) {
        _uiState.update { state ->
            state.withCart(items)
        }
    }

    fun addProduct(product: Product) {
        if (product.stockQty <= BigDecimal.ZERO) {
            setError(UiText.DynamicString("Stok ${product.name} kosong."))
            return
        }

        val currentQuantity = _uiState.value.cartItems
            .firstOrNull { item -> item.productId == product.productId }
            ?.quantity ?: 0
        if ((currentQuantity + 1).toBigDecimal() > product.stockQty) {
            setError(UiText.DynamicString("Stok ${product.name} tidak cukup."))
            return
        }

        _uiState.update { state ->
            val existingItem = state.cartItems.firstOrNull { item ->
                item.productId == product.productId
            }
            val updatedItems = if (existingItem == null) {
                state.cartItems + CartItem(
                    productId = product.productId,
                    productName = product.name,
                    unitPrice = product.unitPrice,
                    quantity = 1,
                    sku = product.sku,
                    unitName = product.unitName
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

    fun updateCartQuantity(
        cartItemId: String,
        quantity: Int
    ) {
        val product = _uiState.value.products.firstOrNull { item ->
            item.productId == cartItemId
        }
        if (product != null && quantity.toBigDecimal() > product.stockQty) {
            setError(UiText.DynamicString("Stok ${product.name} tidak cukup."))
            return
        }

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

    fun clearErrorEvent() {
        _uiState.update { state ->
            state.copy(errorEvent = null)
        }
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

    fun submitCheckout(
        paymentMethod: PaymentMethod,
        amountPaid: BigDecimal
    ) {
        val cartSnapshot = _uiState.value.cartItems
        if (cartSnapshot.isEmpty()) {
            setError(UiText.DynamicString("Keranjang masih kosong."))
            return
        }

        if (_uiState.value.isLoading) {
            return
        }

        if (!_uiState.value.hasActiveCashSession) {
            setError(UiText.DynamicString("Buka sesi kasir untuk akun ini terlebih dahulu sebelum bertransaksi."))
            return
        }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isLoading = true, errorEvent = null)
            }

            val customerId = resolveCustomerIdBeforeCheckout()
            val typedCustomerName = _uiState.value.customerSearchQuery.trim()
            if (customerId == null && paymentMethod.requiresRegisteredCustomer()) {
                setError(
                    UiText.DynamicString(
                        "Transaksi hutang/DP wajib memakai pelanggan terdaftar."
                    )
                )
                _uiState.update { state -> state.copy(isLoading = false) }
                return@launch
            }

            val effectiveAmountPaid = resolveAmountPaid(paymentMethod, amountPaid)
                ?: run {
                    _uiState.update { state -> state.copy(isLoading = false) }
                    return@launch
                }

            if (shouldUseLocalCheckout()) {
                submitLocalCheckout()
                return@launch
            }

            val command = CheckoutSubmitCommand(
                idempotencyKey = resolveIdempotencyKey(
                    items = cartSnapshot,
                    paymentMethod = paymentMethod,
                    amountPaid = effectiveAmountPaid,
                    customerId = customerId,
                    notes = checkoutCustomerNote(typedCustomerName)
                ),
                items = cartSnapshot.map { item ->
                    CheckoutSubmitItem(
                        productId = item.productId,
                        quantity = item.quantity
                    )
                },
                paymentMethod = paymentMethod.apiValue(),
                amountPaid = effectiveAmountPaid.toPlainString(),
                customerId = customerId,
                notes = checkoutCustomerNote(typedCustomerName)
            )

            when (val result = checkoutRepository.submitCheckout(command)) {
                is RepositoryResult.Success -> {
                    pendingOnlineCheckout = null
                    _checkoutEvents.tryEmit(CheckoutEvent.NavigateToReceipt(result.data.transactionId))
                    _uiState.update { state ->
                        CheckoutUiState(
                            products = state.products,
                            productPage = state.productPage,
                            productLimit = state.productLimit,
                            productTotal = state.productTotal,
                            productTotalPages = state.productTotalPages,
                            customers = state.customers,
                            activeCashSession = state.activeCashSession,
                            hasActiveCashSession = state.hasActiveCashSession
                        )
                    }
                }
                is RepositoryResult.Error -> {
                    if (!result.isAmbiguousCheckoutFailure()) pendingOnlineCheckout = null
                    setError(result.toUiText())
                }
                is RepositoryResult.Exception -> setError(
                    UiText.DynamicString("Koneksi ke server bermasalah. Coba lagi.")
                )
            }

            _uiState.update { state ->
                state.copy(isLoading = false)
            }
        }
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
        val customerName = _uiState.value.selectedCustomer?.name
            ?: typedCustomerName.takeIf(String::isNotBlank)

        return customerName?.let { name -> "Pelanggan: $name" }
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
        notes: String?
    ): String {
        val fingerprint = buildString {
            append(paymentMethod.apiValue()).append('|').append(amountPaid.toPlainString()).append('|')
            append(customerId.orEmpty()).append('|').append(notes.orEmpty()).append('|')
            items.sortedBy(CartItem::productId).forEach { item ->
                append(item.productId).append(':').append(item.quantity).append(':')
                    .append(item.discount.toPlainString()).append(';')
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

    private fun RepositoryResult.Error.toUiText(): UiText {
        val message = when (code) {
            "CREDIT_LIMIT_EXCEEDED" -> "Limit kredit pelanggan terlampaui."
            "FINANCIAL_CONSTRAINT_VIOLATION" -> {
                "Checkout ditolak karena melanggar aturan finansial."
            }
            else -> message
        }

        return UiText.DynamicString(message)
    }

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
            if (productLookup.stock < quantity) {
                return LocalCheckoutCommandBuildResult.Failed(
                    "Stok lokal ${cartItem.productName} tidak cukup."
                )
            }

            val grossSubtotal = cartItem.unitPrice.multiply(cartItem.quantity.toBigDecimal())
            val lineSubtotal = grossSubtotal.subtract(cartItem.discount).max(BigDecimal.ZERO)
            items += LocalCheckoutItemCommand(
                productLocalId = productLookup.localId,
                productServerId = cartItem.productId,
                productNameSnapshot = cartItem.productName,
                skuSnapshot = cartItem.sku.takeIf(String::isNotBlank),
                unitNameSnapshot = cartItem.unitName.takeIf(String::isNotBlank),
                quantity = quantity,
                priceAtTransaction = cartItem.unitPrice.toSafeDouble(),
                cogsAtTransaction = productLookup.cogsAtTransaction,
                discount = cartItem.discount.toSafeDouble(),
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
                    localCheckoutLookupService = localCheckoutLookupService,
                    localCheckoutService = localCheckoutService,
                    offlineStatusRepository = offlineStatusRepository,
                    offlineSyncScheduler = offlineSyncScheduler
                )
            }
        }

        private const val MAX_CASH_INPUT_LENGTH = 12
    }

    private fun CheckoutUiState.withCart(items: List<CartItem>): CheckoutUiState {
        val subtotal = items.sumAmounts { item ->
            item.unitPrice.multiply(item.quantity.toBigDecimal())
        }
        val totalDiscount = items.sumAmounts { item ->
            item.discount
        }
        val finalTotal = subtotal.subtract(totalDiscount).max(BigDecimal.ZERO)

        return copy(
            cartItems = items,
            subtotal = subtotal,
            totalDiscount = totalDiscount,
            finalTotal = finalTotal,
            amountPaidInput = when (selectedPaymentMethod) {
                PaymentMethod.HUTANG -> "0"
                PaymentMethod.DP -> amountPaidInput
                else -> finalTotal.toPlainString()
            },
            errorEvent = null
        )
    }

    private fun List<CartItem>.sumAmounts(selector: (CartItem) -> BigDecimal): BigDecimal {
        return fold(BigDecimal.ZERO) { total, item ->
            total.add(selector(item))
        }
    }

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
        .atZone(ZoneId.systemDefault())
        .toOffsetDateTime()
        .toString()
}

private fun BigDecimal.toSafeDouble(): Double = runCatching { toDouble() }.getOrDefault(0.0)

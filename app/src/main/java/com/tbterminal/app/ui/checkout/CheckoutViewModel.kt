package com.tbterminal.app.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.auth.PinUnlockService
import com.tbterminal.app.data.local.cashsession.LocalCashSessionResult
import com.tbterminal.app.data.local.cashsession.LocalCashSessionService
import com.tbterminal.app.data.local.checkout.LocalCheckoutCommand
import com.tbterminal.app.data.local.checkout.LocalCheckoutItemCommand
import com.tbterminal.app.data.local.checkout.LocalCheckoutLookupService
import com.tbterminal.app.data.local.checkout.LocalCheckoutPaymentLineCommand
import com.tbterminal.app.data.local.checkout.LocalCheckoutResult
import com.tbterminal.app.data.local.checkout.LocalCheckoutService
import com.tbterminal.app.data.local.checkout.OfflineItemUnitResolution
import com.tbterminal.app.data.local.checkout.normalizeExactDecimal
import com.tbterminal.app.data.local.checkout.resolveOfflineCheckoutItemUnit
import com.tbterminal.app.data.local.database.CashSessionLocalDataSource
import com.tbterminal.app.data.local.database.CheckoutDraftStore
import com.tbterminal.app.data.local.database.CustomerLocalDataSource
import com.tbterminal.app.data.local.database.LocalAppSettingsDataSource
import com.tbterminal.app.data.local.database.PendingSubmissionStatus
import com.tbterminal.app.data.local.database.PendingSubmissionStore
import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.data.local.database.ProductLocalDataSource
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.mapper.hasUnsyncedLocalChanges
import com.tbterminal.app.data.local.model.AppDataMode
import com.tbterminal.app.data.model.CheckoutPaymentLine
import com.tbterminal.app.data.model.CheckoutSubmitCommand
import com.tbterminal.app.data.model.CheckoutSubmitItem
import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.data.model.CheckoutPreview
import com.tbterminal.app.data.model.CheckoutPreviewCommand
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.remote.NetworkResult
import com.tbterminal.app.data.remote.ServerUnavailabilityClassifier
import com.tbterminal.app.data.remote.UserFacingErrorMapper
import com.tbterminal.app.data.repository.CheckoutRepository
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.PinUnlockResult
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.repository.SystemRepository
import com.tbterminal.app.data.sync.BackendStatus
import com.tbterminal.app.data.sync.InternetStatus
import com.tbterminal.app.data.sync.OfflineSyncScheduler
import com.tbterminal.app.data.sync.defaultElapsedRealtimeMillis
import com.tbterminal.app.data.sync.runSuspendCatching
import com.tbterminal.app.data.sync.OfflineStatus
import com.tbterminal.app.data.sync.OfflineStatusRepository
import com.tbterminal.app.data.validation.CredentialValidator
import com.tbterminal.app.navigation.AppAccessPolicy
import com.tbterminal.app.ui.auth.formatPinLockoutMessage
import com.tbterminal.app.ui.common.UiText
import com.tbterminal.app.ui.common.parseWholeRupiahInput
import com.tbterminal.app.ui.common.sanitizeWholeRupiahInput
import com.tbterminal.app.ui.common.toWholeRupiahInputText
import com.tbterminal.app.ui.common.viewModelFactory
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.runBlocking

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
    private val offlineSyncScheduler: OfflineSyncScheduler? = null,
    private val localAppSettingsDataSource: LocalAppSettingsDataSource? = null,
    private val systemRepository: SystemRepository? = null,
    private val quotationRepository: com.tbterminal.app.data.repository.QuotationRepository? = null,
    private val actorRole: String = "KASIR",
    private val pinUnlockService: PinUnlockService? = null,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
    private val elapsedRealtimeMillis: () -> Long = { defaultElapsedRealtimeMillis() },
    private val lockoutTickIntervalMillis: Long = 1_000L,
    private val pendingSubmissionStore: PendingSubmissionStore? = null,
    private val checkoutDraftStore: CheckoutDraftStore? = null,
    private val productLocalDataSource: ProductLocalDataSource? = null,
    private val customerLocalDataSource: CustomerLocalDataSource? = null,
    private val cartDraftSaveDebounceMs: Long = CheckoutCartDraftCoordinator.DEFAULT_DEBOUNCE_MILLIS,
    private val localCashSessionService: LocalCashSessionService? = null,
    private val cashierUserId: String? = null,
    private val sessionManager: SessionManager? = null,
    private val logoutFlushDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        CheckoutUiState(canOpenCashSession = AppAccessPolicy.canOpenCashSession(actorRole))
    )
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
    private var lockScreenCountdownJob: Job? = null
    private var latestOfflineStatus: OfflineStatus = OfflineStatus()
    private var pendingOnlineCheckout: PendingOnlineCheckout? = null
    private var ambiguousOnlineCheckout: CheckoutSubmitCommand? = null
    private var reorderedProductsCache: Map<String, ReorderResolvedProduct> = emptyMap()
    private val pendingSubmissionPersistence: CheckoutPendingSubmissionPersistence? =
        pendingSubmissionStore?.let { CheckoutPendingSubmissionPersistence(it, clockMillis) }
    private val cartDraftCoordinator: CheckoutCartDraftCoordinator? = checkoutDraftStore?.let { store ->
        CheckoutCartDraftCoordinator(
            store = store,
            restorer = CheckoutCartDraftRestorer(
                productLocalDataSource = productLocalDataSource,
                customerLocalDataSource = customerLocalDataSource,
                inventoryRepository = inventoryRepository,
                customerRepository = customerRepository,
            ),
            debounceMillis = cartDraftSaveDebounceMs,
        )
    }
    private var pendingSubmissionRestoreJob: Job? = null
    private var cashSessionLoadJob: Job? = null
    private var hasCompletedInitialCashSessionLoad = false
    private var hasAmbiguousOpenCashSession = false
    private var beforeSessionClearedListener: (() -> Unit)? = null

    init {
        sessionManager?.let { manager ->
            val listener = {
                val outgoingOwner = manager.resolveOwnerUserId()
                    ?: cashierUserId?.trim()?.takeIf(String::isNotBlank)
                val snapshotState = _uiState.value
                val pendingKey = pendingOnlineCheckout
                val pendingCmd = ambiguousOnlineCheckout
                runBlocking(logoutFlushDispatcher) {
                    cartDraftCoordinator?.flushAndStopForOwner(outgoingOwner, snapshotState)
                    if (outgoingOwner != null && pendingKey != null && pendingCmd != null) {
                        pendingSubmissionPersistence?.saveForOwner(
                            ownerUserId = outgoingOwner,
                            command = pendingCmd,
                            fingerprint = pendingKey.fingerprint,
                            state = snapshotState,
                            status = PendingSubmissionStatus.AMBIGUOUS,
                        )
                    }
                }
                Unit
            }
            manager.addBeforeSessionClearedListener(listener)
            beforeSessionClearedListener = listener
        }
        pendingSubmissionRestoreJob = viewModelScope.launch {
            restorePersistedCheckoutState()
        }
        observeOfflineStatus()
        loadStoreConfig()
        loadCashSession()
        loadProducts()
        loadCustomers()
    }

    private suspend fun restorePersistedCheckoutState() {
        restoreCartDraftIfAvailable()
        restorePendingOnlineCheckout()
        cartDraftCoordinator?.startObserving(viewModelScope, _uiState)
    }

    private suspend fun restoreCartDraftIfAvailable() {
        val restored = cartDraftCoordinator?.restore() ?: return
        _uiState.update { state ->
            val userModifiedActive = state.cartItems.isNotEmpty() ||
                state.selectedCustomer != null ||
                state.transactionDiscount != null ||
                state.isDeliveryEnabled ||
                !state.activeQuotationId.isNullOrBlank()
            val mergedHeldCarts = if (state.heldCarts.isEmpty()) restored.heldCarts else state.heldCarts
            if (userModifiedActive) {
                state.copy(heldCarts = mergedHeldCarts)
            } else {
                val deliveryFeeAmount = if (restored.isDeliveryEnabled) restored.deliveryFee else BigDecimal.ZERO
                state.copy(
                    heldCarts = mergedHeldCarts,
                    selectedCustomer = restored.selectedCustomer,
                    customerSearchQuery = restored.customerSearchQuery,
                    selectedPaymentMethod = restored.selectedPaymentMethod,
                    isDeliveryEnabled = restored.isDeliveryEnabled,
                    deliveryFee = restored.deliveryFee,
                    deliveryFeeInput = restored.deliveryFeeInput,
                    deliveryAddress = restored.deliveryAddress,
                    deliveryCourier = restored.deliveryCourier,
                    deliveryFleet = restored.deliveryFleet,
                    deliveryRecipientPhone = restored.deliveryRecipientPhone,
                    deliveryVehiclePlate = restored.deliveryVehiclePlate,
                    deliverySpecialInstructions = restored.deliverySpecialInstructions,
                    activeQuotationId = restored.activeQuotationId,
                    activeQuotationNumber = restored.activeQuotationNumber,
                ).recalculateCheckoutState(
                    items = restored.activeItems,
                    discount = restored.transactionDiscount,
                    deliveryFeeAmount = deliveryFeeAmount,
                    preserveAmbiguousCheckout = true,
                )
            }
        }
    }

    private suspend fun restorePendingOnlineCheckout() {
        val restored = pendingSubmissionPersistence?.load() ?: return
        pendingOnlineCheckout = PendingOnlineCheckout(
            idempotencyKey = restored.idempotencyKey,
            fingerprint = restored.fingerprint
        )
        ambiguousOnlineCheckout = restored.command
        _uiState.update { state ->
            if (state.cartItems.isNotEmpty()) {
                state.copy(hasAmbiguousCheckout = true)
            } else {
                val deliveryFeeAmount = if (restored.isDeliveryEnabled) restored.deliveryFee else BigDecimal.ZERO
                val recalculated = state.copy(
                    selectedCustomer = restored.customer,
                    customerSearchQuery = restored.customerSearchQuery,
                    selectedPaymentMethod = restored.selectedPaymentMethod,
                    isSplitPaymentEnabled = restored.isSplitPaymentEnabled,
                    isSplitDpMode = restored.isSplitDpMode,
                    splitPaymentLines = restored.splitPaymentLines,
                    isDeliveryEnabled = restored.isDeliveryEnabled,
                    deliveryFee = restored.deliveryFee,
                    deliveryFeeInput = restored.deliveryFeeInput,
                    deliveryAddress = restored.deliveryAddress,
                    deliveryCourier = restored.deliveryCourier,
                    deliveryFleet = restored.deliveryFleet,
                    deliveryRecipientPhone = restored.deliveryRecipientPhone,
                    deliveryVehiclePlate = restored.deliveryVehiclePlate,
                    deliverySpecialInstructions = restored.deliverySpecialInstructions,
                    activeQuotationId = restored.activeQuotationId,
                    activeQuotationNumber = restored.activeQuotationNumber,
                ).recalculateCheckoutState(
                    items = restored.cartItems,
                    discount = restored.transactionDiscount,
                    deliveryFeeAmount = deliveryFeeAmount,
                    preserveAmbiguousCheckout = true,
                )
                recalculated.copy(
                    hasAmbiguousCheckout = true,
                    amountPaidInput = restored.amountPaidInput.ifBlank { recalculated.amountPaidInput },
                )
            }
        }
    }

    fun loadStoreConfig() {
        viewModelScope.launch {
            localAppSettingsDataSource?.let { ds ->
                runCatching { ds.getCachedStoreConfig() }.getOrNull()?.let { cached ->
                    _uiState.update { state ->
                        state.copy(
                            storeName = cached.storeName,
                            storeNpwp = cached.storeNpwp,
                            taxEnabled = cached.taxEnabled,
                            taxMode = cached.taxMode,
                            taxRate = cached.taxRate,
                            taxLabel = cached.taxLabel,
                            cashRoundingUnit = cached.cashRoundingUnit,
                            cashRoundingRule = cached.cashRoundingRule,
                        ).recalculateCheckoutState(
                            state.cartItems,
                            state.transactionDiscount,
                            preserveAmbiguousCheckout = true,
                        )
                    }
                }
            }
            val sysRepo = systemRepository ?: return@launch
            when (val result = sysRepo.getStoreSettings()) {
                is NetworkResult.Success -> {
                    val settings = result.data.data
                    if (result.data.success && settings != null) {
                        runCatching { localAppSettingsDataSource?.setCachedStoreConfig(settings) }
                        _uiState.update { state ->
                            state.copy(
                                storeName = settings.storeName.ifBlank { "TB TERMINAL" },
                                storeNpwp = settings.storeNpwp,
                                taxEnabled = settings.taxEnabled,
                                taxMode = settings.taxMode,
                                taxRate = settings.taxRate,
                                taxLabel = settings.taxLabel,
                                cashRoundingUnit = settings.cashRoundingUnit,
                                cashRoundingRule = settings.cashRoundingRule,
                            ).recalculateCheckoutState(
                                state.cartItems,
                                state.transactionDiscount,
                                preserveAmbiguousCheckout = true,
                            )
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    private fun observeOfflineStatus() {
        val repository = offlineStatusRepository ?: return
        viewModelScope.launch {
            repository.status.collect { status ->
                latestOfflineStatus = status
            }
        }
    }

    fun refreshCashSessionOnResume() {
        if (!hasCompletedInitialCashSessionLoad) return
        if (cashSessionLoadJob?.isActive == true) return
        loadCashSession()
    }

    fun loadCashSession() {
        cashSessionLoadJob?.cancel()
        cashSessionLoadJob = viewModelScope.launch {
            try {
                _uiState.update { state ->
                    state.copy(isCashSessionLoading = true, cashSessionError = null)
                }

                if (shouldUseLocalCheckout()) {
                    if (!restoreLocalOpenCashSessionIfAvailable()) {
                        _uiState.update { state ->
                            state.copy(
                                activeCashSession = null,
                                hasActiveCashSession = false,
                                isCashSessionLoading = false,
                                cashSessionError = null
                            )
                        }
                    }
                    return@launch
                }

                when (val result = cashReconciliationRepository.getActiveSession()) {
                    is RepositoryResult.Success -> {
                        hasAmbiguousOpenCashSession = false
                        val remoteSession = result.data
                        val activeCashSession = if (remoteSession == null) {
                            reconcileNoActiveRemoteSessionSafely()
                            null
                        } else {
                            remoteSession.toResolvedRemoteActiveCashSessionUi()
                        }
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
                        val serverUnavailable = ServerUnavailabilityClassifier.isServerUnavailable(result)
                        if (serverUnavailable) {
                            markBackendUnreachableAndSyncState()
                        }
                        if (!serverUnavailable || !restoreLocalOpenCashSessionIfAvailable()) {
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
                        val serverUnavailable = ServerUnavailabilityClassifier.isServerUnavailable(result)
                        if (serverUnavailable) {
                            markBackendUnreachableAndSyncState()
                        }
                        if (!serverUnavailable || !restoreLocalOpenCashSessionIfAvailable()) {
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
            } finally {
                hasCompletedInitialCashSessionLoad = true
            }
        }
    }

    fun onStartingCashChanged(value: String) {
        hasAmbiguousOpenCashSession = false
        val sanitized = sanitizeWholeRupiahInput(value, MAX_CASH_INPUT_LENGTH)
        _uiState.update { state ->
            state.copy(startingCashInput = sanitized, cashSessionError = null)
        }
    }

    fun openCashSession() {
        if (!_uiState.value.canOpenCashSession) {
            _uiState.update { state ->
                state.copy(cashSessionError = UiText.DynamicString("Hanya kasir yang dapat membuka sesi kas."))
            }
            return
        }
        val startingCash = parseWholeRupiahInput(_uiState.value.startingCashInput)
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

            if (!hasAmbiguousOpenCashSession && shouldUseLocalForMutation()) {
                openOrReuseLocalCashSession(startingCash)
                return@launch
            }

            when (val result = cashReconciliationRepository.openSession(startingCash)) {
                is RepositoryResult.Success -> {
                    hasAmbiguousOpenCashSession = false
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
                    if (ServerUnavailabilityClassifier.isDefinitelyUnprocessedServerUnavailable(result)) {
                        hasAmbiguousOpenCashSession = false
                        markBackendUnreachableAndSyncState()
                        openOrReuseLocalCashSession(startingCash)
                    } else {
                        if (ServerUnavailabilityClassifier.isServerUnavailable(result)) {
                            hasAmbiguousOpenCashSession = true
                            markBackendUnreachableAndSyncState()
                        }
                        _uiState.update { state ->
                            state.copy(
                                isOpeningCashSession = false,
                                cashSessionError = UiText.DynamicString(result.message)
                            )
                        }
                    }
                }
                is RepositoryResult.Exception -> {
                    if (ServerUnavailabilityClassifier.isDefinitelyUnprocessedServerUnavailable(result)) {
                        hasAmbiguousOpenCashSession = false
                        markBackendUnreachableAndSyncState()
                        openOrReuseLocalCashSession(startingCash)
                    } else {
                        if (ServerUnavailabilityClassifier.isServerUnavailable(result)) {
                            hasAmbiguousOpenCashSession = true
                            markBackendUnreachableAndSyncState()
                        }
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
    }

    private suspend fun openOrReuseLocalCashSession(startingCash: BigDecimal) {
        val resolvedCashierId = resolveCashierUserId()
        val existingOpenSession = runCatching {
            cashSessionLocalDataSource?.getLatestOpenSession(resolvedCashierId)
        }.getOrNull()
        if (existingOpenSession != null) {
            _uiState.update { state ->
                state.copy(
                    activeCashSession = existingOpenSession.toActiveCashSessionUi(),
                    hasActiveCashSession = true,
                    isOpeningCashSession = false,
                    startingCashInput = "",
                    cashSessionError = null,
                    errorEvent = UiText.DynamicString(
                        "Sesi kas aktif dari data lokal. Server tidak tersambung; transaksi akan disimpan lokal."
                    )
                )
            }
            return
        }

        val service = localCashSessionService
        if (service == null || resolvedCashierId == null) {
            _uiState.update { state ->
                state.copy(
                    isOpeningCashSession = false,
                    cashSessionError = UiText.DynamicString("Koneksi bermasalah. Sesi kasir gagal dibuka.")
                )
            }
            return
        }

        when (
            val result = service.openLocalSession(
                cashierUserId = resolvedCashierId,
                startingCash = startingCash
            )
        ) {
            is LocalCashSessionResult.Success -> {
                offlineSyncScheduler?.scheduleIfEnabled()
                _uiState.update { state ->
                    state.copy(
                        activeCashSession = result.session.toActiveCashSessionUi(),
                        hasActiveCashSession = true,
                        isOpeningCashSession = false,
                        startingCashInput = "",
                        cashSessionError = null,
                        errorEvent = UiText.DynamicString("Sesi kasir berhasil dibuka.")
                    )
                }
            }
            is LocalCashSessionResult.Failed -> {
                _uiState.update { state ->
                    state.copy(
                        isOpeningCashSession = false,
                        cashSessionError = UiText.DynamicString(result.message)
                    )
                }
            }
        }
    }

    private fun resolveCashierUserId(): String? {
        return cashierUserId
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: _uiState.value.activeCashSession?.cashierUserId?.takeIf(String::isNotBlank)
    }

    fun replaceProducts(products: List<Product>) {
        _uiState.update { state ->
            state.copy(products = products)
        }
    }

    fun refreshProducts() {
        _uiState.update { it.copy(productPage = 1) }
        loadProducts()
    }

    fun loadMoreProducts() {
        val state = _uiState.value
        if (state.isProductLoading || state.isLoadingMoreProducts) return
        if (state.productPage >= state.productTotalPages) return

        viewModelScope.launch {
            val nextPage = state.productPage + 1
            val search = state.searchQuery.trim().takeIf(String::isNotBlank)
            _uiState.update { it.copy(isLoadingMoreProducts = true) }

            when (
                val result = inventoryRepository.getProductStocks(
                    page = nextPage,
                    limit = PRODUCT_PAGE_LIMIT,
                    search = search
                )
            ) {
                is RepositoryResult.Success -> {
                    val productPage = result.data
                    val newItems = productPage.data
                        .filter { it.isActive }
                        .map { it.toCheckoutProduct() }
                    _uiState.update { current ->
                        val existingIds = current.products.map { it.productId }.toSet()
                        val combined = current.products + newItems.filter { it.productId !in existingIds }
                        current.copy(
                            products = combined,
                            productPage = productPage.page.coerceAtLeast(1),
                            productLimit = productPage.limit,
                            productTotal = productPage.total,
                            productTotalPages = productPage.totalPages.coerceAtLeast(1),
                            isLoadingMoreProducts = false
                        )
                    }
                }
                is RepositoryResult.Error -> {
                    _uiState.update { it.copy(isLoadingMoreProducts = false) }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update { it.copy(isLoadingMoreProducts = false) }
                }
            }
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
                                .map { it.toCheckoutProduct() },
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
            val updatedRecipientPhone = customer.phone?.takeIf { it.isNotBlank() } ?: state.deliveryRecipientPhone
            val updatedAddress = if (state.deliveryAddress.isBlank() && !customer.address.isNullOrBlank()) {
                customer.address
            } else {
                state.deliveryAddress
            }
            state.copy(
                selectedCustomer = customer,
                customerSearchQuery = customer.name,
                customerError = null,
                deliveryRecipientPhone = updatedRecipientPhone,
                deliveryAddress = updatedAddress
            ).recalculateCheckoutState(state.cartItems, state.transactionDiscount)
        }
    }

    fun clearSelectedCustomer() {
        _uiState.update { state ->
            state.copy(
                selectedCustomer = null,
                customerSearchQuery = ""
            ).recalculateCheckoutState(state.cartItems, state.transactionDiscount)
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
                        val updatedRecipientPhone = created.phone?.takeIf { p -> p.isNotBlank() } ?: it.deliveryRecipientPhone
                        val updatedAddress = if (it.deliveryAddress.isBlank() && !created.address.isNullOrBlank()) {
                            created.address
                        } else {
                            it.deliveryAddress
                        }
                        it.copy(
                            customers = updatedCustomers,
                            selectedCustomer = created,
                            customerSearchQuery = created.name,
                            isQuickAddCustomerOpen = false,
                            isQuickCustomerSubmitting = false,
                            customerError = null,
                            deliveryRecipientPhone = updatedRecipientPhone,
                            deliveryAddress = updatedAddress,
                            quickFeedbackMessage = "Pelanggan \"${created.name}\" berhasil didaftarkan!"
                        ).recalculateCheckoutState(it.cartItems, it.transactionDiscount)
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

    fun reorderFromTransaction(transaction: com.tbterminal.app.data.model.CashTransactionDetail) {
        if (transaction.items.isEmpty()) {
            setError(UiText.DynamicString("Transaksi ini tidak memiliki item untuk disalin."))
            return
        }
        val receiptNum = transaction.receiptId.ifBlank { transaction.id }
        loadItemsForCartReorder(
            sourceItems = transaction.items,
            customerId = transaction.customerId,
            documentLabel = receiptNum,
            transactionDiscount = null,
            quotationId = null,
            quotationNumber = null,
        )
    }

    fun reorderFromQuotation(quotation: com.tbterminal.app.data.model.QuotationDetail) {
        if (quotation.items.isEmpty()) {
            setError(UiText.DynamicString("Penawaran ini tidak memiliki item untuk dikonversi."))
            return
        }
        val mappedItems = quotation.items.map { item ->
            com.tbterminal.app.data.model.CashTransactionItem(
                productId = item.productId,
                productName = item.productName,
                unitId = item.unitId,
                unitName = item.unitName,
                quantity = item.quantity,
                priceAtTransaction = item.unitPrice,
                subtotal = item.subtotal,
                note = item.note,
            )
        }
        val txDiscount = quotation.transactionDiscountType?.let { type ->
            if (quotation.transactionDiscountValue > BigDecimal.ZERO) {
                CheckoutDiscount(type = type, value = quotation.transactionDiscountValue)
            } else null
        }
        loadItemsForCartReorder(
            sourceItems = mappedItems,
            customerId = quotation.customerId,
            documentLabel = quotation.quotationNumber,
            transactionDiscount = txDiscount,
            quotationId = quotation.id,
            quotationNumber = quotation.quotationNumber,
        )
    }

    private fun loadItemsForCartReorder(
        sourceItems: List<com.tbterminal.app.data.model.CashTransactionItem>,
        customerId: String?,
        documentLabel: String,
        transactionDiscount: CheckoutDiscount?,
        quotationId: String?,
        quotationNumber: String?,
    ) {
        if (!allowCheckoutIntentMutation()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isReordering = true) }

            val distinctProductIds = sourceItems.map { it.productId }.distinct()
            val resolvedMap = mutableMapOf<String, ReorderResolvedProduct>()

            for (productId in distinctProductIds) {
                val cachedFromUi = _uiState.value.products.firstOrNull { it.productId == productId }
                val localLookup = localCheckoutLookupService?.findProductByServerId(productId)
                when (val detailResult = inventoryRepository.getProductDetail(productId)) {
                    is RepositoryResult.Success -> {
                        val detail = detailResult.data
                        val domainProduct = detail.product
                        val stock = detail.stock
                        val resolvedStockQty = stock?.quantity
                            ?: cachedFromUi?.stockQty
                            ?: localLookup?.stock
                            ?: BigDecimal.ZERO
                        val uiProduct = Product(
                            productId = domainProduct.id,
                            name = domainProduct.name,
                            unitPrice = domainProduct.priceRetail,
                            sku = domainProduct.sku,
                            barcode = domainProduct.sku,
                            unitName = stock?.unitName ?: cachedFromUi?.unitName.orEmpty(),
                            categoryName = stock?.categoryName ?: cachedFromUi?.categoryName.orEmpty(),
                            stockQty = resolvedStockQty,
                            secondaryUnitId = domainProduct.secondaryUnitId,
                            secondaryUnitName = stock?.secondaryUnitName ?: cachedFromUi?.secondaryUnitName,
                            secondaryUnitFactor = domainProduct.secondaryUnitFactor,
                            contractorPrice = domainProduct.priceContractor,
                            priceTiers = domainProduct.priceTiers,
                            masterDiscount = domainProduct.discount,
                            baseUnitId = domainProduct.baseUnitId,
                        )
                        resolvedMap[productId] = ReorderResolvedProduct(
                            product = uiProduct,
                            isActive = domainProduct.isActive,
                        )
                    }
                    else -> {
                        if (cachedFromUi != null) {
                            resolvedMap[productId] = ReorderResolvedProduct(
                                product = cachedFromUi,
                                isActive = true,
                            )
                        }
                    }
                }
            }

            val resolvedCustomer = customerId
                ?.takeIf { it.isNotBlank() }
                ?.let { cid ->
                    when (val custResult = customerRepository.getCustomerById(cid)) {
                        is RepositoryResult.Success -> custResult.data.takeIf { it.isActive }
                        else -> _uiState.value.customers.firstOrNull { it.id == cid && it.isActive }
                    }
                }

            val plan = buildCheckoutReorderPlan(
                transactionItems = sourceItems,
                resolvedProducts = resolvedMap,
            )

            if (plan.items.isEmpty()) {
                _uiState.update { it.copy(isReordering = false) }
                setError(UiText.DynamicString(plan.buildFeedbackMessage()))
                return@launch
            }

            reorderedProductsCache = reorderedProductsCache + resolvedMap

            if (_uiState.value.cartItems.isEmpty()) {
                applyReorderReplace(
                    plan = plan,
                    customer = resolvedCustomer,
                    feedback = plan.buildFeedbackMessage(),
                    transactionDiscount = transactionDiscount,
                    quotationId = quotationId,
                    quotationNumber = quotationNumber,
                )
            } else {
                _uiState.update { state ->
                    state.copy(
                        isReordering = false,
                        pendingReorderConflict = PendingReorderConflict(
                            receiptNumber = documentLabel,
                            plan = plan,
                            customer = resolvedCustomer,
                            resolvedProducts = resolvedMap,
                            quotationId = quotationId,
                            quotationNumber = quotationNumber,
                            transactionDiscount = transactionDiscount,
                        ),
                    )
                }
            }
        }
    }

    fun resolveReorderConflict(replaceExisting: Boolean) {
        val conflict = _uiState.value.pendingReorderConflict ?: return
        if (!allowCheckoutIntentMutation()) return
        if (replaceExisting) {
            applyReorderReplace(
                plan = conflict.plan,
                customer = conflict.customer,
                feedback = conflict.plan.buildFeedbackMessage(),
                transactionDiscount = conflict.transactionDiscount,
                quotationId = conflict.quotationId,
                quotationNumber = conflict.quotationNumber,
            )
        } else {
            val mergeResult = mergeReorderItemsIntoCart(
                existingItems = _uiState.value.cartItems,
                incomingItems = conflict.plan.items,
                resolvedProducts = conflict.resolvedProducts,
            )
            val feedback = buildString {
                append("${conflict.plan.items.size} item dari #${conflict.receiptNumber} ditambahkan ke keranjang")
                if (mergeResult.cappedItems.isNotEmpty()) {
                    append(" (${mergeResult.cappedItems.size} dibatasi stok tersedia)")
                }
                append(".")
            }
            _uiState.update { state ->
                val resolvedCust = state.selectedCustomer ?: conflict.customer
                state.copy(
                    isReordering = false,
                    pendingReorderConflict = null,
                    selectedCustomer = resolvedCust,
                    customerSearchQuery = resolvedCust?.name ?: state.customerSearchQuery,
                    activeQuotationId = conflict.quotationId ?: state.activeQuotationId,
                    activeQuotationNumber = conflict.quotationNumber ?: state.activeQuotationNumber,
                    quickFeedbackMessage = feedback,
                ).recalculateCheckoutState(
                    items = mergeResult.items,
                    discount = state.transactionDiscount ?: conflict.transactionDiscount,
                )
            }
        }
    }

    fun dismissReorderConflict() {
        _uiState.update { it.copy(pendingReorderConflict = null, isReordering = false) }
    }

    fun openSaveQuotationDialog() {
        if (_uiState.value.cartItems.isEmpty()) {
            setError(UiText.DynamicString("Keranjang belanja masih kosong."))
            return
        }
        _uiState.update {
            it.copy(
                isSaveQuotationDialogOpen = true,
                quotationValidDaysInput = it.quotationValidDaysInput.ifBlank { "14" },
            )
        }
    }

    fun dismissSaveQuotationDialog() {
        if (_uiState.value.isSavingQuotation) return
        _uiState.update { it.copy(isSaveQuotationDialogOpen = false) }
    }

    fun onQuotationValidDaysChanged(value: String) {
        val clean = value.filter(Char::isDigit).take(3)
        _uiState.update { it.copy(quotationValidDaysInput = clean) }
    }

    fun onQuotationNotesChanged(value: String) {
        _uiState.update { it.copy(quotationNotesInput = value.take(300)) }
    }

    fun submitQuotation() {
        val repo = quotationRepository ?: run {
            setError(UiText.DynamicString("Fitur penawaran belum siap."))
            return
        }
        val state = _uiState.value
        if (state.isSavingQuotation) return
        if (state.cartItems.isEmpty()) {
            setError(UiText.DynamicString("Keranjang belanja kosong."))
            return
        }
        val validDays = state.quotationValidDaysInput.toIntOrNull()?.coerceIn(1, 365) ?: 14

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingQuotation = true) }
            val command = com.tbterminal.app.data.model.CreateQuotationCommand(
                customerId = state.selectedCustomer?.id,
                customerName = if (state.selectedCustomer == null) {
                    state.customerSearchQuery.trim().takeIf { it.isNotEmpty() }
                } else null,
                items = state.cartItems.map { item ->
                    com.tbterminal.app.data.model.CreateQuotationItemCommand(
                        productId = item.productId,
                        qty = item.equivalentBaseQuantityBigDecimal,
                        discountRequest = item.discountRequest,
                        note = item.note,
                    )
                },
                transactionDiscount = state.transactionDiscount,
                validDays = validDays,
                notes = state.quotationNotesInput.trim().takeIf { it.isNotEmpty() },
            )
            when (val result = repo.createQuotation(command)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSavingQuotation = false,
                            isSaveQuotationDialogOpen = false,
                            quotationNotesInput = "",
                            quickFeedbackMessage = "Penawaran ${result.data.quotationNumber} berhasil disimpan.",
                        )
                    }
                }
                is RepositoryResult.Error -> {
                    _uiState.update { it.copy(isSavingQuotation = false) }
                    setError(UiText.DynamicString(result.message))
                }
                is RepositoryResult.Exception -> {
                    _uiState.update { it.copy(isSavingQuotation = false) }
                    setError(UiText.DynamicString("Simpan penawaran memerlukan koneksi ke server."))
                }
            }
        }
    }

    private fun applyReorderReplace(
        plan: CheckoutReorderPlan,
        customer: Customer?,
        feedback: String,
        transactionDiscount: CheckoutDiscount? = null,
        quotationId: String? = null,
        quotationNumber: String? = null,
    ) {
        _uiState.update { state ->
            val customerAddress = customer?.address?.takeIf { it.isNotBlank() }.orEmpty()
            val customerPhone = customer?.phone?.takeIf { it.isNotBlank() }.orEmpty()
            state.copy(
                isReordering = false,
                pendingReorderConflict = null,
                activeQuotationId = quotationId,
                activeQuotationNumber = quotationNumber,
                selectedCustomer = customer,
                customerSearchQuery = customer?.name.orEmpty(),
                customerError = null,
                isDeliveryEnabled = false,
                deliveryFee = BigDecimal.ZERO,
                deliveryFeeInput = "",
                deliveryAddress = customerAddress,
                deliveryCourier = "",
                deliveryFleet = "",
                deliveryRecipientPhone = customerPhone,
                deliveryVehiclePlate = "",
                deliverySpecialInstructions = "",
                isSplitPaymentEnabled = false,
                isSplitDpMode = false,
                splitPaymentLines = emptyList(),
                amountPaidInput = "",
                selectedPaymentMethod = PaymentMethod.TUNAI,
                quickFeedbackMessage = feedback,
            ).recalculateCheckoutState(
                items = plan.items,
                discount = transactionDiscount,
                deliveryFeeAmount = BigDecimal.ZERO,
            )
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
                    isSecondaryUnit = isSecondary,
                    retailBaseUnitPrice = product.unitPrice,
                    contractorBaseUnitPrice = product.contractorPrice,
                    priceTiers = product.priceTiers,
                    masterUnitDiscount = product.masterDiscount,
                    baseUnitId = product.baseUnitId,
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
                        val uiProduct = found.toCheckoutProduct()
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
            ?: reorderedProductsCache[currentItem.productId]?.product

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
            ?: reorderedProductsCache[currentItem.productId]?.product
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
                            discountRequest = null,
                            hasManualDiscountOverride = false,
                            isMasterDiscountApplied = false,
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
                        cartItem.copy(
                            discountRequest = discount,
                            hasManualDiscountOverride = true,
                            isMasterDiscountApplied = false,
                        )
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
            deliveryFleet = state.deliveryFleet,
            deliveryRecipientPhone = state.deliveryRecipientPhone,
            deliveryVehiclePlate = state.deliveryVehiclePlate,
            deliverySpecialInstructions = state.deliverySpecialInstructions,
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
                deliveryFleet = "",
                deliveryRecipientPhone = "",
                deliveryVehiclePlate = "",
                deliverySpecialInstructions = "",
            )
        }
        cartDraftCoordinator?.let { coordinator ->
            viewModelScope.launch { coordinator.flushNow(_uiState.value) }
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
        _uiState.update {
            it.copy(pettyCashAmountInput = sanitizeWholeRupiahInput(amount, MAX_CASH_INPUT_LENGTH), pettyCashError = null)
        }
    }

    fun onPettyCashCategoryChanged(category: String) {
        _uiState.update { it.copy(pettyCashCategoryInput = category, pettyCashError = null) }
    }

    fun onPettyCashDescriptionChanged(description: String) {
        _uiState.update { it.copy(pettyCashDescriptionInput = description, pettyCashError = null) }
    }

    fun submitPettyCash() {
        val state = _uiState.value
        val amount = parseWholeRupiahInput(state.pettyCashAmountInput)
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
                    deliveryFleet = state.deliveryFleet,
                    deliveryRecipientPhone = state.deliveryRecipientPhone,
                    deliveryVehiclePlate = state.deliveryVehiclePlate,
                    deliverySpecialInstructions = state.deliverySpecialInstructions,
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
                deliveryFeeInput = if (target.deliveryFee > BigDecimal.ZERO) target.deliveryFee.toWholeRupiahInputText() else "",
                deliveryAddress = target.deliveryAddress,
                deliveryCourier = target.deliveryCourier,
                deliveryFleet = target.deliveryFleet,
                deliveryRecipientPhone = target.deliveryRecipientPhone,
                deliveryVehiclePlate = target.deliveryVehiclePlate,
                deliverySpecialInstructions = target.deliverySpecialInstructions,
                quickFeedbackMessage = "Pesanan \"${target.note}\" dimuat ke keranjang."
            ).recalculateCheckoutState(
                items = target.items,
                discount = target.transactionDiscount,
                deliveryFeeAmount = if (target.isDeliveryEnabled) target.deliveryFee else BigDecimal.ZERO
            )
        }
        cartDraftCoordinator?.let { coordinator ->
            viewModelScope.launch { coordinator.flushNow(_uiState.value) }
        }
    }

    fun deleteHeldCart(heldCartId: String) {
        _uiState.update { current ->
            current.copy(
                heldCarts = current.heldCarts.filterNot { it.id == heldCartId },
                quickFeedbackMessage = "Pesanan terparkir dihapus."
            )
        }
        cartDraftCoordinator?.let { coordinator ->
            viewModelScope.launch { coordinator.flushNow(_uiState.value) }
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
        val sanitized = sanitizeWholeRupiahInput(feeInput)
        val feeAmount = parseWholeRupiahInput(sanitized) ?: BigDecimal.ZERO
        _uiState.update { state ->
            state.copy(
                deliveryFeeInput = sanitized,
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

    fun updateDeliveryFleet(fleet: String) {
        _uiState.update { it.copy(deliveryFleet = fleet) }
    }

    fun updateDeliveryRecipientPhone(phone: String) {
        _uiState.update { it.copy(deliveryRecipientPhone = phone) }
    }

    fun updateDeliveryVehiclePlate(plate: String) {
        _uiState.update { it.copy(deliveryVehiclePlate = plate) }
    }

    fun updateDeliverySpecialInstructions(instructions: String) {
        _uiState.update { it.copy(deliverySpecialInstructions = instructions) }
    }

    fun lockScreen() {
        lockScreenCountdownJob?.cancel()
        _uiState.update {
            it.copy(
                isScreenLocked = true,
                lockPinInput = "",
                lockPinError = null,
                isUnlockingScreen = false,
                lockScreenLockedUntilEpochMillis = null,
                lockScreenRemainingSeconds = 0L,
            )
        }
    }

    fun updateLockPinInput(pin: String) {
        val currentState = _uiState.value
        if (currentState.isUnlockingScreen || currentState.lockScreenRemainingSeconds > 0L) {
            return
        }
        val cleanPin = pin.filter(Char::isDigit).take(CredentialValidator.PIN_MAX_LENGTH)
        _uiState.update { it.copy(lockPinInput = cleanPin, lockPinError = null) }
    }

    fun unlockScreen() {
        val currentState = _uiState.value
        if (currentState.isUnlockingScreen || currentState.lockScreenRemainingSeconds > 0L) {
            return
        }
        val input = currentState.lockPinInput
        if (!CredentialValidator.isValidPin(input)) {
            _uiState.update {
                it.copy(
                    lockPinError = "PIN harus terdiri dari ${CredentialValidator.PIN_MIN_LENGTH} sampai ${CredentialValidator.PIN_MAX_LENGTH} digit angka."
                )
            }
            return
        }
        val verifierService = pinUnlockService
        if (verifierService == null) {
            _uiState.update {
                it.copy(
                    lockPinInput = "",
                    lockPinError = "Layanan verifikasi PIN tidak tersedia.",
                )
            }
            return
        }
        lockScreenCountdownJob?.cancel()
        _uiState.update { it.copy(isUnlockingScreen = true, lockPinError = null) }
        viewModelScope.launch {
            when (val result = verifierService.unlock(input)) {
                is RepositoryResult.Success -> handleQuickPinUnlockResult(result.data)
                is RepositoryResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isScreenLocked = true,
                            isUnlockingScreen = false,
                            lockPinInput = "",
                            lockPinError = result.message,
                        )
                    }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isScreenLocked = true,
                            isUnlockingScreen = false,
                            lockPinInput = "",
                            lockPinError = "Terjadi kesalahan saat memverifikasi PIN. Coba lagi.",
                        )
                    }
                }
            }
        }
    }

    private fun handleQuickPinUnlockResult(result: PinUnlockResult) {
        when (result) {
            PinUnlockResult.Unlocked -> {
                lockScreenCountdownJob?.cancel()
                _uiState.update {
                    it.copy(
                        isScreenLocked = false,
                        lockPinInput = "",
                        lockPinError = null,
                        isUnlockingScreen = false,
                        lockScreenLockedUntilEpochMillis = null,
                        lockScreenRemainingSeconds = 0L,
                    )
                }
            }

            is PinUnlockResult.ForceLogout -> {
                lockScreenCountdownJob?.cancel()
                _uiState.update {
                    it.copy(
                        isScreenLocked = true,
                        lockPinInput = "",
                        lockPinError = result.message,
                        isUnlockingScreen = false,
                    )
                }
            }

            is PinUnlockResult.Failed -> {
                if (result.isLocked) {
                    val nowWall = clockMillis()
                    val initialRemainingMillis = result.remainingLockMillis?.takeIf { it > 0L }
                        ?: result.lockedUntilEpochMillis?.let { (it - nowWall).takeIf { diff -> diff > 0L } }
                        ?: DEFAULT_LOCKOUT_FALLBACK_MILLIS
                    val deadlineElapsedMillis = elapsedRealtimeMillis() + initialRemainingMillis
                    val initialRemainingSeconds = remainingLockSecondsFromMillis(initialRemainingMillis)
                    val displayLockedUntil = result.lockedUntilEpochMillis?.takeIf { it > nowWall } ?: 0L
                    startLockScreenCountdown(
                        displayLockedUntilEpochMillis = displayLockedUntil,
                        deadlineElapsedMillis = deadlineElapsedMillis,
                        initialRemainingSeconds = initialRemainingSeconds,
                    )
                } else {
                    val errorText = when {
                        result.attemptsLeft > 0 -> "PIN salah. Sisa percobaan: ${result.attemptsLeft}."
                        else -> "PIN salah. Coba lagi."
                    }
                    _uiState.update {
                        it.copy(
                            isScreenLocked = true,
                            lockPinInput = "",
                            lockPinError = errorText,
                            isUnlockingScreen = false,
                        )
                    }
                }
            }
        }
    }

    private fun startLockScreenCountdown(
        displayLockedUntilEpochMillis: Long,
        deadlineElapsedMillis: Long,
        initialRemainingSeconds: Long,
    ) {
        lockScreenCountdownJob?.cancel()
        val storedLockedUntil = displayLockedUntilEpochMillis.takeIf { it > 0L }
        _uiState.update {
            it.copy(
                isScreenLocked = true,
                lockPinInput = "",
                isUnlockingScreen = false,
                lockScreenLockedUntilEpochMillis = storedLockedUntil,
                lockScreenRemainingSeconds = initialRemainingSeconds,
                lockPinError = formatPinLockoutMessage(displayLockedUntilEpochMillis, initialRemainingSeconds),
            )
        }
        lockScreenCountdownJob = viewModelScope.launch {
            var remaining = initialRemainingSeconds
            while (remaining > 0L) {
                delay(lockoutTickIntervalMillis)
                remaining = remainingLockSecondsUntilElapsed(deadlineElapsedMillis)
                if (remaining > 0L) {
                    _uiState.update {
                        it.copy(
                            lockScreenRemainingSeconds = remaining,
                            lockPinError = formatPinLockoutMessage(displayLockedUntilEpochMillis, remaining),
                        )
                    }
                }
            }
            _uiState.update {
                it.copy(
                    lockScreenLockedUntilEpochMillis = null,
                    lockScreenRemainingSeconds = 0L,
                    lockPinError = null,
                )
            }
        }
    }

    private fun remainingLockSecondsUntilElapsed(deadlineElapsedMillis: Long): Long {
        return remainingLockSecondsFromMillis(deadlineElapsedMillis - elapsedRealtimeMillis())
    }

    private fun remainingLockSecondsFromMillis(diffMillis: Long): Long {
        if (diffMillis <= 0L) return 0L
        return (diffMillis + 999L) / 1000L
    }

    override fun onCleared() {
        beforeSessionClearedListener?.let { listener ->
            sessionManager?.removeBeforeSessionClearedListener(listener)
        }
        beforeSessionClearedListener = null
        lockScreenCountdownJob?.cancel()
        cartDraftCoordinator?.stop()
        super.onCleared()
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
            val updated = state.copy(
                selectedPaymentMethod = paymentMethod,
                isSplitPaymentEnabled = false,
                isSplitDpMode = false,
            ).recalculateCheckoutState(state.cartItems, state.transactionDiscount)
            updated.copy(
                amountPaidInput = when (paymentMethod) {
                    PaymentMethod.HUTANG -> "0"
                    PaymentMethod.DP -> {
                        if (state.selectedPaymentMethod == PaymentMethod.DP) state.amountPaidInput else ""
                    }
                    else -> updated.finalTotal.toWholeRupiahInputText()
                }
            )
        }
    }

    fun onAmountPaidChanged(value: String) {
        _uiState.update { state ->
            state.copy(amountPaidInput = sanitizeWholeRupiahInput(value, MAX_CASH_INPUT_LENGTH), errorEvent = null)
        }
    }

    fun setItemNote(cartItemId: String, note: String?) {
        if (!allowCheckoutIntentMutation()) return
        val cleaned = note?.trim()?.takeIf { it.isNotEmpty() }?.take(300)
        _uiState.update { state ->
            val updatedItems = state.cartItems.map { item ->
                if (item.cartItemId == cartItemId) item.copy(note = cleaned) else item
            }
            state.withCart(updatedItems)
        }
    }

    fun toggleSplitPayment(enabled: Boolean) {
        if (!allowCheckoutIntentMutation()) return
        _uiState.update { state ->
            if (!enabled) {
                state.copy(
                    isSplitPaymentEnabled = false,
                    isSplitDpMode = false,
                    errorEvent = null
                ).recalculateCheckoutState(state.cartItems, state.transactionDiscount)
            } else {
                val existingLines = state.splitPaymentLines
                val starterLines = if (existingLines.isNotEmpty()) {
                    existingLines
                } else {
                    val primaryMethod = if (state.selectedPaymentMethod in setOf(PaymentMethod.TUNAI, PaymentMethod.TRANSFER, PaymentMethod.QRIS)) {
                        state.selectedPaymentMethod
                    } else {
                        PaymentMethod.TUNAI
                    }
                    val secondaryMethod = if (primaryMethod == PaymentMethod.TUNAI) PaymentMethod.QRIS else PaymentMethod.TUNAI
                    listOf(
                        SplitPaymentLineUi(
                            method = primaryMethod,
                            amountInput = state.finalTotal.stripTrailingZeros().toPlainString()
                        ),
                        SplitPaymentLineUi(
                            method = secondaryMethod,
                            amountInput = "0"
                        )
                    )
                }
                state.copy(
                    isSplitPaymentEnabled = true,
                    splitPaymentLines = starterLines,
                    errorEvent = null
                ).recalculateCheckoutState(state.cartItems, state.transactionDiscount)
            }
        }
    }

    fun setSplitDpMode(enabled: Boolean) {
        _uiState.update { it.copy(isSplitDpMode = enabled, errorEvent = null) }
    }

    fun addSplitPaymentLine(method: PaymentMethod = PaymentMethod.TRANSFER) {
        _uiState.update { state ->
            if (state.splitPaymentLines.size >= 5) return@update state
            val preferredMethod = if (method == PaymentMethod.TUNAI && state.splitPaymentLines.any { it.method == PaymentMethod.TUNAI }) {
                PaymentMethod.TRANSFER
            } else {
                method
            }
            val currentPaid = state.splitPaymentLines.fold(BigDecimal.ZERO) { acc, line ->
                acc.add(parseWholeRupiahInput(line.amountInput) ?: BigDecimal.ZERO)
            }
            val remaining = state.finalTotal.subtract(currentPaid).coerceAtLeast(BigDecimal.ZERO)
            val newLines = state.splitPaymentLines + SplitPaymentLineUi(
                method = preferredMethod,
                amountInput = if (remaining > BigDecimal.ZERO) remaining.stripTrailingZeros().toPlainString() else "0"
            )
            state.copy(splitPaymentLines = newLines, errorEvent = null)
                .recalculateCheckoutState(state.cartItems, state.transactionDiscount)
        }
    }

    fun removeSplitPaymentLine(lineId: String) {
        _uiState.update { state ->
            if (state.splitPaymentLines.size <= 1) return@update state
            val newLines = state.splitPaymentLines.filterNot { it.id == lineId }
            state.copy(splitPaymentLines = newLines, errorEvent = null)
                .recalculateCheckoutState(state.cartItems, state.transactionDiscount)
        }
    }

    fun updateSplitPaymentLineMethod(lineId: String, method: PaymentMethod) {
        if (method !in setOf(PaymentMethod.TUNAI, PaymentMethod.TRANSFER, PaymentMethod.QRIS)) return
        _uiState.update { state ->
            if (method == PaymentMethod.TUNAI && state.splitPaymentLines.any { it.id != lineId && it.method == PaymentMethod.TUNAI }) {
                return@update state.copy(errorEvent = UiText.DynamicString("Maksimal 1 baris pembayaran Tunai dalam split payment."))
            }
            val newLines = state.splitPaymentLines.map { line ->
                if (line.id == lineId) line.copy(method = method) else line
            }
            state.copy(splitPaymentLines = newLines, errorEvent = null)
                .recalculateCheckoutState(state.cartItems, state.transactionDiscount)
        }
    }

    fun updateSplitPaymentLineAmount(lineId: String, value: String) {
        _uiState.update { state ->
            val sanitized = sanitizeWholeRupiahInput(value, MAX_CASH_INPUT_LENGTH)
            val newLines = state.splitPaymentLines.map { line ->
                if (line.id == lineId) line.copy(amountInput = sanitized) else line
            }
            state.copy(splitPaymentLines = newLines, errorEvent = null)
                .recalculateCheckoutState(state.cartItems, state.transactionDiscount)
        }
    }

    fun updateSplitPaymentLineReference(lineId: String, reference: String) {
        _uiState.update { state ->
            val newLines = state.splitPaymentLines.map { line ->
                if (line.id == lineId) line.copy(reference = reference.take(100)) else line
            }
            state.copy(splitPaymentLines = newLines)
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
            pendingSubmissionRestoreJob?.join()
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
            val resolvedItems = when (val resolution = resolveOnlineCheckoutItems(cartSnapshot)) {
                is OnlineCheckoutItemsResult.Success -> resolution.items
                is OnlineCheckoutItemsResult.Error -> {
                    invalidatePendingCheckout()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPreviewLoading = false,
                            errorEvent = UiText.DynamicString(resolution.message),
                        )
                    }
                    return@launch
                }
            }
            val previewCommand = CheckoutPreviewCommand(
                items = resolvedItems,
                transactionDiscount = _uiState.value.transactionDiscount,
                customerId = _uiState.value.selectedCustomer?.id,
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
                    if (ServerUnavailabilityClassifier.isServerUnavailable(result)) {
                        handlePreviewServerUnavailable(result.toUiText())
                    } else {
                        invalidatePendingCheckout()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isPreviewLoading = false,
                                errorEvent = result.toUiText(),
                            )
                        }
                    }
                }
                is RepositoryResult.Exception -> {
                    if (ServerUnavailabilityClassifier.isServerUnavailable(result)) {
                        handlePreviewServerUnavailable(
                            UiText.DynamicString(
                                "Preview checkout gagal karena koneksi bermasalah. Coba lagi.",
                            )
                        )
                    } else {
                        invalidatePendingCheckout()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isPreviewLoading = false,
                                errorEvent = UiText.DynamicString(UserFacingErrorMapper.message()),
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun handlePreviewServerUnavailable(fallbackError: UiText) {
        markBackendUnreachableAndSyncState()
        // Never create an offline transaction if an ambiguous online checkout already exists.
        if (ambiguousOnlineCheckout != null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isPreviewLoading = false,
                    errorEvent = fallbackError,
                )
            }
            return
        }
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
            return
        }
        _uiState.update { it.copy(isPreviewLoading = false) }
        submitLocalCheckout()
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
        val isSplit = state.isSplitPaymentEnabled
        val splitResult = if (isSplit) {
            validateSplitCheckoutPayment(
                lines = state.splitPaymentLines,
                total = state.finalTotal,
                isDpMode = state.isSplitDpMode,
                customerSelected = customerId != null
            )
        } else null

        if (splitResult?.error != null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorEvent = UiText.DynamicString(splitResult.error)
                )
            }
            return
        }

        if (!isSplit && customerId == null && paymentMethod.requiresRegisteredCustomer()) {
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
        val effectiveAmountPaid = if (isSplit) {
            splitResult!!.totalTendered
        } else {
            resolveAmountPaid(paymentMethod, fallbackAmount) ?: run {
                _uiState.update { it.copy(isLoading = false) }
                return
            }
        }
        val paymentLinesForSubmit = if (isSplit) {
            state.splitPaymentLines.mapNotNull { line ->
                val amt = parseWholeRupiahInput(line.amountInput) ?: return@mapNotNull null
                CheckoutPaymentLine(
                    method = line.method.apiValue(),
                    amount = amt.toPlainString(),
                    reference = line.reference.trim().takeIf { it.isNotEmpty() }
                )
            }
        } else null
        val paymentMethodForSubmit = if (isSplit) {
            if (state.isSplitDpMode) "dp" else ""
        } else {
            paymentMethod.apiValue()
        }
        val cartSnapshot = _uiState.value.cartItems
        val resolvedItems = when (val resolution = resolveOnlineCheckoutItems(cartSnapshot)) {
            is OnlineCheckoutItemsResult.Success -> resolution.items
            is OnlineCheckoutItemsResult.Error -> {
                invalidatePendingCheckout()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isPreviewLoading = false,
                        errorEvent = UiText.DynamicString(resolution.message),
                    )
                }
                return
            }
        }
        val notes = checkoutCustomerNote(typedCustomerName)
        val approvalAttemptId = preview.checkoutAttemptId.takeIf { preview.approvalRequired }
        val command = CheckoutSubmitCommand(
            idempotencyKey = resolveIdempotencyKey(
                items = cartSnapshot,
                paymentMethod = paymentMethod,
                amountPaid = effectiveAmountPaid,
                paymentLines = paymentLinesForSubmit,
                isSplitDpMode = state.isSplitDpMode,
                customerId = customerId,
                notes = notes,
                transactionDiscount = state.transactionDiscount,
                checkoutAttemptId = approvalAttemptId,
                managerApprovalId = managerApprovalId,
            ),
            items = resolvedItems,
            paymentMethod = paymentMethodForSubmit,
            amountPaid = effectiveAmountPaid.toPlainString(),
            payments = paymentLinesForSubmit,
            customerId = customerId,
            notes = notes,
            transactionDiscount = state.transactionDiscount,
            checkoutAttemptId = approvalAttemptId,
            managerApprovalId = managerApprovalId,
        )

        val persistedInFlight = pendingSubmissionPersistence?.saveInFlight(
            command = command,
            fingerprint = pendingOnlineCheckout?.fingerprint.orEmpty(),
            state = _uiState.value,
        ) ?: true
        if (!persistedInFlight) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isPreviewLoading = false,
                    errorEvent = UiText.DynamicString(UserFacingErrorMapper.message()),
                )
            }
            return
        }
        ambiguousOnlineCheckout = command
        executeOnlineCheckout(command)
    }

    private suspend fun executeOnlineCheckout(command: CheckoutSubmitCommand) {
        when (val result = checkoutRepository.submitCheckout(command)) {
            is RepositoryResult.Success -> {
                pendingOnlineCheckout = null
                ambiguousOnlineCheckout = null
                pendingSubmissionPersistence?.clear()
                val convertedQuotationId = _uiState.value.activeQuotationId
                if (!convertedQuotationId.isNullOrBlank()) {
                    viewModelScope.launch {
                        runCatching {
                            quotationRepository?.updateQuotationStatus(
                                id = convertedQuotationId,
                                status = com.tbterminal.app.data.model.QuotationStatus.CONVERTED,
                                convertedTransactionId = result.data.transactionId,
                            )
                        }
                    }
                }
                _checkoutEvents.tryEmit(CheckoutEvent.NavigateToReceipt(result.data.transactionId))
                _uiState.update { current -> current.resetAfterSuccessfulCheckout() }
                cartDraftCoordinator?.flushNow(_uiState.value)
            }
            is RepositoryResult.Error -> {
                if (ServerUnavailabilityClassifier.isServerUnavailable(result)) {
                    markBackendUnreachableAndSyncState()
                }
                val ambiguous = result.isAmbiguousCheckoutFailure()
                if (!ambiguous) {
                    pendingOnlineCheckout = null
                    ambiguousOnlineCheckout = null
                    pendingSubmissionPersistence?.clear()
                } else {
                    pendingSubmissionPersistence?.markAmbiguous(
                        command = command,
                        fingerprint = pendingOnlineCheckout?.fingerprint.orEmpty(),
                        state = _uiState.value,
                    )
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
                if (ServerUnavailabilityClassifier.isServerUnavailable(result)) {
                    markBackendUnreachableAndSyncState()
                    pendingSubmissionPersistence?.markAmbiguous(
                        command = command,
                        fingerprint = pendingOnlineCheckout?.fingerprint.orEmpty(),
                        state = _uiState.value,
                    )
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
                } else {
                    pendingOnlineCheckout = null
                    ambiguousOnlineCheckout = null
                    pendingSubmissionPersistence?.clear()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPreviewLoading = false,
                            hasAmbiguousCheckout = false,
                            errorEvent = UiText.DynamicString(UserFacingErrorMapper.message()),
                        )
                    }
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
                            state.resetAfterSuccessfulCheckout().copy(
                                errorEvent = UiText.DynamicString(
                                    "Transaksi disimpan lokal. Data akan disinkronkan saat server tersedia."
                                )
                            )
                        }
                        cartDraftCoordinator?.flushNow(_uiState.value)
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

        return when (repository.currentBackendStatus()) {
            BackendStatus.CONNECTED -> {
                if (repository.isBackendHealthStale()) {
                    val refreshedStatus = repository.refreshBackendHealth(force = false)
                    latestOfflineStatus = status.copy(
                        backendStatus = refreshedStatus,
                        isServerReachable = refreshedStatus == BackendStatus.CONNECTED
                    )
                    refreshedStatus != BackendStatus.CONNECTED
                } else {
                    false
                }
            }
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

    /**
     * Pre-flight check for cash mutations: trusts fresh UNREACHABLE without probing,
     * otherwise runs a quick /api/readiness probe so dead local forwards (e.g. adb reverse)
     * switch to the local path before sending the mutation request.
     */
    private suspend fun shouldUseLocalForMutation(): Boolean {
        val repository = offlineStatusRepository ?: return false
        if (!repository.isOnline()) return true
        val status = latestOfflineStatus
        if (status.appDataMode == AppDataMode.OFFLINE_ONLY) return true
        if (!status.isOnline || !status.isInternetOnline || status.internetStatus == InternetStatus.OFFLINE) {
            return true
        }
        if (repository.currentBackendStatus() == BackendStatus.UNREACHABLE && !repository.isBackendHealthStale()) {
            return true
        }
        val refreshedStatus = repository.refreshBackendHealth(force = true)
        latestOfflineStatus = status.copy(
            backendStatus = refreshedStatus,
            isServerReachable = refreshedStatus == BackendStatus.CONNECTED
        )
        return refreshedStatus != BackendStatus.CONNECTED
    }

    private fun markBackendUnreachableAndSyncState() {
        offlineStatusRepository?.markBackendUnreachable()
        latestOfflineStatus = latestOfflineStatus.copy(
            backendStatus = if (latestOfflineStatus.isInternetOnline) BackendStatus.UNREACHABLE else BackendStatus.UNKNOWN,
            isServerReachable = false,
        )
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
                if (state.deliveryFleet.isNotBlank()) append(" | Armada: ${state.deliveryFleet.trim()}")
                if (state.deliveryCourier.isNotBlank()) append(" | Supir: ${state.deliveryCourier.trim()}")
                if (state.deliveryRecipientPhone.isNotBlank()) append(" | Telp: ${state.deliveryRecipientPhone.trim()}")
                if (state.deliveryVehiclePlate.isNotBlank()) append(" | Plat: ${state.deliveryVehiclePlate.trim()}")
                if (state.deliverySpecialInstructions.isNotBlank()) append(" | Catatan: ${state.deliverySpecialInstructions.trim()}")
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
        paymentLines: List<CheckoutPaymentLine>?,
        isSplitDpMode: Boolean,
        customerId: String?,
        notes: String?,
        transactionDiscount: CheckoutDiscount?,
        checkoutAttemptId: String?,
        managerApprovalId: String?,
    ): String {
        val fingerprint = buildOnlineCheckoutIdempotencyFingerprint(
            items = items,
            paymentMethod = paymentMethod,
            amountPaid = amountPaid,
            paymentLines = paymentLines,
            isSplitDpMode = isSplitDpMode,
            customerId = customerId,
            notes = notes,
            transactionDiscount = transactionDiscount,
            checkoutAttemptId = checkoutAttemptId,
            managerApprovalId = managerApprovalId,
        )
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
        pendingSubmissionPersistence?.let { persistence ->
            viewModelScope.launch { persistence.clear() }
        }
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
        canOpenCashSession = canOpenCashSession,
        heldCarts = heldCarts,
        storeName = storeName,
        storeNpwp = storeNpwp,
        taxEnabled = taxEnabled,
        taxMode = taxMode,
        taxRate = taxRate,
        taxLabel = taxLabel,
        cashRoundingUnit = cashRoundingUnit,
        cashRoundingRule = cashRoundingRule,
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

    private fun RepositoryResult.Error.isAmbiguousCheckoutFailure(): Boolean {
        val normalized = code.uppercase()
        return normalized.startsWith("HTTP_5") || normalized in setOf(
            "NETWORK_TIMEOUT", "NETWORK_UNAVAILABLE", "CONNECTION_FAILED",
            "CONNECTION_INTERRUPTED", "INVALID_RESPONSE", "EMPTY_BODY"
        )
    }

    private suspend fun restoreLocalOpenCashSessionIfAvailable(): Boolean {
        val dataSource = cashSessionLocalDataSource ?: return false
        val currentCashierUserId = resolveCashierUserId()
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

        val subtotal = state.subtotal
        val discount = state.totalDiscount
        val total = state.finalTotal
        val selectedCustomer = state.selectedCustomer
        val splitValidation = if (state.isSplitPaymentEnabled) {
            validateSplitCheckoutPayment(
                lines = state.splitPaymentLines,
                total = state.finalTotal,
                isDpMode = state.isSplitDpMode,
                customerSelected = selectedCustomer != null
            )
        } else null
        if (splitValidation?.error != null) {
            return LocalCheckoutCommandBuildResult.Failed(splitValidation.error)
        }
        val paidAmount = if (state.isSplitPaymentEnabled) {
            if (state.isSplitDpMode) splitValidation!!.totalTendered else state.finalTotal
        } else {
            resolveLocalCheckoutPaidAmount(state)
                ?: return LocalCheckoutCommandBuildResult.Failed("Nominal bayar belum valid.")
        }

        if (subtotal < BigDecimal.ZERO || discount < BigDecimal.ZERO || total < BigDecimal.ZERO) {
            return LocalCheckoutCommandBuildResult.Failed("Nilai transaksi lokal belum valid.")
        }
        if (paidAmount < BigDecimal.ZERO) {
            return LocalCheckoutCommandBuildResult.Failed("Nominal bayar tidak boleh negatif.")
        }
        if (paidAmount > total) {
            return LocalCheckoutCommandBuildResult.Failed("Nominal bayar melebihi total transaksi.")
        }

        if (!state.isSplitPaymentEnabled && state.selectedPaymentMethod.requiresRegisteredCustomer() && selectedCustomer == null) {
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
            val quantity = cartItem.quantity.toBigDecimal()
            val unitResolution = resolveOfflineCheckoutItemUnit(
                productName = cartItem.productName,
                quantity = cartItem.quantity,
                isSecondaryUnit = cartItem.isSecondaryUnit,
                cartBaseUnitId = cartItem.baseUnitId,
                cartSecondaryUnitId = cartItem.secondaryUnitId,
                cartSecondaryUnitFactor = cartItem.secondaryUnitFactor,
                cachedBaseUnitId = productLookup.baseUnitServerId,
                cachedSecondaryUnitId = productLookup.secondaryUnitServerId,
                cachedSecondaryUnitFactor = productLookup.secondaryUnitFactor
            )
            if (unitResolution is OfflineItemUnitResolution.Error) {
                return LocalCheckoutCommandBuildResult.Failed(unitResolution.reason)
            }
            unitResolution as OfflineItemUnitResolution.Success
            val totalProductBaseQty = state.cartItems
                .filter { it.productId == cartItem.productId }
                .fold(BigDecimal.ZERO) { acc, item ->
                    val itemFactor = if (item.isSecondaryUnit) {
                        item.secondaryUnitFactor ?: productLookup.secondaryUnitFactor ?: BigDecimal.ONE
                    } else {
                        BigDecimal.ONE
                    }
                    acc.add(item.quantity.toBigDecimal().multiply(itemFactor))
                }
            if (productLookup.stock < totalProductBaseQty) {
                return LocalCheckoutCommandBuildResult.Failed(
                    "Stok lokal ${cartItem.productName} tidak cukup."
                )
            }

            val lineSubtotal = cartItem.unitPrice
                .multiply(quantity)
                .setScale(2, java.math.RoundingMode.HALF_UP)
            val lineCogs = productLookup.cogsAtTransaction
                .multiply(unitResolution.conversionFactor)
                .setScale(2, java.math.RoundingMode.HALF_UP)
            items += LocalCheckoutItemCommand(
                productLocalId = productLookup.localId,
                productServerId = cartItem.productId,
                productNameSnapshot = cartItem.productName,
                skuSnapshot = cartItem.sku.takeIf(String::isNotBlank),
                unitNameSnapshot = cartItem.unitName.takeIf(String::isNotBlank),
                quantity = quantity,
                baseQuantity = unitResolution.baseQuantity,
                conversionFactor = unitResolution.conversionFactor,
                priceAtTransaction = cartItem.unitPrice,
                cogsAtTransaction = lineCogs,
                discount = BigDecimal.ZERO,
                subtotal = lineSubtotal,
                note = cartItem.note,
                unitServerId = unitResolution.unitServerId,
                baseQuantityExact = unitResolution.baseQuantity,
                conversionFactorExact = unitResolution.conversionFactor
            )
        }

        val remainingAmount = total.subtract(paidAmount).coerceAtLeast(BigDecimal.ZERO)
        val localSplitLines = if (state.isSplitPaymentEnabled) {
            val remainingCashApplied = if (!state.isSplitDpMode) {
                state.finalTotal.subtract(splitValidation!!.nonCashPaid).coerceAtLeast(BigDecimal.ZERO)
            } else null
            state.splitPaymentLines.mapNotNull { line ->
                val rawAmt = parseWholeRupiahInput(line.amountInput) ?: return@mapNotNull null
                val appliedAmt = if (line.method == PaymentMethod.TUNAI && remainingCashApplied != null) {
                    remainingCashApplied
                } else rawAmt
                LocalCheckoutPaymentLineCommand(
                    method = line.method.apiValue(),
                    amount = appliedAmt,
                    reference = line.reference.trim().takeIf { it.isNotEmpty() }
                )
            }
        } else null
        return LocalCheckoutCommandBuildResult.Success(
            LocalCheckoutCommand(
                cashierUserId = activeSession.cashierUserId,
                cashSessionLocalId = cashSessionLocalId,
                cashSessionServerId = activeSession.serverId,
                customerLocalId = customerLookup?.localId,
                customerServerId = selectedCustomer?.id,
                customerName = selectedCustomer?.name ?: typedCustomerName.takeIf(String::isNotBlank),
                paymentMethod = if (state.isSplitPaymentEnabled) {
                    if (state.isSplitDpMode) "dp" else "tunai"
                } else state.selectedPaymentMethod.apiValue(),
                payments = localSplitLines,
                subtotal = subtotal,
                discount = discount,
                taxEnabled = state.taxEnabled,
                taxMode = if (state.taxEnabled) state.taxMode else null,
                taxRate = if (state.taxEnabled) state.taxRate else BigDecimal.ZERO,
                taxLabel = if (state.taxEnabled) state.taxLabel else null,
                taxableBase = if (state.taxEnabled) state.taxableBase else BigDecimal.ZERO,
                taxAmount = if (state.taxEnabled) state.taxAmount else BigDecimal.ZERO,
                roundingAmount = state.roundingAmount,
                total = total,
                paidAmount = paidAmount,
                remainingAmount = remainingAmount,
                items = items,
                note = checkoutCustomerNote(typedCustomerName)
            )
        )
    }

    private fun resolveLocalCheckoutPaidAmount(state: CheckoutUiState): BigDecimal? {
        return when (state.selectedPaymentMethod) {
            PaymentMethod.HUTANG -> BigDecimal.ZERO
            PaymentMethod.DP -> parseWholeRupiahInput(state.amountPaidInput)
            else -> state.finalTotal
        }
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
            offlineSyncScheduler: OfflineSyncScheduler? = null,
            localAppSettingsDataSource: LocalAppSettingsDataSource? = null,
            systemRepository: SystemRepository? = null,
            quotationRepository: com.tbterminal.app.data.repository.QuotationRepository? = null,
            actorRole: String = "KASIR",
            pinUnlockService: PinUnlockService? = null,
            pendingSubmissionStore: PendingSubmissionStore? = null,
            checkoutDraftStore: CheckoutDraftStore? = null,
            productLocalDataSource: ProductLocalDataSource? = null,
            customerLocalDataSource: CustomerLocalDataSource? = null,
            localCashSessionService: LocalCashSessionService? = null,
            cashierUserId: String? = null,
            sessionManager: SessionManager? = null,
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
                    offlineSyncScheduler = offlineSyncScheduler,
                    localAppSettingsDataSource = localAppSettingsDataSource,
                    systemRepository = systemRepository,
                    quotationRepository = quotationRepository,
                    actorRole = actorRole,
                    pinUnlockService = pinUnlockService,
                    elapsedRealtimeMillis = { defaultElapsedRealtimeMillis() },
                    pendingSubmissionStore = pendingSubmissionStore,
                    checkoutDraftStore = checkoutDraftStore,
                    productLocalDataSource = productLocalDataSource,
                    customerLocalDataSource = customerLocalDataSource,
                    localCashSessionService = localCashSessionService,
                    cashierUserId = cashierUserId,
                    sessionManager = sessionManager,
                )
            }
        }

        private const val MAX_CASH_INPUT_LENGTH = 12
        private const val DEFAULT_LOCKOUT_FALLBACK_MILLIS = 30_000L
    }

    private fun CheckoutUiState.withCart(items: List<CartItem>): CheckoutUiState =
        recalculateCheckoutState(items, transactionDiscount)

    private fun CheckoutUiState.withTransactionDiscount(discount: CheckoutDiscount?): CheckoutUiState =
        recalculateCheckoutState(cartItems, discount)

    private fun PaymentMethod.requiresRegisteredCustomer(): Boolean {
        return this == PaymentMethod.HUTANG || this == PaymentMethod.DP
    }

    private suspend fun CashSession.toResolvedRemoteActiveCashSessionUi(): ActiveCashSessionUi? {
        val mirroredEntity = mirrorCashSessionEntitySafely(this)
        if (mirroredEntity != null &&
            mirroredEntity.hasUnsyncedLocalChanges() &&
            !mirroredEntity.status.equals("OPEN", ignoreCase = true)
        ) {
            return runSuspendCatching {
                cashSessionLocalDataSource?.getLatestOpenSession(resolveCashierUserId())
            }.getOrNull()?.toActiveCashSessionUi()
        }
        return toActiveCashSessionUi(localId = mirroredEntity?.localId)
    }

    private suspend fun CashSession.toMirroredActiveCashSessionUi(): ActiveCashSessionUi {
        val localId = mirrorCashSessionSafely(this)
        return toActiveCashSessionUi(localId = localId)
    }

    private suspend fun mirrorCashSessionSafely(session: CashSession): Long? {
        return mirrorCashSessionEntitySafely(session)?.localId
    }

    private suspend fun mirrorCashSessionEntitySafely(session: CashSession): LocalCashSessionEntity? {
        return runSuspendCatching {
            cashSessionLocalDataSource?.mirrorRemoteSession(session)
        }.getOrNull()
    }

    private suspend fun reconcileNoActiveRemoteSessionSafely() {
        runSuspendCatching {
            cashSessionLocalDataSource?.reconcileNoActiveRemoteSession(
                cashierUserId = resolveCashierUserId()
            )
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

private fun CheckoutUiState.hasDiscountRequest(): Boolean =
    transactionDiscount != null || cartItems.any { it.discountRequest != null }

internal fun CheckoutUiState.recalculateCheckoutState(
    items: List<CartItem>,
    discount: CheckoutDiscount?,
    deliveryFeeAmount: BigDecimal = if (isDeliveryEnabled) deliveryFee else BigDecimal.ZERO,
    preserveAmbiguousCheckout: Boolean = false,
): CheckoutUiState {
    val repricedItems = repriceCartItemsForCheckout(
        items = items,
        isContractorCustomer = selectedCustomer?.isContractor == true,
    )
    val totals = estimateCheckoutTotals(repricedItems, discount)
    val taxCalc = estimateTaxCalculation(
        netGoodsTotal = totals.netTotal,
        taxEnabled = taxEnabled,
        taxRate = taxRate,
        taxMode = taxMode,
        taxLabel = taxLabel,
    )
    val totalBeforeRounding = taxCalc.totalWithTax.add(deliveryFeeAmount).coerceAtLeast(BigDecimal.ZERO)
    val hasCashPayment = if (isSplitPaymentEnabled) {
        splitPaymentLines.any {
            it.method == PaymentMethod.TUNAI &&
                (parseWholeRupiahInput(it.amountInput) ?: BigDecimal.ZERO) > BigDecimal.ZERO
        }
    } else {
        selectedPaymentMethod == PaymentMethod.TUNAI
    }
    val roundingCalc = estimateCashRoundingCalculation(
        totalBeforeRounding = totalBeforeRounding,
        hasCashPayment = hasCashPayment,
        roundingUnit = cashRoundingUnit,
        roundingRule = cashRoundingRule,
    )
    val calculatedFinalTotal = roundingCalc.finalTotal
    return copy(
        cartItems = repricedItems,
        subtotal = totals.grossSubtotal,
        itemDiscountTotal = totals.itemDiscountTotal,
        transactionDiscount = discount,
        transactionDiscountAmount = totals.transactionDiscountAmount,
        totalDiscount = totals.totalDiscountAmount,
        taxableBase = taxCalc.taxableBase,
        taxAmount = taxCalc.taxAmount,
        roundingAmount = roundingCalc.roundingAmount,
        finalTotal = calculatedFinalTotal,
        checkoutPreview = null,
        approvedManagerApprovalId = null,
        hasAmbiguousCheckout = if (preserveAmbiguousCheckout) hasAmbiguousCheckout else false,
        isPreviewLoading = false,
        amountPaidInput = when (selectedPaymentMethod) {
            PaymentMethod.HUTANG -> "0"
            PaymentMethod.DP -> amountPaidInput
            else -> calculatedFinalTotal.toWholeRupiahInputText()
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
        startingCash = startingCash.toBigDecimal(),
        status = status
    )
}

private fun Long.toOffsetDateTimeString(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.of("Asia/Jakarta"))
        .toOffsetDateTime()
        .toString()
}

private fun com.tbterminal.app.data.model.ProductStock.toCheckoutProduct(): Product = Product(
    productId = productId,
    name = productName,
    unitPrice = priceRetail,
    sku = sku,
    barcode = sku,
    unitName = unitName,
    categoryName = categoryName,
    stockQty = quantity,
    secondaryUnitId = secondaryUnitId,
    secondaryUnitName = secondaryUnitName,
    secondaryUnitFactor = secondaryUnitFactor,
    contractorPrice = priceContractor,
    priceTiers = priceTiers,
    masterDiscount = discount,
    photoUrl = photoUrl,
    baseUnitId = baseUnitId
)

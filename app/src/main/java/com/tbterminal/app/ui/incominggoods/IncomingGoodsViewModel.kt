package com.tbterminal.app.ui.incominggoods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.CreatePurchaseCommand
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.PurchaseItemCommand
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.data.numbering.DocumentNumberGenerator
import com.tbterminal.app.data.numbering.DocumentNumberType
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.common.viewModelFactory
import java.math.BigDecimal
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class IncomingGoodsViewModel(
    private val inventoryRepository: InventoryRepository,
    private val purchasingRepository: PurchasingRepository,
    private val documentNumberGenerator: DocumentNumberGenerator,
    private val initialProductId: String? = null,
    private val autoSelectFirst: Boolean = true
) : ViewModel() {
    private val _uiState = MutableStateFlow(IncomingGoodsUiState(isLoadingProducts = true))
    val uiState: StateFlow<IncomingGoodsUiState> = _uiState.asStateFlow()
    private var productSearchJob: Job? = null
    private var initialSelectionApplied = false

    init {
        refresh()
    }

    fun refresh() {
        loadProducts()
        loadSuppliers()
    }

    fun loadProducts() {
        viewModelScope.launch {
            val query = _uiState.value.productSearchQuery.trim().takeIf(String::isNotBlank)
            _uiState.update { it.copy(isLoadingProducts = true, errorMessage = null) }
            when (
                val result = inventoryRepository.getProductStocks(
                    page = 1,
                    limit = INCOMING_GOODS_PRODUCT_LIMIT,
                    search = query
                )
            ) {
                is RepositoryResult.Success -> applyProducts(result.data.data.filter(ProductStock::isActive))
                is RepositoryResult.Error -> setLoadError(result.message)
                is RepositoryResult.Exception -> setLoadError("Koneksi bermasalah. Produk gagal dimuat.")
            }
        }
    }

    fun loadSuppliers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSuppliers = true, errorMessage = null) }
            when (val result = purchasingRepository.getSuppliers(page = 1, limit = INCOMING_GOODS_SUPPLIER_LIMIT)) {
                is RepositoryResult.Success -> applySuppliers(result.data.data.filter(Supplier::isActive))
                is RepositoryResult.Error -> setSupplierError(result.message)
                is RepositoryResult.Exception -> setSupplierError("Koneksi bermasalah. Supplier gagal dimuat.")
            }
        }
    }

    fun onProductSearchChanged(query: String) {
        _uiState.update { it.copy(productSearchQuery = query, currentPage = 1) }
        productSearchJob?.cancel()
        productSearchJob = viewModelScope.launch {
            delay(350)
            loadProducts()
        }
    }

    fun onCategoryFilterChanged(categoryName: String?) {
        _uiState.update { it.copy(categoryFilter = categoryName, currentPage = 1) }
    }

    fun nextPage() {
        _uiState.update { state ->
            state.copy(currentPage = (state.tablePage + 1).coerceAtMost(state.totalTablePages))
        }
    }

    fun previousPage() {
        _uiState.update { state ->
            state.copy(currentPage = (state.tablePage - 1).coerceAtLeast(1))
        }
    }

    fun selectProduct(product: ProductStock) {
        _uiState.update { state ->
            val nextState = state.copy(
                selectedProduct = product,
                buyPriceInput = state.buyPriceInput.ifBlank { product.priceBuy.toInputText() },
                errorMessage = null,
                message = null
            )
            nextState.withSyncedCashPayment()
        }
    }

    fun selectSupplier(supplier: Supplier) {
        _uiState.update { it.copy(selectedSupplier = supplier, errorMessage = null, message = null) }
    }

    fun onSupplierNameChanged(value: String) {
        _uiState.update { it.copy(newSupplierNameInput = value) }
    }

    fun createSupplier() {
        val name = _uiState.value.newSupplierNameInput.trim()
        if (name.isBlank()) return setActionError("Nama supplier wajib diisi.")
        if (_uiState.value.isSavingSupplier) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingSupplier = true, errorMessage = null, message = null) }
            when (val result = purchasingRepository.createSupplier(name = name, paymentTermDays = DEFAULT_DUE_DAYS)) {
                is RepositoryResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            suppliers = (state.suppliers + result.data).distinctBy(Supplier::id),
                            selectedSupplier = result.data,
                            newSupplierNameInput = "",
                            isSavingSupplier = false,
                            message = "Supplier ${result.data.name} berhasil ditambahkan."
                        )
                    }
                }
                is RepositoryResult.Error -> setActionError(result.message)
                is RepositoryResult.Exception -> setActionError("Supplier gagal disimpan karena koneksi bermasalah.")
            }
        }
    }

    fun onInvoiceChanged(value: String) {
        _uiState.update { it.copy(invoiceNoInput = value) }
    }

    fun generateInvoiceNumber() {
        _uiState.update {
            it.copy(
                invoiceNoInput = documentNumberGenerator.generate(DocumentNumberType.IncomingGoods),
                errorMessage = null,
                message = null
            )
        }
    }

    fun onQuantityChanged(value: String) {
        _uiState.update { it.copy(quantityInput = value.decimalInput()).withSyncedCashPayment() }
    }

    fun onBuyPriceChanged(value: String) {
        _uiState.update { it.copy(buyPriceInput = value.decimalInput()).withSyncedCashPayment() }
    }

    fun onAmountPaidChanged(value: String) {
        _uiState.update { it.copy(amountPaidInput = value.decimalInput()) }
    }

    fun onDueDaysChanged(value: String) {
        _uiState.update { it.copy(dueDaysInput = value.filter(Char::isDigit)) }
    }

    fun onNotesChanged(value: String) {
        _uiState.update { it.copy(notesInput = value) }
    }

    fun onPaymentMethodChanged(method: IncomingPaymentMethod) {
        _uiState.update { state ->
            val amountPaid = when (method) {
                IncomingPaymentMethod.CASH,
                IncomingPaymentMethod.TRANSFER,
                IncomingPaymentMethod.QRIS -> state.total?.toInputText().orEmpty()
                IncomingPaymentMethod.DEBT -> BigDecimal.ZERO.toInputText()
                IncomingPaymentMethod.DOWN_PAYMENT -> state.amountPaidInput
            }
            state.copy(paymentMethod = method, amountPaidInput = amountPaid)
        }
    }

    fun submitIncomingGoods() {
        val state = _uiState.value
        val product = state.selectedProduct ?: return setActionError("Pilih produk yang akan direstok.")
        val supplier = state.selectedSupplier ?: return setActionError("Pilih supplier terlebih dahulu.")
        val quantity = state.quantity ?: return setActionError("Jumlah masuk wajib diisi dengan angka valid.")
        val buyPrice = state.buyPrice ?: return setActionError("Harga beli wajib diisi dengan angka valid.")
        val total = state.total ?: return setActionError("Total pembelian tidak valid.")
        val amountPaid = state.effectiveAmountPaid ?: return setActionError("Jumlah bayar tidak valid.")
        val dueDays = state.dueDays ?: return setActionError("Termin bayar wajib berupa angka.")

        if (quantity <= BigDecimal.ZERO) return setActionError("Jumlah masuk harus lebih dari nol.")
        if (buyPrice <= BigDecimal.ZERO) return setActionError("Harga beli harus lebih dari nol.")
        if (amountPaid < BigDecimal.ZERO) return setActionError("Jumlah bayar tidak boleh negatif.")
        if (amountPaid > total) return setActionError("Jumlah bayar tidak boleh melebihi total pembelian.")
        if (state.paymentMethod == IncomingPaymentMethod.DOWN_PAYMENT && amountPaid <= BigDecimal.ZERO) {
            return setActionError("Nominal DP harus lebih dari nol.")
        }
        if (state.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, message = null) }
            val command = CreatePurchaseCommand(
                supplierId = supplier.id,
                invoiceNo = state.invoiceNoInput.trim().takeIf(String::isNotBlank),
                paymentMethod = state.paymentMethod.apiValue,
                amountPaid = amountPaid,
                notes = state.notesInput.trim().takeIf(String::isNotBlank),
                dueDays = dueDays,
                items = listOf(
                    PurchaseItemCommand(
                        productId = product.productId,
                        qty = quantity,
                        price = buyPrice
                    )
                )
            )

            when (val result = purchasingRepository.createPurchase(command)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            quantityInput = "",
                            notesInput = "",
                            invoiceNoInput = "",
                            message = "Barang masuk berhasil dicatat untuk ${result.data.supplierName}."
                        )
                    }
                    loadProducts()
                }
                is RepositoryResult.Error -> setActionError(result.message)
                is RepositoryResult.Exception -> setActionError("Barang masuk gagal dicatat karena koneksi bermasalah.")
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(errorMessage = null, message = null) }
    }

    private fun applyProducts(products: List<ProductStock>) {
        _uiState.update { state ->
            val selectedProduct = resolveSelectedProduct(state, products)
            val categoryFilter = state.categoryFilter
                ?.takeIf { filter -> products.any { it.categoryName == filter } }
            state.copy(
                products = products,
                selectedProduct = selectedProduct,
                categoryFilter = categoryFilter,
                currentPage = state.currentPage.coerceAtLeast(1),
                buyPriceInput = state.buyPriceInput.ifBlank { selectedProduct?.priceBuy?.toInputText().orEmpty() },
                isLoadingProducts = false,
                errorMessage = null
            )
        }
        initialSelectionApplied = true
    }

    private fun resolveSelectedProduct(
        state: IncomingGoodsUiState,
        products: List<ProductStock>
    ): ProductStock? {
        val current = state.selectedProduct
            ?.let { selected -> products.firstOrNull { it.productId == selected.productId } }
        if (current != null) return current

        val initial = if (!initialSelectionApplied) {
            initialProductId?.let { id -> products.firstOrNull { it.productId == id } }
        } else {
            null
        }

        return initial ?: products.firstOrNull().takeIf { autoSelectFirst }
    }

    private fun applySuppliers(suppliers: List<Supplier>) {
        _uiState.update { state ->
            val selected = state.selectedSupplier
                ?.let { current -> suppliers.firstOrNull { it.id == current.id } }
                ?: suppliers.firstOrNull()

            state.copy(
                suppliers = suppliers,
                selectedSupplier = selected,
                isLoadingSuppliers = false,
                errorMessage = null
            )
        }
    }

    private fun setLoadError(message: String) {
        _uiState.update { it.copy(isLoadingProducts = false, errorMessage = message) }
    }

    private fun setSupplierError(message: String) {
        _uiState.update { it.copy(isLoadingSuppliers = false, errorMessage = message) }
    }

    private fun setActionError(message: String) {
        _uiState.update { it.copy(isSubmitting = false, isSavingSupplier = false, errorMessage = message) }
    }

    companion object {
        fun factory(
            inventoryRepository: InventoryRepository,
            purchasingRepository: PurchasingRepository,
            documentNumberGenerator: DocumentNumberGenerator,
            initialProductId: String? = null,
            autoSelectFirst: Boolean = true
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                IncomingGoodsViewModel(
                    inventoryRepository = inventoryRepository,
                    purchasingRepository = purchasingRepository,
                    documentNumberGenerator = documentNumberGenerator,
                    initialProductId = initialProductId,
                    autoSelectFirst = autoSelectFirst
                )
            }
        }
    }
}

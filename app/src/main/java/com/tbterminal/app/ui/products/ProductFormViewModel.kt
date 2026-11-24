package com.tbterminal.app.ui.products

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.media.ImageCompressor
import com.tbterminal.app.data.model.CreateProductCommand
import com.tbterminal.app.data.model.Product
import com.tbterminal.app.data.model.UpdateProductCommand
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.sync.NetworkConnectivity
import com.tbterminal.app.ui.common.viewModelFactory
import java.io.File
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductFormViewModel(
    private val inventoryRepository: InventoryRepository,
    private val productId: String? = null,
    private val imageCompressor: ImageCompressor? = null,
    private val networkConnectivity: NetworkConnectivity? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ProductFormUiState(
            isEditMode = productId != null,
            isLoading = true,
            isOnline = networkConnectivity?.isOnline() ?: true
        )
    )
    val uiState: StateFlow<ProductFormUiState> = _uiState.asStateFlow()

    init {
        if (networkConnectivity != null) {
            viewModelScope.launch {
                networkConnectivity.observeOnline().collect { online ->
                    _uiState.update { state -> state.copy(isOnline = online) }
                }
            }
        }
        loadInitialData()
    }

    fun onInputChanged(input: ProductFormInput) {
        _uiState.update { state -> state.copy(input = input, errorMessage = null) }
    }

    fun onPhotoPicked(uri: Uri, tempFileToDelete: File? = null) {
        val compressor = imageCompressor
        if (compressor == null) {
            tempFileToDelete?.delete()
            setPhotoError("Kompresor gambar tidak tersedia.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(photoStatus = ProductPhotoUploadStatus.Uploading, errorMessage = null) }
            try {
                val compressed = compressor.compressForUpload(uri).getOrElse { err ->
                    setPhotoError(err.message ?: "Gagal memproses foto produk.")
                    return@launch
                }
                _uiState.update { state ->
                    state.copy(
                        pendingPhoto = PendingProductPhoto(
                            previewUri = uri.toString(),
                            bytes = compressed.bytes,
                            fileName = compressed.fileName
                        ),
                        removePhotoRequested = false,
                        photoStatus = ProductPhotoUploadStatus.Idle,
                        errorMessage = null
                    )
                }
            } finally {
                tempFileToDelete?.delete()
            }
        }
    }

    fun onPhotoSelectionError(message: String) {
        _uiState.update { state ->
            state.copy(photoStatus = ProductPhotoUploadStatus.Error(message))
        }
    }

    fun onRemovePhoto() {
        _uiState.update { state ->
            state.copy(
                pendingPhoto = null,
                removePhotoRequested = !state.photoUrl.isNullOrBlank(),
                photoStatus = ProductPhotoUploadStatus.Idle,
                errorMessage = null
            )
        }
    }

    fun retryPhotoUpload() {
        val state = _uiState.value
        val targetId = state.createdProductId ?: productId ?: return
        if (state.isSaving || state.photoStatus is ProductPhotoUploadStatus.Uploading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            applyPendingPhotoChanges(targetId)
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving || state.photoStatus is ProductPhotoUploadStatus.Uploading) return
        if (state.createdProductId != null) {
            retryPhotoUpload()
            return
        }
        val input = state.input

        val priceBuy = input.priceBuy.toProductDecimalOrNull()
        val priceRetail = input.priceRetail.toProductDecimalOrNull()
        val priceContractor = input.priceContractor.toProductDecimalOrNull()
        val minStock = input.minStock.toProductDecimalOrNull()
        val secondaryUnitFactor = input.secondaryUnitFactor.toProductDecimalOrNull()

        val validationError = validateProductFormInput(input, state.isEditMode)
        when {
            validationError != null -> setError(validationError)
            else -> submit(
                priceBuy = requireNotNull(priceBuy),
                priceRetail = requireNotNull(priceRetail),
                priceContractor = requireNotNull(priceContractor),
                minStock = requireNotNull(minStock),
                secondaryUnitFactor = secondaryUnitFactor
            )
        }
    }

    fun resetSavedState() {
        _uiState.update { state -> state.copy(isSaved = false) }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { state -> state.copy(isLoading = true, errorMessage = null) }

            val categoriesResult = inventoryRepository.getCategories()
            val unitsResult = inventoryRepository.getUnits()
            val productResult = productId?.let { id -> inventoryRepository.getProduct(id) }

            val categories = when (categoriesResult) {
                is RepositoryResult.Success -> categoriesResult.data
                is RepositoryResult.Error -> return@launch setLoadError(categoriesResult.message)
                is RepositoryResult.Exception -> return@launch setLoadError("Kategori gagal dimuat karena koneksi bermasalah.")
            }
            val units = when (unitsResult) {
                is RepositoryResult.Success -> unitsResult.data
                is RepositoryResult.Error -> return@launch setLoadError(unitsResult.message)
                is RepositoryResult.Exception -> return@launch setLoadError("Satuan gagal dimuat karena koneksi bermasalah.")
            }
            val product = when (productResult) {
                null -> null
                is RepositoryResult.Success -> productResult.data
                is RepositoryResult.Error -> return@launch setLoadError(productResult.message)
                is RepositoryResult.Exception -> return@launch setLoadError("Produk gagal dimuat karena koneksi bermasalah.")
            }

            _uiState.update { state ->
                state.copy(
                    categories = categories,
                    units = units,
                    input = product?.toFormInput()
                        ?: state.input.copy(
                            categoryId = categories.firstOrNull()?.id.orEmpty(),
                            baseUnitId = units.firstOrNull()?.id.orEmpty()
                        ),
                    photoUrl = product?.photoUrl,
                    isLoading = false,
                    errorMessage = null
                )
            }
        }
    }

    private fun submit(
        priceBuy: BigDecimal,
        priceRetail: BigDecimal,
        priceContractor: BigDecimal,
        minStock: BigDecimal,
        secondaryUnitFactor: BigDecimal?
    ) {
        viewModelScope.launch {
            val input = _uiState.value.input
            _uiState.update { state -> state.copy(isSaving = true, errorMessage = null) }

            val result = if (productId == null) {
                inventoryRepository.createProduct(
                    CreateProductCommand(
                        categoryId = input.categoryId,
                        baseUnitId = input.baseUnitId,
                        sku = normalizeProductSku(input.sku),
                        name = input.name.trim(),
                        priceBuy = priceBuy,
                        priceRetail = priceRetail,
                        priceContractor = priceContractor,
                        discount = BigDecimal.ZERO,
                        minStock = minStock,
                        secondaryUnitId = input.secondaryUnitId.takeIf { input.usesSecondaryUnit },
                        secondaryUnitFactor = secondaryUnitFactor.takeIf { input.usesSecondaryUnit }
                    )
                )
            } else {
                inventoryRepository.updateProduct(
                    id = productId,
                    command = UpdateProductCommand(
                        categoryId = input.categoryId,
                        baseUnitId = input.baseUnitId,
                        name = input.name.trim(),
                        priceBuy = priceBuy,
                        priceRetail = priceRetail,
                        priceContractor = priceContractor,
                        discount = BigDecimal.ZERO,
                        minStock = minStock,
                        secondaryUnitId = input.secondaryUnitId.takeIf { input.usesSecondaryUnit },
                        secondaryUnitFactor = secondaryUnitFactor.takeIf { input.usesSecondaryUnit }
                    )
                )
            }

            when (result) {
                is RepositoryResult.Success -> {
                    val targetId = result.data.id
                    if (productId == null) {
                        _uiState.update { state -> state.copy(createdProductId = targetId) }
                    }
                    applyPendingPhotoChanges(targetId)
                }
                is RepositoryResult.Error -> setError(result.message)
                is RepositoryResult.Exception -> setError("Produk gagal disimpan karena koneksi bermasalah.")
            }
        }
    }

    private suspend fun applyPendingPhotoChanges(targetProductId: String) {
        val state = _uiState.value
        val pending = state.pendingPhoto
        when {
            pending != null -> {
                _uiState.update { it.copy(photoStatus = ProductPhotoUploadStatus.Uploading) }
                when (val res = inventoryRepository.uploadProductPhoto(targetProductId, pending.bytes, pending.fileName)) {
                    is RepositoryResult.Success -> {
                        _uiState.update {
                            it.copy(
                                photoUrl = res.data,
                                pendingPhoto = null,
                                removePhotoRequested = false,
                                photoStatus = ProductPhotoUploadStatus.Idle,
                                isSaving = false,
                                isSaved = true
                            )
                        }
                    }
                    is RepositoryResult.Error -> setPhotoError(res.message)
                    is RepositoryResult.Exception -> setPhotoError("Foto produk gagal diunggah karena koneksi bermasalah.")
                }
            }
            state.removePhotoRequested && !state.photoUrl.isNullOrBlank() -> {
                _uiState.update { it.copy(photoStatus = ProductPhotoUploadStatus.Uploading) }
                when (val res = inventoryRepository.deleteProductPhoto(targetProductId)) {
                    is RepositoryResult.Success -> {
                        _uiState.update {
                            it.copy(
                                photoUrl = null,
                                removePhotoRequested = false,
                                photoStatus = ProductPhotoUploadStatus.Idle,
                                isSaving = false,
                                isSaved = true
                            )
                        }
                    }
                    is RepositoryResult.Error -> setPhotoError(res.message)
                    is RepositoryResult.Exception -> setPhotoError("Foto produk gagal dihapus karena koneksi bermasalah.")
                }
            }
            else -> {
                _uiState.update {
                    it.copy(
                        photoStatus = ProductPhotoUploadStatus.Idle,
                        isSaving = false,
                        isSaved = true
                    )
                }
            }
        }
    }

    private fun setLoadError(message: String) {
        _uiState.update { state ->
            state.copy(isLoading = false, errorMessage = message)
        }
    }

    private fun setError(message: String) {
        _uiState.update { state ->
            state.copy(isSaving = false, errorMessage = message)
        }
    }

    private fun setPhotoError(message: String) {
        _uiState.update { state ->
            state.copy(
                isSaving = false,
                photoStatus = ProductPhotoUploadStatus.Error(message)
            )
        }
    }

    companion object {
        fun factory(
            inventoryRepository: InventoryRepository,
            productId: String? = null,
            imageCompressor: ImageCompressor? = null,
            networkConnectivity: NetworkConnectivity? = null
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                ProductFormViewModel(
                    inventoryRepository = inventoryRepository,
                    productId = productId,
                    imageCompressor = imageCompressor,
                    networkConnectivity = networkConnectivity
                )
            }
        }
    }
}

private fun Product.toFormInput(): ProductFormInput {
    return ProductFormInput(
        sku = sku,
        name = name,
        categoryId = categoryId,
        baseUnitId = baseUnitId,
        usesSecondaryUnit = secondaryUnitId != null && secondaryUnitFactor != null,
        secondaryUnitId = secondaryUnitId.orEmpty(),
        secondaryUnitFactor = secondaryUnitFactor?.stripTrailingZeros()?.toPlainString().orEmpty(),
        priceBuy = priceBuy.toPlainString(),
        priceRetail = priceRetail.toPlainString(),
        priceContractor = priceContractor.toPlainString(),
        minStock = minStock.toPlainString()
    )
}

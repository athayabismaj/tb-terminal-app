package com.tbterminal.app.ui.products

import android.app.Application
import android.net.Uri
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.tbterminal.app.data.media.CompressedImage
import com.tbterminal.app.data.media.ImageCompressor
import com.tbterminal.app.data.model.CreateProductCommand
import com.tbterminal.app.data.model.Product
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.data.model.ProductCategoryPage
import com.tbterminal.app.data.model.ProductCsvImportResult
import com.tbterminal.app.data.model.ProductCsvPreview
import com.tbterminal.app.data.model.ProductDetail
import com.tbterminal.app.data.model.ProductPage
import com.tbterminal.app.data.model.ProductStockPage
import com.tbterminal.app.data.model.ProductUnit
import com.tbterminal.app.data.model.ProductUnitPage
import com.tbterminal.app.data.model.StockAdjustmentPage
import com.tbterminal.app.data.model.StockCard
import com.tbterminal.app.data.model.UpdateProductCommand
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.sync.NetworkConnectivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ProductFormViewModelPhotoTest {

    @Test
    fun `add product upload failure stores createdProductId and retry uploads without recreating product`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val repo = FakeFormInventoryRepository(failFirstUpload = true)
            val compressor = FakeImageCompressor()
            val connectivity = FakeConnectivity(initialOnline = true)

            val vm = ProductFormViewModel(
                inventoryRepository = repo,
                productId = null,
                imageCompressor = compressor,
                networkConnectivity = connectivity
            )
            vm.onInputChanged(
                vm.uiState.value.input.copy(
                    sku = "SKU-100",
                    name = "Cat Tembok",
                    priceBuy = "40000",
                    priceRetail = "50000",
                    priceContractor = "45000",
                    minStock = "5"
                )
            )
            vm.onPhotoPicked(Uri.parse("content://media/picker/1"))

            vm.save()

            assertEquals(1, repo.createCalls)
            assertEquals(1, repo.uploadCalls)
            assertEquals("prod-created-1", vm.uiState.value.createdProductId)
            assertFalse(vm.uiState.value.isSaved)
            assertTrue(vm.uiState.value.photoStatus is ProductPhotoUploadStatus.Error)

            vm.retryPhotoUpload()

            assertEquals(1, repo.createCalls)
            assertEquals(2, repo.uploadCalls)
            assertTrue(vm.uiState.value.isSaved)
            assertEquals("https://res.cloudinary.com/demo/image/upload/v2/cat.jpg", vm.uiState.value.photoUrl)
            assertNull(vm.uiState.value.pendingPhoto)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `edit product removes existing photo only when save is pressed and tracks offline connectivity`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val existing = sampleProduct(id = "prod-edit-1", photoUrl = "https://res.cloudinary.com/demo/image/upload/v1/old.jpg")
            val repo = FakeFormInventoryRepository(existingProduct = existing)
            val connectivity = FakeConnectivity(initialOnline = true)

            val vm = ProductFormViewModel(
                inventoryRepository = repo,
                productId = "prod-edit-1",
                imageCompressor = FakeImageCompressor(),
                networkConnectivity = connectivity
            )

            assertEquals(existing.photoUrl, vm.uiState.value.photoUrl)
            vm.onRemovePhoto()
            assertEquals(0, repo.deletePhotoCalls)
            assertTrue(vm.uiState.value.removePhotoRequested)

            connectivity.onlineFlow.value = false
            assertFalse(vm.uiState.value.isOnline)

            connectivity.onlineFlow.value = true
            vm.save()

            assertEquals(1, repo.updateCalls)
            assertEquals(1, repo.deletePhotoCalls)
            assertTrue(vm.uiState.value.isSaved)
            assertNull(vm.uiState.value.photoUrl)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeConnectivity(initialOnline: Boolean) : NetworkConnectivity {
        val onlineFlow = MutableStateFlow(initialOnline)
        override fun isOnline(): Boolean = onlineFlow.value
        override fun observeOnline(): Flow<Boolean> = onlineFlow
    }

    private class FakeImageCompressor : ImageCompressor {
        override suspend fun compressForUpload(uri: Uri): Result<CompressedImage> =
            Result.success(CompressedImage(bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())))
    }

    private class FakeFormInventoryRepository(
        private val existingProduct: Product? = null,
        private val failFirstUpload: Boolean = false
    ) : InventoryRepository {
        var createCalls = 0
        var updateCalls = 0
        var uploadCalls = 0
        var deletePhotoCalls = 0

        override suspend fun getCategories(): RepositoryResult<List<ProductCategory>> =
            RepositoryResult.Success(listOf(ProductCategory("cat-1", "Semen", "")))

        override suspend fun getUnits(): RepositoryResult<List<ProductUnit>> =
            RepositoryResult.Success(listOf(ProductUnit("unit-1", "Sak", "sak", "")))

        override suspend fun getProduct(id: String): RepositoryResult<Product> =
            existingProduct?.let { RepositoryResult.Success(it) }
                ?: RepositoryResult.Error("NOT_FOUND", "Produk tidak ditemukan")

        override suspend fun createProduct(command: CreateProductCommand): RepositoryResult<Product> {
            createCalls += 1
            return RepositoryResult.Success(sampleProduct(id = "prod-created-1", photoUrl = null))
        }

        override suspend fun updateProduct(id: String, command: UpdateProductCommand): RepositoryResult<Product> {
            updateCalls += 1
            return RepositoryResult.Success(sampleProduct(id = id, photoUrl = existingProduct?.photoUrl))
        }

        override suspend fun uploadProductPhoto(productId: String, bytes: ByteArray, fileName: String): RepositoryResult<String> {
            uploadCalls += 1
            if (failFirstUpload && uploadCalls == 1) {
                return RepositoryResult.Error("UPLOAD_FAILED", "Upload timeout")
            }
            return RepositoryResult.Success("https://res.cloudinary.com/demo/image/upload/v$uploadCalls/cat.jpg")
        }

        override suspend fun deleteProductPhoto(productId: String): RepositoryResult<Unit> {
            deletePhotoCalls += 1
            return RepositoryResult.Success(Unit)
        }

        override suspend fun getProductStocks(page: Int, limit: Int, search: String?): RepositoryResult<ProductStockPage> = TODO()
        override suspend fun getProductsPage(page: Int, limit: Int, search: String?): RepositoryResult<ProductPage> = TODO()
        override suspend fun getProductDetail(id: String): RepositoryResult<ProductDetail> = TODO()
        override suspend fun deleteProduct(id: String): RepositoryResult<Unit> = TODO()
        override suspend fun activateProduct(id: String): RepositoryResult<Product> = TODO()
        override suspend fun getStockAdjustments(page: Int, limit: Int, search: String?, type: String?): RepositoryResult<StockAdjustmentPage> = TODO()
        override suspend fun getStockCard(page: Int, limit: Int, productId: String?, search: String?, type: String?, startDate: String?, endDate: String?): RepositoryResult<StockCard> = TODO()
        override suspend fun executeStockOpname(productId: String, adjustmentType: String, actualQty: BigDecimal, notes: String?): RepositoryResult<Unit> = TODO()
        override suspend fun previewProductCsv(csv: String): RepositoryResult<ProductCsvPreview> = TODO()
        override suspend fun commitProductCsv(csv: String): RepositoryResult<ProductCsvImportResult> = TODO()
        override suspend fun createOpeningStock(productId: String, date: String, quantity: BigDecimal, note: String): RepositoryResult<Unit> = TODO()
        override suspend fun getCategoryPage(page: Int, limit: Int, search: String?): RepositoryResult<ProductCategoryPage> = TODO()
        override suspend fun createCategory(name: String): RepositoryResult<ProductCategory> = TODO()
        override suspend fun updateCategory(id: String, name: String): RepositoryResult<ProductCategory> = TODO()
        override suspend fun deleteCategory(id: String): RepositoryResult<Unit> = TODO()
        override suspend fun getUnitPage(page: Int, limit: Int, search: String?): RepositoryResult<ProductUnitPage> = TODO()
        override suspend fun createUnit(name: String, symbol: String): RepositoryResult<ProductUnit> = TODO()
        override suspend fun updateUnit(id: String, name: String, symbol: String): RepositoryResult<ProductUnit> = TODO()
        override suspend fun deleteUnit(id: String): RepositoryResult<Unit> = TODO()
    }

    private companion object {
        fun sampleProduct(id: String, photoUrl: String?) = Product(
            id = id,
            categoryId = "cat-1",
            baseUnitId = "unit-1",
            sku = "SKU-100",
            name = "Cat Tembok",
            priceBuy = BigDecimal("40000"),
            priceRetail = BigDecimal("50000"),
            priceContractor = BigDecimal("45000"),
            discount = BigDecimal.ZERO,
            minStock = BigDecimal("5"),
            photoUrl = photoUrl,
            isActive = true
        )
    }
}

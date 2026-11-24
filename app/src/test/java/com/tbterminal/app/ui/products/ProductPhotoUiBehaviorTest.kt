package com.tbterminal.app.ui.products

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tbterminal.app.data.model.Product
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.data.model.ProductDetail
import com.tbterminal.app.data.model.ProductUnit
import com.tbterminal.app.ui.theme.TbterminalappTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class ProductPhotoUiBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h1064dp-mdpi")
    fun offlineFormDisablesPhotoButtonsAndShowsRetryWhenUploadFailed() {
        var retried = false
        val uiState = ProductFormUiState(
            categories = listOf(ProductCategory("c1", "Semen", "")),
            units = listOf(ProductUnit("u1", "Sak", "sak", "")),
            isLoading = false,
            isOnline = false,
            createdProductId = "p-1",
            photoStatus = ProductPhotoUploadStatus.Error("Upload gagal")
        )

        composeRule.setContent {
            TbterminalappTheme {
                ProductFormContent(
                    modifier = Modifier.fillMaxSize(),
                    uiState = uiState,
                    onInputChanged = {},
                    onRetryPhotoUpload = { retried = true },
                    onSave = {},
                    onCancel = {}
                )
            }
        }

        composeRule.onNodeWithTag("product-photo-gallery-btn").assertIsNotEnabled()
        composeRule.onNodeWithTag("product-photo-camera-btn").assertIsNotEnabled()
        composeRule.onNodeWithTag("product-photo-offline-hint").assertIsDisplayed()
        composeRule.onNodeWithTag("product-photo-retry").performClick()
        assertTrue(retried)
    }

    @Test
    @Config(qualifiers = "w390dp-h1064dp-mdpi")
    fun detailPhotoOpensFullscreenZoomDialogOnTap() {
        val product = Product(
            id = "p-1",
            categoryId = "c1",
            baseUnitId = "u1",
            sku = "SKU-1",
            name = "Semen Gresik",
            priceBuy = BigDecimal("50000"),
            priceRetail = BigDecimal("60000"),
            priceContractor = BigDecimal("55000"),
            discount = BigDecimal.ZERO,
            minStock = BigDecimal("10"),
            photoUrl = "https://res.cloudinary.com/demo/image/upload/v1/semen.jpg",
            isActive = true
        )

        composeRule.setContent {
            TbterminalappTheme {
                ProductDetailContent(
                    modifier = Modifier.fillMaxSize(),
                    uiState = ProductDetailUiState(
                        detail = ProductDetail(product = product, stock = null),
                        isLoading = false
                    ),
                    onRetry = {},
                    onEditProductClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("product-detail-photo").performClick()
        composeRule.onNodeWithTag("product-detail-photo-zoom").assertIsDisplayed()
        composeRule.onNodeWithText("Semen Gresik").assertIsDisplayed()
    }
}

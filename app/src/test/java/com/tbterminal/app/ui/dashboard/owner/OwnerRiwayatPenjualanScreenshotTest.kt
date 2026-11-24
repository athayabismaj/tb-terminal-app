package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.io.File
import java.math.BigDecimal
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class OwnerRiwayatPenjualanScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun captureRiwayatPenjualanEmptyPhone() {
        composeRule.setContent {
            OwnerRiwayatPenjualanEmptyPhonePreview()
        }
        val output = File("build/roborazzi/riwayat_penjualan_kosong_ponsel.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun captureRiwayatPenjualanEmptyTablet() {
        composeRule.setContent {
            OwnerRiwayatPenjualanEmptyTabletPreview()
        }
        val output = File("build/roborazzi/riwayat_penjualan_kosong_tablet.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-mdpi")
    fun captureRiwayatPenjualanNarrow360LargeNumbers() {
        composeRule.setContent {
            TbterminalappTheme {
                val spec = ownerSalesEmptySpec(hasActiveFilterOrSearch = true)
                OwnerRiwayatPenjualanScreen(
                    summary = OwnerSalesSummaryUiState(
                        totalSales = BigDecimal("12500000000"),
                        totalTransactions = 1420L,
                        paidTotal = BigDecimal("11300000000"),
                        debtDoTotal = BigDecimal("1200000000")
                    ),
                    sortLabel = "Urut: Terbaru",
                    isSearchExpanded = false,
                    searchQuery = "",
                    isLoading = false,
                    errorMessage = null,
                    transactions = emptyList(),
                    emptyTitle = spec.title,
                    emptyDescription = spec.description,
                    onBack = {},
                    onOpenSearch = {},
                    onCloseSearch = {},
                    onSearchChanged = {},
                    onOpenFilter = {},
                    onReportClick = {},
                    onNavSelect = {},
                    autoFocusSearch = false
                )
            }
        }
        val output = File("build/roborazzi/riwayat_penjualan_360dp_miliar.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun captureRiwayatPenjualanLargeSystemFont() {
        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 1.5f)
            ) {
                OwnerRiwayatPenjualanEmptyPhonePreview()
            }
        }
        val output = File("build/roborazzi/riwayat_penjualan_kosong_font_besar.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }
}

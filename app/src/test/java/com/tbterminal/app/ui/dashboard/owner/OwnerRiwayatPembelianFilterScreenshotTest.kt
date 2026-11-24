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
import com.tbterminal.app.ui.purchasehistory.PurchaseSortOrder
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.io.File
import java.time.LocalDate
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
class OwnerRiwayatPembelianFilterScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun captureFilterPembelianPhone() {
        composeRule.setContent {
            OwnerPurchaseFilterPhonePreview()
        }
        val output = File("build/roborazzi/filter_pembelian_ponsel.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun captureFilterPembelianTablet() {
        composeRule.setContent {
            OwnerPurchaseFilterTabletPreview()
        }
        val output = File("build/roborazzi/filter_pembelian_tablet.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-mdpi")
    fun captureFilterPembelianCustomSelectedNarrow() {
        composeRule.setContent {
            TbterminalappTheme {
                PurchaseFilterReferenceFrame(
                    isTablet = false,
                    draft = PurchaseFilterDraftState(
                        preset = "Kustom",
                        startDate = LocalDate.of(2026, 11, 1),
                        endDate = LocalDate.of(2026, 11, 15),
                        supplierId = "s1",
                        sortOrder = PurchaseSortOrder.HIGHEST_AMOUNT
                    )
                )
            }
        }
        val output = File("build/roborazzi/filter_pembelian_360dp_kustom.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun captureFilterPembelianLargeFont() {
        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 1.5f)
            ) {
                OwnerPurchaseFilterPhonePreview()
            }
        }
        val output = File("build/roborazzi/filter_pembelian_font_besar.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }
}

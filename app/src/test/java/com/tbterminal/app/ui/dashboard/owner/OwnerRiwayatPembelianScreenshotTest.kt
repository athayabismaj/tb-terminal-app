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
import java.io.File
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
class OwnerRiwayatPembelianScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun captureRiwayatPembelianEmptyPhone() {
        composeRule.setContent {
            OwnerRiwayatPembelianEmptyPhonePreview()
        }
        val output = File("build/roborazzi/riwayat_pembelian_kosong_ponsel.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun captureRiwayatPembelianNotFoundPhone() {
        composeRule.setContent {
            OwnerRiwayatPembelianNotFoundPhonePreview()
        }
        val output = File("build/roborazzi/riwayat_pembelian_tidak_ditemukan_ponsel.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun captureRiwayatPembelianEmptyTablet() {
        composeRule.setContent {
            OwnerRiwayatPembelianEmptyTabletPreview()
        }
        val output = File("build/roborazzi/riwayat_pembelian_kosong_tablet.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w390dp-h880dp-mdpi")
    fun captureRiwayatPembelianDataPhone() {
        composeRule.setContent {
            OwnerRiwayatPembelianDataPhonePreview()
        }
        val output = File("build/roborazzi/riwayat_pembelian_data_ponsel.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun captureRiwayatPembelianDataTablet() {
        composeRule.setContent {
            OwnerRiwayatPembelianDataTabletPreview()
        }
        val output = File("build/roborazzi/riwayat_pembelian_data_tablet.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun captureRiwayatPembelianLargeSystemFont() {
        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 1.5f)
            ) {
                OwnerRiwayatPembelianEmptyPhonePreview()
            }
        }
        val output = File("build/roborazzi/riwayat_pembelian_kosong_font_besar.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }
}

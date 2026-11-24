package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
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
class OwnerTambahPelangganScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h920dp-mdpi")
    fun captureOwnerTambahPelangganPhone() {
        composeRule.setContent {
            OwnerTambahPelangganPhonePreview()
        }
        val output = File("build/roborazzi/owner_tambah_pelanggan_phone.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun captureOwnerTambahPelangganTablet() {
        composeRule.setContent {
            OwnerTambahPelangganTabletPreview()
        }
        val output = File("build/roborazzi/owner_tambah_pelanggan_tablet.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w390dp-h920dp-mdpi")
    fun captureOwnerTambahPelangganPhoneError() {
        composeRule.setContent {
            OwnerTambahPelangganPhoneErrorPreview()
        }
        val output = File("build/roborazzi/owner_tambah_pelanggan_phone_error.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w390dp-h520dp-mdpi")
    fun captureOwnerTambahPelangganPhoneKeyboardOpenHeight() {
        composeRule.setContent {
            OwnerTambahPelangganPhonePreview()
        }
        val output = File("build/roborazzi/owner_tambah_pelanggan_phone_ime.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w390dp-h920dp-mdpi")
    fun captureOwnerEditPelangganPhone() {
        composeRule.setContent {
            OwnerEditPelangganPhonePreview()
        }
        val output = File("build/roborazzi/owner_edit_pelanggan_phone.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun captureOwnerEditPelangganTablet() {
        composeRule.setContent {
            OwnerEditPelangganTabletPreview()
        }
        val output = File("build/roborazzi/owner_edit_pelanggan_tablet.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }
}

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
class OwnerSatuanProdukScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h1064dp-mdpi")
    fun captureOwnerSatuanDataPhone() {
        composeRule.setContent {
            OwnerSatuanDataPhonePreview()
        }
        val output = File("build/roborazzi/owner_satuan_data_phone.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun captureOwnerSatuanDataTablet() {
        composeRule.setContent {
            OwnerSatuanDataTabletPreview()
        }
        val output = File("build/roborazzi/owner_satuan_data_tablet.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun captureOwnerSatuanEmptyPhone() {
        composeRule.setContent {
            OwnerSatuanEmptyPhonePreview()
        }
        val output = File("build/roborazzi/owner_satuan_empty_phone.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun captureOwnerTambahSatuanPhone() {
        composeRule.setContent {
            OwnerTambahSatuanPhonePreview()
        }
        val output = File("build/roborazzi/owner_tambah_satuan_phone.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun captureOwnerEditSatuanPhone() {
        composeRule.setContent {
            OwnerEditSatuanPhonePreview()
        }
        val output = File("build/roborazzi/owner_edit_satuan_phone.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun captureOwnerTambahSatuanTablet() {
        composeRule.setContent {
            OwnerTambahSatuanTabletPreview()
        }
        val output = File("build/roborazzi/owner_tambah_satuan_tablet.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun captureOwnerEditSatuanTablet() {
        composeRule.setContent {
            OwnerEditSatuanTabletPreview()
        }
        val output = File("build/roborazzi/owner_edit_satuan_tablet.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }
}

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
class OwnerKategoriProdukScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h1064dp-mdpi")
    fun captureOwnerKategoriDataPhone() {
        composeRule.setContent {
            OwnerKategoriDataPhonePreview()
        }
        val output = File("build/roborazzi/owner_kategori_data_phone.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun captureOwnerKategoriDataTablet() {
        composeRule.setContent {
            OwnerKategoriDataTabletPreview()
        }
        val output = File("build/roborazzi/owner_kategori_data_tablet.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun captureOwnerKategoriEmptyPhone() {
        composeRule.setContent {
            OwnerKategoriEmptyPhonePreview()
        }
        val output = File("build/roborazzi/owner_kategori_empty_phone.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun captureOwnerTambahKategoriPhone() {
        composeRule.setContent {
            OwnerTambahKategoriPhonePreview()
        }
        val output = File("build/roborazzi/owner_tambah_kategori_phone.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun captureOwnerEditKategoriPhone() {
        composeRule.setContent {
            OwnerEditKategoriPhonePreview()
        }
        val output = File("build/roborazzi/owner_edit_kategori_phone.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun captureOwnerEditKategoriTablet() {
        composeRule.setContent {
            OwnerEditKategoriTabletPreview()
        }
        val output = File("build/roborazzi/owner_edit_kategori_tablet.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }
}

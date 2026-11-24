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
class OwnerDetailPelangganScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h880dp-mdpi")
    fun captureOwnerDetailPelangganPhone() {
        composeRule.setContent {
            OwnerDetailPelangganPhonePreview()
        }
        val output = File("build/roborazzi/owner_detail_pelanggan_phone.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun captureOwnerDetailPelangganTablet() {
        composeRule.setContent {
            OwnerDetailPelangganTabletPreview()
        }
        val output = File("build/roborazzi/owner_detail_pelanggan_tablet.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w390dp-h880dp-mdpi")
    fun captureOwnerDetailPelangganInactivePhone() {
        composeRule.setContent {
            OwnerDetailPelangganInactivePhonePreview()
        }
        val output = File("build/roborazzi/owner_detail_pelanggan_inactive_phone.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun captureOwnerDetailPelangganInactiveTablet() {
        composeRule.setContent {
            OwnerDetailPelangganInactiveTabletPreview()
        }
        val output = File("build/roborazzi/owner_detail_pelanggan_inactive_tablet.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }
}

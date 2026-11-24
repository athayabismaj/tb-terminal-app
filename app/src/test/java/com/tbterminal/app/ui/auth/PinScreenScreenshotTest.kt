package com.tbterminal.app.ui.auth

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
class PinScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun capturePinEmpty() {
        composeRule.setContent {
            PinEmptyDesignPreview()
        }
        val output = File("build/roborazzi/pin_empty.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun capturePinFilled3() {
        composeRule.setContent {
            PinFilled3DesignPreview()
        }
        val output = File("build/roborazzi/pin_filled_3.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun capturePinError() {
        composeRule.setContent {
            PinErrorDesignPreview()
        }
        composeRule.mainClock.advanceTimeBy(350)
        val output = File("build/roborazzi/pin_error.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun capturePinTablet() {
        composeRule.setContent {
            PinTabletDesignPreview()
        }
        val output = File("build/roborazzi/pin_tablet.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun capturePinAlertError() {
        composeRule.setContent {
            PinAlertErrorDesignPreview()
        }
        composeRule.mainClock.advanceTimeBy(300)
        val output = File("build/roborazzi/pin_alert.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }
}

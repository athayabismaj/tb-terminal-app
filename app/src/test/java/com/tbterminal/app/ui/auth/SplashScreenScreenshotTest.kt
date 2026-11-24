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
class SplashScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun captureSplashPhone() {
        composeRule.setContent {
            SplashPhoneDesignPreview()
        }
        val output = File("build/roborazzi/splash_phone.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun captureSplashTablet() {
        composeRule.setContent {
            SplashTabletDesignPreview()
        }
        val output = File("build/roborazzi/splash_tablet.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }
}

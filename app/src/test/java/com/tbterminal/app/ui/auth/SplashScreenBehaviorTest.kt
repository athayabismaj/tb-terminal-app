package com.tbterminal.app.ui.auth

import android.app.Application
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.tbterminal.app.ui.theme.TbterminalappTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class SplashScreenBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun splashDisplaysBrandElementsCenteredOnPhone() {
        composeRule.setContent {
            TbterminalappTheme {
                SplashScreen(animateEntrance = false)
            }
        }

        composeRule.onNodeWithTag("splash-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("splash-logo").assertIsDisplayed()
        composeRule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit))
            .assertIsDisplayed()
        composeRule.onNodeWithText("Barokah Jaya").assertIsDisplayed()
        composeRule.onNodeWithText("Sistem Kasir & Inventaris").assertIsDisplayed()
        composeRule.onNodeWithTag("splash-loading-bar").assertIsDisplayed()
        composeRule.onNodeWithText("Terminal · v1.0.0").assertDoesNotExist()

        val logoBounds = composeRule.onNodeWithTag("splash-logo").getBoundsInRoot()
        val loadingBounds = composeRule.onNodeWithTag("splash-loading-bar").getBoundsInRoot()

        assertTrue(loadingBounds.top > logoBounds.bottom)

        val logoCenterX = (logoBounds.left + logoBounds.right).value / 2f
        assertEquals(195f, logoCenterX, 2f)
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun splashCentersLogoOnTablet() {
        composeRule.setContent {
            TbterminalappTheme {
                SplashScreen(animateEntrance = false)
            }
        }

        val logoBounds = composeRule.onNodeWithTag("splash-logo").getBoundsInRoot()
        val logoCenterX = (logoBounds.left + logoBounds.right).value / 2f
        assertEquals(512f, logoCenterX, 2f)
    }
}

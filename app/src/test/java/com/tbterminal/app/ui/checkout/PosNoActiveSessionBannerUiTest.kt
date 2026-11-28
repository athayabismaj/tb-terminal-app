package com.tbterminal.app.ui.checkout

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tbterminal.app.ui.theme.TbterminalappTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PosNoActiveSessionBannerUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cashierPosScreen_showsNoticeAndNavigatesToKas_whenNoSessionAndNotLoading() {
        var navigatedToKasCount = 0

        composeRule.setContent {
            TbterminalappTheme {
                CashierPosScreen(
                    state = CheckoutUiState(
                        isCashSessionLoading = false,
                        hasActiveCashSession = false,
                        canOpenCashSession = true,
                    ),
                    onSearchChanged = {},
                    onRefreshProducts = {},
                    onProductClick = {},
                    onCustomerSearchChanged = {},
                    onSelectCustomer = {},
                    onClearCustomer = {},
                    onUseCustomerName = {},
                    onRefreshCustomers = {},
                    onUpdateQty = { _, _ -> },
                    onSelectPayment = {},
                    onAmountPaidChanged = {},
                    onStartingCashChanged = {},
                    onOpenCashSession = {},
                    onRefreshCashSession = {},
                    onCheckout = {},
                    onNavigateToCart = {},
                    onCashSessionClick = { navigatedToKasCount++ },
                )
            }
        }

        composeRule.onNodeWithTag("pos-no-cash-session-banner").assertIsDisplayed()
        composeRule.onNodeWithText("Shift kasir belum dibuka").assertIsDisplayed()
        composeRule.onNodeWithTag("pos-open-cash-session-nav-button").assertIsDisplayed().performClick()
        assertEquals(1, navigatedToKasCount)
        composeRule.onNodeWithText("Buka Shift & Mulai Transaksi").assertDoesNotExist()
    }

    @Test
    fun cashierPosScreen_hidesNoSessionNotice_whileLoadingOrSessionActive() {
        composeRule.setContent {
            TbterminalappTheme {
                CashierPosScreen(
                    state = CheckoutUiState(
                        isCashSessionLoading = true,
                        hasActiveCashSession = false,
                        canOpenCashSession = true,
                    ),
                    onSearchChanged = {},
                    onRefreshProducts = {},
                    onProductClick = {},
                    onCustomerSearchChanged = {},
                    onSelectCustomer = {},
                    onClearCustomer = {},
                    onUseCustomerName = {},
                    onRefreshCustomers = {},
                    onUpdateQty = { _, _ -> },
                    onSelectPayment = {},
                    onAmountPaidChanged = {},
                    onStartingCashChanged = {},
                    onOpenCashSession = {},
                    onRefreshCashSession = {},
                    onCheckout = {},
                    onNavigateToCart = {},
                )
            }
        }

        composeRule.onNodeWithTag("pos-no-cash-session-banner").assertDoesNotExist()
    }
}

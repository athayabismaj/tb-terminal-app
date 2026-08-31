package com.tbterminal.app.ui.cashhistory

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.ui.dashboard.BackofficeAdaptiveShell
import com.tbterminal.app.ui.dashboard.BackofficeSection
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal

@Preview(name = "Kas Harian - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
fun PhoneCashHistoryPreview() {
    TbterminalappTheme {
        BackofficeAdaptiveShell(
            userName = "Pemilik Toko",
            role = "owner",
            activeSection = BackofficeSection.FINANCE,
            onSectionSelected = {},
            onProfileClick = {},
            onLogout = {},
            pageTitle = "Kas Harian",
            onBack = {}
        ) { contentModifier ->
            CashSessionHistoryScreen(
            modifier = contentModifier,
            uiState = CashSessionHistoryUiState(
                sessions = listOf(
                    CashSession(
                        id = "session-1",
                        userId = "cashier-1",
                        userName = "Ayu Kasir",
                        openedAt = "2026-08-25T08:00:00+07:00",
                        closedAt = null,
                        openingCash = BigDecimal("500000"),
                        closingCash = null,
                        systemCash = BigDecimal("2850000"),
                        difference = BigDecimal.ZERO,
                        totalExpenses = BigDecimal("50000"),
                        notes = null,
                        status = "OPEN"
                    ),
                    CashSession(
                        id = "session-2",
                        userId = "cashier-2",
                        userName = "Budi Kasir",
                        openedAt = "2026-08-24T08:15:00+07:00",
                        closedAt = "2026-08-24T17:05:00+07:00",
                        openingCash = BigDecimal("400000"),
                        closingCash = BigDecimal("1945000"),
                        systemCash = BigDecimal("1950000"),
                        difference = BigDecimal("-5000"),
                        totalExpenses = BigDecimal("25000"),
                        notes = "Selisih pencatatan",
                        status = "CLOSED"
                    )
                ),
                selectedDate = "2026-08-25",
                startDate = "2026-08-25",
                endDate = "2026-08-25",
                selectedPreset = "Hari ini",
                totalSessions = 2,
                totalPages = 1,
                isLoading = false
            ),
            onSearchChanged = {},
            onStatusFilterChanged = {},
            onRefresh = {},
            onDateChanged = {},
            onDatePresetSelected = {},
            onPreviousDate = {},
            onNextDate = {},
            onShowDetail = {},
            onPreviousPage = {},
            onNextPage = {}
            )
        }
    }
}

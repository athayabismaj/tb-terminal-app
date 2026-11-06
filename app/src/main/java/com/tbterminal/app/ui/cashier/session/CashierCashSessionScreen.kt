package com.tbterminal.app.ui.cashier.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.cash.CashReconciliationUiState
import com.tbterminal.app.ui.cashier.session.components.CloseShiftCard
import com.tbterminal.app.ui.cashier.session.components.LocalCashSessionBadges
import com.tbterminal.app.ui.cashier.session.components.OpenShiftForm
import com.tbterminal.app.ui.cashier.session.components.SessionSummarySection
import com.tbterminal.app.ui.cashier.session.components.SessionTransactionsCard
import com.tbterminal.app.ui.cashier.session.components.ShiftClosureBottomSheetModal
import com.tbterminal.app.ui.components.SkeletonList
import com.tbterminal.app.ui.components.TopFloatingCapsule

private val CanvasBg = Color(0xFFF8FAF9)

@Composable
fun CashierCashSessionScreen(
    modifier: Modifier = Modifier,
    uiState: CashReconciliationUiState,
    onOpeningCashChanged: (String) -> Unit,
    onOpenSession: () -> Unit,
    onClosingCashChanged: (String) -> Unit,
    onClosingNotesChanged: (String) -> Unit,
    onCloseSession: () -> Unit,
    onShowExpenseDialog: () -> Unit,
    onTransactionHistoryClick: () -> Unit,
    onClearMessage: () -> Unit = {}
) {
    val isClosingError = uiState.hasActiveSession && !uiState.isExpenseDialogOpen && uiState.errorMessage != null
    val closingErrorText = if (isClosingError) uiState.errorMessage else null

    var isPrivacyHidden by rememberSaveable { mutableStateOf(false) }
    var showCloseShiftModal by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBg)
    ) {
        TopFloatingCapsule(
            message = uiState.message ?: uiState.errorMessage,
            isError = uiState.errorMessage != null,
            onDismiss = onClearMessage
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            val isDesktop = maxWidth >= 840.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = if (isDesktop) 720.dp else 512.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sesi lokal offline badge jika ada
                LocalCashSessionBadges(uiState = uiState)

                // ── KONTEN UTAMA ──
                if (uiState.isLoading && !uiState.isSubmitting) {
                    SkeletonList(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        itemCount = 3
                    )
                } else if (!uiState.hasActiveSession) {
                    // FORM BUKA SESI SHIFT BARU
                    OpenShiftForm(
                        modifier = Modifier.fillMaxWidth(),
                        uiState = uiState,
                        onOpeningCashChanged = onOpeningCashChanged,
                        onOpenSession = onOpenSession
                    )
                } else {
                    // SHIFT AKTIF:
                    // 1. CardRingkasanKasLaci
                    SessionSummarySection(
                        modifier = Modifier.fillMaxWidth(),
                        uiState = uiState,
                        isPrivacyHidden = isPrivacyHidden,
                        onTogglePrivacy = { isPrivacyHidden = !isPrivacyHidden },
                        onShowExpenseDialog = onShowExpenseDialog
                    )

                    // 2. CardTutupShiftKasir (Compact action card yang membuka modal pop-up bottom sheet)
                    CloseShiftCard(
                        modifier = Modifier.fillMaxWidth(),
                        onOpenModal = { showCloseShiftModal = true }
                    )

                    // 3. SectionTransaksiSesiIni
                    SessionTransactionsCard(
                        modifier = Modifier.fillMaxWidth(),
                        uiState = uiState,
                        onTransactionHistoryClick = onTransactionHistoryClick
                    )
                }
            }
        }

        // ── 4. POP-UP BOTTOM SHEET MODAL (Tutup Shift Kasir) ──
        if (showCloseShiftModal && uiState.hasActiveSession) {
            ShiftClosureBottomSheetModal(
                onDismiss = { showCloseShiftModal = false },
                uiState = uiState,
                cashAmount = uiState.closingCashInput,
                notes = uiState.closingNotesInput,
                onCashChange = onClosingCashChanged,
                onNotesChange = onClosingNotesChanged,
                onSubmit = {
                    onCloseSession()
                },
                isSubmitting = uiState.isSubmitting,
                isError = isClosingError,
                supportingText = closingErrorText
            )
        }
    }
}

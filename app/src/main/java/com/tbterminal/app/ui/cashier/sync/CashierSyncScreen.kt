package com.tbterminal.app.ui.cashier.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.repository.CashierSyncItemKind
import com.tbterminal.app.data.repository.CashierSyncItemStatus
import com.tbterminal.app.data.repository.CashierSyncItemUi
import com.tbterminal.app.data.sync.BackendStatus
import com.tbterminal.app.ui.cashier.sync.components.CashierClockWarningBanner
import com.tbterminal.app.ui.cashier.sync.components.CashierSyncEmptyCard
import com.tbterminal.app.ui.cashier.sync.components.CashierSyncItemCard
import com.tbterminal.app.ui.cashier.sync.components.CashierSyncStatusCard
import com.tbterminal.app.ui.cashier.sync.components.CashierSyncSummarySection
import com.tbterminal.app.ui.components.OfflineSessionAgeBanner
import com.tbterminal.app.ui.components.SkeletonCard
import com.tbterminal.app.ui.components.SkeletonList
import com.tbterminal.app.ui.theme.TbterminalappTheme

private val ScreenBackground = Color(0xFFF8FAF9)
private val SectionTitleColor = Color(0xFF0F172A)

@Composable
fun CashierSyncScreen(
    uiState: CashierSyncUiState,
    onSyncNowClick: () -> Unit,
    onRefreshServerStatus: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground),
    ) {
        val compact = maxWidth < 700.dp

        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
            contentPadding = PaddingValues(
                horizontal = if (compact) 16.dp else 24.dp,
                vertical = if (compact) 16.dp else 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.isInitialLoading) {
                item { SkeletonCard() }
                item { SkeletonList(modifier = Modifier.fillMaxWidth(), itemCount = 3) }
            } else {
                uiState.offlineSessionAgeAlert?.let { alert ->
                    item {
                        OfflineSessionAgeBanner(
                            alert = alert,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                uiState.clockWarning?.let { warning ->
                    item {
                        CashierClockWarningBanner(
                            warning = warning,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                item {
                    CashierSyncStatusCard(
                        uiState = uiState,
                        onSyncNowClick = onSyncNowClick,
                        onRefreshServerStatus = onRefreshServerStatus,
                        onDismissFeedback = onDismissFeedback,
                    )
                }

                item {
                    CashierSyncSummarySection(
                        pendingCount = uiState.pendingCount,
                        sendingCount = uiState.sendingCount,
                        failedCount = uiState.failedCount,
                        needsCheckCount = uiState.needsCheckCount,
                        compact = compact,
                    )
                }

                item {
                    Text(
                        text = "Daftar Data (${uiState.items.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SectionTitleColor,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                if (uiState.items.isEmpty()) {
                    item {
                        CashierSyncEmptyCard()
                    }
                } else {
                    items(
                        items = uiState.items,
                        key = { it.id },
                    ) { item ->
                        CashierSyncItemCard(item = item)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CashierSyncScreenPreview() {
    TbterminalappTheme {
        CashierSyncScreen(
            uiState = CashierSyncUiState(
                isInitialLoading = false,
                isServerOnline = true,
                isInternetOnline = true,
                backendStatus = BackendStatus.CONNECTED,
                lastSuccessfulSyncAtMillis = 1_795_000_000_000L,
                pendingCount = 1,
                sendingCount = 0,
                failedCount = 1,
                needsCheckCount = 1,
                items = listOf(
                    CashierSyncItemUi(
                        id = "queue-1",
                        kind = CashierSyncItemKind.TRANSACTION,
                        title = "Transaksi Offline",
                        summary = "TRX-20261128-A1B2 · Rp 125.000 (Lunas)",
                        createdAtMillis = 1_795_000_000_000L,
                        status = CashierSyncItemStatus.FAILED,
                        failureReason = "Harga produk di server berubah. Minta admin meninjau transaksi ini.",
                    ),
                    CashierSyncItemUi(
                        id = "queue-2",
                        kind = CashierSyncItemKind.OPEN_CASH_SESSION,
                        title = "Buka Sesi Kas",
                        summary = "Modal awal Rp 200.000",
                        createdAtMillis = 1_794_995_000_000L,
                        status = CashierSyncItemStatus.PENDING,
                    ),
                    CashierSyncItemUi(
                        id = "pending-kasir-refund",
                        kind = CashierSyncItemKind.REFUND_TRANSACTION,
                        title = "Refund Transaksi",
                        summary = "Retur semen rusak",
                        createdAtMillis = 1_794_990_000_000L,
                        status = CashierSyncItemStatus.NEEDS_CHECK,
                        failureReason = "Status di server belum pasti karena koneksi sempat terputus. Cek dulu di riwayat transaksi sebelum kirim ulang.",
                        isPendingSubmission = true,
                    ),
                ),
            ),
            onSyncNowClick = {},
            onRefreshServerStatus = {},
            onDismissFeedback = {},
        )
    }
}

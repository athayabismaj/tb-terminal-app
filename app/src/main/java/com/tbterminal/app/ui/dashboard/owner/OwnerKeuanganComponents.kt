package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.OwnerAmberInk

internal val KeuanganHeroSoftText = Color(0xFFCFE5DB)
internal val KeuanganPositiveInflowText = Color(0xFFA9DCC4)

@Immutable
internal data class KeuanganMenuRowModel(
    val id: KeuanganActionId,
    val title: String,
    val subtitle: String?,
    val icon: ImageVector,
    val iconTint: Color
)

@Immutable
internal data class KeuanganMenuGroupModel(
    val title: String,
    val items: List<KeuanganMenuRowModel>
)

internal fun buildKeuanganMenuGroups(state: KeuanganUiState): List<KeuanganMenuGroupModel> {
    val sessionSubtitle = if (state.hasActiveSession) "Sesi aktif" else "Sesi ditutup"
    val expenseSubtitle = if (state.balanceHidden) {
        "Hari ini Rp ••••••"
    } else {
        "Hari ini ${formatRupiahSpaced(state.todayExpensesTotal)}"
    }
    val receivableSubtitle = if (state.balanceHidden) {
        "${state.receivableNotesCount} nota · Rp ••••••"
    } else {
        "${state.receivableNotesCount} nota · ${formatRupiahSpaced(state.activeReceivables)}"
    }
    val payableSubtitle = if (state.balanceHidden) {
        "${state.payableNotesCount} nota · Rp ••••••"
    } else {
        "${state.payableNotesCount} nota · ${formatRupiahSpaced(state.duePayables)}"
    }

    return listOf(
        KeuanganMenuGroupModel(
            title = "Kas & pengeluaran",
            items = listOf(
                KeuanganMenuRowModel(
                    id = KeuanganActionId.DAILY_CASH,
                    title = "Kas harian",
                    subtitle = sessionSubtitle,
                    icon = OwnerIcons.ClipboardCheck,
                    iconTint = Brand
                ),
                KeuanganMenuRowModel(
                    id = KeuanganActionId.CASH_EXPENSES,
                    title = "Pengeluaran kas",
                    subtitle = expenseSubtitle,
                    icon = OwnerIcons.Banknote,
                    iconTint = OwnerAmberInk
                )
            )
        ),
        KeuanganMenuGroupModel(
            title = "Piutang & hutang",
            items = listOf(
                KeuanganMenuRowModel(
                    id = KeuanganActionId.RECEIVABLES,
                    title = "Piutang pelanggan",
                    subtitle = receivableSubtitle,
                    icon = OwnerIcons.Banknote,
                    iconTint = Brand
                ),
                KeuanganMenuRowModel(
                    id = KeuanganActionId.RECEIVABLE_PAYMENTS,
                    title = "Pembayaran piutang",
                    subtitle = null,
                    icon = OwnerIcons.CreditCard,
                    iconTint = Brand
                ),
                KeuanganMenuRowModel(
                    id = KeuanganActionId.SUPPLIER_DEBTS,
                    title = "Hutang supplier",
                    subtitle = payableSubtitle,
                    icon = OwnerIcons.Wallet,
                    iconTint = OwnerAmberInk
                )
            )
        ),
        KeuanganMenuGroupModel(
            title = "Laporan & analitik",
            items = listOf(
                KeuanganMenuRowModel(
                    id = KeuanganActionId.FINANCE_REPORTS,
                    title = "Laporan bisnis & penjualan",
                    subtitle = null,
                    icon = OwnerIcons.TrendLineUp,
                    iconTint = Brand
                )
            )
        )
    )
}

/**
 * Baris 3 kolom metrik di header hijau Keuangan sesuai CSS `.st` dan `.st+.st`.
 */
@Composable
internal fun KeuanganHeroStatsRow(
    state: KeuanganUiState,
    onReceivablesClick: () -> Unit,
    onPayablesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lineColor = Color.White.copy(alpha = 0.18f)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(lineColor)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            KeuanganHeroStatItem(
                label = "Kas hari ini",
                value = when {
                    state.loading -> "—"
                    state.balanceHidden -> "Rp ••••••"
                    else -> formatKeuanganCashDelta(state.todayCashInflow)
                },
                valueColor = KeuanganPositiveInflowText,
                modifier = Modifier.weight(1f)
            )
            KeuanganHeroStatItem(
                label = "Piutang aktif",
                value = when {
                    state.loading -> "—"
                    state.balanceHidden -> "Rp ••••••"
                    else -> formatKeuanganHeroMetric(state.activeReceivables)
                },
                valueColor = OnBrand,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .ownerHeroLeftDivider(lineColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onReceivablesClick
                    )
                    .testTag("keuangan-stat-receivables")
            )
            KeuanganHeroStatItem(
                label = "Hutang tempo",
                value = when {
                    state.loading -> "—"
                    state.balanceHidden -> "Rp ••••••"
                    else -> formatKeuanganHeroMetric(state.duePayables)
                },
                valueColor = OnBrand,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .ownerHeroLeftDivider(lineColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onPayablesClick
                    )
                    .testTag("keuangan-stat-payables")
            )
        }
    }
}

@Composable
private fun KeuanganHeroStatItem(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = ownerTextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal,
                color = KeuanganHeroSoftText
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = ownerTextStyle(
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
                tabularNumbers = true
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun KeuanganMenuSection(
    group: KeuanganMenuGroupModel,
    onActionClick: (KeuanganActionId) -> Unit,
    modifier: Modifier = Modifier
) {
    OwnerPlainMenuSection(
        group = OwnerPlainMenuGroup(
            title = group.title,
            items = group.items.map { item ->
                OwnerPlainMenuItem(
                    tag = "keuangan-menu-${item.id.name}",
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = item.icon,
                    iconTint = item.iconTint,
                    onClick = { onActionClick(item.id) }
                )
            }
        ),
        modifier = modifier
    )
}

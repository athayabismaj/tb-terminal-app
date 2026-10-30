package com.tbterminal.app.ui.reports.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.remote.CashierSalesSummaryDto
import com.tbterminal.app.data.remote.PaymentMethodSummaryDto
import com.tbterminal.app.data.remote.SalesReceivableSummaryDto
import com.tbterminal.app.data.remote.SalesReportResponseDto
import com.tbterminal.app.data.remote.TopProductSalesDto
import com.tbterminal.app.data.remote.TransactionStatusSummaryDto
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun SalesReportAggregateSection(
    report: SalesReportResponseDto?,
    isLoading: Boolean,
    error: String?,
    detailsExpanded: Boolean,
    onDetailsExpandedChange: (Boolean) -> Unit,
    onRetry: () -> Unit,
    showPrimaryKpis: Boolean = true,
) {
    when {
        isLoading && report == null -> SalesReportLoadingCard()
        error != null && report == null -> SalesReportErrorCard(message = error, onRetry = onRetry)
        report == null -> SalesReportEmptyCard()
        else -> BoxWithConstraints {
            val compact = maxWidth < 720.dp
            val stacked = maxWidth < 1040.dp
            val medium = maxWidth < 1180.dp
            Column(verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 24.dp)) {
                if (showPrimaryKpis) {
                    PrimaryKpiSection(report = report, useGrid = compact || medium)
                }
                ReportDetailsDisclosure(
                    expanded = detailsExpanded,
                    onClick = { onDetailsExpandedChange(!detailsExpanded) },
                )
                if (detailsExpanded) {
                    if (report.voided.transactionCount > 0) {
                        BaseCard(title = "Transaksi void") {
                            StatusRow(
                                label = "VOIDED",
                                qty = "${report.voided.transactionCount} Trx",
                                amount = report.voided.amount.toShortCurrency(),
                                color = ReportColors.Error
                            )
                        }
                    }
                    if (stacked) {
                        Column(verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 18.dp)) {
                            PaymentMethodsCard(report.paymentMethods, Modifier.fillMaxWidth())
                            TransactionStatusCard(report.transactionStatuses, Modifier.fillMaxWidth())
                            ReceivablesSummaryCard(report.receivables, Modifier.fillMaxWidth())
                            TopProductsCard(report.topProducts, Modifier.fillMaxWidth())
                            CashierPerformanceCard(report.cashiers, Modifier.fillMaxWidth())
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Box(Modifier.weight(1f)) { PaymentMethodsCard(report.paymentMethods, Modifier.fillMaxWidth()) }
                            Box(Modifier.weight(1f)) { TransactionStatusCard(report.transactionStatuses, Modifier.fillMaxWidth()) }
                            Box(Modifier.weight(1f)) { ReceivablesSummaryCard(report.receivables, Modifier.fillMaxWidth()) }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            TopProductsCard(report.topProducts, Modifier.weight(1f))
                            CashierPerformanceCard(report.cashiers, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactBusinessSummary(
    report: SalesReportResponseDto?,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
) {
    when {
        isLoading && report == null -> SalesReportLoadingCard()
        error != null && report == null -> SalesReportErrorCard(message = error, onRetry = onRetry)
        report == null -> SalesReportEmptyCard()
        else -> Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ReportColors.OutlineSoft),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                // Hero Total Omzet
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "TOTAL OMZET BISNIS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.6.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = report.totals.grossRevenue.toReportCurrency(),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ReportColors.Primary,
                        letterSpacing = (-0.5).sp,
                    )
                }

                HorizontalDivider(color = ReportColors.Slate100, thickness = 1.dp)

                CompactSummaryRow(
                    label = "Total Transaksi",
                    value = "${report.totals.transactionCount} Transaksi",
                    valueColor = ReportColors.OnSurface,
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    iconTint = Color(0xFF2563EB),
                )
                HorizontalDivider(color = ReportColors.Slate100, thickness = 1.dp)
                CompactSummaryRow(
                    label = "Uang Diterima",
                    value = report.totals.paidAmount.toReportCurrency(),
                    valueColor = Color(0xFF059669),
                    icon = Icons.Outlined.AccountBalanceWallet,
                    iconTint = Color(0xFF059669),
                )
                HorizontalDivider(color = ReportColors.Slate100, thickness = 1.dp)
                CompactSummaryRow(
                    label = "Sisa Piutang",
                    value = report.totals.outstandingAmount.toReportCurrency(),
                    valueColor = if (report.totals.outstandingAmount.signum() > 0) ReportColors.Error else ReportColors.OnSurface,
                    icon = Icons.Outlined.PendingActions,
                    iconTint = ReportColors.Error,
                )
                HorizontalDivider(color = ReportColors.Slate100, thickness = 1.dp)
                CompactSummaryRow(
                    label = "Laba Kotor",
                    value = "${report.totals.grossProfit.toReportCurrency()} (${report.grossMarginText()})",
                    valueColor = Color(0xFF7C3AED),
                    icon = Icons.Outlined.Savings,
                    iconTint = Color(0xFF7C3AED),
                )
            }
        }
    }
}

@Composable
private fun CompactSummaryRow(
    label: String,
    value: String,
    valueColor: Color,
    icon: ImageVector,
    iconTint: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconTint.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ReportColors.Outline,
            )
        }
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PrimaryKpiSection(report: SalesReportResponseDto, useGrid: Boolean) {
    val firstRow: @Composable RowScope.() -> Unit = {
        KpiCard("Omzet", report.totals.grossRevenue.toReportCurrency(), "${report.totals.transactionCount} transaksi", Icons.AutoMirrored.Outlined.ReceiptLong, ReportColors.Primary, Modifier.weight(1f))
        KpiCard("Diterima", report.totals.paidAmount.toReportCurrency(), "Pembayaran masuk", Icons.Outlined.AccountBalanceWallet, Color(0xFF059669), Modifier.weight(1f))
    }
    val secondRow: @Composable RowScope.() -> Unit = {
        KpiCard("Piutang", report.totals.outstandingAmount.toReportCurrency(), "Belum diterima", Icons.Outlined.PendingActions, ReportColors.Error, Modifier.weight(1f))
        KpiCard("Laba kotor", report.totals.grossProfit.toReportCurrency(), report.grossMarginText(), Icons.Outlined.Savings, Color(0xFF7C3AED), Modifier.weight(1f))
    }
    if (useGrid) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { firstRow() }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { secondRow() }
        }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            firstRow()
            secondRow()
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    supporting: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.heightIn(min = 112.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ReportColors.OutlineSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 11.sp,
                    color = ReportColors.Outline,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    color = color.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(18.dp),
                    )
                }
            }
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ReportColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = supporting,
                fontSize = 11.sp,
                color = ReportColors.Outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ReportDetailsDisclosure(expanded: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("report-details-toggle"),
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (expanded) "Sembunyikan rincian analisis" else "Tampilkan rincian analisis",
                fontSize = 13.sp,
                color = ReportColors.OnSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Sembunyikan rincian" else "Tampilkan rincian",
                tint = ReportColors.Outline,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun PaymentMethodsCard(
    paymentMethods: List<PaymentMethodSummaryDto>,
    modifier: Modifier = Modifier
) {
    val total = paymentMethods.fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }
    BaseCard(
        title = "Metode pembayaran",
        icon = Icons.Outlined.Payments,
        modifier = modifier
    ) {
        if (paymentMethods.isEmpty()) {
            EmptyText("Belum ada pembayaran pada rentang tanggal ini.")
        } else {
            paymentMethods.forEachIndexed { index, item ->
                ProgressBarItem(
                    label = item.method.toPaymentMethodLabel(),
                    value = item.amount.toShortCurrency(),
                    progress = item.amount.progressAgainst(total),
                    color = methodColor(index)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun TransactionStatusCard(
    statuses: List<TransactionStatusSummaryDto>,
    modifier: Modifier = Modifier
) {
    BaseCard(
        title = "Status transaksi",
        icon = Icons.Outlined.AssignmentTurnedIn,
        modifier = modifier
    ) {
        if (statuses.isEmpty()) {
            EmptyText("Belum ada transaksi pada rentang tanggal ini.")
        } else {
            statuses.forEach { item ->
                StatusRow(
                    label = item.status.toStatusLabel(),
                    qty = "${item.transactionCount} Trx",
                    amount = item.revenue.toShortCurrency(),
                    color = statusColor(item.status)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ReceivablesSummaryCard(
    receivables: SalesReceivableSummaryDto,
    modifier: Modifier = Modifier
) {
    BaseCard(
        title = "Piutang",
        icon = Icons.Outlined.AccountBalance,
        modifier = modifier
    ) {
        ReceivableRow("Terbentuk", receivables.createdReceivableAmount.toShortCurrency(), ReportColors.SurfaceSoft, ReportColors.Slate500)
        Spacer(modifier = Modifier.height(6.dp))
        ReceivableRow("Terbayar", receivables.paidAmount.toShortCurrency(), ReportColors.PrimarySoft, ReportColors.Primary)
        Spacer(modifier = Modifier.height(6.dp))
        ReceivableRow("Sisa", receivables.remainingAmount.toShortCurrency(), ReportColors.ErrorSoft, ReportColors.Error, ReportColors.Error.copy(alpha = 0.12f))
        Row(
            modifier = Modifier.padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(ReportColors.Slate100, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = receivables.receivableCount.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
            Text(
                text = "  transaksi piutang pada periode ini",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = ReportColors.Slate400,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                )
            )
        }
    }
}

@Composable
private fun TopProductsCard(
    products: List<TopProductSalesDto>,
    modifier: Modifier = Modifier
) {
    BaseCard(title = "Produk terlaris", badgeText = "Top ${products.size.coerceAtMost(5)}", modifier = modifier) {
        TableHeaderRow("NAMA PRODUK", "QTY", "OMZET")
        if (products.isEmpty()) {
            EmptyText("Belum ada produk terjual pada rentang tanggal ini.")
        } else {
            products.take(5).forEach { product ->
                ProductRow(product.productName, product.qtySold.toDisplayNumber(), product.revenue.toShortCurrency())
            }
        }
    }
}

@Composable
private fun CashierPerformanceCard(
    cashiers: List<CashierSalesSummaryDto>,
    modifier: Modifier = Modifier
) {
    BaseCard(title = "Performa kasir", badgeText = "Aktif", modifier = modifier) {
        TableHeaderRow("KASIR", "TRX", "OMZET")
        if (cashiers.isEmpty()) {
            EmptyText("Belum ada transaksi kasir pada rentang tanggal ini.")
        } else {
            cashiers.take(5).forEach { cashier ->
                CashierRow(
                    initial = cashier.cashierName.initials(),
                    name = cashier.cashierName,
                    qty = cashier.transactionCount.toString(),
                    amount = cashier.revenue.toShortCurrency()
                )
            }
        }
    }
}

@Composable
private fun BaseCard(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    badgeText: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ReportColors.Surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ReportColors.OutlineSoft)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = ReportColors.OnSurface,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    if (badgeText != null) {
                        Spacer(modifier = Modifier.size(8.dp))
                        Box(
                            modifier = Modifier
                                .background(ReportColors.PrimarySoft, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ReportColors.Primary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                    }
                }
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = ReportColors.Slate400, modifier = Modifier.size(18.dp))
                }
            }
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 0.dp)) {
                content()
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProgressBarItem(label: String, value: String, progress: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall.copy(color = ReportColors.Slate500, fontWeight = FontWeight.Medium))
            Text(value, style = MaterialTheme.typography.bodySmall.copy(color = ReportColors.OnSurface, fontWeight = FontWeight.ExtraBold))
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = color,
            trackColor = ReportColors.Slate100
        )
    }
}

@Composable
private fun StatusRow(label: String, qty: String, amount: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ReportColors.SurfaceSoft.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .border(1.dp, ReportColors.SurfaceSoft, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.size(10.dp))
            Text(label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(qty, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold))
            Text(
                text = amount,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (color == ReportColors.Error) color else ReportColors.Slate400,
                    fontWeight = if (color == ReportColors.Error) FontWeight.Bold else FontWeight.Normal
                )
            )
        }
    }
}

@Composable
private fun ReceivableRow(
    label: String,
    amount: String,
    background: Color,
    contentColor: Color,
    border: Color = Color.Transparent
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(12.dp))
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = contentColor,
                fontWeight = FontWeight.ExtraBold
            )
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodySmall.copy(
                color = contentColor,
                fontWeight = FontWeight.ExtraBold
            )
        )
    }
}

@Composable
private fun TableHeaderRow(first: String, second: String, third: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ReportColors.SurfaceSoft)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        HeaderText(first, Modifier.weight(2f))
        HeaderText(second, Modifier.weight(1f), TextAlign.End)
        HeaderText(third, Modifier.weight(1f), TextAlign.End)
    }
}

@Composable
private fun ProductRow(name: String, qty: String, amount: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(
            text = name,
            modifier = Modifier.weight(2f),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = qty,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.End
        )
        Text(
            text = amount,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun CashierRow(initial: String, name: String, qty: String, amount: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(2f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(22.dp).background(ReportColors.Slate100, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(initial, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = ReportColors.Slate500))
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(qty, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
        Text(
            amount,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun HeaderText(text: String, modifier: Modifier, textAlign: TextAlign = TextAlign.Start) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(
            color = ReportColors.Slate400,
            fontWeight = FontWeight.ExtraBold
        ),
        textAlign = textAlign
    )
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        color = ReportColors.Slate500,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SalesReportLoadingCard() {
    ReportSurfaceCard {
        com.tbterminal.app.ui.components.SkeletonCard(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SalesReportErrorCard(message: String, onRetry: () -> Unit) {
    ReportSurfaceCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(message, color = ReportColors.Error, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
                Text("Coba Lagi")
            }
        }
    }
}

@Composable
private fun SalesReportEmptyCard() {
    ReportSurfaceCard {
        Text(
            text = "Laporan agregat belum tersedia.",
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            color = ReportColors.Slate500,
            textAlign = TextAlign.Center
        )
    }
}

private fun BigDecimal.progressAgainst(total: BigDecimal): Float {
    if (total.signum() <= 0) return 0f
    return divide(total, 4, RoundingMode.HALF_UP).toFloat()
}

private fun BigDecimal.toShortCurrency(): String {
    val million = BigDecimal("1000000")
    val thousand = BigDecimal("1000")
    return when {
        abs() >= million -> "Rp ${divide(million, 1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()}M"
        abs() >= thousand -> "Rp ${divide(thousand, 0, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()}k"
        else -> toReportCurrency()
    }
}

private fun BigDecimal.toDisplayNumber(): String {
    return stripTrailingZeros().toPlainString()
}

private fun SalesReportResponseDto.grossMarginText(): String {
    if (totals.grossRevenue.signum() <= 0) return "0% Margin"
    val margin = totals.grossProfit.multiply(BigDecimal("100")).divide(totals.grossRevenue, 1, RoundingMode.HALF_UP)
    return "${margin.stripTrailingZeros().toPlainString()}% Margin"
}

private fun String.initials(): String {
    return trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "-" }
}

private fun methodColor(index: Int): Color {
    return listOf(ReportColors.Primary, ReportColors.Secondary, ReportColors.Purple, ReportColors.Orange, ReportColors.Error)[index % 5]
}

private fun statusColor(status: String): Color {
    return when (status.lowercase()) {
        "paid", "lunas" -> ReportColors.Primary
        "dp" -> ReportColors.Secondary
        "debt", "hutang" -> ReportColors.Error
        else -> ReportColors.Slate500
    }
}

private fun String.toPaymentMethodLabel(): String {
    return when (lowercase()) {
        "cash", "tunai" -> "Tunai"
        "transfer" -> "Transfer Bank"
        "qris" -> "QRIS / E-Wallet"
        "debt", "hutang" -> "Hutang"
        "dp" -> "DP"
        else -> replaceFirstChar { it.uppercase() }
    }
}

private fun String.toStatusLabel(): String {
    return when (lowercase()) {
        "paid", "lunas" -> "Lunas"
        "debt", "hutang" -> "Hutang"
        "dp" -> "DP (Proses)"
        "cancelled", "canceled", "batal" -> "Batal"
        else -> replaceFirstChar { it.uppercase() }
    }
}

package com.tbterminal.app.ui.reports.components

import androidx.compose.foundation.background
import com.tbterminal.app.ui.components.TbPagination
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.CashTransaction
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ReportsTransactionTable(
    transactions: List<CashTransaction>,
    isLoading: Boolean,
    error: String?,
    page: Int,
    totalPages: Int,
    totalItems: Long,
    onRetry: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ReportSurfaceCard(contentPadding = 0.dp) {
            if (showHeader) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReportSectionHeader(title = "Transaksi")
                }
                ReportDivider()
            }

            when {
                isLoading && transactions.isEmpty() -> ReportsTransactionLoading()
                error != null && transactions.isEmpty() -> ReportsTransactionError(message = error, onRetry = onRetry)
                transactions.isEmpty() -> ReportsTransactionEmptyState()
                else -> {
                    BoxWithConstraints {
                        val compact = maxWidth < 680.dp
                        Column {
                            if (!compact) ReportsTransactionHeader()
                            transactions.forEach { transaction ->
                                if (compact) ReportsTransactionMobileRow(transaction) else ReportsTransactionRow(transaction)
                                ReportDivider()
                            }
                        }
                    }
                }
            }
        }
        ReportsTransactionPagination(
            page = page,
            totalPages = totalPages,
            totalItems = totalItems,
            isLoading = isLoading,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage
        )
    }
}

@Composable
private fun ReportsTransactionMobileRow(transaction: CashTransaction) {
    val remaining = transaction.remainingAmount
        ?: transaction.total.subtract(transaction.paidAmount).coerceAtLeastZero()
    val displayStatus = if (remaining.signum() == 0) "Lunas" else transaction.status.toDisplayStatus()
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.receiptId, color = ReportColors.Primary, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodyMedium)
                Text(transaction.createdAt.toCompactReportDate(), color = ReportColors.Outline, style = MaterialTheme.typography.bodySmall)
            }
            TransactionStatusBadge(displayStatus)
        }
        Spacer(Modifier.height(12.dp))
        Text(transaction.customerName ?: "Pelanggan umum", color = ReportColors.OnSurface, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Total", color = ReportColors.Outline, style = MaterialTheme.typography.labelSmall)
                Text(transaction.total.toReportCurrency(), color = ReportColors.OnSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Sisa", color = ReportColors.Outline, style = MaterialTheme.typography.labelSmall)
                Text(remaining.toReportCurrency(), color = if (remaining.signum() > 0) ReportColors.Error else ReportColors.Outline, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ReportsTransactionHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ReportColors.SurfaceSoft)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableHeader("Transaksi", Modifier.weight(1.7f))
        TableHeader("Customer", Modifier.weight(1.25f))
        TableHeader("Total", Modifier.weight(1f), TextAlign.End)
        TableHeader("Sisa", Modifier.weight(0.85f), TextAlign.End)
    }
}

@Composable
private fun ReportsTransactionRow(transaction: CashTransaction) {
    val remaining = transaction.remainingAmount
        ?: transaction.total.subtract(transaction.paidAmount).coerceAtLeastZero()
    val displayStatus = if (remaining.signum() == 0) "Lunas" else transaction.status.toDisplayStatus()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.7f)) {
            TableCell(transaction.receiptId, Modifier.fillMaxWidth(), color = ReportColors.Primary, bold = true)
            Spacer(modifier = Modifier.height(3.dp))
            TableCell(
                text = transaction.createdAt.toCompactReportDate(),
                modifier = Modifier.fillMaxWidth(),
                color = ReportColors.Outline
            )
        }
        Column(modifier = Modifier.weight(1.25f)) {
            TableCell(transaction.customerName ?: "Umum", Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(5.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                TransactionStatusBadge(displayStatus)
            }
        }
        TableCell(transaction.total.toReportCurrency(), Modifier.weight(1f), TextAlign.End, bold = true)
        TableCell(
            text = remaining.toReportCurrency(),
            modifier = Modifier.weight(0.85f),
            textAlign = TextAlign.End,
            color = if (remaining.signum() > 0) ReportColors.Error else ReportColors.Outline,
            bold = remaining.signum() > 0
        )
    }
}

@Composable
private fun ReportsTransactionPagination(
    page: Int,
    totalPages: Int,
    totalItems: Long,
    isLoading: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    TbPagination(
        currentPage = page,
        totalPages = totalPages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        supportingText = "$totalItems data",
        isLoading = isLoading,
        testTag = "report-transactions-pagination",
    )
}

@Composable
private fun TransactionStatusBadge(text: String) {
    val color = when (text.lowercase()) {
        "lunas" -> ReportColors.Primary
        "hutang" -> ReportColors.Secondary
        "dp" -> ReportColors.Orange
        "batal" -> ReportColors.Error
        else -> ReportColors.Outline
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = color,
                fontWeight = FontWeight.ExtraBold
            )
        )
    }
}

@Composable
private fun ReportsTransactionLoading() {
    com.tbterminal.app.ui.components.SkeletonList(
        modifier = Modifier.fillMaxWidth(),
        itemCount = 5,
    )
}

@Composable
private fun ReportsTransactionError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = ReportColors.Error,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry, shape = RoundedCornerShape(14.dp)) {
            Text("Coba Lagi")
        }
    }
}

@Composable
private fun ReportsTransactionEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 196.dp)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            color = ReportColors.PrimarySoft,
            shape = CircleShape,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = ReportColors.Primary,
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Belum ada transaksi",
            color = ReportColors.OnSurface,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = "Data pada periode yang dipilih akan tampil di sini.",
            color = ReportColors.Outline,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TableHeader(
    text: String,
    modifier: Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = ReportColors.Outline,
        fontWeight = FontWeight.ExtraBold,
        style = MaterialTheme.typography.labelSmall,
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun TableCell(
    text: String,
    modifier: Modifier,
    textAlign: TextAlign = TextAlign.Start,
    color: Color = ReportColors.OnSurface,
    bold: Boolean = false
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Medium,
        style = MaterialTheme.typography.bodySmall,
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private fun String.toCompactReportDate(): String {
    return runCatching {
        OffsetDateTime.parse(this)
            .atZoneSameInstant(ZoneId.of("Asia/Jakarta"))
            .format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
    }.getOrElse {
        take(16).replace('T', ' ')
    }
}

private fun String.toDisplayStatus(): String {
    return when (lowercase()) {
        "paid", "lunas" -> "Lunas"
        "debt", "hutang" -> "Hutang"
        "dp" -> "DP"
        "cancelled", "canceled", "batal" -> "Batal"
        else -> replaceFirstChar { it.uppercase() }
    }
}

private fun BigDecimal.coerceAtLeastZero(): BigDecimal {
    return if (signum() < 0) BigDecimal.ZERO else this
}

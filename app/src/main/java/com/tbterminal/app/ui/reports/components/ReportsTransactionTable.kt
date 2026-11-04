package com.tbterminal.app.ui.reports.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.components.TbPagination
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val REPORTS_PAGE_SIZE = 10

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
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, ReportColors.OutlineSoft),
        ) {
            Column {
                if (showHeader) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daftar Transaksi",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ReportColors.OnSurface,
                        )
                    }
                    HorizontalDivider(color = ReportColors.Slate100)
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
                                transactions.forEachIndexed { index, transaction ->
                                    if (compact) {
                                        ReportsTransactionMobileRow(transaction)
                                    } else {
                                        ReportsTransactionRow(transaction)
                                    }
                                    if (index < transactions.lastIndex) {
                                        HorizontalDivider(color = ReportColors.Slate100)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (transactions.isNotEmpty()) {
            BoxWithConstraints {
                val compact = maxWidth < 680.dp
                ReportsTransactionPagination(
                    page = page,
                    totalPages = totalPages,
                    totalItems = totalItems,
                    compact = compact,
                    isLoading = isLoading,
                    onPreviousPage = onPreviousPage,
                    onNextPage = onNextPage
                )
            }
        }
    }
}

@Composable
private fun ReportsTransactionMobileRow(transaction: CashTransaction) {
    val remaining = transaction.remainingAmount
        ?: transaction.total.subtract(transaction.paidAmount).coerceAtLeastZero()
    val displayStatus = if (remaining.signum() == 0) "Lunas" else transaction.status.toDisplayStatus()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header: Icon + Details & Status Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, ReportColors.OutlineSoft),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Description,
                            contentDescription = null,
                            tint = Color(0xFF1E293B),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.receiptId,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ReportColors.OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${transaction.customerName ?: "Pelanggan umum"} • ${transaction.createdAt.toCompactReportDate()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = ReportColors.Outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            TransactionStatusBadge(displayStatus)
        }

        // Details Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "TOTAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.4.sp,
                )
                Text(
                    text = transaction.total.toReportCurrency(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReportColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "SISA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.4.sp,
                )
                Text(
                    text = remaining.toReportCurrency(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (remaining.signum() > 0) ReportColors.Error else ReportColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TableHeader("NO. TRANSAKSI & WAKTU", Modifier.weight(1.5f))
        TableHeader("PELANGGAN", Modifier.weight(1.3f))
        TableHeader("STATUS", Modifier.weight(0.9f))
        TableHeader("TOTAL", Modifier.weight(1.1f), TextAlign.End)
        TableHeader("SISA", Modifier.weight(1.1f), TextAlign.End)
    }
    HorizontalDivider(color = ReportColors.OutlineSoft)
}

@Composable
private fun ReportsTransactionRow(transaction: CashTransaction) {
    val remaining = transaction.remainingAmount
        ?: transaction.total.subtract(transaction.paidAmount).coerceAtLeastZero()
    val displayStatus = if (remaining.signum() == 0) "Lunas" else transaction.status.toDisplayStatus()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1.5f)) {
            TableCell(
                text = transaction.receiptId,
                modifier = Modifier.fillMaxWidth(),
                color = ReportColors.OnSurface,
                bold = true,
            )
            Spacer(modifier = Modifier.height(2.dp))
            TableCell(
                text = transaction.createdAt.toCompactReportDate(),
                modifier = Modifier.fillMaxWidth(),
                color = ReportColors.Outline,
            )
        }
        TableCell(
            text = transaction.customerName ?: "Pelanggan umum",
            modifier = Modifier.weight(1.3f),
            color = ReportColors.OnSurface,
        )
        Box(modifier = Modifier.weight(0.9f), contentAlignment = Alignment.CenterStart) {
            TransactionStatusBadge(displayStatus)
        }
        TableCell(
            text = transaction.total.toReportCurrency(),
            modifier = Modifier.weight(1.1f),
            textAlign = TextAlign.End,
            bold = true,
        )
        TableCell(
            text = remaining.toReportCurrency(),
            modifier = Modifier.weight(1.1f),
            textAlign = TextAlign.End,
            color = if (remaining.signum() > 0) ReportColors.Error else ReportColors.Outline,
            bold = remaining.signum() > 0,
        )
    }
}

@Composable
private fun ReportsTransactionPagination(
    page: Int,
    totalPages: Int,
    totalItems: Long,
    compact: Boolean,
    isLoading: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    val firstItem = if (totalItems == 0L) 0 else ((page - 1) * REPORTS_PAGE_SIZE) + 1
    val lastItem = minOf(page * REPORTS_PAGE_SIZE, totalItems.toInt())
    val text = if (compact) {
        if (totalItems == 0L) "0 data" else "$totalItems data"
    } else {
        if (totalItems == 0L) "0 data" else "Menampilkan $firstItem–$lastItem dari $totalItems data"
    }
    TbPagination(
        currentPage = page,
        totalPages = totalPages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        supportingText = text,
        isLoading = isLoading,
        compact = compact,
        testTag = "report-transactions-pagination",
    )
}

@Composable
private fun TransactionStatusBadge(text: String) {
    val (bg, textColor) = when (text.lowercase()) {
        "lunas" -> Color(0xFFECFDF5) to Color(0xFF059669)
        "hutang" -> Color(0xFFFEF3C7) to Color(0xFFD97706)
        "dp" -> Color(0xFFFFFBEB) to Color(0xFFB45309)
        "batal" -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
        else -> Color(0xFFF1F5F9) to Color(0xFF64748B)
    }
    Surface(
        shape = CircleShape,
        color = bg,
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun ReportsTransactionLoading() {
    Column {
        repeat(4) { index ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(16.dp),
                        shape = RoundedCornerShape(4.dp),
                    )
                    SkeletonBox(
                        modifier = Modifier
                            .width(60.dp)
                            .height(20.dp),
                        shape = CircleShape,
                    )
                }
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(12.dp),
                    shape = RoundedCornerShape(4.dp),
                )
            }
            if (index < 3) {
                HorizontalDivider(color = ReportColors.Slate100)
            }
        }
    }
}

@Composable
private fun ReportsTransactionError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            color = ReportColors.Error,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
        )
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ReportColors.Primary,
                contentColor = Color.White,
            ),
        ) {
            Text("Coba Lagi", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ReportsTransactionEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFFF1F5F9), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color(0xFF94A3B8),
            )
        }
        Text(
            text = "Belum Ada Transaksi",
            color = ReportColors.OnSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Data transaksi pada rentang waktu ini akan tampil di sini.",
            color = ReportColors.Outline,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TableHeader(
    text: String,
    modifier: Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = ReportColors.Outline,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp,
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun TableCell(
    text: String,
    modifier: Modifier,
    textAlign: TextAlign = TextAlign.Start,
    color: Color = ReportColors.OnSurface,
    bold: Boolean = false,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        fontSize = 13.sp,
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
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

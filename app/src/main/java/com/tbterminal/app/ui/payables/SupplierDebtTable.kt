package com.tbterminal.app.ui.payables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.draw.rotate

import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.withStyle

import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.ui.text.withStyle
import androidx.compose.material3.Icon

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.SupplierPayable
import com.tbterminal.app.ui.components.SkeletonBox

@Composable
internal fun DebtTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().background(DebtSoft).padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DebtHeaderText("Supplier", Modifier.weight(2f))
        DebtHeaderText("Pembelian", Modifier.weight(1.35f))
        DebtHeaderText("Total / dibayar", Modifier.weight(1.45f), Alignment.End)
        DebtHeaderText("Sisa", Modifier.weight(1.25f), Alignment.End)
        DebtHeaderText("Jatuh tempo", Modifier.weight(1.5f))
        DebtHeaderText("Status", Modifier.weight(1.1f), Alignment.CenterHorizontally)
        DebtHeaderText("Aksi", Modifier.weight(0.8f), Alignment.End)
    }
}

@Composable
internal fun SupplierDebtRows(
    modifier: Modifier,
    uiState: SupplierDebtUiState,
    onPayClick: (SupplierPayable) -> Unit,
    compact: Boolean = false,
    onSupplierDetailClick: (String) -> Unit = {},
) {
    when {
        uiState.isLoading && uiState.payables.isEmpty() -> SupplierDebtLoading(modifier, compact)
        uiState.errorMessage != null && uiState.payables.isEmpty() -> SupplierDebtError(modifier)
        uiState.filteredPayables.isEmpty() -> SupplierDebtEmpty(modifier)
        compact -> {
            val groupedBySupplier = uiState.filteredPayables.groupBy { it.supplierId.ifBlank { it.supplierName } }
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                groupedBySupplier.entries.take(SUPPLIER_DEBT_USERS_PER_PAGE).forEachIndexed { index, (supplierId, payables) ->
                    SupplierDebtGroupCard(
                        supplierId = supplierId,
                        payables = payables,
                        onPayClick = onPayClick,
                        onSupplierDetailClick = onSupplierDetailClick,
                        defaultExpanded = false
                    )
                }
            }
        }
        else -> Column(modifier = modifier.fillMaxWidth()) {
            uiState.filteredPayables.forEachIndexed { index, payable ->
                if (index > 0) HorizontalDivider(color = DebtLine)
                SupplierDebtRow(payable, onPayClick)
            }
        }
    }
}

@Composable
private fun SupplierDebtCompactCard(payable: SupplierPayable, onPayClick: (SupplierPayable) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("supplier-debt-${payable.id}"),
        color = DebtSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, DebtLine),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SupplierInitial(payable.supplierName)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        payable.supplierName,
                        style = MaterialTheme.typography.titleSmall,
                        color = DebtText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(payable.purchaseId.shortId(), style = MaterialTheme.typography.bodySmall, color = DebtMuted, maxLines = 1)
                }
                StatusBadge(payable.status)
            }

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Sisa hutang", style = MaterialTheme.typography.labelMedium, color = DebtMuted)
                Text(
                    payable.remainingAmount.currencyText(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = DebtText,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SupplierDebtAmountDetail("Total tagihan", payable.amount.currencyText(), Modifier.weight(1f))
                SupplierDebtAmountDetail("Sudah dibayar", payable.paidAmount.currencyText(), Modifier.weight(1f))
            }

            HorizontalDivider(color = DebtLine.copy(alpha = 0.75f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Jatuh tempo", style = MaterialTheme.typography.labelSmall, color = DebtMuted)
                    Text(
                        payable.dueDate.simpleDate(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = payable.dueDate.dueColor(),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(payable.dueDate.dueRelativeText(), style = MaterialTheme.typography.labelSmall, color = payable.dueDate.dueColor())
                }
                if (!payable.isPaid()) {
                    FilledTonalButton(
                        onClick = { onPayClick(payable) },
                        modifier = Modifier.height(48.dp).testTag("pay-supplier-debt-${payable.id}"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = DebtPrimary.copy(alpha = 0.14f),
                            contentColor = DebtPrimaryDark,
                        ),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp),
                    ) { Text("Bayar", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun SupplierDebtAmountDetail(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = DebtMuted)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = DebtText,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SupplierDebtRow(payable: SupplierPayable, onPayClick: (SupplierPayable) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag("supplier-debt-${payable.id}").padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(2f), verticalAlignment = Alignment.CenterVertically) {
            SupplierInitial(payable.supplierName)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    payable.supplierName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DebtText,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(payable.createdAt.simpleDate(), style = MaterialTheme.typography.labelSmall, color = DebtMuted)
            }
        }
        Text(
            payable.purchaseId.shortId(),
            Modifier.weight(1.35f),
            style = MaterialTheme.typography.bodySmall,
            color = DebtMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Column(modifier = Modifier.weight(1.45f).padding(end = 8.dp), horizontalAlignment = Alignment.End) {
            Text(payable.amount.currencyText(), style = MaterialTheme.typography.bodySmall, color = DebtText, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(payable.paidAmount.currencyText(), style = MaterialTheme.typography.labelSmall, color = DebtMuted, maxLines = 1)
        }
        Text(
            payable.remainingAmount.currencyText(),
            Modifier.weight(1.25f).padding(horizontal = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = DebtText,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
        Column(modifier = Modifier.weight(1.5f).padding(start = 8.dp)) {
            Text(payable.dueDate.simpleDate(), style = MaterialTheme.typography.bodySmall, color = DebtText, fontWeight = FontWeight.Medium)
            Text(payable.dueDate.dueRelativeText(), style = MaterialTheme.typography.labelSmall, color = payable.dueDate.dueColor(), maxLines = 1)
        }
        Box(modifier = Modifier.weight(1.1f), contentAlignment = Alignment.Center) { StatusBadge(payable.status) }
        Box(modifier = Modifier.weight(0.8f), contentAlignment = Alignment.CenterEnd) {
            if (payable.isPaid()) {
                Text("Lunas", style = MaterialTheme.typography.labelMedium, color = DebtMuted)
            } else {
                Button(
                    onClick = { onPayClick(payable) },
                    modifier = Modifier.height(48.dp).testTag("pay-supplier-debt-${payable.id}"),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DebtPrimaryDark),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Bayar", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun SupplierInitial(name: String) {
    Surface(color = DebtSoft, shape = CircleShape, modifier = Modifier.size(40.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(name.initial(), color = DebtPrimaryDark, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun SupplierDebtLoading(modifier: Modifier, compact: Boolean) {
    if (compact) {
        Column(
            modifier = modifier.fillMaxWidth().testTag("supplier-debt-skeleton"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            repeat(3) {
                SupplierDebtGroupCardSkeleton()
            }
        }
    } else {
        Box(modifier = modifier.fillMaxWidth().testTag("supplier-debt-skeleton")) {
            SupplierDebtTableDesktopSkeleton()
        }
    }
}

@Composable
private fun SupplierDebtGroupCardSkeleton() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 0.5.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SkeletonBox(Modifier.size(44.dp), shape = CircleShape)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SkeletonBox(Modifier.fillMaxWidth(0.55f).height(15.dp), shape = RoundedCornerShape(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            SkeletonBox(Modifier.width(90.dp).height(12.dp), shape = RoundedCornerShape(3.dp))
                            SkeletonBox(Modifier.width(60.dp).height(10.dp), shape = RoundedCornerShape(3.dp))
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SkeletonBox(Modifier.width(46.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                    SkeletonBox(Modifier.size(20.dp), shape = CircleShape)
                }
            }
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SkeletonBox(Modifier.width(100.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                    SkeletonBox(Modifier.width(50.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SkeletonBox(Modifier.width(70.dp).height(12.dp), shape = RoundedCornerShape(3.dp))
                        SkeletonBox(Modifier.width(100.dp).height(18.dp), shape = RoundedCornerShape(4.dp))
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SkeletonBox(Modifier.width(45.dp).height(12.dp), shape = RoundedCornerShape(3.dp))
                        SkeletonBox(Modifier.width(65.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                    }
                }
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SkeletonBox(Modifier.width(80.dp).height(12.dp), shape = RoundedCornerShape(3.dp))
                        SkeletonBox(Modifier.width(90.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SkeletonBox(Modifier.width(80.dp).height(12.dp), shape = RoundedCornerShape(3.dp))
                        SkeletonBox(Modifier.width(50.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SupplierDebtTableDesktopSkeleton() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DebtSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, DebtLine),
    ) {
        Column {
            DebtTableHeader()
            repeat(5) { index ->
                if (index > 0) HorizontalDivider(color = DebtLine)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(modifier = Modifier.weight(2f), verticalAlignment = Alignment.CenterVertically) {
                        SkeletonBox(Modifier.size(34.dp), shape = CircleShape)
                        Spacer(Modifier.width(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SkeletonBox(Modifier.width(110.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                            SkeletonBox(Modifier.width(75.dp).height(11.dp), shape = RoundedCornerShape(3.dp))
                        }
                    }
                    Box(Modifier.weight(1.35f)) {
                        SkeletonBox(Modifier.width(85.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                    }
                    Column(Modifier.weight(1.45f), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        SkeletonBox(Modifier.width(90.dp).height(13.dp), shape = RoundedCornerShape(4.dp))
                        SkeletonBox(Modifier.width(65.dp).height(11.dp), shape = RoundedCornerShape(3.dp))
                    }
                    Box(Modifier.weight(1.25f), contentAlignment = Alignment.CenterEnd) {
                        SkeletonBox(Modifier.width(80.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                    }
                    Box(Modifier.weight(1.5f)) {
                        SkeletonBox(Modifier.width(85.dp).height(13.dp), shape = RoundedCornerShape(4.dp))
                    }
                    Box(Modifier.weight(1.1f), contentAlignment = Alignment.Center) {
                        SkeletonBox(Modifier.width(62.dp).height(22.dp), shape = RoundedCornerShape(6.dp))
                    }
                    Box(Modifier.weight(0.8f), contentAlignment = Alignment.CenterEnd) {
                        SkeletonBox(Modifier.width(64.dp).height(28.dp), shape = RoundedCornerShape(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun SupplierDebtEmpty(modifier: Modifier = Modifier, searchQuery: String = "") {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 36.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFF1F5F9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (searchQuery.isNotBlank()) Icons.Outlined.Search else Icons.AutoMirrored.Outlined.ReceiptLong,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = if (searchQuery.isNotBlank()) "Hutang tidak ditemukan" else "Belum ada data hutang",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = if (searchQuery.isNotBlank()) "Tidak ada hutang supplier yang cocok dengan \"$searchQuery\"" else "Data kewajiban hutang supplier akan muncul di sini setelah transaksi pembelian tempo dicatat.",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun SupplierDebtError(modifier: Modifier) {
    SupplierDebtFeedback(modifier, "Hutang supplier gagal dimuat", "Tarik layar ke bawah untuk mencoba lagi.", DebtDanger)
}

@Composable
private fun SupplierDebtFeedback(
    modifier: Modifier,
    title: String,
    message: String,
    titleColor: androidx.compose.ui.graphics.Color,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DebtSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, DebtLine),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 46.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = titleColor, fontWeight = FontWeight.SemiBold)
            Text(message, style = MaterialTheme.typography.bodySmall, color = DebtMuted, textAlign = TextAlign.Center)
        }
    }
}

private fun SupplierPayable.isPaid(): Boolean = status.equals(SupplierDebtStatusFilter.Paid.apiValue, ignoreCase = true)



@Composable
internal fun SupplierDebtGroupCard(
    supplierId: String,
    payables: List<com.tbterminal.app.data.model.SupplierPayable>,
    onPayClick: (com.tbterminal.app.data.model.SupplierPayable) -> Unit,
    onSupplierDetailClick: (String) -> Unit = {},
    defaultExpanded: Boolean = false,
) {
    val supplierName = payables.firstOrNull()?.supplierName ?: "Unknown"
    val totalRemaining = payables.sumOf { it.remainingAmount }
    val allPaid = totalRemaining <= java.math.BigDecimal.ZERO
    var isExpanded by remember { mutableStateOf(defaultExpanded) }
    
    val brand600 = Color(0xFF256B57)
    val slate100 = Color(0xFFF1F5F9)
    val slate200 = Color(0xFFE2E8F0)
    val slate400 = Color(0xFF94A3B8)
    val slate500 = Color(0xFF64748B)
    val slate700 = Color(0xFF334155)
    val slate900 = Color(0xFF0F172A)
    
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevronRotation"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("supplier-group-$supplierId"),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, slate200),
        shadowElevation = 0.5.dp
    ) {
        Column {
            // Header Accordion
            Surface(
                onClick = { isExpanded = !isExpanded },
                color = Color.White,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Supplier Avatar Pill (44.dp x 44.dp, bg #F1F5F9, border #E2E8F0, text #334155)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(slate100, CircleShape)
                                .border(1.dp, slate200, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = supplierName.take(1).uppercase(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = slate700
                            )
                        }
                        
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = supplierName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = totalRemaining.currencyText(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = brand600,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (allPaid) "(Semua lunas)" else "(Belum lunas)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = slate400,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    
                    // Header Badge & Chevron Action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${payables.size} Nota",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = slate500,
                            maxLines = 1,
                            softWrap = false
                        )
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Tutup nota" else "Buka nota",
                            tint = slate400,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(rotationState)
                        )
                    }
                }
            }
            
            // Expanded Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = slate100)
                    
                    val limit = 3
                    val itemsToDisplay = payables.take(limit)
                    val remainingCount = payables.size - limit
                    
                    itemsToDisplay.forEachIndexed { index, payable ->
                        if (index > 0) {
                            HorizontalDivider(color = slate100)
                        }
                        SupplierDebtAccordionItem(
                            payable = payable,
                            index = index,
                            onPayClick = onPayClick
                        )
                    }
                    
                    HorizontalDivider(color = slate100)
                    Surface(
                        onClick = { onSupplierDetailClick(supplierId) },
                        color = Color.White,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val text = if (remainingCount > 0) {
                                "Lihat $remainingCount nota lainnya & detail →"
                            } else {
                                "Lihat Detail Hutang →"
                            }
                            Text(
                                text = text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = brand600
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupplierDebtAccordionItem(
    payable: com.tbterminal.app.data.model.SupplierPayable,
    index: Int,
    onPayClick: (com.tbterminal.app.data.model.SupplierPayable) -> Unit
) {
    val isPaid = payable.isPaid()
    val brand600 = Color(0xFF256B57)
    val slate50 = Color(0xFFF8FAFC)
    val slate100 = Color(0xFFF1F5F9)
    val slate400 = Color(0xFF94A3B8)
    val slate500 = Color(0xFF64748B)
    val slate700 = Color(0xFF334155)
    val slate800 = Color(0xFF1E293B)
    val amber700 = Color(0xFFB45309)
    val emerald500 = Color(0xFF10B981)
    val emerald600 = Color(0xFF059669)
    val red600 = Color(0xFFDC2626)

    val isOverdue = remember(payable.dueDate) {
        val date = runCatching { java.time.LocalDate.parse(payable.dueDate.take(10)) }.getOrNull()
        if (date != null) {
            java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), date) < 0
        } else false
    }

    val itemBg = if (isPaid) slate50.copy(alpha = 0.5f) else Color.White

    Surface(
        onClick = { if (!isPaid) onPayClick(payable) },
        color = itemBg,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("supplier-debt-${payable.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Line 1: Nota Number / ID & Index
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                        contentDescription = null,
                        tint = slate400,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = payable.purchaseId.shortId(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = slate800,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                Text(
                    text = "Nota ${index + 1}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = slate500
                )
            }
            
            // Line 2: Sisa Hutang (Left) & Status (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Sisa Hutang",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = slate500
                    )
                    Text(
                        text = payable.remainingAmount.currencyText(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaid) slate700 else brand600,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Status",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = slate500
                    )
                    val (statusText, statusColor) = when {
                        isPaid -> "Terbayar Penuh" to brand600
                        payable.paidAmount > java.math.BigDecimal.ZERO -> "Sebagian" to amber700
                        else -> "Belum Bayar" to red600
                    }
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            
            // Line 3: Total Tagihan & Sudah Dibayar
            HorizontalDivider(color = slate100)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Tagihan",
                        fontSize = 12.sp,
                        color = slate500
                    )
                    Text(
                        text = payable.amount.currencyText(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = slate700,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Sudah Dibayar",
                        fontSize = 12.sp,
                        color = slate500
                    )
                    val paidColor = if (payable.paidAmount > java.math.BigDecimal.ZERO) brand600 else slate700
                    Text(
                        text = payable.paidAmount.currencyText(),
                        fontSize = 14.sp,
                        fontWeight = if (payable.paidAmount > java.math.BigDecimal.ZERO) FontWeight.SemiBold else FontWeight.Medium,
                        color = paidColor,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            
            // Line 4: Jatuh tempo & Status Dot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Jatuh tempo: ",
                        fontSize = 12.sp,
                        color = slate500
                    )
                    Text(
                        text = payable.dueDate.simpleDate(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = slate700
                    )
                }
                
                val isPartial = !isPaid && (payable.paidAmount > java.math.BigDecimal.ZERO || payable.status.equals("sebagian", true) || payable.status.equals("partial", true))
                val (dotColor, dotTextColor, dotText) = when {
                    isPaid -> Triple(emerald500, emerald600, "Lunas")
                    isOverdue -> Triple(red600, red600, "Lewat Jatuh Tempo")
                    isPartial -> Triple(amber700, amber700, "Sebagian")
                    else -> Triple(red600, red600, "Belum Lunas")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(dotColor, CircleShape)
                    )
                    Text(
                        text = dotText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = dotTextColor
                    )
                }
            }
        }
    }
}

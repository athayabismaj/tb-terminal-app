package com.tbterminal.app.ui.payables

import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Receipt
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
                groupedBySupplier.forEach { (supplierId, payables) ->
                    SupplierDebtGroupCard(supplierId, payables, onPayClick, onSupplierDetailClick)
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
    Column(
        modifier = modifier.fillMaxWidth().testTag("supplier-debt-skeleton"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(if (compact) 4 else 5) {
            SkeletonBox(Modifier.fillMaxWidth().height(if (compact) 190.dp else 62.dp))
        }
    }
}

@Composable
internal fun SupplierDebtEmpty(modifier: Modifier) {
    SupplierDebtFeedback(modifier, "Hutang supplier tidak ditemukan", "Coba ubah pencarian atau filter yang digunakan.", DebtText)
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
    onSupplierDetailClick: (String) -> Unit = {}
) {
    val supplierName = payables.firstOrNull()?.supplierName ?: "Unknown"
    val totalRemaining = payables.sumOf { it.remainingAmount }
    val allPaid = totalRemaining <= java.math.BigDecimal.ZERO
    var isExpanded by remember { mutableStateOf(false) }
    
    val brand50 = Color(0xFFF0F6F3)
    val brand100 = Color(0xFFE1ECE6)
    val brand600 = Color(0xFF256B57)
    val brand700 = Color(0xFF1D5545)
    val slate100 = Color(0xFFF1F5F9)
    val slate200 = Color(0xFFE2E8F0)
    val slate400 = Color(0xFF94A3B8)
    val slate600 = Color(0xFF475569)
    val slate700 = Color(0xFF334155)
    val slate900 = Color(0xFF0F172A)
    val surfaceBorder = Color(0xFFE5EBE8)
    
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth().testTag("supplier-group-$supplierId"),
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, surfaceBorder),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Surface(
                onClick = { isExpanded = !isExpanded },
                color = Color.White,
            ) {
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
                        val circleBg = if (allPaid) brand50 else slate100
                        val circleBorder = if (allPaid) brand100 else slate200
                        val circleText = if (allPaid) brand700 else slate700
                        
                        Box(
                            modifier = Modifier.size(40.dp).background(circleBg, CircleShape).border(1.dp, circleBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(supplierName.take(1).uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = circleText)
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = supplierName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                val badgeBg = if (allPaid) brand50 else slate100
                                val badgeText = if (allPaid) brand600 else slate600
                                Surface(color = badgeBg, shape = RoundedCornerShape(50)) {
                                    Text(
                                        text = "${payables.size} Nota",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeText,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                val amountColor = if (allPaid) brand600 else brand700
                                Text(totalRemaining.currencyText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = amountColor, maxLines = 1)
                                Text(if (allPaid) "(Semua lunas)" else "(Belum lunas)", fontSize = 10.sp, color = slate400, maxLines = 1)
                            }
                        }
                    }
                    
                    Icon(
                        if (isExpanded) androidx.compose.material.icons.Icons.Default.ExpandLess else androidx.compose.material.icons.Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = slate400,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            if (isExpanded) {
                HorizontalDivider(color = surfaceBorder)
                Column(modifier = Modifier.fillMaxWidth()) {
                    val limit = 3
                    val itemsToDisplay = payables.take(limit)
                    val remainingCount = payables.size - limit
                    
                    itemsToDisplay.forEachIndexed { index, payable ->
                        if (index > 0) {
                            HorizontalDivider(color = surfaceBorder.copy(alpha = 0.8f))
                        }
                        SupplierDebtAccordionItem(payable = payable, index = index, onPayClick = onPayClick)
                    }
                    
                    HorizontalDivider(color = surfaceBorder)
                    Surface(
                        onClick = { onSupplierDetailClick(supplierId) },
                        color = Color(0xFFF8FAF9),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val text = if (remainingCount > 0) "Lihat $remainingCount nota lainnya & detail →" else "Lihat Detail Hutang →"
                            Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = brand600)
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
    val brand50 = Color(0xFFF0F6F3)
    val brand600 = Color(0xFF256B57)
    val brand700 = Color(0xFF1D5545)
    val slate100 = Color(0xFFF1F5F9)
    val slate400 = Color(0xFF94A3B8)
    val slate500 = Color(0xFF64748B)
    val slate700 = Color(0xFF334155)
    val slate800 = Color(0xFF1E293B)
    val slate900 = Color(0xFF0F172A)

    Surface(
        onClick = { if (!isPaid) onPayClick(payable) },
        color = Color.White,
        modifier = Modifier.testTag("supplier-debt-${payable.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(androidx.compose.material.icons.Icons.Outlined.Receipt, contentDescription = null, tint = slate400, modifier = Modifier.size(18.dp))
                    Text(payable.purchaseId.shortId(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = slate900, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
                Surface(color = brand50, shape = RoundedCornerShape(50)) {
                    Text("Nota ${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = brand600, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("SISA HUTANG", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = slate400, letterSpacing = 0.5.sp)
                    Text(payable.remainingAmount.currencyText(), fontSize = 20.sp, fontWeight = FontWeight.Black, color = if (isPaid) slate900 else brand700, modifier = Modifier.padding(top = 2.dp))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("STATUS", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = slate400, letterSpacing = 0.5.sp)
                    Text(if (isPaid) "Terbayar Penuh" else "Sebagian", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isPaid) brand600 else slate700, modifier = Modifier.padding(top = 2.dp))
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth().drawBehind {
                drawLine(
                    color = slate100,
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = 1f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }.padding(top = 8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("TOTAL TAGIHAN", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = slate400, letterSpacing = 0.5.sp)
                    Text(payable.amount.currencyText(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = slate800, modifier = Modifier.padding(top = 2.dp))
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("SUDAH DIBAYAR", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = slate400, letterSpacing = 0.5.sp)
                    Text(payable.paidAmount.currencyText(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isPaid) slate800 else brand600, modifier = Modifier.padding(top = 2.dp))
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth().drawBehind {
                drawLine(
                    color = slate100, 
                    start = androidx.compose.ui.geometry.Offset(0f, 0f), 
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f), 
                    strokeWidth = 1f
                )
            }.padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    androidx.compose.ui.text.buildAnnotatedString {
                        withStyle(androidx.compose.ui.text.SpanStyle(color = slate500)) { append("Jatuh tempo: ") }
                        withStyle(androidx.compose.ui.text.SpanStyle(color = slate800, fontWeight = FontWeight.SemiBold)) { append(payable.dueDate.simpleDate()) }
                    },
                    fontSize = 12.sp
                )
                if (!isPaid) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(brand600, CircleShape))
                        Text("Aktif", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = brand600)
                    }
                }
            }
        }
    }
}

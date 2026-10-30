package com.tbterminal.app.ui.suppliers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.ui.components.AppConfirmDialog
import com.tbterminal.app.ui.components.AppConfirmationSpec
import com.tbterminal.app.ui.components.SkeletonBox

@Composable
internal fun SupplierMessage(message: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = SupplierEmerald50,
        border = BorderStroke(1.dp, SupplierEmerald100),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = SupplierEmerald600,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = message,
                    color = SupplierEmerald700,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Tutup",
                    tint = SupplierEmerald700,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
internal fun SupplierLoadingState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SupplierSlate200),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            repeat(4) { index ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SkeletonBox(modifier = Modifier.size(40.dp), shape = CircleShape)
                            SkeletonBox(modifier = Modifier.size(width = 140.dp, height = 16.dp))
                        }
                        SkeletonBox(modifier = Modifier.size(width = 60.dp, height = 22.dp), shape = RoundedCornerShape(999.dp))
                    }
                    SkeletonBox(modifier = Modifier.size(width = 180.dp, height = 14.dp))
                    SkeletonBox(modifier = Modifier.size(width = 240.dp, height = 14.dp))
                    HorizontalDivider(color = SupplierSlate100, thickness = 1.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        SkeletonBox(modifier = Modifier.size(width = 110.dp, height = 14.dp))
                        SkeletonBox(modifier = Modifier.size(width = 50.dp, height = 18.dp))
                    }
                }
                if (index < 3) {
                    HorizontalDivider(color = SupplierSlate100, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
internal fun SupplierEmptyState(query: String = "") {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SupplierSlate200),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(SupplierEmerald50, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (query.isNotBlank()) Icons.Outlined.SearchOff else Icons.Outlined.Inventory2,
                    contentDescription = null,
                    tint = SupplierEmerald600,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = if (query.isNotBlank()) "Supplier tidak ditemukan" else "Belum ada supplier",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = SupplierSlate900,
            )
            Text(
                text = if (query.isNotBlank()) {
                    "Tidak ada supplier dengan kata kunci \"$query\""
                } else {
                    "Tambahkan data supplier untuk pencatatan restok barang."
                },
                fontSize = 13.sp,
                color = SupplierSlate500,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

@Composable
internal fun SupplierDeactivateDialog(supplier: Supplier, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AppConfirmDialog(
        spec = AppConfirmationSpec(
            title = "Nonaktifkan supplier",
            target = supplier.name,
            consequence = "Supplier tidak dapat dipakai pada restok baru setelah dinonaktifkan.",
            confirmLabel = "Nonaktifkan",
        ),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
internal fun SupplierTableLabel(text: String, modifier: Modifier, alignment: Alignment.Horizontal = Alignment.Start) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(text, color = SupplierSlate400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SupplierHeaderAddButton(
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (compact) {
        IconButton(
            onClick = onClick,
            modifier = modifier
                .padding(end = 4.dp)
                .testTag("supplier-add"),
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Tambah supplier",
                tint = Color(0xFF1E293B),
                modifier = Modifier.size(26.dp),
            )
        }
        return
    }
    Button(
        onClick = onClick,
        modifier = modifier
            .padding(end = 12.dp)
            .height(42.dp)
            .testTag("supplier-add"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SupplierEmerald100,
            contentColor = SupplierEmerald700,
        ),
        border = BorderStroke(1.dp, SupplierEmeraldBorder),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = SupplierEmerald700,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Tambah Supplier",
            color = SupplierEmerald700,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SupplierFilterSheet(
    statusFilter: SupplierStatusFilter,
    onApplyFilter: (SupplierStatusFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempStatusFilter by remember(statusFilter) { mutableStateOf(statusFilter) }
    var statusExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 4.dp)
                        .background(Color(0xFFCBD5E1), RoundedCornerShape(999.dp)),
                )
            }
        },
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp)
                .testTag("supplier-filter-sheet"),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Header: Title, Description, and Close Button
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Filter Supplier",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        lineHeight = 24.sp,
                    )
                    Text(
                        text = "Atur status supplier",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF64748B),
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup filter",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Filter Options Section: STATUS
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "STATUS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF94A3B8),
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { statusExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = tempStatusFilter.label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155),
                            )
                            Icon(
                                imageVector = Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFF94A3B8),
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false },
                        shape = RoundedCornerShape(12.dp),
                        containerColor = Color.White,
                    ) {
                        SupplierStatusFilter.entries.forEach { statusOption ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = statusOption.label,
                                        fontSize = 13.sp,
                                        fontWeight = if (tempStatusFilter == statusOption) FontWeight.Bold else FontWeight.Normal,
                                        color = if (tempStatusFilter == statusOption) SupplierEmerald600 else Color(0xFF1E293B),
                                    )
                                },
                                onClick = {
                                    tempStatusFilter = statusOption
                                    statusExpanded = false
                                },
                                modifier = Modifier.testTag("supplier-status-option-${statusOption.name}")
                            )
                        }
                    }
                }
            }

            // Action Button: Terapkan
            Button(
                onClick = {
                    onApplyFilter(tempStatusFilter)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("supplier-filter-done"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SupplierEmerald700),
            ) {
                Text(
                    text = "Terapkan",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }

            // iOS Home Indicator Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 128.dp, height = 4.dp)
                        .background(Color(0xFF0F172A), RoundedCornerShape(999.dp)),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SupplierSummarySheet(
    uiState: SupplierUiState,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activeCount = uiState.suppliers.count { it.isActive }
    val inactiveCount = uiState.suppliers.count { !it.isActive }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .background(Color(0xFFCBD5E1), RoundedCornerShape(999.dp)),
                )
            }
        },
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp)
                .testTag("supplier-summary-sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Sheet Header with Title, Description, and Close Button
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Ringkasan Supplier",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        lineHeight = 24.sp,
                    )
                    Text(
                        text = "Rincian data dan status supplier",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF64748B),
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup Ringkasan",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Metrics List: divide-y divide-slate-100 (simpel tanpa card)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("metrics-summary-grid"),
            ) {
                SupplierSummaryRow(
                    label = "Total Supplier",
                    value = uiState.totalSuppliers.toString(),
                )
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                SupplierSummaryRow(
                    label = "Supplier Aktif",
                    value = activeCount.toString(),
                )
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                SupplierSummaryRow(
                    label = "Supplier Nonaktif",
                    value = inactiveCount.toString(),
                )
            }

            // Primary Action: Button Selesai
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("mobile-sheet-done"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SupplierEmerald700),
            ) {
                Text(
                    text = "Selesai",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }

            // Bottom Home Indicator Bar (iOS style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 128.dp, height = 4.dp)
                        .background(Color(0xFF1E293B).copy(alpha = 0.6f), RoundedCornerShape(999.dp)),
                )
            }
        }
    }
}

@Composable
private fun SupplierSummaryRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF64748B),
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
        )
    }
}

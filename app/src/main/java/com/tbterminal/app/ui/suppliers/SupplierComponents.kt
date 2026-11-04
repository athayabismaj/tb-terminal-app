package com.tbterminal.app.ui.suppliers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
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
import androidx.compose.material3.OutlinedButton
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
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(36.dp)
            .testTag("supplier-add"),
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Tambah supplier",
            tint = Color(0xFF1E293B),
            modifier = Modifier.size(22.dp),
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
    var tempStatus by remember(statusFilter) { mutableStateOf(statusFilter) }

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
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .background(Color(0xFFCBD5E1), RoundedCornerShape(999.dp))
                )
            }
        },
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Filter Supplier",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Sesuaikan kriteria daftar supplier",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Status Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "STATUS SUPPLIER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF64748B)
                )
                SupplierFilterOptionCard(
                    title = "Semua Status",
                    selected = tempStatus == SupplierStatusFilter.ALL,
                    onClick = { tempStatus = SupplierStatusFilter.ALL },
                    testTag = "supplier-status-option-ALL"
                )
                SupplierFilterOptionCard(
                    title = "Aktif",
                    selected = tempStatus == SupplierStatusFilter.ACTIVE,
                    onClick = { tempStatus = SupplierStatusFilter.ACTIVE },
                    testTag = "supplier-status-option-ACTIVE"
                )
                SupplierFilterOptionCard(
                    title = "Nonaktif",
                    selected = tempStatus == SupplierStatusFilter.INACTIVE,
                    onClick = { tempStatus = SupplierStatusFilter.INACTIVE },
                    testTag = "supplier-status-option-INACTIVE"
                )
            }

            // Sticky footer buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        tempStatus = SupplierStatusFilter.ALL
                        onApplyFilter(SupplierStatusFilter.ALL)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Text("Reset", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        onApplyFilter(tempStatus)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("supplier-filter-done"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF256B57),
                        contentColor = Color.White
                    )
                ) {
                    Text("Terapkan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SupplierFilterOptionCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFFEAF3EF) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (selected) Color(0xFF256B57) else Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color(0xFF256B57) else Color(0xFF334155)
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color(0xFF256B57),
                    modifier = Modifier.size(18.dp)
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
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .background(Color(0xFFCBD5E1), RoundedCornerShape(999.dp))
                )
            }
        },
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .testTag("supplier-summary-sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ringkasan Supplier",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup ringkasan",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Hero Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "TOTAL SUPPLIER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "${uiState.totalSuppliers} Supplier",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF256B57)
                    )
                }
            }

            // Metrics
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Supplier Aktif", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                Text("$activeCount Supplier", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
            }
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Supplier Nonaktif", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                Text("$inactiveCount Supplier", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("mobile-sheet-done"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF256B57),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text("Selesai", fontSize = 14.sp, fontWeight = FontWeight.Bold)
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

package com.tbterminal.app.ui.suppliers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.Supplier

@Composable
internal fun SupplierTable(
    suppliers: List<Supplier>,
    onEdit: (Supplier) -> Unit,
    onDelete: (Supplier) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("supplier-table"),
        color = SupplierSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SupplierSlate200),
    ) {
        Column(Modifier.fillMaxWidth()) {
            SupplierTableHeader()
            suppliers.forEachIndexed { index, supplier ->
                SupplierTableRow(supplier, onEdit, onDelete)
                if (index < suppliers.lastIndex) {
                    HorizontalDivider(color = SupplierSlate100, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
internal fun SupplierMobileList(
    suppliers: List<Supplier>,
    onEdit: (Supplier) -> Unit,
    onDelete: (Supplier) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("supplier-card-list"),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SupplierSlate200),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            suppliers.forEachIndexed { index, supplier ->
                SupplierMobileCard(supplier, onEdit, onDelete)
                if (index < suppliers.lastIndex) {
                    HorizontalDivider(color = SupplierSlate100, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun SupplierTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SupplierSlate50)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SupplierTableLabel("Supplier", Modifier.weight(2.4f))
        SupplierTableLabel("Kontak", Modifier.weight(1.5f))
        SupplierTableLabel("Termin", Modifier.weight(0.9f))
        SupplierTableLabel("Status", Modifier.weight(1.1f))
        SupplierTableLabel("Aksi", Modifier.weight(0.7f), Alignment.End)
    }
}

@Composable
private fun SupplierTableRow(
    supplier: Supplier,
    onEdit: (Supplier) -> Unit,
    onDelete: (Supplier) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(2.4f), verticalAlignment = Alignment.CenterVertically) {
            SupplierAvatar(supplier.name)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = supplier.name,
                    color = SupplierSlate900,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = supplier.address ?: "Alamat belum diisi",
                    color = SupplierSlate500,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = supplier.phone ?: "-",
            modifier = Modifier.weight(1.5f),
            color = SupplierSlate600,
            fontSize = 13.sp,
            maxLines = 1,
        )
        Text(
            text = "${supplier.paymentTermDays} hari",
            modifier = Modifier.weight(0.9f),
            color = SupplierSlate900,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        Box(Modifier.weight(1.1f)) {
            SupplierStatusBadge(supplier.isActive, Modifier.testTag("supplier-status-${supplier.id}"))
        }
        Box(Modifier.weight(0.7f), contentAlignment = Alignment.CenterEnd) {
            SupplierActionsMenu(supplier, onEdit, onDelete)
        }
    }
}

@Composable
private fun SupplierMobileCard(
    supplier: Supplier,
    onEdit: (Supplier) -> Unit,
    onDelete: (Supplier) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("supplier-card-${supplier.id}")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Item Header: Avatar, Name & Status Inline, More Menu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SupplierAvatar(supplier.name)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = supplier.name,
                        color = SupplierSlate900,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    SupplierStatusInline(
                        isActive = supplier.isActive,
                        modifier = Modifier.testTag("supplier-status-${supplier.id}"),
                    )
                }
            }
            SupplierActionsMenu(supplier, onEdit, onDelete)
        }

        // Contact & Address Clean List
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Telepon Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = SupplierEmerald600,
                )
                Text(
                    text = "Telepon:",
                    fontSize = 12.sp,
                    color = SupplierSlate500,
                    fontWeight = FontWeight.Normal,
                )
                if (supplier.phone.isNullOrBlank()) {
                    Text(
                        text = "Belum diisi",
                        fontSize = 12.sp,
                        color = SupplierSlate400,
                        fontStyle = FontStyle.Italic,
                    )
                } else {
                    Text(
                        text = supplier.phone,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SupplierSlate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Alamat Row
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).padding(top = 1.dp),
                    tint = SupplierEmerald600,
                )
                Text(
                    text = "Alamat:",
                    fontSize = 12.sp,
                    color = SupplierSlate500,
                    fontWeight = FontWeight.Normal,
                )
                if (supplier.address.isNullOrBlank()) {
                    Text(
                        text = "Belum diisi",
                        fontSize = 12.sp,
                        color = SupplierSlate400,
                        fontStyle = FontStyle.Italic,
                    )
                } else {
                    Text(
                        text = supplier.address,
                        fontSize = 12.sp,
                        color = SupplierSlate700,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Divider & Payment Term (Clean inline text without container)
        HorizontalDivider(color = SupplierSlate100, thickness = 1.dp)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Termin Pembayaran",
                fontSize = 12.sp,
                color = SupplierSlate500,
                fontWeight = FontWeight.Normal,
            )
            Text(
                text = "${supplier.paymentTermDays} hari",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = SupplierSlate900,
            )
        }
    }
}

@Composable
private fun SupplierStatusInline(isActive: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isActive) SupplierEmerald600 else SupplierSlate400),
        )
        Text(
            text = if (isActive) "Aktif" else "Nonaktif",
            color = if (isActive) SupplierEmerald600 else SupplierSlate500,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SupplierAvatar(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(SupplierEmerald100),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().firstOrNull()?.uppercase() ?: "S",
            color = SupplierEmerald600,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SupplierStatusBadge(isActive: Boolean, modifier: Modifier = Modifier) {
    val bg = if (isActive) SupplierEmerald100 else SupplierSlate100
    val fg = if (isActive) SupplierEmerald600 else SupplierSlate500

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(fg),
        )
        Text(
            text = if (isActive) "Aktif" else "Nonaktif",
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SupplierActionsMenu(
    supplier: Supplier,
    onEdit: (Supplier) -> Unit,
    onDelete: (Supplier) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(30.dp).testTag("supplier-actions-${supplier.id}"),
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Opsi supplier",
                modifier = Modifier.size(18.dp),
                tint = SupplierSlate400,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SupplierSurface, RoundedCornerShape(12.dp)),
        ) {
            DropdownMenuItem(
                text = { Text("Edit supplier", fontSize = 13.sp, color = SupplierSlate900) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = SupplierSlate700,
                    )
                },
                onClick = {
                    expanded = false
                    onEdit(supplier)
                },
                modifier = Modifier.heightIn(min = 44.dp),
            )
            if (supplier.isActive) {
                DropdownMenuItem(
                    text = { Text("Nonaktifkan", fontSize = 13.sp, color = SupplierDanger) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = SupplierDanger,
                        )
                    },
                    onClick = {
                        expanded = false
                        onDelete(supplier)
                    },
                    modifier = Modifier.heightIn(min = 44.dp),
                )
            }
        }
    }
}

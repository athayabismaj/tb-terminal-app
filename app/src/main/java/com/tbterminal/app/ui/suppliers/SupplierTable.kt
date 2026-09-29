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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
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
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, SupplierSlate200.copy(alpha = 0.8f)),
    ) {
        Column(Modifier.fillMaxWidth()) {
            SupplierTableHeader()
            suppliers.forEachIndexed { index, supplier ->
                SupplierTableRow(supplier, onEdit, onDelete)
                if (index < suppliers.lastIndex) {
                    HorizontalDivider(Modifier.padding(start = 24.dp), color = SupplierSlate200.copy(alpha = 0.55f))
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
    Column(
        modifier = Modifier.fillMaxWidth().testTag("supplier-card-list"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        suppliers.forEach { supplier -> SupplierMobileCard(supplier, onEdit, onDelete) }
    }
}

@Composable
private fun SupplierTableHeader() {
    Row(
        Modifier.fillMaxWidth().background(SupplierSlate50.copy(alpha = 0.55f)).padding(horizontal = 24.dp, vertical = 14.dp),
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
private fun SupplierTableRow(supplier: Supplier, onEdit: (Supplier) -> Unit, onDelete: (Supplier) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(2.4f), verticalAlignment = Alignment.CenterVertically) {
            SupplierAvatar(supplier.name)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(supplier.name, color = SupplierSlate900, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(supplier.address ?: "Alamat belum diisi", color = SupplierSlate500, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(supplier.phone ?: "-", Modifier.weight(1.5f), color = SupplierSlate500, fontSize = 13.sp, maxLines = 1)
        Text("${supplier.paymentTermDays} hari", Modifier.weight(0.9f), color = SupplierSlate900, fontSize = 13.sp)
        Box(Modifier.weight(1.1f)) { SupplierStatusBadge(supplier.isActive) }
        Box(Modifier.weight(0.7f), contentAlignment = Alignment.CenterEnd) {
            SupplierActionsMenu(supplier, onEdit, onDelete)
        }
    }
}

@Composable
private fun SupplierMobileCard(supplier: Supplier, onEdit: (Supplier) -> Unit, onDelete: (Supplier) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("supplier-card-${supplier.id}"),
        color = SupplierSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, com.tbterminal.app.ui.theme.TbOutline.copy(alpha = 0.7f)),
        shadowElevation = 1.dp
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SupplierAvatar(supplier.name)
                Spacer(Modifier.width(12.dp))
                Text(
                    supplier.name,
                    modifier = Modifier.weight(1f),
                    color = SupplierSlate900,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                SupplierStatusBadge(
                    supplier.isActive,
                    Modifier.testTag("supplier-status-${supplier.id}"),
                )
                Spacer(Modifier.width(6.dp))
                SupplierActionsMenu(supplier, onEdit, onDelete)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                SupplierInfoRow(
                    icon = Icons.Outlined.Phone,
                    label = "Nomor telepon",
                    value = supplier.phone ?: "Belum diisi",
                    modifier = Modifier.weight(1f),
                    isEmpty = supplier.phone.isNullOrBlank()
                )
                SupplierInfoRow(
                    icon = Icons.Outlined.Place,
                    label = "Alamat",
                    value = supplier.address ?: "Belum diisi",
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    isEmpty = supplier.address.isNullOrBlank()
                )
            }

            HorizontalDivider(color = SupplierSlate200.copy(alpha = 0.55f))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Termin pembayaran", color = SupplierSlate500, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text("${supplier.paymentTermDays} hari", color = SupplierSlate900, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SupplierInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    isEmpty: Boolean = false,
) {
    Row(
        modifier = modifier.heightIn(min = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(com.tbterminal.app.ui.theme.TbGreenLight.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = com.tbterminal.app.ui.theme.TbGreenDark,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = SupplierSlate500, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(
                value,
                modifier = Modifier.padding(top = 4.dp),
                color = if (isEmpty) SupplierSlate500 else SupplierSlate900,
                fontSize = 12.sp,
                fontWeight = if (isEmpty) FontWeight.Normal else FontWeight.SemiBold,
                fontStyle = if (isEmpty) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SupplierAvatar(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier.size(40.dp).clip(CircleShape).background(com.tbterminal.app.ui.theme.TbGreenLight),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.trim().firstOrNull()?.uppercase() ?: "S", color = com.tbterminal.app.ui.theme.TbGreenDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SupplierStatusBadge(isActive: Boolean, modifier: Modifier = Modifier) {
    val tint = if (isActive) SupplierEmerald700 else SupplierSlate500
    Row(
        modifier.clip(RoundedCornerShape(999.dp)).background(tint.copy(alpha = 0.11f)).padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(tint))
        Text(if (isActive) "Aktif" else "Nonaktif", color = tint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SupplierActionsMenu(supplier: Supplier, onEdit: (Supplier) -> Unit, onDelete: (Supplier) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(32.dp).testTag("supplier-actions-${supplier.id}"),
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = "Aksi supplier", modifier = Modifier.size(20.dp), tint = SupplierSlate500)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SupplierSurface, RoundedCornerShape(14.dp)),
        ) {
            DropdownMenuItem(
                text = { Text("Edit supplier") },
                leadingIcon = { Icon(Icons.Outlined.Edit, null, Modifier.size(19.dp), tint = SupplierSlate900) },
                onClick = {
                    expanded = false
                    onEdit(supplier)
                },
                modifier = Modifier.heightIn(min = 48.dp),
            )
            if (supplier.isActive) {
                DropdownMenuItem(
                    text = { Text("Nonaktifkan", color = SupplierDanger) },
                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(19.dp), tint = SupplierDanger) },
                    onClick = {
                        expanded = false
                        onDelete(supplier)
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
    }
}

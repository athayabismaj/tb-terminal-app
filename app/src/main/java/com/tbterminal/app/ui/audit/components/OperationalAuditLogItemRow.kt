package com.tbterminal.app.ui.audit.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.AuditLogItem
import com.tbterminal.app.ui.audit.OperationalAuditType
import com.tbterminal.app.ui.audit.operationalAuditType
import com.tbterminal.app.ui.audit.operationalTitle
import com.tbterminal.app.ui.theme.TbAmber
import com.tbterminal.app.ui.theme.TbError
import com.tbterminal.app.ui.theme.TbGreen
import com.tbterminal.app.ui.theme.TbGreenDark
import com.tbterminal.app.ui.theme.TbOutline
import com.tbterminal.app.ui.theme.TbSurface
import com.tbterminal.app.ui.theme.TbSurfaceMuted
import com.tbterminal.app.ui.theme.TbText
import com.tbterminal.app.ui.theme.TbTextMuted
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AuditLogItemRow(log: AuditLogItem, compact: Boolean = false) {
    var expanded by rememberSaveable(log.id) { mutableStateOf(false) }
    val type = log.operationalAuditType()
    val actionColor = type.actionColor()
    val activityIcon = log.activityIcon(type)
    val formattedDate = runCatching {
        OffsetDateTime.parse(log.createdAt).format(
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID")),
        )
    }.getOrDefault(log.createdAt)
    val hasChanges = log.oldData.isMeaningfulAuditData() || log.newData.isMeaningfulAuditData()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TbSurface),
        border = BorderStroke(1.dp, TbOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            AuditHeader(
                log = log,
                badge = type?.module ?: log.actionLabel(),
                actionColor = actionColor,
                formattedDate = formattedDate,
                activityIcon = activityIcon,
                compact = compact,
            )

            if (hasChanges) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                ) {
                    Text(if (expanded) "Tutup detail" else "Lihat detail")
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                AuditChangeDetails(log)
            }
        }
    }
}

@Composable
private fun AuditHeader(
    log: AuditLogItem,
    badge: String,
    actionColor: Color,
    formattedDate: String,
    activityIcon: ImageVector,
    compact: Boolean,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        AuditIcon(actionColor, activityIcon)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = log.operationalTitle(),
                color = TbText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = if (compact) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formattedDate,
                modifier = Modifier.padding(top = 3.dp),
                color = TbTextMuted,
                style = MaterialTheme.typography.labelMedium,
            )
            AuditActorText(log)
        }
        AuditBadge(badge, actionColor)
    }
}

@Composable
private fun AuditActorText(log: AuditLogItem) {
    Text(
        text = "${log.actorName ?: "Sistem"} · ${log.actorRole?.roleLabel() ?: "Sistem"}",
        modifier = Modifier.padding(top = 2.dp),
        color = TbTextMuted,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun AuditIcon(actionColor: Color, imageVector: ImageVector) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(actionColor.copy(alpha = 0.10f), RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector,
            contentDescription = null,
            tint = actionColor,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun AuditBadge(text: String, actionColor: Color) {
    Surface(color = actionColor.copy(alpha = 0.10f), shape = RoundedCornerShape(999.dp)) {
        Text(
            text = text,
            modifier = Modifier.widthIn(max = 104.dp).padding(horizontal = 10.dp, vertical = 5.dp),
            color = actionColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AuditChangeDetails(log: AuditLogItem) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        color = TbSurfaceMuted,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Referensi ${log.schemaName}.${log.tableName}${log.recordId?.let { " · $it" }.orEmpty()}",
                color = TbTextMuted,
                style = MaterialTheme.typography.labelSmall,
            )
            if (log.oldData.isMeaningfulAuditData()) {
                AuditDataBlock("Sebelum", log.oldData.orEmpty(), TbError)
            }
            if (log.newData.isMeaningfulAuditData()) {
                AuditDataBlock("Sesudah", log.newData.orEmpty(), TbGreenDark)
            }
        }
    }
}

@Composable
private fun AuditDataBlock(label: String, value: String, color: Color) {
    Column {
        Text(label, color = TbTextMuted, style = MaterialTheme.typography.labelSmall)
        Text(
            text = value,
            modifier = Modifier.padding(top = 3.dp),
            color = color,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun OperationalAuditType?.actionColor(): Color = when (this) {
    OperationalAuditType.ProductCreated,
    OperationalAuditType.IncomingGoods,
    OperationalAuditType.SupplierDebtPayment,
    OperationalAuditType.CustomerReceivablePayment,
    OperationalAuditType.CashSession -> TbGreen
    OperationalAuditType.PriceUpdated,
    OperationalAuditType.ProductUpdated,
    OperationalAuditType.CategoryUnitUpdated,
    OperationalAuditType.StockOpname,
    OperationalAuditType.StockCorrection -> TbAmber
    OperationalAuditType.ProductDisabled,
    OperationalAuditType.DamagedReturn,
    OperationalAuditType.CancelRefund -> TbError
    null -> TbGreenDark
}

private fun String?.isMeaningfulAuditData(): Boolean = this != null && this != "null" && this != "{}"

private fun String.roleLabel(): String = lowercase().replaceFirstChar { it.titlecase() }

private fun AuditLogItem.actionLabel(): String = when (action) {
    "INSERT" -> "Tambah"
    "UPDATE" -> "Ubah"
    "DELETE" -> "Nonaktif"
    else -> action
}

private fun AuditLogItem.activityIcon(type: OperationalAuditType?): ImageVector = when (type) {
    OperationalAuditType.ProductCreated,
    OperationalAuditType.ProductUpdated,
    OperationalAuditType.ProductDisabled,
    OperationalAuditType.PriceUpdated,
    OperationalAuditType.CategoryUnitUpdated,
    OperationalAuditType.StockOpname,
    OperationalAuditType.StockCorrection,
    OperationalAuditType.DamagedReturn,
    OperationalAuditType.IncomingGoods -> Icons.Outlined.Inventory2

    OperationalAuditType.SupplierDebtPayment,
    OperationalAuditType.CustomerReceivablePayment,
    OperationalAuditType.CashSession -> Icons.Outlined.AccountBalanceWallet

    OperationalAuditType.CancelRefund -> Icons.AutoMirrored.Outlined.ReceiptLong
    null -> when {
        tableName.contains("user", ignoreCase = true) -> Icons.Outlined.PersonOutline
        schemaName.contains("security", ignoreCase = true) -> Icons.Outlined.Security
        else -> Icons.Outlined.History
    }
}

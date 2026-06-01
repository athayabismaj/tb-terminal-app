package com.tbterminal.app.ui.audit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.AuditLogItem
import com.tbterminal.app.ui.audit.OperationalAuditType
import com.tbterminal.app.ui.audit.operationalAuditType
import com.tbterminal.app.ui.audit.operationalModule
import com.tbterminal.app.ui.audit.operationalTitle
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AuditLogItemRow(log: AuditLogItem) {
    val type = log.operationalAuditType()
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))
    val formattedDate = try {
        OffsetDateTime.parse(log.createdAt).format(dateFormatter)
    } catch (e: Exception) {
        log.createdAt
    }

    val actionColor = when (type) {
        OperationalAuditType.ProductCreated,
        OperationalAuditType.IncomingGoods,
        OperationalAuditType.SupplierDebtPayment,
        OperationalAuditType.CustomerReceivablePayment,
        OperationalAuditType.CashSession -> Color(0xFF10B981)
        OperationalAuditType.PriceUpdated,
        OperationalAuditType.ProductUpdated,
        OperationalAuditType.CategoryUnitUpdated,
        OperationalAuditType.StockOpname,
        OperationalAuditType.StockCorrection -> Color(0xFFF59E0B)
        OperationalAuditType.ProductDisabled,
        OperationalAuditType.DamagedReturn,
        OperationalAuditType.CancelRefund -> Color(0xFFEF4444)
        else -> Color(0xFF64748B)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(actionColor.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = type?.module?.uppercase() ?: log.actionLabel(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = actionColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = log.operationalTitle(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                )
            }
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Aktor",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                )
                Text(
                    text = "${log.actorName ?: "Sistem"} (${log.actorRole ?: "-"})",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF334155))
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Area",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                )
                Text(
                    text = "${log.operationalModule()} - ${log.schemaName}.${log.tableName}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF334155))
                )
            }
        }

        if (log.newData != null || log.oldData != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(12.dp)
            ) {
                Column {
                    if (log.oldData != null && log.oldData != "null" && log.oldData != "{}") {
                        Text("Data Lama:", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B)))
                        Text(
                            text = log.oldData,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFEF4444),
                                fontFamily = FontFamily.Monospace
                            ),
                            maxLines = 3
                        )
                    }
                    if (log.oldData != null && log.newData != null && log.oldData != "null" && log.newData != "null" && log.oldData != "{}" && log.newData != "{}") {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (log.newData != null && log.newData != "null" && log.newData != "{}") {
                        Text("Data Baru:", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B)))
                        Text(
                            text = log.newData,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF10B981),
                                fontFamily = FontFamily.Monospace
                            ),
                            maxLines = 3
                        )
                    }
                }
            }
        }
    }
}

private fun AuditLogItem.actionLabel(): String {
    return when (action) {
        "INSERT" -> "TAMBAH"
        "UPDATE" -> "UBAH"
        "DELETE" -> "NONAKTIF"
        else -> action
    }
}

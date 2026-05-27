package com.tbterminal.app.ui.audit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tbterminal.app.data.model.AuditLogItem
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOperationalAuditScreen(
    viewModel: AdminOperationalAuditViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(24.dp)
    ) {
        Text(
            text = "Audit Operasional",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Riwayat aktivitas sensitif dan operasional sistem",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF64748B)),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                selected = uiState.selectedAction == null,
                onClick = { viewModel.setActionFilter(null) },
                label = { Text("Semua") }
            )
            FilterChip(
                selected = uiState.selectedAction == "INSERT",
                onClick = { viewModel.setActionFilter("INSERT") },
                label = { Text("Tambah") }
            )
            FilterChip(
                selected = uiState.selectedAction == "UPDATE",
                onClick = { viewModel.setActionFilter("UPDATE") },
                label = { Text("Ubah") }
            )
            FilterChip(
                selected = uiState.selectedAction == "DELETE",
                onClick = { viewModel.setActionFilter("DELETE") },
                label = { Text("Hapus") }
            )
        }

        if (uiState.isLoading && uiState.logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null && uiState.logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(uiState.error ?: "Error", color = Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadLogs() }) {
                        Text("Coba Lagi")
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.logs, key = { it.id }) { log ->
                        AuditLogItemRow(log)
                        Divider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(top = 12.dp))
                    }

                    if (uiState.totalPages > 1) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { viewModel.loadLogs(page = uiState.currentPage - 1) },
                                    enabled = uiState.currentPage > 1
                                ) {
                                    Text("Sebelumnya")
                                }
                                Text("Halaman ${uiState.currentPage} dari ${uiState.totalPages}")
                                TextButton(
                                    onClick = { viewModel.loadLogs(page = uiState.currentPage + 1) },
                                    enabled = uiState.currentPage < uiState.totalPages
                                ) {
                                    Text("Selanjutnya")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogItemRow(log: AuditLogItem) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    val formattedDate = try {
        OffsetDateTime.parse(log.createdAt).format(dateFormatter)
    } catch (e: Exception) {
        log.createdAt
    }

    val actionColor = when (log.action) {
        "INSERT" -> Color(0xFF10B981)
        "UPDATE" -> Color(0xFFF59E0B)
        "DELETE" -> Color(0xFFEF4444)
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
                        text = log.action,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = actionColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = log.activityLabel,
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
                    text = "Tabel",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                )
                Text(
                    text = "${log.schemaName}.${log.tableName}",
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
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
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
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            maxLines = 3
                        )
                    }
                }
            }
        }
    }
}

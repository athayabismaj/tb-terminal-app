package com.tbterminal.app.ui.audit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.audit.components.AuditLogItemRow
import com.tbterminal.app.ui.audit.components.AuditPagination
import com.tbterminal.app.ui.audit.components.OperationalAuditEmptyState
import com.tbterminal.app.ui.audit.components.OperationalAuditFilterChips
import com.tbterminal.app.ui.audit.components.OperationalAuditHeader

@Composable
fun AdminOperationalAuditScreen(
    modifier: Modifier = Modifier,
    uiState: AdminOperationalAuditUiState,
    onActionFilterChanged: (String?) -> Unit,
    onRetry: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(24.dp)
    ) {
        OperationalAuditHeader()
        OperationalAuditFilterChips(
            selectedAction = uiState.selectedAction,
            onActionFilterChanged = onActionFilterChanged
        )

        if (uiState.isLoading && uiState.logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null && uiState.logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(uiState.error ?: "Error", color = Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRetry) {
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
                    if (uiState.logs.isEmpty()) {
                        item {
                            OperationalAuditEmptyState()
                        }
                        if (uiState.totalPages > 1) {
                            item {
                                AuditPagination(
                                    currentPage = uiState.currentPage,
                                    totalPages = uiState.totalPages,
                                    onPrevious = onPreviousPage,
                                    onNext = onNextPage
                                )
                            }
                        }
                    } else {
                        items(uiState.logs, key = { it.id }) { log ->
                            AuditLogItemRow(log)
                            HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(top = 12.dp))
                        }

                        if (uiState.totalPages > 1) {
                            item {
                                AuditPagination(
                                    currentPage = uiState.currentPage,
                                    totalPages = uiState.totalPages,
                                    onPrevious = onPreviousPage,
                                    onNext = onNextPage
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

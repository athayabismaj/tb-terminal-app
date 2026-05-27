package com.tbterminal.app.ui.reports

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
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsScreen(
    viewModel: AdminReportsViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(24.dp)
    ) {
        Text(
            text = "Laporan Analitik",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Ringkasan performa penjualan dan stok",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF64748B)),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (uiState.isLoading && uiState.dashboardMetrics == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null && uiState.dashboardMetrics == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(uiState.error ?: "Error", color = Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadReports() }) {
                        Text("Coba Lagi")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dashboard Metrics Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MetricCard(
                            title = "Pendapatan Hari Ini",
                            value = currencyFormat.format(uiState.dashboardMetrics?.totalRevenueToday ?: 0.0),
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF3B82F6)
                        )
                        MetricCard(
                            title = "Pendapatan Bulan Ini",
                            value = currencyFormat.format(uiState.dashboardMetrics?.totalRevenueThisMonth ?: 0.0),
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF10B981)
                        )
                        MetricCard(
                            title = "Total Piutang Aktif",
                            value = currencyFormat.format(uiState.dashboardMetrics?.totalActiveReceivables ?: 0.0),
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFF59E0B)
                        )
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Laporan Penjualan (${uiState.startDate} - ${uiState.endDate})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (uiState.dailySales.isEmpty()) {
                                Text("Belum ada data penjualan pada rentang waktu ini.", color = Color(0xFF64748B))
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F5F9)).padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Tanggal", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                    Text("Transaksi", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                    Text("Total Omset", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                }
                                uiState.dailySales.forEach { sale ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(sale.date, modifier = Modifier.weight(1f))
                                        Text(sale.transactionCount.toString(), modifier = Modifier.weight(1f))
                                        Text(currencyFormat.format(sale.totalRevenue), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                    }
                                    Divider(color = Color(0xFFE2E8F0))
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Peringatan Stok Rendah (${uiState.dashboardMetrics?.lowStockCount ?: 0} item)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            val lowStockItems = uiState.dashboardMetrics?.lowStockItems ?: emptyList()
                            if (lowStockItems.isEmpty()) {
                                Text("Stok semua produk dalam kondisi aman.", color = Color(0xFF64748B))
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(Color(0xFFFEF2F2)).padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Produk", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
                                    Text("Stok Sisa", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                    Text("Min. Stok", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                }
                                lowStockItems.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${item.sku} - ${item.productName}", modifier = Modifier.weight(2f))
                                        Text(item.quantity.toString(), modifier = Modifier.weight(1f), color = Color(0xFFEF4444), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                        Text(item.minStock.toString(), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                    }
                                    Divider(color = Color(0xFFE2E8F0))
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
fun MetricCard(title: String, value: String, modifier: Modifier = Modifier, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF64748B))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
        }
    }
}

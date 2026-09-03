package com.tbterminal.app.ui.suppliers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.ui.products.components.MasterDataPagination

@Composable
internal fun SupplierListCard(
    modifier: Modifier,
    uiState: SupplierUiState,
    onSearchChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onEdit: (Supplier) -> Unit,
    onDelete: (Supplier) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SupplierSurface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, SupplierSlate200)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SupplierListHeader(uiState.totalSuppliers)
            HorizontalDivider(color = SupplierSlate100)
            SupplierToolbar(uiState.searchQuery, onSearchChanged)
            SupplierTableHeader()
            SupplierTableBody(uiState, onEdit, onDelete)
            MasterDataPagination("supplier", uiState.totalSuppliers, uiState.page, uiState.totalPages, uiState.pageSize, uiState.isLoading, onPreviousPage, onNextPage)
        }
    }
}

@Composable
private fun SupplierListHeader(total: Long) {
    Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        SupplierCardTitle(Icons.AutoMirrored.Outlined.ListAlt, "Daftar Supplier", 0.dp)
        Box(Modifier.background(SupplierEmerald50, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 4.dp)) {
            Text("$total total", color = SupplierEmerald600, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SupplierToolbar(searchQuery: String, onSearchChanged: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Cari supplier...", color = SupplierSlate400, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = SupplierSlate400) },
            modifier = Modifier.weight(1f).height(54.dp),
            colors = supplierTextFieldColors(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )
    }
}

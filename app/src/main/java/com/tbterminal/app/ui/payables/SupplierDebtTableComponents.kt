package com.tbterminal.app.ui.payables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.SupplierPayable
import com.tbterminal.app.ui.components.TbMobileSummaryButton

@Composable
internal fun SupplierDebtTableCard(
    modifier: Modifier,
    uiState: SupplierDebtUiState,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (SupplierDebtStatusFilter) -> Unit,
    @Suppress("UNUSED_PARAMETER") onRefresh: () -> Unit,
    onPayClick: (SupplierPayable) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    compact: Boolean = false,
    onOpenMobileOverview: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SupplierDebtToolbar(uiState, onSearchChanged, onStatusFilterChanged, compact)
        Spacer(Modifier.height(if (compact) 14.dp else 16.dp))
        if (!uiState.isLoading || uiState.payables.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Daftar hutang",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = DebtText,
                    fontWeight = FontWeight.SemiBold,
                )
                if (compact) {
                    TbMobileSummaryButton(
                        onClick = onOpenMobileOverview,
                        testTag = "supplier-debt-open-overview",
                    )
                }
            }
        }

        if (!compact && !uiState.isLoading && uiState.errorMessage == null && uiState.filteredPayables.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DebtSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, DebtLine),
            ) {
                Column {
                    DebtTableHeader()
                    SupplierDebtRows(Modifier.fillMaxWidth(), uiState, onPayClick, compact = false)
                }
            }
        } else {
            SupplierDebtRows(Modifier.fillMaxWidth(), uiState, onPayClick, compact = compact)
        }

        if (!uiState.isLoading && uiState.errorMessage == null && uiState.filteredPayables.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            SupplierDebtFooter(uiState, onPreviousPage, onNextPage, compact)
        }
    }
}

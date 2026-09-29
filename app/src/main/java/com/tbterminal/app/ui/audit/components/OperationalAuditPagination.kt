package com.tbterminal.app.ui.audit.components

import androidx.compose.runtime.Composable
import com.tbterminal.app.ui.components.TbPagination

@Composable
fun AuditPagination(
    currentPage: Int,
    totalPages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    TbPagination(
        currentPage = currentPage,
        totalPages = totalPages,
        onPreviousPage = onPrevious,
        onNextPage = onNext,
        testTag = "audit-pagination",
    )
}

package com.tbterminal.app.ui.products.components

import androidx.compose.runtime.Composable
import com.tbterminal.app.ui.components.TbPagination

@Composable
internal fun MasterDataPagination(
    itemLabel: String,
    total: Long,
    page: Int,
    totalPages: Int,
    pageSize: Int,
    isLoading: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    val safePage = page.coerceAtLeast(1)
    val first = if (total == 0L) 0 else (safePage - 1L) * pageSize + 1
    val last = minOf(safePage.toLong() * pageSize, total)
    TbPagination(
        currentPage = safePage,
        totalPages = totalPages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        supportingText = "$first-$last dari $total $itemLabel",
        isLoading = isLoading,
        testTag = "master-data-pagination",
    )
}

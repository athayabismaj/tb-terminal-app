package com.tbterminal.app.ui.products.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.components.TbPagination
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    compact: Boolean = false,
) {
    val safePage = page.coerceAtLeast(1)
    val safeTotalPages = totalPages.coerceAtLeast(1)
    val first = if (total == 0L) 0 else (safePage - 1L) * pageSize + 1
    val last = minOf(safePage.toLong() * pageSize, total)
    val text = if (compact) {
        "$total $itemLabel"
    } else {
        "Menampilkan $first–$last dari $total $itemLabel"
    }

    TbPagination(
        currentPage = safePage,
        totalPages = safeTotalPages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        supportingText = text,
        isLoading = isLoading,
        compact = compact,
        testTag = "master-data-pagination",
    )
}

internal fun String.formatMasterDate(): String = runCatching {
    OffsetDateTime.parse(this).format(
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID"))
    )
}.getOrElse {
    runCatching {
        LocalDate.parse(this.substringBefore("T")).format(
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID"))
        )
    }.getOrDefault(this.substringBefore("T"))
}

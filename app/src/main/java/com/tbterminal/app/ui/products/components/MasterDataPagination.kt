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

    if (compact) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 4.dp)
                .testTag("master-data-pagination"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            ) {
                Text(
                    text = "$total $itemLabel",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569),
                )
            }

            Surface(
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val canPrev = safePage > 1 && !isLoading
                    val canNext = safePage < safeTotalPages && !isLoading
                    IconButton(
                        onClick = onPreviousPage,
                        enabled = canPrev,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Halaman sebelumnya",
                            tint = if (canPrev) Color(0xFF475569) else Color(0xFFCBD5E1),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = "$safePage / $safeTotalPages",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155),
                    )
                    IconButton(
                        onClick = onNextPage,
                        enabled = canNext,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Halaman berikutnya",
                            tint = if (canNext) Color(0xFF475569) else Color(0xFFCBD5E1),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    } else {
        val first = if (total == 0L) 0 else (safePage - 1L) * pageSize + 1
        val last = minOf(safePage.toLong() * pageSize, total)
        TbPagination(
            currentPage = safePage,
            totalPages = safeTotalPages,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage,
            supportingText = "Menampilkan $first–$last dari $total $itemLabel",
            isLoading = isLoading,
            testTag = "master-data-pagination",
        )
    }
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

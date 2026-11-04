package com.tbterminal.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * Komponen Pagination standar tunggal untuk seluruh halaman TB Terminal.
 *
 * Mengikuti desain standar Riwayat Pembelian:
 * - Dua kapsul dengan latar #F1F5F9, border #E2E8F0, dan sudut RoundedCornerShape(12.dp) setinggi 36.dp.
 * - Kapsul kiri: Informasi jumlah item / rentang data (supportingText).
 * - Kapsul kanan: Kontrol navigasi [ < ] [ halaman / total ] [ > ].
 */
@Composable
fun TbPagination(
    currentPage: Int,
    totalPages: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isLoading: Boolean = false,
    compact: Boolean = true,
    testTag: String = "tb-pagination",
) {
    val safeTotalPages = totalPages.coerceAtLeast(1)
    val safePage = currentPage.coerceIn(1, safeTotalPages)
    val canPrev = safePage > 1 && !isLoading
    val canNext = safePage < safeTotalPages && !isLoading

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 16.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Kapsul Kiri: Supporting text / Info data
        if (!supportingText.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.height(36.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = supportingText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                }
            }
        } else {
            Spacer(Modifier.weight(1f, fill = false))
        }

        // Kapsul Kanan: Navigasi [ < ] [ page / total ] [ > ]
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF1F5F9),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.height(36.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onPreviousPage,
                    enabled = canPrev,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Halaman sebelumnya",
                        tint = if (canPrev) Color(0xFF475569) else Color(0xFFCBD5E1),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "$safePage / $safeTotalPages",
                    modifier = Modifier.padding(horizontal = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                IconButton(
                    onClick = onNextPage,
                    enabled = canNext,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Halaman berikutnya",
                        tint = if (canNext) Color(0xFF475569) else Color(0xFFCBD5E1),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Format string standar untuk teks pendukung pagination.
 * Contoh output:
 * - "0 nota"
 * - "25 nota"
 * - "10 dari 50 nota"
 */
fun tbPaginationSupportingText(
    visibleCount: Int,
    totalCount: Long,
    unit: String,
    startIndex: Int? = null,
    endIndex: Int? = null,
): String {
    if (totalCount <= 0L) return "0 $unit"
    if (startIndex != null && endIndex != null && totalCount > (endIndex - startIndex + 1)) {
        return "$startIndex–$endIndex dari $totalCount $unit"
    }
    if (visibleCount.toLong() >= totalCount) {
        return "$totalCount $unit"
    }
    return "$visibleCount dari $totalCount $unit"
}

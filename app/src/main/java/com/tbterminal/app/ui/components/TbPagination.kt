package com.tbterminal.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Komponen Pagination standar tunggal untuk seluruh halaman TB Terminal.
 *
 * Mengikuti desain standar industri Penawaran (RAB):
 * - Kapsul tunggal terpusat (CircleShape) dengan latar putih, border #E2E8F0, dan shadow elevation 0.5.dp.
 * - Format isi: [ supportingText • < safePage / safeTotalPages > ]
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 14.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.8f)),
            shadowElevation = 0.5.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!supportingText.isNullOrBlank()) {
                    Text(
                        text = supportingText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = "•",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }

                IconButton(
                    onClick = onPreviousPage,
                    enabled = canPrev,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = "Halaman sebelumnya",
                        tint = if (canPrev) Color(0xFF334155) else Color(0xFFCBD5E1),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "$safePage",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF10533C)
                )

                Text(
                    text = "/ $safeTotalPages",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8)
                )

                IconButton(
                    onClick = onNextPage,
                    enabled = canNext,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "Halaman berikutnya",
                        tint = if (canNext) Color(0xFF334155) else Color(0xFFCBD5E1),
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
    if (startIndex != null && endIndex != null) {
        return "$startIndex–$endIndex dari $totalCount $unit"
    }
    if (visibleCount > 0 && visibleCount.toLong() < totalCount) {
        return "$visibleCount dari $totalCount $unit"
    }
    return "$totalCount $unit"
}

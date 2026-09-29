package com.tbterminal.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.theme.TbOutline
import com.tbterminal.app.ui.theme.TbSurface
import com.tbterminal.app.ui.theme.TbText
import com.tbterminal.app.ui.theme.TbTextMuted

/** Pagination visual tunggal untuk seluruh halaman TB Terminal. */
@Composable
fun TbPagination(
    currentPage: Int,
    totalPages: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isLoading: Boolean = false,
    testTag: String = "tb-pagination",
) {
    val safeTotalPages = totalPages.coerceAtLeast(1)
    val safePage = currentPage.coerceIn(1, safeTotalPages)
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (supportingText != null) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                Surface(
                    color = TbSurface,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, TbOutline.copy(alpha = 0.8f)),
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = supportingText,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = TbTextMuted,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        Surface(
            color = TbSurface,
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, TbOutline.copy(alpha = 0.8f)),
            shadowElevation = 0.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                IconButton(
                    onClick = onPreviousPage,
                    enabled = safePage > 1 && !isLoading,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = "Halaman sebelumnya",
                        tint = if (safePage > 1 && !isLoading) TbText else TbTextMuted.copy(alpha = 0.4f),
                    )
                }
                Box(modifier = Modifier.widthIn(min = 54.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "$safePage / $safeTotalPages",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TbText,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
                IconButton(
                    onClick = onNextPage,
                    enabled = safePage < safeTotalPages && !isLoading,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "Halaman berikutnya",
                        tint = if (safePage < safeTotalPages && !isLoading) TbText else TbTextMuted.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

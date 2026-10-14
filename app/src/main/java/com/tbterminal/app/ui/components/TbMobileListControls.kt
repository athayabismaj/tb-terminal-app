package com.tbterminal.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.theme.TbGreenDark
import com.tbterminal.app.ui.theme.TbGreenLight
import com.tbterminal.app.ui.theme.TbOutline
import com.tbterminal.app.ui.theme.TbSurface
import com.tbterminal.app.ui.theme.TbSurfaceMuted
import com.tbterminal.app.ui.theme.TbText
import com.tbterminal.app.ui.theme.TbTextMuted

/** Segmented period presets with a visually separate calendar control. */
@Composable
fun <T> TbPeriodFilterRow(
    selectedValue: T?,
    presets: List<Pair<T, String>>,
    dateLabel: String,
    dateSelected: Boolean,
    onPresetSelected: (T) -> Unit,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "period-filter-row",
    presetTestTags: List<String> = emptyList(),
    dateTestTag: String = "period-filter-date",
) {
    Row(
        modifier = modifier.fillMaxWidth().height(50.dp).testTag(testTag),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            color = TbSurfaceMuted.copy(alpha = 0.55f),
            shape = RoundedCornerShape(15.dp),
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                presets.forEachIndexed { index, (value, label) ->
                    val baseModifier = Modifier.weight(1f).fillMaxHeight()
                    val optionModifier = presetTestTags.getOrNull(index)
                        ?.let { baseModifier.testTag(it) }
                        ?: baseModifier
                    Surface(
                        onClick = { onPresetSelected(value) },
                        modifier = optionModifier,
                        color = if (selectedValue == value) TbGreenLight else Color.Transparent,
                        contentColor = if (selectedValue == value) TbGreenDark else TbTextMuted,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedValue == value) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
        Surface(
            onClick = onDateClick,
            modifier = Modifier.width(96.dp).fillMaxHeight().testTag(dateTestTag),
            color = if (dateSelected) TbGreenLight else TbSurface,
            contentColor = if (dateSelected) TbGreenDark else TbTextMuted,
            shape = RoundedCornerShape(15.dp),
            border = BorderStroke(1.dp, if (dateSelected) TbGreenDark.copy(alpha = 0.28f) else TbOutline),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(17.dp))
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Compact entry point for secondary filters on phone-sized layouts. */
@Composable
fun TbMobileFilterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    contentDescription: String = "Buka filter",
    testTag: String = "mobile-filter-button",
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(50.dp).testTag(testTag),
        color = if (active) MaterialTheme.colorScheme.primaryContainer else TbSurface,
        contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else TbText,
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, TbOutline),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Tune, contentDescription = contentDescription, modifier = Modifier.size(21.dp))
        }
    }
}

/** Lightweight action used from a list section title to reveal summary data. */
@Composable
fun TbMobileSummaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Ringkasan",
    testTag: String = "mobile-summary-button",
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(48.dp).testTag(testTag),
        colors = ButtonDefaults.textButtonColors(contentColor = TbGreenDark),
    ) {
        Icon(Icons.Outlined.Insights, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Three-bar chart icon with rounded pill bars matching the mobile design mockup.
 * Left bar: medium height, Center bar: tall height, Right bar: short height.
 */
@Composable
fun SummaryBarChartIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF1E293B),
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barWidth = w * 0.22f
        val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

        // Bar 1 (left): height ~60%
        val h1 = h * 0.60f
        drawRoundRect(
            color = tint,
            topLeft = Offset(x = w * 0.08f, y = h - h1),
            size = Size(barWidth, h1),
            cornerRadius = cornerRadius,
        )

        // Bar 2 (center): height ~98%
        val h2 = h * 0.98f
        drawRoundRect(
            color = tint,
            topLeft = Offset(x = w * 0.39f, y = h - h2),
            size = Size(barWidth, h2),
            cornerRadius = cornerRadius,
        )

        // Bar 3 (right): height ~45%
        val h3 = h * 0.45f
        drawRoundRect(
            color = tint,
            topLeft = Offset(x = w * 0.70f, y = h - h3),
            size = Size(barWidth, h3),
            cornerRadius = cornerRadius,
        )
    }
}

/**
 * Square summary button with 8.dp rounded corners and the 3-bar chart icon.
 */
@Composable
fun SummaryIconButton(
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    contentDescription: String = "Ringkasan",
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SummaryBarChartIcon(
            modifier = Modifier.size(16.dp),
            tint = Color(0xFF1E293B),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TbMobileControlSheet(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "mobile-control-sheet",
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = TbSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .testTag(testTag),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, color = TbText, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TbTextMuted)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "Tutup", tint = TbTextMuted)
                }
            }
            content()
        }
    }
}

@Composable
fun TbMobileSheetDoneButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Selesai",
    testTag: String = "mobile-sheet-done",
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(50.dp).testTag(testTag),
        shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TbGreenDark),
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

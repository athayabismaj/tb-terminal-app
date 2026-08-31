package com.tbterminal.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class TbWindowWidthClass { Compact, Medium, Expanded }

data class TbLayoutInfo(
    val widthClass: TbWindowWidthClass,
    val horizontalPadding: Dp,
    val verticalSpacing: Dp
) {
    val isCompact: Boolean get() = widthClass == TbWindowWidthClass.Compact
    val isExpanded: Boolean get() = widthClass == TbWindowWidthClass.Expanded
}

fun tbWindowWidthClass(width: Dp): TbWindowWidthClass = when {
    width < 600.dp -> TbWindowWidthClass.Compact
    width < 900.dp -> TbWindowWidthClass.Medium
    else -> TbWindowWidthClass.Expanded
}

@Composable
fun TbAdaptiveBox(
    modifier: Modifier = Modifier,
    content: @Composable (TbLayoutInfo) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val widthClass = tbWindowWidthClass(maxWidth)
        content(
            TbLayoutInfo(
                widthClass = widthClass,
                horizontalPadding = when (widthClass) {
                    TbWindowWidthClass.Compact -> 16.dp
                    TbWindowWidthClass.Medium -> 24.dp
                    TbWindowWidthClass.Expanded -> 32.dp
                },
                verticalSpacing = if (widthClass == TbWindowWidthClass.Compact) 16.dp else 24.dp
            )
        )
    }
}

@Composable
fun TbPageSurface(
    modifier: Modifier = Modifier,
    maxContentWidth: Dp = 1440.dp,
    content: @Composable (TbLayoutInfo, Modifier) -> Unit
) {
    TbAdaptiveBox(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) { info ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            content(
                info,
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = maxContentWidth)
                    .padding(horizontal = info.horizontalPadding, vertical = info.verticalSpacing)
            )
        }
    }
}

@Composable
fun TbCompactDashboardTopBar(
    userName: String,
    role: String,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Outlined.Menu, contentDescription = "Buka navigasi")
            }
            Box(
                modifier = Modifier.size(36.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("TB", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("TB Terminal", style = MaterialTheme.typography.titleMedium)
                Text(
                    "$userName · ${role.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

package com.tbterminal.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TBTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A), // Slate 900
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color(0xFF1E293B) // Slate 800 (touch target 48dp)
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            navigationIconContentColor = Color(0xFF1E293B),
            titleContentColor = Color(0xFF0F172A)
        ),
        modifier = modifier.drawBehind {
            val stroke = 1.dp.toPx()
            drawLine(
                color = Color(0xFFF1F5F9), // slate-100
                start = Offset(0f, size.height - stroke / 2f),
                end = Offset(size.width, size.height - stroke / 2f),
                strokeWidth = stroke,
            )
        }
    )
}


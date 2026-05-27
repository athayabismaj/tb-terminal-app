package com.tbterminal.app.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ProductCategoryCardTitle(
    icon: ImageVector,
    title: String,
    bottomPadding: Dp = 24.dp
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = bottomPadding)) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CategoryEmerald50),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = CategoryEmerald600, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CategorySlate900)
    }
}

@Composable
internal fun ProductCategoryMessage(message: String) {
    val isError = message.contains("gagal", ignoreCase = true) ||
        message.contains("tidak", ignoreCase = true) ||
        message.contains("kosong", ignoreCase = true)
    val background = if (isError) ProductDanger.copy(alpha = 0.12f) else CategoryEmerald50
    val content = if (isError) ProductDanger else CategoryEmerald800

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(message, color = content, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun productCategoryTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CategoryEmerald600,
    unfocusedBorderColor = CategorySlate200,
    focusedContainerColor = CategorySlate50,
    unfocusedContainerColor = CategorySlate50
)

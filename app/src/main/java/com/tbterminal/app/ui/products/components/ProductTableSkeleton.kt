package com.tbterminal.app.ui.products.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.products.ProductLine
import com.tbterminal.app.ui.products.ProductSoft
import com.tbterminal.app.ui.products.ProductSurface

@Composable
internal fun ProductTableSkeletonRows(rowCount: Int = 5) {
    Column {
        repeat(rowCount) { index ->
            ProductTableSkeletonRow(useAlternateBackground = index % 2 != 0)
            HorizontalDivider(color = ProductLine.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun ProductTableSkeletonRow(useAlternateBackground: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (useAlternateBackground) ProductSoft.copy(alpha = 0.76f) else ProductSurface)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonTextPair(Modifier.weight(2.5f), primaryWidth = 0.68f, secondaryWidth = 0.42f)
        SkeletonTextPair(Modifier.weight(2f), primaryWidth = 0.62f, secondaryWidth = 0.48f)
        SkeletonTextPair(Modifier.weight(2.5f), primaryWidth = 0.58f, secondaryWidth = 0.44f)
        Box(modifier = Modifier.weight(1f)) {
            ProductShimmerBox(Modifier.size(54.dp, 16.dp))
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            ProductShimmerBox(Modifier.size(62.dp, 25.dp))
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductShimmerBox(Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(14.dp))
            ProductShimmerBox(Modifier.size(38.dp, 20.dp))
        }
    }
}

@Composable
private fun SkeletonTextPair(
    modifier: Modifier,
    primaryWidth: Float,
    secondaryWidth: Float
) {
    Column(modifier = modifier) {
        ProductShimmerBox(Modifier.fillMaxWidth(primaryWidth).height(16.dp))
        Spacer(modifier = Modifier.height(8.dp))
        ProductShimmerBox(Modifier.fillMaxWidth(secondaryWidth).height(13.dp))
    }
}

@Composable
private fun ProductShimmerBox(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "product-table-skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.36f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 820),
            repeatMode = RepeatMode.Reverse
        ),
        label = "product-table-skeleton-alpha"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFFD8E2E7).copy(alpha = alpha))
    )
}

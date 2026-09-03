package com.tbterminal.app.ui.components

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

internal enum class ContentLoadPhase {
    INITIAL_LOADING,
    CONTENT,
    REFRESHING,
    EMPTY,
    ERROR,
}

internal fun resolveContentLoadPhase(
    isLoading: Boolean,
    hasContent: Boolean,
    error: String?,
): ContentLoadPhase = when {
    isLoading && !hasContent -> ContentLoadPhase.INITIAL_LOADING
    isLoading -> ContentLoadPhase.REFRESHING
    hasContent -> ContentLoadPhase.CONTENT
    error != null -> ContentLoadPhase.ERROR
    else -> ContentLoadPhase.EMPTY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshableContent(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        content()
    }
}

@Composable
fun SkeletonBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.38f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )
    Box(
        modifier = modifier
            .alpha(alpha)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(12.dp),
            ),
    )
}

@Composable
fun SkeletonListItem(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SkeletonBox(Modifier.size(44.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBox(Modifier.fillMaxWidth(0.68f).height(16.dp))
            SkeletonBox(Modifier.fillMaxWidth(0.42f).height(12.dp))
        }
    }
}

@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SkeletonBox(Modifier.fillMaxWidth(0.42f).height(14.dp))
        SkeletonBox(Modifier.fillMaxWidth(0.7f).height(28.dp))
        SkeletonBox(Modifier.fillMaxWidth().height(12.dp))
    }
}

@Composable
fun SkeletonTableRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(4) { index ->
            SkeletonBox(Modifier.weight(if (index == 0) 1.5f else 1f).height(14.dp))
        }
    }
}

@Composable
fun SkeletonList(
    modifier: Modifier = Modifier,
    itemCount: Int = 6,
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        repeat(itemCount) { SkeletonListItem() }
    }
}

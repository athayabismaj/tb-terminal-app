package com.tbterminal.app.ui.reports.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ReportsLoadingState(modifier: Modifier = Modifier) {
    com.tbterminal.app.ui.components.SkeletonList(modifier = modifier.fillMaxSize(), itemCount = 6)
}

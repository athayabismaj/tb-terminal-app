package com.tbterminal.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tbterminal.app.BuildConfig

internal fun usesWideAuthLayout(widthDp: Int): Boolean = widthDp >= 720

@Composable
internal fun AuthAdaptiveScaffold(
    modifier: Modifier = Modifier,
    compactContentAlignment: Alignment = Alignment.CenterStart,
    compactFooter: (@Composable () -> Unit)? = null,
    content: @Composable (compact: Boolean) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .systemBarsPadding()
    ) {
        AuthFormArea(
            compact = !usesWideAuthLayout(maxWidth.value.toInt()),
            modifier = Modifier.fillMaxSize(),
            compactContentAlignment = compactContentAlignment,
            compactFooter = compactFooter,
            content = content
        )
    }
}

@Composable
private fun AuthFormArea(
    compact: Boolean,
    modifier: Modifier,
    compactContentAlignment: Alignment,
    compactFooter: (@Composable () -> Unit)?,
    content: @Composable (compact: Boolean) -> Unit
) {
    if (compact) {
        Box(
            modifier = modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxHeight()
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = compactContentAlignment
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp, vertical = 24.dp)
                    ) {
                        content(true)
                    }
                }
                compactFooter?.let {
                    Spacer(Modifier.height(8.dp))
                    it()
                }
                Spacer(Modifier.height(4.dp))
                AuthVersionText()
            }
        }
    } else {
        Box(
            modifier = modifier.padding(horizontal = 48.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content(false)
                Spacer(Modifier.height(24.dp))
                AuthVersionText()
            }
        }
    }
}

@Composable
private fun AuthVersionText() {
    Text(
        text = "TB Terminal · v${BuildConfig.VERSION_NAME}",
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

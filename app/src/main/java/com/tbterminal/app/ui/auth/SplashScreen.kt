package com.tbterminal.app.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.BrandSoftText
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.TbterminalappTheme

/**
 * Branded splash screen aligned with the Login and PIN visual system
 * (Brand green canvas, concentric rings, fading dot matrix, and Barokah Jaya mark).
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    animateEntrance: Boolean = true
) {
    val entranceProgress = remember(animateEntrance) {
        Animatable(if (animateEntrance) 0f else 1f)
    }

    LaunchedEffect(animateEntrance) {
        if (animateEntrance) {
            entranceProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
            )
        }
    }

    val progress = entranceProgress.value
    val logoScale = 0.90f + (0.10f * progress)
    val contentTranslationY = (1f - progress) * 14f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brand)
            .clipToBounds()
            .testTag("splash-screen")
    ) {
        // Motif lingkaran konsentris kanan atas (selaras dengan Login & PIN)
        RingsPattern(
            radii = listOf(64.dp, 120.dp, 176.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .wrapContentSize(align = Alignment.TopEnd, unbounded = true)
                .offset(x = 96.dp, y = (-96).dp)
                .size(370.dp)
        )

        // Motif lingkaran konsentris halus kiri bawah untuk keseimbangan layar penuh
        RingsPattern(
            radii = listOf(44.dp, 84.dp, 124.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .wrapContentSize(align = Alignment.BottomStart, unbounded = true)
                .offset(x = (-108).dp, y = 108.dp)
                .size(260.dp)
        )

        // Pola titik gradasi kanan bawah (selaras dengan Login & PIN)
        DotsPattern(
            dotRadius = 1.5.dp,
            step = 20.dp,
            maskAtBottomRight = true,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(width = 210.dp, height = 180.dp)
        )

        // Blok merek utama di tengah layar
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp)
                .graphicsLayer {
                    alpha = progress
                    translationY = contentTranslationY
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BrandLogoMark(
                containerSize = 80.dp,
                cornerRadius = 22.dp,
                iconSize = 40.dp,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                    }
                    .testTag("splash-logo")
            )

            Spacer(modifier = Modifier.height(24.dp))

            HeroTitle(
                text = "Barokah Jaya",
                fontSize = 34.sp,
                lineBoxHeight = (34f * 1.1f).dp,
                textAlign = TextAlign.Center,
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            HeroSubtitle(
                text = "Sistem Kasir & Inventaris",
                fontSize = 15.sp,
                lineBoxHeight = (15f * 1.21f).dp,
                textAlign = TextAlign.Center,
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            SplashLoadingBar(
                animate = animateEntrance,
                modifier = Modifier.testTag("splash-loading-bar")
            )
        }
    }
}

@Composable
private fun SplashLoadingBar(
    animate: Boolean,
    modifier: Modifier = Modifier
) {
    val fraction = if (animate) {
        val transition = rememberInfiniteTransition(label = "splash-progress")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "splash-progress-fraction"
        )
        value
    } else {
        0.45f
    }

    val trackWidthDp = 120f
    val indicatorWidthDp = 44f
    val offsetX = (trackWidthDp + indicatorWidthDp) * fraction - indicatorWidthDp

    Box(
        modifier = modifier
            .width(trackWidthDp.dp)
            .height(3.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.22f))
    ) {
        Box(
            modifier = Modifier
                .offset(x = offsetX.dp)
                .width(indicatorWidthDp.dp)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(OnBrand)
        )
    }
}

@Preview(name = "Barokah Jaya Splash - Ponsel (390x844)", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun SplashPhoneDesignPreview() {
    TbterminalappTheme {
        SplashScreen(animateEntrance = false)
    }
}

@Preview(name = "Barokah Jaya Splash - Tablet (1024x768)", widthDp = 1024, heightDp = 768, showBackground = true)
@Composable
fun SplashTabletDesignPreview() {
    TbterminalappTheme {
        SplashScreen(animateEntrance = false)
    }
}

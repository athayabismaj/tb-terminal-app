package com.tbterminal.app.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.BrandSoftText
import com.tbterminal.app.ui.theme.KawanSans
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.SurfaceWhite
import kotlin.math.hypot

@Composable
internal fun RingsPattern(
    radii: List<Dp>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val stroke = Stroke(width = 1.5.dp.toPx())
        val color = Color.White.copy(alpha = 0.14f)
        radii.forEach { radius ->
            drawCircle(
                color = color,
                radius = radius.toPx(),
                center = center,
                style = stroke
            )
        }
    }
}

@Composable
internal fun DotsPattern(
    dotRadius: Dp,
    step: Dp,
    maskAtBottomRight: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clipToBounds()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        val stepPx = step.toPx()
        val radiusPx = dotRadius.toPx()
        val dotColor = Color.White.copy(alpha = 0.35f)
        val start = stepPx / 2f

        var y = start
        while (y < size.height + radiusPx) {
            var x = start
            while (x < size.width + radiusPx) {
                drawCircle(
                    color = dotColor,
                    radius = radiusPx,
                    center = Offset(x, y)
                )
                x += stepPx
            }
            y += stepPx
        }

        val maskCenter = if (maskAtBottomRight) {
            Offset(size.width, size.height)
        } else {
            Offset(0f, size.height)
        }
        val maskRadius = hypot(size.width, size.height)
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color.Black,
                    0.72f to Color.Transparent
                ),
                center = maskCenter,
                radius = maskRadius
            ),
            blendMode = BlendMode.DstIn
        )
    }
}

@Composable
internal fun HeroIconBox(
    containerSize: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(containerSize)
            .background(SurfaceWhite, RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
internal fun BrandLogoMark(
    containerSize: Dp,
    cornerRadius: Dp,
    iconSize: Dp,
    modifier: Modifier = Modifier
) {
    val roofPath = remember { PathParser().parsePathString("M4 9l1.5-5h13L20 9").toPath() }
    val awningPath = remember {
        PathParser().parsePathString("M4 9c0 1.7 1.3 3 3 3s3-1.3 3-3c0 1.7 1.3 3 3 3s3-1.3 3-3c0 1.7 1.3 3 3 3").toPath()
    }
    val basePath = remember { PathParser().parsePathString("M5 12v8h14v-8").toPath() }

    HeroIconBox(
        containerSize = containerSize,
        cornerRadius = cornerRadius,
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.size(iconSize)) {
            val scale = size.width / 24f
            val stroke = Stroke(
                width = 1.8f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
            withTransform({ scale(scale, scale, Offset.Zero) }) {
                drawPath(path = roofPath, color = Brand, style = stroke)
                drawPath(path = awningPath, color = Brand, style = stroke)
                drawPath(path = basePath, color = Brand, style = stroke)
            }
        }
    }
}

@Composable
internal fun HeroTitle(
    text: String,
    fontSize: TextUnit,
    lineBoxHeight: Dp,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    contentAlignment: Alignment = Alignment.CenterStart
) {
    Box(
        modifier = modifier
            .height(lineBoxHeight)
            .wrapContentHeight(align = Alignment.CenterVertically, unbounded = true),
        contentAlignment = contentAlignment
    ) {
        Text(
            text = text,
            fontFamily = KawanSans,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = OnBrand,
            textAlign = textAlign,
            lineHeight = 1.1.em,
            letterSpacing = (-0.03).em,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None
                )
            )
        )
    }
}

@Composable
internal fun HeroSubtitle(
    text: String,
    fontSize: TextUnit,
    lineBoxHeight: Dp,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    contentAlignment: Alignment = Alignment.CenterStart
) {
    Box(
        modifier = modifier
            .height(lineBoxHeight)
            .wrapContentHeight(align = Alignment.CenterVertically, unbounded = true),
        contentAlignment = contentAlignment
    ) {
        Text(
            text = text,
            fontFamily = KawanSans,
            fontSize = fontSize,
            fontWeight = FontWeight.Normal,
            color = BrandSoftText,
            textAlign = textAlign,
            lineHeight = 1.21.em,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None
                )
            )
        )
    }
}

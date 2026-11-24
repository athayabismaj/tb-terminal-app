package com.tbterminal.app.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.theme.AlertActionColor
import com.tbterminal.app.ui.theme.AlertBorder
import com.tbterminal.app.ui.theme.AlertErrorCircle
import com.tbterminal.app.ui.theme.AlertErrorIcon
import com.tbterminal.app.ui.theme.AlertMessageColor
import com.tbterminal.app.ui.theme.AlertShadowSpot
import com.tbterminal.app.ui.theme.AlertSuccessCircle
import com.tbterminal.app.ui.theme.AlertSuccessColor
import com.tbterminal.app.ui.theme.AlertSurface
import com.tbterminal.app.ui.theme.AlertTitleColor
import com.tbterminal.app.ui.theme.AlertWarningCircle
import com.tbterminal.app.ui.theme.AlertWarningColor
import com.tbterminal.app.ui.theme.KawanSans
import com.tbterminal.app.ui.theme.RadiusControl
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

enum class AlertType {
    Error,
    Success,
    Warning
}

@Composable
fun AppAlert(
    type: AlertType,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    onDismiss: () -> Unit = {},
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(visible, type, title, message) {
        if (visible && (type == AlertType.Success || type == AlertType.Warning)) {
            delay(4000L)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200)) +
            slideInVertically(animationSpec = tween(200)) { it / 2 },
        exit = fadeOut(animationSpec = tween(200)) +
            slideOutVertically(animationSpec = tween(200)) { it / 2 },
        modifier = modifier
    ) {
        AppAlertCard(
            type = type,
            title = title,
            message = message,
            actionLabel = actionLabel,
            onAction = onAction,
            onDismiss = onDismiss
        )
    }
}

@Composable
internal fun AppAlertCard(
    type: AlertType,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var dragOffsetX by remember(type, title, message) { mutableFloatStateOf(0f) }
    val shape = RoundedCornerShape(RadiusControl)

    val circleColor: Color
    val iconColor: Color
    when (type) {
        AlertType.Error -> {
            circleColor = AlertErrorCircle
            iconColor = AlertErrorIcon
        }
        AlertType.Success -> {
            circleColor = AlertSuccessCircle
            iconColor = AlertSuccessColor
        }
        AlertType.Warning -> {
            circleColor = AlertWarningCircle
            iconColor = AlertWarningColor
        }
    }

    val swipeModifier = if (type == AlertType.Error) {
        Modifier
            .offset { IntOffset(dragOffsetX.roundToInt(), 0) }
            .pointerInput(type, title, message) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (abs(dragOffsetX) > 96f) {
                            onDismiss()
                        } else {
                            dragOffsetX = 0f
                        }
                    },
                    onDragCancel = { dragOffsetX = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        dragOffsetX += dragAmount
                    }
                )
            }
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .then(swipeModifier)
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = AlertShadowSpot,
                spotColor = AlertShadowSpot
            )
            .clip(shape)
            .background(AlertSurface)
            .border(width = 1.dp, color = AlertBorder, shape = shape)
            .padding(1.dp)
            .padding(start = 12.dp, top = 10.dp, end = 8.dp, bottom = 10.dp)
            .semantics {
                liveRegion = if (type == AlertType.Error) {
                    LiveRegionMode.Assertive
                } else {
                    LiveRegionMode.Polite
                }
            }
            .testTag("app-alert-card"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(circleColor),
            contentAlignment = Alignment.Center
        ) {
            AlertTypeIcon(
                type = type,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier.heightIn(min = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = title,
                    fontFamily = KawanSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp,
                    color = AlertTitleColor,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.None
                        )
                    )
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier.heightIn(min = 18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = message,
                    fontFamily = KawanSans,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 18.sp,
                    color = AlertMessageColor,
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

        if (!actionLabel.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onAction
                    )
                    .padding(horizontal = 14.dp)
                    .testTag("app-alert-action"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionLabel,
                    fontFamily = KawanSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = (14f * 1.21f).sp,
                    color = AlertActionColor,
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
    }
}

@Composable
private fun AlertTypeIcon(
    type: AlertType,
    tint: Color,
    modifier: Modifier = Modifier
) {
    // Lucide 24x24 paths, stroke-width 1.8
    val wifiDotPath = remember { PathParser().parsePathString("M12 20h.01").toPath() }
    val wifiInnerPath = remember { PathParser().parsePathString("M8.5 16.429a5 5 0 0 1 7 0").toPath() }
    val wifiMidLeftPath = remember { PathParser().parsePathString("M5 12.859a10 10 0 0 1 5.17-2.69").toPath() }
    val wifiMidRightPath = remember { PathParser().parsePathString("M19 12.859a10 10 0 0 0-2.007-1.523").toPath() }
    val wifiOuterLeftPath = remember { PathParser().parsePathString("M2 8.82a15 15 0 0 1 4.177-2.643").toPath() }
    val wifiOuterRightPath = remember { PathParser().parsePathString("M22 8.82a15 15 0 0 0-11.288-3.764").toPath() }
    val wifiSlashPath = remember { PathParser().parsePathString("M2 2l20 20").toPath() }

    val checkCirclePath = remember { PathParser().parsePathString("M22 11.08V12a10 10 0 1 1-5.93-9.14").toPath() }
    val checkMarkPath = remember { PathParser().parsePathString("M22 4L12 14.01l-3-3").toPath() }

    val warnTrianglePath = remember {
        PathParser().parsePathString("M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z").toPath()
    }
    val warnLinePath = remember { PathParser().parsePathString("M12 9v4").toPath() }
    val warnDotPath = remember { PathParser().parsePathString("M12 17h.01").toPath() }

    Canvas(modifier = modifier) {
        val scale = size.width / 24f
        val stroke = Stroke(
            width = 1.8f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        withTransform({ scale(scale, scale, Offset.Zero) }) {
            when (type) {
                AlertType.Error -> {
                    drawPath(wifiDotPath, color = tint, style = stroke)
                    drawPath(wifiInnerPath, color = tint, style = stroke)
                    drawPath(wifiMidLeftPath, color = tint, style = stroke)
                    drawPath(wifiMidRightPath, color = tint, style = stroke)
                    drawPath(wifiOuterLeftPath, color = tint, style = stroke)
                    drawPath(wifiOuterRightPath, color = tint, style = stroke)
                    drawPath(wifiSlashPath, color = tint, style = stroke)
                }
                AlertType.Success -> {
                    drawPath(checkCirclePath, color = tint, style = stroke)
                    drawPath(checkMarkPath, color = tint, style = stroke)
                }
                AlertType.Warning -> {
                    drawPath(warnTrianglePath, color = tint, style = stroke)
                    drawPath(warnLinePath, color = tint, style = stroke)
                    drawPath(warnDotPath, color = tint, style = stroke)
                }
            }
        }
    }
}

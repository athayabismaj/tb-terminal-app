package com.tbterminal.app.ui.stockopname

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.theme.TbAmber
import com.tbterminal.app.ui.theme.TbBackground
import com.tbterminal.app.ui.theme.TbError
import com.tbterminal.app.ui.theme.TbGreen
import com.tbterminal.app.ui.theme.TbGreenDark
import com.tbterminal.app.ui.theme.TbOutline
import com.tbterminal.app.ui.theme.TbSurface
import com.tbterminal.app.ui.theme.TbSurfaceMuted
import com.tbterminal.app.ui.theme.TbText
import com.tbterminal.app.ui.theme.TbTextMuted
import java.math.BigDecimal

internal val OpnameBackground = TbBackground
internal val OpnameSurface = Color.White
internal val OpnameSoft = TbSurfaceMuted
internal val OpnameLine = TbOutline
internal val OpnameText = TbText
internal val OpnameMuted = TbTextMuted
internal val OpnamePrimary = TbGreen
internal val OpnamePrimaryDark = TbGreenDark
internal val OpnameDanger = TbError
internal val OpnameWarning = TbAmber

@Composable
internal fun OpnameTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = OpnamePrimaryDark,
    unfocusedBorderColor = OpnameLine.copy(alpha = 0.7f),
    disabledBorderColor = OpnameLine.copy(alpha = 0.55f),
    focusedContainerColor = OpnameSoft,
    unfocusedContainerColor = OpnameSoft
)

@Composable
internal fun HeaderText(
    text: String,
    modifier: Modifier,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(text, color = OpnameMuted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
internal fun LoadingState() {
    com.tbterminal.app.ui.components.SkeletonList(modifier = Modifier.fillMaxSize(), itemCount = 6)
}

@Composable
internal fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = OpnameMuted)
    }
}

internal fun BigDecimal.qtyText(): String {
    return stripTrailingZeros().toPlainString()
}

internal fun BigDecimal.signedQtyText(): String {
    val value = qtyText()
    return if (signum() > 0) "+$value" else value
}

internal fun BigDecimal?.differenceColor(): Color {
    if (this == null || compareTo(BigDecimal.ZERO) == 0) return OpnameMuted
    return if (signum() > 0) OpnamePrimaryDark else OpnameDanger
}

package com.tbterminal.app.ui.products

import androidx.compose.ui.graphics.Color
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
import java.text.NumberFormat
import java.util.Locale

internal val ProductBackground = TbBackground
internal val ProductSurface = TbSurface
internal val ProductPrimary = TbGreen
internal val ProductPrimaryDark = TbGreenDark
internal val ProductText = TbText
internal val ProductMuted = TbTextMuted
internal val ProductLine = TbOutline
internal val ProductSoft = TbSurfaceMuted
internal val ProductDanger = TbError
internal val ProductWarning = TbAmber
internal val ProductInfo = Color(0xFF2563EB)

internal fun String.numericInput(): String {
    return filter { char -> char.isDigit() || char == '.' || char == ',' }
}

internal fun BigDecimal.moneyText(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))
    formatter.maximumFractionDigits = 0
    return formatter.format(this)
}

internal fun BigDecimal.quantityText(): String {
    return stripTrailingZeros().toPlainString()
}

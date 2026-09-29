package com.tbterminal.app.ui.products

import androidx.compose.ui.graphics.Color
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.ui.theme.TbBackground
import com.tbterminal.app.ui.theme.TbGreen
import com.tbterminal.app.ui.theme.TbGreenDark
import com.tbterminal.app.ui.theme.TbOutline
import com.tbterminal.app.ui.theme.TbSurfaceMuted
import com.tbterminal.app.ui.theme.TbText
import com.tbterminal.app.ui.theme.TbTextMuted

internal val CategoryWhite = Color.White
internal val CategorySurfaceBg = TbBackground
internal val CategorySlate50 = TbSurfaceMuted
internal val CategorySlate100 = TbSurfaceMuted
internal val CategorySlate200 = TbOutline
internal val CategorySlate400 = TbTextMuted
internal val CategorySlate500 = TbTextMuted
internal val CategorySlate600 = TbTextMuted
internal val CategorySlate900 = TbText
internal val CategoryEmerald50 = TbGreen.copy(alpha = 0.10f)
internal val CategoryEmerald100 = TbGreen.copy(alpha = 0.18f)
internal val CategoryEmerald600 = TbGreen
internal val CategoryEmerald700 = TbGreenDark
internal val CategoryEmerald800 = TbGreenDark

internal fun ProductCategory.initial(): String {
    return name.firstOrNull()?.uppercase() ?: "?"
}

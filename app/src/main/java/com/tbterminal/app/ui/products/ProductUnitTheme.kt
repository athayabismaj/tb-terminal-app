package com.tbterminal.app.ui.products

import androidx.compose.ui.graphics.Color
import com.tbterminal.app.data.model.ProductUnit
import com.tbterminal.app.ui.theme.TbBackground
import com.tbterminal.app.ui.theme.TbGreen
import com.tbterminal.app.ui.theme.TbGreenDark
import com.tbterminal.app.ui.theme.TbOutline
import com.tbterminal.app.ui.theme.TbSurfaceMuted
import com.tbterminal.app.ui.theme.TbText
import com.tbterminal.app.ui.theme.TbTextMuted

internal val UnitWhite = Color.White
internal val UnitSurfaceBg = TbBackground
internal val UnitSlate50 = TbSurfaceMuted
internal val UnitSlate100 = TbSurfaceMuted
internal val UnitSlate200 = TbOutline
internal val UnitSlate400 = TbTextMuted
internal val UnitSlate500 = TbTextMuted
internal val UnitSlate600 = TbTextMuted
internal val UnitSlate900 = TbText
internal val UnitEmerald50 = TbGreen.copy(alpha = 0.10f)
internal val UnitEmerald100 = TbGreen.copy(alpha = 0.18f)
internal val UnitEmerald600 = TbGreen
internal val UnitEmerald700 = TbGreenDark
internal val UnitEmerald800 = TbGreenDark

internal fun ProductUnit.initial(): String {
    return name.firstOrNull()?.uppercase() ?: symbol.firstOrNull()?.uppercase() ?: "?"
}

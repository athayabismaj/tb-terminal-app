package com.tbterminal.app.ui.products

import androidx.compose.ui.graphics.Color
import com.tbterminal.app.data.model.ProductUnit

internal val UnitWhite = Color.White
internal val UnitSurfaceBg = Color(0xFFF4FAFD)
internal val UnitSlate50 = Color(0xFFF8FAFC)
internal val UnitSlate100 = Color(0xFFF1F5F9)
internal val UnitSlate200 = Color(0xFFE2E8F0)
internal val UnitSlate400 = Color(0xFF94A3B8)
internal val UnitSlate500 = Color(0xFF64748B)
internal val UnitSlate600 = Color(0xFF475569)
internal val UnitSlate900 = Color(0xFF0F172A)
internal val UnitEmerald50 = Color(0xFFECFDF5)
internal val UnitEmerald100 = Color(0xFFD1FAE5)
internal val UnitEmerald600 = Color(0xFF059669)
internal val UnitEmerald700 = Color(0xFF047857)
internal val UnitEmerald800 = Color(0xFF065F46)

internal fun ProductUnit.initial(): String {
    return name.firstOrNull()?.uppercase() ?: symbol.firstOrNull()?.uppercase() ?: "?"
}

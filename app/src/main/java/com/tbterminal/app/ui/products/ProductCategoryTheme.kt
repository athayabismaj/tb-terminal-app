package com.tbterminal.app.ui.products

import androidx.compose.ui.graphics.Color
import com.tbterminal.app.data.model.ProductCategory

internal val CategoryWhite = Color.White
internal val CategorySurfaceBg = Color(0xFFF4FAFD)
internal val CategorySlate50 = Color(0xFFF8FAFC)
internal val CategorySlate100 = Color(0xFFF1F5F9)
internal val CategorySlate200 = Color(0xFFE2E8F0)
internal val CategorySlate400 = Color(0xFF94A3B8)
internal val CategorySlate500 = Color(0xFF64748B)
internal val CategorySlate600 = Color(0xFF475569)
internal val CategorySlate900 = Color(0xFF0F172A)
internal val CategoryEmerald50 = Color(0xFFECFDF5)
internal val CategoryEmerald100 = Color(0xFFD1FAE5)
internal val CategoryEmerald600 = Color(0xFF059669)
internal val CategoryEmerald700 = Color(0xFF047857)
internal val CategoryEmerald800 = Color(0xFF065F46)

internal fun ProductCategory.initial(): String {
    return name.firstOrNull()?.uppercase() ?: "?"
}

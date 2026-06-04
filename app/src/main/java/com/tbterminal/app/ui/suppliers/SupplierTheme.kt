package com.tbterminal.app.ui.suppliers

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal val SupplierSurface = Color.White
internal val SupplierSlate50 = Color(0xFFF8FAFC)
internal val SupplierSlate100 = Color(0xFFF1F5F9)
internal val SupplierSlate200 = Color(0xFFE2E8F0)
internal val SupplierSlate400 = Color(0xFF94A3B8)
internal val SupplierSlate500 = Color(0xFF64748B)
internal val SupplierSlate600 = Color(0xFF475569)
internal val SupplierSlate900 = Color(0xFF0F172A)
internal val SupplierEmerald50 = Color(0xFFECFDF5)
internal val SupplierEmerald100 = Color(0xFFD1FAE5)
internal val SupplierEmerald600 = Color(0xFF059669)
internal val SupplierEmerald700 = Color(0xFF047857)
internal val SupplierEmerald800 = Color(0xFF065F46)
internal val SupplierDanger = Color(0xFFDC2626)
internal val SupplierListHeight = 240.dp

@Composable
internal fun supplierTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SupplierEmerald600,
    unfocusedBorderColor = SupplierSlate200,
    focusedContainerColor = SupplierSlate50,
    unfocusedContainerColor = SupplierSlate50
)

package com.tbterminal.app.ui.suppliers

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.theme.TbBackground
import com.tbterminal.app.ui.theme.TbError
import com.tbterminal.app.ui.theme.TbGreen
import com.tbterminal.app.ui.theme.TbGreenDark
import com.tbterminal.app.ui.theme.TbGreenLight
import com.tbterminal.app.ui.theme.TbOutline
import com.tbterminal.app.ui.theme.TbSurfaceMuted
import com.tbterminal.app.ui.theme.TbText
import com.tbterminal.app.ui.theme.TbTextMuted

internal val SupplierBackground = TbBackground
internal val SupplierSurface = Color.White
internal val SupplierSlate50 = TbSurfaceMuted
internal val SupplierSlate100 = TbSurfaceMuted
internal val SupplierSlate200 = TbOutline
internal val SupplierSlate400 = TbTextMuted
internal val SupplierSlate500 = TbTextMuted
internal val SupplierSlate600 = TbText
internal val SupplierSlate900 = TbText
internal val SupplierEmerald50 = TbGreenLight
internal val SupplierEmerald100 = TbGreenLight
internal val SupplierEmerald600 = TbGreen
internal val SupplierEmerald700 = TbGreenDark
internal val SupplierEmerald800 = TbGreenDark
internal val SupplierDanger = TbError
internal val SupplierListHeight = 240.dp

@Composable
internal fun supplierTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SupplierEmerald600,
    unfocusedBorderColor = SupplierSlate200,
    focusedContainerColor = SupplierSlate50,
    unfocusedContainerColor = SupplierSlate50
)

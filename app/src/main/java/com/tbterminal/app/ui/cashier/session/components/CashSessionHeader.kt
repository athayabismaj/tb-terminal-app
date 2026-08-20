package com.tbterminal.app.ui.cashier.session.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.cashier.session.OnSurface

@Composable
fun CashSessionHeader() {
    Column {
        Text("Kas Harian", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = OnSurface)
    }
}

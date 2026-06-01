package com.tbterminal.app.ui.cashier.session.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.cashier.session.OnSurface
import com.tbterminal.app.ui.cashier.session.Primary
import com.tbterminal.app.ui.cashier.session.PrimaryLight
import com.tbterminal.app.ui.cashier.session.Slate400

@Composable
fun CashSessionHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(PrimaryLight)
                .padding(14.dp)
        ) {
            Icon(
                Icons.Outlined.AccountBalanceWallet,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text("Kas Harian & Pengeluaran", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = OnSurface)
            Text("Pantau aktivitas kas dan buka atau tutup shift kasir Anda.", fontSize = 14.sp, color = Slate400)
        }
    }
}

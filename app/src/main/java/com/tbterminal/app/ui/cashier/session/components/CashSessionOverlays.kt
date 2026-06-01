package com.tbterminal.app.ui.cashier.session.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.cashier.session.ErrorColor
import com.tbterminal.app.ui.cashier.session.OnSurface
import com.tbterminal.app.ui.cashier.session.Primary
import com.tbterminal.app.ui.cashier.session.PrimaryLight
import com.tbterminal.app.ui.cashier.session.Slate100
import com.tbterminal.app.ui.cashier.session.Slate400

@Composable
fun CashSessionErrorOverlay(
    message: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Card(modifier = Modifier.padding(24.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Terjadi Kesalahan", color = ErrorColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(message)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss) {
                    Text("Tutup")
                }
            }
        }
    }
}

@Composable
fun SuccessModalOverlay(onDismiss: () -> Unit, onPrint: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OnSurface.copy(alpha = 0.6f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(400.dp)
                .padding(24.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Column(modifier = Modifier.padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(PrimaryLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                Text("Shift Ditutup", fontSize = 28.sp, fontWeight = FontWeight.Black, color = OnSurface)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Laporan akhir berhasil divalidasi. Struk rekap sedang dicetak dan salinan digital telah dikirim.",
                    fontSize = 14.sp,
                    color = Slate400,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("KEMBALI KE BERANDA", fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onPrint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, Slate100),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurface)
                ) {
                    Text("CETAK ULANG STRUK", fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }
    }
}

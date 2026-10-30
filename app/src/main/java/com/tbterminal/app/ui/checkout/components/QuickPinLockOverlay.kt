package com.tbterminal.app.ui.checkout.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BrandGreen = Color(0xFF006948)
private val DarkBg = Color(0xFF0F172A)
private val CardBg = Color(0xFF1E293B)
private val TextWhite = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)
private val ErrorRed = Color(0xFFEF4444)

/**
 * Overlay kunci layar kasir cepat (Quick PIN Screen Lock).
 * Menutup seluruh layar kasir dengan aman saat kasir meninggalkan meja POS.
 */
@Composable
fun QuickPinLockOverlay(
    cashierName: String,
    pinInput: String,
    errorMessage: String?,
    onPinChange: (String) -> Unit,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentTime = remember {
        SimpleDateFormat("HH:mm", Locale.forLanguageTag("id-ID")).format(Date())
    }
    val currentDate = remember {
        SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.forLanguageTag("id-ID")).format(Date())
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg.copy(alpha = 0.96f))
            .zIndex(100f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Jam & Info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currentTime,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite,
                    letterSpacing = 2.sp
                )
                Text(
                    text = currentDate,
                    fontSize = 13.sp,
                    color = TextMuted
                )
            }

            // Avatar & Info Kasir
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(BrandGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Person, null, tint = Color(0xFF34D399), modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cashierName,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontSize = 15.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Lock, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Layar Terkunci Sementara", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Indikator Titik PIN
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                val maxDigits = 6
                for (i in 0 until maxDigits) {
                    val isFilled = i < pinInput.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isFilled) Color(0xFF10B981) else Color(0xFF334155))
                            .border(
                                1.5.dp,
                                if (isFilled) Color(0xFF34D399) else Color(0xFF475569),
                                CircleShape
                            )
                    )
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = ErrorRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Keypad Numerik
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val keypadRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "DEL")
                )

                keypadRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (key == "C" || key == "DEL") CardBg.copy(alpha = 0.5f) else CardBg)
                                    .clickable {
                                        when (key) {
                                            "C" -> onPinChange("")
                                            "DEL" -> if (pinInput.isNotEmpty()) onPinChange(pinInput.dropLast(1))
                                            else -> if (pinInput.length < 6) {
                                                val newPin = pinInput + key
                                                onPinChange(newPin)
                                                if (newPin.length == 6) onUnlock()
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                when (key) {
                                    "DEL" -> Icon(
                                        Icons.AutoMirrored.Outlined.Backspace,
                                        contentDescription = "Hapus",
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    else -> Text(
                                        text = key,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (key == "C") ErrorRed else TextWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tombol Buka Kunci
            Button(
                onClick = onUnlock,
                enabled = pinInput.length >= 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandGreen,
                    disabledContainerColor = Color(0xFF1E293B)
                )
            ) {
                Text(
                    text = "Buka Kunci Layar",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (pinInput.length >= 4) Color.White else Color(0xFF64748B)
                )
            }
        }
    }
}

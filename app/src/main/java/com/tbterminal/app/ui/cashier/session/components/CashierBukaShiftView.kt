package com.tbterminal.app.ui.cashier.session.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CanvasBg = Color(0xFFF8FAF9)
private val BrandGreen = Color(0xFF1B5E3C)
private val BrandGreenLight = Color(0xFFE8F6ED)
private val Slate900 = Color(0xFF0F172A)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)
private val DangerRed = Color(0xFFDC2626)

@Composable
fun CashierBukaShiftView(
    cashierName: String,
    terminalName: String = "Terminal Barokah Jaya",
    openingCashInput: String,
    onOpeningCashChanged: (String) -> Unit,
    onOpenSession: () -> Unit,
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val quickPresets = listOf(
        "100000" to "Rp 100 rb",
        "200000" to "Rp 200 rb",
        "300000" to "Rp 300 rb",
        "500000" to "Rp 500 rb"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ── 1. HEADER BUKA SHIFT ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BrandGreenLight)
                        .border(1.dp, BrandGreen.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Buka Shift Kasir",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "$terminalName · Kasir: $cashierName",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // ── 2. INPUT MODAL AWAL KAS ──
            val isError = errorMessage != null
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MODAL AWAL DI LACI KASIR (IDR)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate600,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "Wajib Diisi",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandGreen
                    )
                }

                OutlinedTextField(
                    value = openingCashInput,
                    onValueChange = onOpeningCashChanged,
                    placeholder = {
                        Text(
                            text = "0",
                            color = Slate400,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Text(
                            text = "Rp",
                            color = if (isError) DangerRed else BrandGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(start = 14.dp, end = 4.dp)
                        )
                    },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text(errorMessage.orEmpty(), color = DangerRed, fontSize = 11.sp) }
                    } else {
                        { Text("Hitung fisik uang tunai di laci sebelum membuka shift.", color = Slate500, fontSize = 11.sp) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50,
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = Slate200,
                        errorBorderColor = DangerRed
                    )
                )

                // ── QUICK DENOMINATION PRESET CHIPS ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickPresets.forEach { (amount, label) ->
                        val isSelected = openingCashInput == amount
                        Surface(
                            onClick = { onOpeningCashChanged(amount) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) BrandGreenLight else Slate50,
                            border = BorderStroke(1.dp, if (isSelected) BrandGreen else Slate200),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BrandGreen else Slate600
                                )
                            }
                        }
                    }
                }
            }

            // ── 3. SATU TOMBOL UTAMA BUKA SHIFT ──
            Button(
                onClick = onOpenSession,
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandGreen,
                    contentColor = Color.White,
                    disabledContainerColor = Slate200,
                    disabledContentColor = Slate400
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Membuka Shift...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.PointOfSale,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Buka Shift & Mulai Transaksi",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

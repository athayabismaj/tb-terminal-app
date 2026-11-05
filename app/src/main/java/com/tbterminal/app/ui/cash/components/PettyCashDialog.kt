package com.tbterminal.app.ui.cash.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tbterminal.app.data.local.cashexpense.CashMovementType
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val BrandGreen = Color(0xFF1B5E3C)
private val BrandRed = Color(0xFFDC2626)
private val SurfaceWhite = Color(0xFFFFFFFF)
private val Slate900 = Color(0xFF0F172A)
private val Slate600 = Color(0xFF475569)
private val Slate400 = Color(0xFF94A3B8)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)

@Composable
fun PettyCashDialog(
    selectedType: CashMovementType = CashMovementType.CASH_OUT,
    amountInput: String,
    categoryInput: String,
    descriptionInput: String,
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
    onTypeChanged: (CashMovementType) -> Unit,
    onAmountChanged: (String) -> Unit,
    onCategoryChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isCashIn = selectedType == CashMovementType.CASH_IN
    val activeColor = if (isCashIn) BrandGreen else BrandRed

    val outCategories = listOf("Bensin Armada", "Air / Galon", "Konsumsi Toko", "ATK & Lakban", "Lainnya")
    val inCategories = listOf("Tambah Modal Kembalian", "Setoran Brankas", "Titipan Kasir", "Lainnya")
    val currentCategories = if (isCashIn) inCategories else outCategories

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceWhite,
            border = BorderStroke(1.dp, Slate200),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(activeColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isCashIn) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                                contentDescription = null,
                                tint = activeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Kas Kecil (Petty Cash)",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = if (isCashIn) "Pemasukan kas ke laci kasir" else "Pengeluaran uang dari laci kasir",
                                fontSize = 12.sp,
                                color = Slate600
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "Tutup", tint = Slate600)
                    }
                }

                // Type Segmented Control (Cash Out vs Cash In)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Slate100,
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = { onTypeChanged(CashMovementType.CASH_OUT) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = if (!isCashIn) Color.White else Color.Transparent,
                            border = if (!isCashIn) BorderStroke(1.dp, BrandRed.copy(alpha = 0.3f)) else null,
                            shadowElevation = if (!isCashIn) 1.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (!isCashIn) BrandRed else Slate600,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Kas Keluar",
                                    fontSize = 13.sp,
                                    fontWeight = if (!isCashIn) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!isCashIn) BrandRed else Slate600
                                )
                            }
                        }

                        Surface(
                            onClick = { onTypeChanged(CashMovementType.CASH_IN) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCashIn) Color.White else Color.Transparent,
                            border = if (isCashIn) BorderStroke(1.dp, BrandGreen.copy(alpha = 0.3f)) else null,
                            shadowElevation = if (isCashIn) 1.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (isCashIn) BrandGreen else Slate600,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Kas Masuk",
                                    fontSize = 13.sp,
                                    fontWeight = if (isCashIn) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCashIn) BrandGreen else Slate600
                                )
                            }
                        }
                    }
                }

                // Quick Category Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Kategori Cepat",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate600
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        currentCategories.take(3).forEach { category ->
                            val isSelected = categoryInput.equals(category, ignoreCase = true)
                            Surface(
                                onClick = {
                                    onCategoryChanged(category)
                                    if (descriptionInput.isBlank() || currentCategories.any { descriptionInput.equals(it, ignoreCase = true) }) {
                                        onDescriptionChanged(category)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) activeColor.copy(alpha = 0.12f) else Slate100,
                                border = BorderStroke(1.dp, if (isSelected) activeColor else Slate200)
                            ) {
                                Text(
                                    text = category,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) activeColor else Slate600,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    if (currentCategories.size > 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            currentCategories.drop(3).forEach { category ->
                                val isSelected = categoryInput.equals(category, ignoreCase = true)
                                Surface(
                                    onClick = {
                                        onCategoryChanged(category)
                                        if (descriptionInput.isBlank() || currentCategories.any { descriptionInput.equals(it, ignoreCase = true) }) {
                                            onDescriptionChanged(category)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) activeColor.copy(alpha = 0.12f) else Slate100,
                                    border = BorderStroke(1.dp, if (isSelected) activeColor else Slate200)
                                ) {
                                    Text(
                                        text = category,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) activeColor else Slate600,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Nominal Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Nominal (Rp)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate600
                    )
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = onAmountChanged,
                        placeholder = { Text("Contoh: 20000", color = Slate400, fontSize = 14.sp) },
                        leadingIcon = {
                            Text("Rp", fontWeight = FontWeight.Bold, color = Slate900, modifier = Modifier.padding(start = 12.dp, end = 4.dp))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activeColor,
                            unfocusedBorderColor = Slate200,
                            focusedContainerColor = Slate100.copy(alpha = 0.5f),
                            unfocusedContainerColor = Slate100.copy(alpha = 0.5f)
                        )
                    )

                    // Quick Nominal Adders (+10k, +20k, +50k, +100k)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(10_000L, 20_000L, 50_000L, 100_000L).forEach { addAmount ->
                            Surface(
                                onClick = {
                                    val current = amountInput.filter(Char::isDigit).toLongOrNull() ?: 0L
                                    onAmountChanged((current + addAmount).toString())
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Slate100,
                                border = BorderStroke(1.dp, Slate200)
                            ) {
                                Text(
                                    text = "+${addAmount / 1000}rb",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate600,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // Description Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Keterangan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate600
                    )
                    OutlinedTextField(
                        value = descriptionInput,
                        onValueChange = onDescriptionChanged,
                        placeholder = { Text("Alasan atau rincian mutasi kas...", color = Slate400, fontSize = 14.sp) },
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activeColor,
                            unfocusedBorderColor = Slate200,
                            focusedContainerColor = Slate100.copy(alpha = 0.5f),
                            unfocusedContainerColor = Slate100.copy(alpha = 0.5f)
                        )
                    )
                }

                // Error message if any
                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Text("Batal", color = Slate600, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onConfirm,
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = activeColor)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (isCashIn) "Simpan Kas Masuk" else "Simpan Pengeluaran",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

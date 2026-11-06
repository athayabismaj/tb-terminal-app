package com.tbterminal.app.ui.cash.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.local.cashexpense.CashMovementType
import java.text.NumberFormat
import java.util.Locale

private val Forest900 = Color(0xFF10533C)       // Brand green primary tone #10533c
private val Forest950 = Color(0xFF0B3828)       // #0b3828
private val Forest50 = Color(0xFFECFDF5)        // bg-emerald-50
private val ForestBorder = Color(0xFF6EE7B7)    // border-emerald-300
private val ForestIndicator = Color(0xFF16A34A) // border-emerald-600

private val Rose600 = Color(0xFFE11D48)         // bg-rose-600 / border-rose-600
private val Rose700 = Color(0xFFBE123C)         // text-rose-700
private val Rose50 = Color(0xFFFFF1F2)          // bg-rose-50
private val RoseBorder = Color(0xFFFDA4AF)      // border-rose-300

private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)

private fun formatIDR(value: Long): String {
    return NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(value)
}

private fun parseAmount(input: String): Long {
    val clean = input.filter { it.isDigit() }
    return clean.toLongOrNull() ?: 0L
}

/**
 * Bottom Sheet Modal Kas Keluar & Kas Masuk (Petty Cash).
 * Dibuat persis sesuai desain mockup HTML terbaru:
 * - Top Grab Handle Bar
 * - Header dengan judul "Kas Keluar & Kas Masuk" dan subjudul dinamis
 * - Underline Tab Navigation Switcher (Kas Keluar merah rose vs Kas Masuk hijau forest)
 * - Pilihan Kategori Cepat (berbeda untuk keluar dan masuk)
 * - Input Nominal (Rp) dengan chips cepat (+10rb, +20rb, +50rb, +100rb)
 * - Input Catatan / Alasan
 * - Stacked Native Mobile Action Buttons (Simpan Pengeluaran / Simpan Kas Masuk & Batal)
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    val categoriesKeluar = remember {
        listOf("Bensin Armada", "Air / Galon", "Konsumsi Toko", "ATK & Lakban", "Lainnya")
    }
    val categoriesMasuk = remember {
        listOf("Tambah Modal Kembalian", "Setoran Brankas", "Titipan Kasir", "Lainnya")
    }
    val currentCategories = if (isCashIn) categoriesMasuk else categoriesKeluar

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // ── Top Grab Handle ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Slate200)
                )
            }

            // ── Modal Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kas Keluar & Kas Masuk",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        lineHeight = 20.sp
                    )
                    Text(
                        text = if (isCashIn) "Catat kas masuk atau penambahan modal laci" else "Catat petty cash atau mutasi uang laci",
                        fontSize = 12.sp,
                        color = Slate500,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Slate400,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // ── Modal Body Content ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Underline Tab Navigation Switcher
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        color = Slate200,
                        thickness = 1.dp
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Tab Kas Keluar
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onTypeChanged(CashMovementType.CASH_OUT)
                                }
                                .padding(top = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (!isCashIn) Rose600 else Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Kas Keluar",
                                    fontSize = 12.5.sp,
                                    fontWeight = if (!isCashIn) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (!isCashIn) Rose600 else Slate500
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(if (!isCashIn) Rose600 else Color.Transparent)
                            )
                        }

                        // Tab Kas Masuk
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onTypeChanged(CashMovementType.CASH_IN)
                                }
                                .padding(top = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (isCashIn) Forest900 else Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Kas Masuk",
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isCashIn) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isCashIn) Forest900 else Slate500
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(if (isCashIn) ForestIndicator else Color.Transparent)
                            )
                        }
                    }
                }

                // 2. Kategori Cepat
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pilih Kategori",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700
                    )

                    // Mengelompokkan tombol kategori agar tersusun rapi sesuai mockup
                    val chunkCount = if (isCashIn) 2 else 3
                    currentCategories.chunked(chunkCount).forEach { rowCategories ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowCategories.forEach { cat ->
                                val isSelected = categoryInput.equals(cat, ignoreCase = true)
                                val (chipBg, chipBorder, chipText) = if (!isCashIn) {
                                    if (isSelected) Triple(Rose50, RoseBorder, Rose700)
                                    else Triple(Color.White, Slate200, Slate700)
                                } else {
                                    if (isSelected) Triple(Forest50, ForestBorder, Forest900)
                                    else Triple(Color.White, Slate200, Slate700)
                                }

                                Surface(
                                    onClick = {
                                        onCategoryChanged(cat)
                                        if (descriptionInput.isBlank() || currentCategories.any { descriptionInput.equals(it, ignoreCase = true) }) {
                                            onDescriptionChanged(cat)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = chipBg,
                                    border = BorderStroke(1.dp, chipBorder),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = chipText,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Nominal Input (Rp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Nominal (Rp)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Rp",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = amountInput,
                                onValueChange = onAmountChanged,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate900
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Quick Chips (+10rb, +20rb, +50rb, +100rb)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            10_000L to "+10rb",
                            20_000L to "+20rb",
                            50_000L to "+50rb",
                            100_000L to "+100rb"
                        ).forEach { (addVal, label) ->
                            Surface(
                                onClick = {
                                    val current = parseAmount(amountInput)
                                    val next = current + addVal
                                    onAmountChanged(formatIDR(next))
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Slate50,
                                border = BorderStroke(1.dp, Slate200.copy(alpha = 0.9f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Slate700
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Catatan / Alasan (Opsional)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Catatan / Alasan (Opsional)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            if (descriptionInput.isEmpty()) {
                                Text(
                                    text = "Alasan atau rincian mutasi kas...",
                                    color = Slate400,
                                    fontSize = 12.sp
                                )
                            }
                            BasicTextField(
                                value = descriptionInput,
                                onValueChange = onDescriptionChanged,
                                minLines = 2,
                                maxLines = 4,
                                textStyle = TextStyle(
                                    fontSize = 12.sp,
                                    color = Slate800
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Error Message (jika ada)
                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }

                // 5. Stacked Mobile Native Actions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val submitColor = if (!isCashIn) Rose600 else Forest900
                    val submitLabel = if (!isCashIn) "Simpan Pengeluaran" else "Simpan Kas Masuk"

                    Button(
                        onClick = onConfirm,
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = submitColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = submitLabel,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Batal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate600
                        )
                    }
                }
            }
        }
    }
}

package com.tbterminal.app.ui.checkout.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.ceil

// ═════════════════════════════════════════════════════════════════════════════
// PALET WARNA MODERN TB TERMINAL
// ═════════════════════════════════════════════════════════════════════════════
private val BrandForest = Color(0xFF10533C)
private val BrandGreen = Color(0xFF15803D)
private val BrandGreenLight = Color(0xFFF0FDF4)
private val BrandGreenBorder = Color(0xFFBBF7D0)

private val Slate900 = Color(0xFF0F172A)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)

/**
 * Kalkulator Cepat Estimasi Kebutuhan Material Bangunan di Layar POS Kasir.
 * Didesain sebagai Bottom Sheet modern sesuai sistem desain TB Terminal:
 * 1. Estimasi Keramik / Granit Lantai & Dinding (Panjang x Lebar -> Dus + Waste 5-10%)
 * 2. Estimasi Plesteran Dinding & Pasangan Bata (m² -> Sak Semen & Pasir)
 * 3. Estimasi Cat Dinding (m² & Lapis -> Liter, Galon 2.5L, Pail 20L)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialEstimatorDialog(
    onDismiss: () -> Unit,
    onApplySearchQuery: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Keramik & Granit", "Plester & Bata", "Cat Dinding")
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(Color(0xFFE2E8F0), CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .testTag("material-estimator-sheet")
        ) {
            // Header: Icon badge + Title + Subtitle on left, Circular Close button on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BrandGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Calculate,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Kalkulator Bahan Bangunan",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            letterSpacing = (-0.3).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Estimasi kebutuhan ubin, semen, dan cat",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Slate100)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup Kalkulator",
                        tint = Slate600,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Modern Segmented Tab Switcher
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate100, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isSelected) Color.White else Color.Transparent)
                                .then(
                                    if (isSelected) Modifier.border(1.dp, Slate200, RoundedCornerShape(9.dp))
                                    else Modifier
                                )
                                .clickable { selectedTab = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Slate900 else Slate500,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content with internal scrolling
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedTab) {
                    0 -> TileEstimatorTab(context, onDismiss, onApplySearchQuery)
                    1 -> PlasterEstimatorTab(context, onDismiss, onApplySearchQuery)
                    2 -> PaintEstimatorTab(context, onDismiss, onApplySearchQuery)
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// PILIHAN CHIP KUSTOM MODERN
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun EstimatorChoiceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) BrandGreenLight else Slate50)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) BrandGreen else Slate200,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) BrandGreen else Slate700,
            maxLines = 1
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// TAB 1: KERAMIK & GRANIT
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun TileEstimatorTab(
    context: Context,
    onDismiss: () -> Unit,
    onApplySearchQuery: (String) -> Unit
) {
    var lengthInput by remember { mutableStateOf("4") }
    var widthInput by remember { mutableStateOf("5") }

    val tileSizes = listOf(
        TilePreset("40x40 cm", 1.0, "Keramik 40x40"),
        TilePreset("50x50 cm", 1.0, "Keramik 50x50"),
        TilePreset("60x60 cm", 1.44, "Granit 60x60"),
        TilePreset("30x60 cm", 1.44, "Keramik 30x60"),
        TilePreset("20x25 cm", 1.0, "Keramik Dinding 20x25")
    )
    var selectedPresetIndex by remember { mutableIntStateOf(2) } // default 60x60
    var wastePercent by remember { mutableIntStateOf(10) } // default 10%

    val length = lengthInput.toDoubleOrNull() ?: 0.0
    val width = widthInput.toDoubleOrNull() ?: 0.0
    val areaNet = length * width
    val areaGross = areaNet * (1.0 + wastePercent / 100.0)
    val coveragePerBox = tileSizes[selectedPresetIndex].coveragePerBox
    val boxesNeeded = if (coveragePerBox > 0) ceil(areaGross / coveragePerBox).toInt() else 0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Section 1: Ukuran Ruangan
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "1. UKURAN RUANGAN (PANJANG X LEBAR)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500,
                letterSpacing = 0.8.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = lengthInput,
                    onValueChange = { lengthInput = it },
                    label = { Text("Panjang (m)", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandForest,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50
                    )
                )
                OutlinedTextField(
                    value = widthInput,
                    onValueChange = { widthInput = it },
                    label = { Text("Lebar (m)", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandForest,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50
                    )
                )
            }
        }

        // Section 2: Ukuran Keramik / Granit
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "2. UKURAN UBIN / KERAMIK",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500,
                letterSpacing = 0.8.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tileSizes.take(3).forEachIndexed { index, preset ->
                    EstimatorChoiceChip(
                        label = preset.label,
                        isSelected = selectedPresetIndex == index,
                        onClick = { selectedPresetIndex = index },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tileSizes.drop(3).forEachIndexed { subIndex, preset ->
                    val index = subIndex + 3
                    EstimatorChoiceChip(
                        label = preset.label,
                        isSelected = selectedPresetIndex == index,
                        onClick = { selectedPresetIndex = index },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Section 3: Cadangan Potongan (Waste)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "3. CADANGAN POTONGAN (WASTE)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500,
                letterSpacing = 0.8.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(5 to "5% Standar", 10 to "10% Pola / Sudut", 0 to "0% Pas").forEach { (pct, label) ->
                    EstimatorChoiceChip(
                        label = label,
                        isSelected = wastePercent == pct,
                        onClick = { wastePercent = pct },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Card Hasil Perhitungan
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BrandGreenLight),
            border = BorderStroke(1.dp, BrandGreenBorder),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Luas Ruangan Bersih", fontSize = 12.sp, color = Slate600)
                    Text(
                        text = String.format(Locale.US, "%.2f m²", areaNet),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate900
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Luas + Cadangan ($wastePercent%)", fontSize = 12.sp, color = Slate600)
                    Text(
                        text = String.format(Locale.US, "%.2f m²", areaGross),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate900
                    )
                }
                HorizontalDivider(color = BrandGreenBorder, modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Kebutuhan Ubin", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                        Text(tileSizes[selectedPresetIndex].label, fontSize = 11.sp, color = Slate500)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandGreen)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$boxesNeeded DUS",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Tombol Aksi
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    val summary = "Estimasi Keramik: Ruangan ${length}x${width}m (${String.format(Locale.US, "%.2f", areaNet)}m²) butuh $boxesNeeded Dus (${tileSizes[selectedPresetIndex].label})."
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Estimasi Keramik", summary))
                    Toast.makeText(context, "Hasil estimasi disalin ke clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Slate200),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700)
            ) {
                Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Salin Hasil", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = {
                    val query = tileSizes[selectedPresetIndex].searchQuery
                    onDismiss()
                    onApplySearchQuery(query)
                },
                modifier = Modifier
                    .weight(1.2f)
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = Color.White)
            ) {
                Icon(Icons.Outlined.Search, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Cari di POS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// TAB 2: PLESTER & BATA
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun PlasterEstimatorTab(
    context: Context,
    onDismiss: () -> Unit,
    onApplySearchQuery: (String) -> Unit
) {
    var wallLengthInput by remember { mutableStateOf("10") }
    var wallHeightInput by remember { mutableStateOf("3") }
    var workType by remember { mutableIntStateOf(0) } // 0: Plesteran Dinding (1 sisi 15mm), 1: Pasangan Bata Merah

    val length = wallLengthInput.toDoubleOrNull() ?: 0.0
    val height = wallHeightInput.toDoubleOrNull() ?: 0.0
    val wallArea = length * height

    val cementSacks = if (workType == 0) ceil(wallArea * 0.20).toInt() else ceil(wallArea * 0.23).toInt()
    val sandM3 = if (workType == 0) wallArea * 0.024 else wallArea * 0.043
    val bricksCount = if (workType == 1) ceil(wallArea * 70).toInt() else 0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Section 1: Dimensi Dinding
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "1. DIMENSI DINDING (PANJANG X TINGGI)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500,
                letterSpacing = 0.8.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = wallLengthInput,
                    onValueChange = { wallLengthInput = it },
                    label = { Text("Panjang Dinding (m)", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandForest,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50
                    )
                )
                OutlinedTextField(
                    value = wallHeightInput,
                    onValueChange = { wallHeightInput = it },
                    label = { Text("Tinggi Dinding (m)", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandForest,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50
                    )
                )
            }
        }

        // Section 2: Jenis Pengerjaan
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "2. JENIS PENGERJAAN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500,
                letterSpacing = 0.8.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                EstimatorChoiceChip(
                    label = "Plesteran (Tebal 15mm)",
                    isSelected = workType == 0,
                    onClick = { workType = 0 },
                    modifier = Modifier.weight(1f)
                )
                EstimatorChoiceChip(
                    label = "Pasang Bata Merah",
                    isSelected = workType == 1,
                    onClick = { workType = 1 },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Card Hasil Perhitungan (Amber tone)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Luas Bidang Dinding", fontSize = 12.sp, color = Slate600)
                    Text(
                        text = String.format(Locale.US, "%.2f m²", wallArea),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate900
                    )
                }
                HorizontalDivider(color = Color(0xFFFDE68A), modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kebutuhan Semen (50kg)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFD97706))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$cementSacks SAK",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kebutuhan Pasir Pasang", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Slate700)
                    Text(
                        text = String.format(Locale.US, "%.2f m³", sandM3),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309)
                    )
                }
                if (workType == 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Kebutuhan Bata Merah", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Slate700)
                        Text(
                            text = "$bricksCount PCS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            }
        }

        // Tombol Aksi
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    val summary = "Estimasi Dinding ${length}x${height}m (${String.format(Locale.US, "%.2f", wallArea)}m²): Butuh $cementSacks Sak Semen, ${String.format(Locale.US, "%.2f", sandM3)}m³ Pasir."
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Estimasi Plesteran", summary))
                    Toast.makeText(context, "Hasil estimasi disalin ke clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Slate200),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700)
            ) {
                Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Salin Hasil", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = {
                    onDismiss()
                    onApplySearchQuery("Semen")
                },
                modifier = Modifier
                    .weight(1.2f)
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = Color.White)
            ) {
                Icon(Icons.Outlined.Search, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Cari di POS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// TAB 3: CAT DINDING
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun PaintEstimatorTab(
    context: Context,
    onDismiss: () -> Unit,
    onApplySearchQuery: (String) -> Unit
) {
    var paintAreaInput by remember { mutableStateOf("50") }
    var coatsCount by remember { mutableIntStateOf(2) } // default 2 lapis rekomendasi pabrik

    val area = paintAreaInput.toDoubleOrNull() ?: 0.0
    val totalCoverageArea = area * coatsCount
    val litersNeeded = if (totalCoverageArea > 0) totalCoverageArea / 10.0 else 0.0
    val gallonsNeeded = ceil(litersNeeded / 2.5).toInt()
    val pailsNeeded = ceil(litersNeeded / 20.0).toInt()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Section 1: Luas Permukaan Cat
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "1. LUAS PERMUKAAN YANG DICAT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500,
                letterSpacing = 0.8.sp
            )
            OutlinedTextField(
                value = paintAreaInput,
                onValueChange = { paintAreaInput = it },
                label = { Text("Total Luas Bidang Dinding (m²)", fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandForest,
                    unfocusedBorderColor = Slate200,
                    focusedContainerColor = Slate50,
                    unfocusedContainerColor = Slate50
                )
            )
        }

        // Section 2: Jumlah Lapisan Cat
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "2. JUMLAH LAPISAN CAT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500,
                letterSpacing = 0.8.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(1 to "1 Lapis", 2 to "2 Lapis Standar", 3 to "3 Lapis Gelap").forEach { (coats, label) ->
                    EstimatorChoiceChip(
                        label = label,
                        isSelected = coatsCount == coats,
                        onClick = { coatsCount = coats },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Card Hasil Perhitungan (Blue tone)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Luas Sapuan ($coatsCount Lapis)", fontSize = 12.sp, color = Slate600)
                    Text(
                        text = String.format(Locale.US, "%.1f m²", totalCoverageArea),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate900
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Kebutuhan Cairan", fontSize = 12.sp, color = Slate600)
                    Text(
                        text = String.format(Locale.US, "%.1f Liter", litersNeeded),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D4ED8)
                    )
                }
                HorizontalDivider(color = Color(0xFFBFDBFE), modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kemasan Galon (2.5 Liter)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2563EB))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$gallonsNeeded GALON",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Atau Kemasan Pail (20 Liter)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Slate700)
                    Text(
                        text = "$pailsNeeded PAIL",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D4ED8)
                    )
                }
            }
        }

        // Tombol Aksi
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    val summary = "Estimasi Cat Dinding (${String.format(Locale.US, "%.1f", area)}m², $coatsCount Lapis): Butuh ${String.format(Locale.US, "%.1f", litersNeeded)} Liter (Setara $gallonsNeeded Galon 2.5L atau $pailsNeeded Pail 20L)."
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Estimasi Cat", summary))
                    Toast.makeText(context, "Hasil estimasi disalin ke clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Slate200),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700)
            ) {
                Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Salin Hasil", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = {
                    onDismiss()
                    onApplySearchQuery("Cat Tembok")
                },
                modifier = Modifier
                    .weight(1.2f)
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = Color.White)
            ) {
                Icon(Icons.Outlined.Search, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Cari di POS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class TilePreset(
    val label: String,
    val coveragePerBox: Double,
    val searchQuery: String
)

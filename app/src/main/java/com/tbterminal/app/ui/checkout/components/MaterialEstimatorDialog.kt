package com.tbterminal.app.ui.checkout.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.ceil

// ═════════════════════════════════════════════════════════════════════════════
// PALET WARNA REFERENSI TERMINAL BAROKAH JAYA
// ═════════════════════════════════════════════════════════════════════════════
private val BrandPrimary = Color(0xFF10533C)
private val BrandMintIcon = Color(0xFFA7F3D0)

private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate300 = Color(0xFFCBD5E1)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)

/**
 * Kalkulator Cepat Estimasi Kebutuhan Material Bangunan di Layar POS & Dashboard Kasir.
 * Mengikuti layout desain modern underline-tab bottom sheet:
 * 1. Keramik (Panjang x Lebar, ukuran ubin 4-kolom, cadangan waste, card hasil)
 * 2. Semen & Pasir (Panjang x Tinggi dinding, Plesteran / Pasang Bata, card hasil)
 * 3. Cat Dinding (Luas bidang + preset cepat, 1/2/3 lapisan, card hasil)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialEstimatorDialog(
    onDismiss: () -> Unit,
    onApplySearchQuery: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // State Tab 1: Keramik
    var tileLengthInput by remember { mutableStateOf("4") }
    var tileWidthInput by remember { mutableStateOf("5") }
    var selectedTilePreset by remember { mutableIntStateOf(0) } // default 40x40
    var wastePercent by remember { mutableIntStateOf(10) }

    // State Tab 2: Semen & Pasir
    var wallLengthInput by remember { mutableStateOf("10") }
    var wallHeightInput by remember { mutableStateOf("3") }
    var wallWorkType by remember { mutableIntStateOf(0) } // 0: Plesteran, 1: Pasang Bata

    // State Tab 3: Cat Dinding
    var paintAreaInput by remember { mutableStateOf("50") }
    var coatsCount by remember { mutableIntStateOf(2) }

    val tilePresets = remember {
        listOf(
            TilePreset("40×40", "40×40 cm", "1.0 m²", 1.0, "Keramik 40x40"),
            TilePreset("50×50", "50×50 cm", "1.0 m²", 1.0, "Keramik 50x50"),
            TilePreset("60×60", "60×60 cm", "1.44 m²", 1.44, "Granit 60x60"),
            TilePreset("80×80", "80×80 cm", "1.92 m²", 1.92, "Granit 80x80"),
        )
    }

    val activeActionData = remember(
        selectedTab,
        tileLengthInput, tileWidthInput, selectedTilePreset, wastePercent,
        wallLengthInput, wallHeightInput, wallWorkType,
        paintAreaInput, coatsCount
    ) {
        when (selectedTab) {
            0 -> {
                val l = tileLengthInput.parseDecimalInput()
                val w = tileWidthInput.parseDecimalInput()
                val areaNet = l * w
                val areaGross = areaNet * (1.0 + wastePercent / 100.0)
                val preset = tilePresets[selectedTilePreset]
                val boxes = if (preset.coveragePerBox > 0 && areaGross > 0) ceil(areaGross / preset.coveragePerBox).toInt() else 0
                EstimatorActionData(
                    clipboardLabel = "Estimasi Keramik",
                    clipboardText = "[Kalkulator Bahan POS] ${preset.fullLabel} • Waste $wastePercent%\nLuas: ${String.format(Locale.US, "%.2f", areaNet)} m²\nEstimasi: $boxes Dus",
                    searchQuery = preset.searchQuery
                )
            }
            1 -> {
                val l = wallLengthInput.parseDecimalInput()
                val h = wallHeightInput.parseDecimalInput()
                val area = l * h
                val sacks = if (area > 0) (if (wallWorkType == 0) ceil(area * 0.20).toInt() else ceil(area * 0.23).toInt()) else 0
                val label = if (wallWorkType == 0) "Semen 50kg (Plesteran 1 sisi)" else "Semen 50kg (Pasang Bata 1:5)"
                EstimatorActionData(
                    clipboardLabel = "Estimasi Semen & Pasir",
                    clipboardText = "[Kalkulator Bahan POS] $label\nLuas Bidang: ${String.format(Locale.US, "%.1f", area)} m²\nEstimasi: $sacks Sak",
                    searchQuery = "Semen"
                )
            }
            else -> {
                val area = paintAreaInput.parseDecimalInput()
                val totalSweep = area * coatsCount
                val liters = if (totalSweep > 0) totalSweep / 10.0 else 0.0
                val gallons = if (liters > 0) ceil(liters / 2.5).toInt() else 0
                EstimatorActionData(
                    clipboardLabel = "Estimasi Cat Dinding",
                    clipboardText = "[Kalkulator Bahan POS] Kemasan Galon 2.5 Liter ($coatsCount Lapis)\nTotal Sapuan: ${formatNum(totalSweep)} m²\nEstimasi: $gallons Galon",
                    searchQuery = "Cat Tembok"
                )
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Slate300, CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .testTag("material-estimator-sheet")
        ) {
            // ── 1. MODAL HEADER ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Kalkulator Bahan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Estimasi kebutuhan keramik, semen, dan cat",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Slate100)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup modal",
                        tint = Slate500,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // ── 2. CLEAN UNDERLINE TAB NAVIGATION ──
            EstimatorUnderlineTabs(
                tabs = listOf("Keramik", "Semen & Pasir", "Cat Dinding"),
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it }
            )

            // ── 3. SCROLLABLE VIEWPORT ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> TileEstimatorTab(
                        lengthInput = tileLengthInput,
                        widthInput = tileWidthInput,
                        onLengthChange = { tileLengthInput = sanitizeDecimalInput(it) },
                        onWidthChange = { tileWidthInput = sanitizeDecimalInput(it) },
                        presets = tilePresets,
                        selectedPresetIndex = selectedTilePreset,
                        onSelectPreset = { selectedTilePreset = it },
                        wastePercent = wastePercent,
                        onSelectWaste = { wastePercent = it }
                    )
                    1 -> PlasterEstimatorTab(
                        lengthInput = wallLengthInput,
                        heightInput = wallHeightInput,
                        onLengthChange = { wallLengthInput = sanitizeDecimalInput(it) },
                        onHeightChange = { wallHeightInput = sanitizeDecimalInput(it) },
                        workType = wallWorkType,
                        onSelectWorkType = { wallWorkType = it }
                    )
                    else -> PaintEstimatorTab(
                        areaInput = paintAreaInput,
                        onAreaChange = { paintAreaInput = sanitizeDecimalInput(it) },
                        coatsCount = coatsCount,
                        onSelectCoats = { coatsCount = it }
                    )
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // ── 4. BOTTOM ACTION BUTTONS (5 : 7 GRID PROPORTION) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(activeActionData.clipboardLabel, activeActionData.clipboardText)
                        )
                        Toast.makeText(context, "Hasil kalkulasi berhasil disalin!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(5f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Slate200),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        tint = Slate500,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Salin Hasil",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = {
                        onDismiss()
                        onApplySearchQuery(activeActionData.searchQuery)
                    },
                    modifier = Modifier
                        .weight(7f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = BrandMintIcon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Cari di POS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// UNDERLINE TAB & SHARED PRIMITIVES
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun EstimatorUnderlineTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedIndex == index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .clickable { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) BrandPrimary else Slate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 10.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (isSelected) BrandPrimary else Color.Transparent)
                    )
                }
            }
        }
        HorizontalDivider(color = Slate200, thickness = 1.dp)
    }
}

@Composable
private fun EstimatorSectionHeader(
    title: String,
    trailingText: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Slate700,
            letterSpacing = 0.4.sp
        )
        if (!trailingText.isNullOrBlank()) {
            Text(
                text = trailingText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Slate500
            )
        }
    }
}

@Composable
private fun EstimatorCompactField(
    value: String,
    placeholder: String,
    unit: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(
            width = if (isFocused) 1.5.dp else 1.dp,
            color = if (isFocused) BrandPrimary else Slate200
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate400
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    cursorBrush = SolidColor(BrandPrimary),
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate900
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = unit,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Slate400
            )
        }
    }
}

@Composable
private fun EstimatorResultCard(
    eyebrow: String,
    summarySubtitle: String,
    heroValue: String,
    metrics: List<Pair<String, String>>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Slate50.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, Slate200.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = eyebrow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = summarySubtitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = heroValue,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900,
                    letterSpacing = (-0.5).sp
                )
            }

            HorizontalDivider(color = Slate200, thickness = 1.dp)

            // 3-Column Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                metrics.forEach { (label, valText) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            color = Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = valText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate800,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// TAB 1: KERAMIK
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun TileEstimatorTab(
    lengthInput: String,
    widthInput: String,
    onLengthChange: (String) -> Unit,
    onWidthChange: (String) -> Unit,
    presets: List<TilePreset>,
    selectedPresetIndex: Int,
    onSelectPreset: (Int) -> Unit,
    wastePercent: Int,
    onSelectWaste: (Int) -> Unit
) {
    val length = lengthInput.parseDecimalInput()
    val width = widthInput.parseDecimalInput()
    val cleanArea = length * width
    val totalWithWaste = cleanArea * (1.0 + wastePercent / 100.0)
    val selectedPreset = presets[selectedPresetIndex]
    val dusNeeded = if (selectedPreset.coveragePerBox > 0 && totalWithWaste > 0) {
        ceil(totalWithWaste / selectedPreset.coveragePerBox).toInt()
    } else 0

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Ukuran Ruangan
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            EstimatorSectionHeader(
                title = "UKURAN RUANGAN",
                trailingText = "Luas: ${formatNum(cleanArea)} m²"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EstimatorCompactField(
                    value = lengthInput,
                    placeholder = "Panjang",
                    unit = "m",
                    onValueChange = onLengthChange,
                    modifier = Modifier.weight(1f)
                )
                EstimatorCompactField(
                    value = widthInput,
                    placeholder = "Lebar",
                    unit = "m",
                    onValueChange = onWidthChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Pilihan Ukuran Keramik (4-kolom)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            EstimatorSectionHeader(title = "UKURAN KERAMIK / GRANIT")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEachIndexed { index, preset ->
                    val isSelected = selectedPresetIndex == index
                    Surface(
                        onClick = { onSelectPreset(index) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) BrandPrimary else Slate200
                        ),
                        shadowElevation = if (isSelected) 0.5.dp else 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = preset.shortLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) BrandPrimary else Slate800,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = preset.coverageLabel,
                                fontSize = 10.sp,
                                color = Slate400,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Pilihan Cadangan / Waste (3-kolom)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            EstimatorSectionHeader(title = "CADANGAN POTONGAN (WASTE)")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    0 to "0% (Pas)",
                    5 to "5% (Standar)",
                    10 to "10% (Sudut)"
                ).forEach { (pct, label) ->
                    val isSelected = wastePercent == pct
                    Surface(
                        onClick = { onSelectWaste(pct) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) BrandPrimary else Slate200
                        ),
                        shadowElevation = if (isSelected) 0.5.dp else 0.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) BrandPrimary else Slate700,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Card Hasil Estimasi
        EstimatorResultCard(
            eyebrow = "HASIL ESTIMASI",
            summarySubtitle = "${selectedPreset.fullLabel} • Waste $wastePercent%",
            heroValue = "$dusNeeded Dus",
            metrics = listOf(
                "Luas Bersih" to "${String.format(Locale.US, "%.2f", cleanArea)} m²",
                "Total + Waste" to "${String.format(Locale.US, "%.2f", totalWithWaste)} m²",
                "Isi / Dus" to "${String.format(Locale.US, "%.2f", selectedPreset.coveragePerBox)} m²"
            )
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// TAB 2: SEMEN & PASIR
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun PlasterEstimatorTab(
    lengthInput: String,
    heightInput: String,
    onLengthChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    workType: Int,
    onSelectWorkType: (Int) -> Unit
) {
    val length = lengthInput.parseDecimalInput()
    val height = heightInput.parseDecimalInput()
    val wallArea = length * height

    val sakNeeded = if (wallArea > 0) {
        if (workType == 0) ceil(wallArea * 0.20).toInt() else ceil(wallArea * 0.23).toInt()
    } else 0
    val sandM3 = if (workType == 0) wallArea * 0.024 else wallArea * 0.043
    val bricksNeeded = if (workType == 1 && wallArea > 0) ceil(wallArea * 70).toInt() else 0

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Dimensi Dinding
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            EstimatorSectionHeader(
                title = "DIMENSI DINDING",
                trailingText = "Luas: ${formatNum(wallArea)} m²"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EstimatorCompactField(
                    value = lengthInput,
                    placeholder = "Panjang",
                    unit = "m",
                    onValueChange = onLengthChange,
                    modifier = Modifier.weight(1f)
                )
                EstimatorCompactField(
                    value = heightInput,
                    placeholder = "Tinggi",
                    unit = "m",
                    onValueChange = onHeightChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Jenis Pengerjaan
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            EstimatorSectionHeader(title = "JENIS PENGERJAAN")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(
                    Triple(0, "Plesteran", "Tebal ±15 mm"),
                    Triple(1, "Pasang Bata", "Bata merah 1:5")
                ).forEach { (idx, title, subtitle) ->
                    val isSelected = workType == idx
                    Surface(
                        onClick = { onSelectWorkType(idx) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) BrandPrimary else Slate200
                        ),
                        shadowElevation = if (isSelected) 0.5.dp else 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) BrandPrimary else Slate800
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                    }
                }
            }
        }

        // Card Hasil Estimasi
        EstimatorResultCard(
            eyebrow = "ESTIMASI KEBUTUHAN",
            summarySubtitle = if (workType == 0) "Semen 50kg (Plesteran 1 sisi)" else "Semen 50kg (Pasang Bata 1:5)",
            heroValue = "$sakNeeded Sak",
            metrics = listOf(
                "Pasir Pasang" to "${String.format(Locale.US, "%.2f", sandM3)} m³",
                (if (workType == 0) "Acian / Plamir" else "Kebutuhan Bata") to (if (workType == 0) "Opsional" else "$bricksNeeded Bata"),
                "Luas Bidang" to "${String.format(Locale.US, "%.1f", wallArea)} m²"
            )
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// TAB 3: CAT DINDING
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun PaintEstimatorTab(
    areaInput: String,
    onAreaChange: (String) -> Unit,
    coatsCount: Int,
    onSelectCoats: (Int) -> Unit
) {
    val area = areaInput.parseDecimalInput()
    val totalSapuan = area * coatsCount
    val totalLiter = if (totalSapuan > 0) totalSapuan / 10.0 else 0.0
    val galonNeeded = if (totalLiter > 0) ceil(totalLiter / 2.5).toInt() else 0
    val pailNeeded = if (totalLiter > 0) ceil(totalLiter / 20.0).toInt() else 0

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Luas Permukaan Cat & Quick Presets
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                EstimatorSectionHeader(
                    title = "LUAS PERMUKAAN CAT",
                    trailingText = "Daya sebar ~10 m²/L"
                )
                EstimatorCompactField(
                    value = areaInput,
                    placeholder = "Luas dinding",
                    unit = "m²",
                    onValueChange = onAreaChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("20", "50", "100", "150").forEach { preset ->
                    val isSelected = areaInput.trim() == preset
                    Surface(
                        onClick = { onAreaChange(preset) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) BrandPrimary else Slate200
                        ),
                        shadowElevation = if (isSelected) 0.5.dp else 0.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$preset m²",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) BrandPrimary else Slate700
                            )
                        }
                    }
                }
            }
        }

        // Jumlah Lapisan
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            EstimatorSectionHeader(title = "JUMLAH LAPISAN")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(1, "1 Lapis", "Refresh"),
                    Triple(2, "2 Lapis", "Standar"),
                    Triple(3, "3 Lapis", "Warna Gelap")
                ).forEach { (layers, title, subtitle) ->
                    val isSelected = coatsCount == layers
                    Surface(
                        onClick = { onSelectCoats(layers) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) BrandPrimary else Slate200
                        ),
                        shadowElevation = if (isSelected) 0.5.dp else 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) BrandPrimary else Slate800,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                fontSize = 10.sp,
                                color = if (isSelected) Slate500 else Slate400,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Card Hasil Estimasi
        EstimatorResultCard(
            eyebrow = "HASIL ESTIMASI",
            summarySubtitle = "Kemasan Galon 2.5 Liter ($coatsCount Lapis)",
            heroValue = "$galonNeeded Galon",
            metrics = listOf(
                "Total Cairan" to "${String.format(Locale.US, "%.1f", totalLiter)} Liter",
                "Atau Pail 20L" to "$pailNeeded Pail",
                "Total Sapuan" to "${formatNum(totalSapuan)} m²"
            )
        )
    }
}

private fun String.parseDecimalInput(): Double {
    return trim().replace(',', '.').toDoubleOrNull() ?: 0.0
}

private fun sanitizeDecimalInput(raw: String): String {
    val filtered = raw.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
    val firstDot = filtered.indexOf('.')
    val singleDot = if (firstDot >= 0) {
        filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
    } else filtered
    return singleDot.take(8)
}

private fun formatNum(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}

private data class TilePreset(
    val shortLabel: String,
    val fullLabel: String,
    val coverageLabel: String,
    val coveragePerBox: Double,
    val searchQuery: String
)

private data class EstimatorActionData(
    val clipboardLabel: String,
    val clipboardText: String,
    val searchQuery: String
)

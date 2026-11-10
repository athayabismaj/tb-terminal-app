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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Layers
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
import androidx.compose.ui.graphics.vector.ImageVector
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
// PALET WARNA MODERN TB TERMINAL
// ═════════════════════════════════════════════════════════════════════════════
private val BrandEmerald = Color(0xFF256B57)
private val BrandForest = Color(0xFF10533C)
private val BrandMintBg = Color(0xFFECFDF5)
private val BrandMintBorder = Color(0xFFA7F3D0)

private val Slate900 = Color(0xFF0F172A)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)

/**
 * Kalkulator Cepat Estimasi Kebutuhan Material Bangunan di Layar POS & Dashboard Kasir.
 * Didesain modern dan selaras dengan sistem desain Bottom Sheet TB Terminal:
 * 1. Estimasi Keramik & Granit (Panjang x Lebar -> Dus + Cadangan Waste)
 * 2. Estimasi Plesteran & Pasangan Bata (Panjang x Tinggi -> Sak Semen, Pasir, Bata)
 * 3. Estimasi Cat Dinding (Luas Bidang x Lapisan -> Liter, Galon 2.5L, Pail 20L)
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

    // State Tab 1: Keramik & Granit
    var tileLengthInput by remember { mutableStateOf("4") }
    var tileWidthInput by remember { mutableStateOf("5") }
    var selectedTilePreset by remember { mutableIntStateOf(2) } // 60x60 cm
    var wastePercent by remember { mutableIntStateOf(10) }

    // State Tab 2: Semen & Bata
    var wallLengthInput by remember { mutableStateOf("10") }
    var wallHeightInput by remember { mutableStateOf("3") }
    var wallWorkType by remember { mutableIntStateOf(0) } // 0: Plester, 1: Bata

    // State Tab 3: Cat Dinding
    var paintAreaInput by remember { mutableStateOf("50") }
    var coatsCount by remember { mutableIntStateOf(2) }

    val tilePresets = remember {
        listOf(
            TilePreset("40×40 cm", "1.0 m²/dus", 1.0, "Keramik 40x40"),
            TilePreset("50×50 cm", "1.0 m²/dus", 1.0, "Keramik 50x50"),
            TilePreset("60×60 cm", "1.44 m²/dus", 1.44, "Granit 60x60"),
            TilePreset("80×80 cm", "1.92 m²/dus", 1.92, "Granit 80x80"),
            TilePreset("30×60 cm", "1.44 m²/dus", 1.44, "Keramik 30x60"),
            TilePreset("25×40 cm", "1.0 m²/dus", 1.0, "Keramik Dinding 25x40"),
        )
    }

    // Active output metadata for pinned footer actions
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
                    clipboardText = "Estimasi Keramik (${preset.label}): Ruangan ${formatNum(l)}×${formatNum(w)}m (${formatNum(areaNet)} m², waste $wastePercent%) membutuhkan $boxes Dus.",
                    searchQuery = preset.searchQuery
                )
            }
            1 -> {
                val l = wallLengthInput.parseDecimalInput()
                val h = wallHeightInput.parseDecimalInput()
                val area = l * h
                val sacks = if (area > 0) (if (wallWorkType == 0) ceil(area * 0.20).toInt() else ceil(area * 0.23).toInt()) else 0
                val sand = if (wallWorkType == 0) area * 0.024 else area * 0.043
                val bricks = if (wallWorkType == 1 && area > 0) ceil(area * 70).toInt() else 0
                val workLabel = if (wallWorkType == 0) "Plesteran 15mm" else "Pasang Bata Merah"
                val extra = if (wallWorkType == 1) ", $bricks PCS Bata Merah" else ""
                EstimatorActionData(
                    clipboardLabel = "Estimasi Dinding",
                    clipboardText = "Estimasi $workLabel (${formatNum(l)}×${formatNum(h)}m = ${formatNum(area)} m²): $sacks Sak Semen (50kg), ${String.format(Locale.US, "%.2f", sand)} m³ Pasir$extra.",
                    searchQuery = "Semen"
                )
            }
            else -> {
                val area = paintAreaInput.parseDecimalInput()
                val totalSweep = area * coatsCount
                val liters = if (totalSweep > 0) totalSweep / 10.0 else 0.0
                val gallons = if (liters > 0) ceil(liters / 2.5).toInt() else 0
                val pails = if (liters > 0) ceil(liters / 20.0).toInt() else 0
                EstimatorActionData(
                    clipboardLabel = "Estimasi Cat Dinding",
                    clipboardText = "Estimasi Cat Dinding (${formatNum(area)} m², $coatsCount Lapis): ${String.format(Locale.US, "%.1f", liters)} Liter ($gallons Galon 2.5L atau $pails Pail 20L).",
                    searchQuery = "Cat Tembok"
                )
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(5.dp)
                    .background(Color(0xFFCBD5E1), CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .testTag("material-estimator-sheet")
        ) {
            // ── 1. HEADER ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Kalkulator Bahan",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Hitung cepat estimasi keramik, semen & cat dinding",
                        fontSize = 13.sp,
                        color = Slate500,
                        lineHeight = 17.sp
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
                        contentDescription = "Tutup Kalkulator",
                        tint = Slate600,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // ── 2. SEGMENTED PILL TABS ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 14.dp, bottom = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate100,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        EstimatorSegmentTab(
                            title = "Keramik",
                            icon = Icons.Outlined.GridView,
                            isSelected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        EstimatorSegmentTab(
                            title = "Semen & Bata",
                            icon = Icons.Outlined.Layers,
                            isSelected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.weight(1f)
                        )
                        EstimatorSegmentTab(
                            title = "Cat Dinding",
                            icon = Icons.Outlined.FormatPaint,
                            isSelected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── 3. SCROLLABLE BODY ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                when (selectedTab) {
                    0 -> TileEstimatorBody(
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
                    1 -> PlasterEstimatorBody(
                        lengthInput = wallLengthInput,
                        heightInput = wallHeightInput,
                        onLengthChange = { wallLengthInput = sanitizeDecimalInput(it) },
                        onHeightChange = { wallHeightInput = sanitizeDecimalInput(it) },
                        workType = wallWorkType,
                        onSelectWorkType = { wallWorkType = it }
                    )
                    2 -> PaintEstimatorBody(
                        areaInput = paintAreaInput,
                        onAreaChange = { paintAreaInput = sanitizeDecimalInput(it) },
                        coatsCount = coatsCount,
                        onSelectCoats = { coatsCount = it }
                    )
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // ── 4. STICKY ACTION FOOTER ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(activeActionData.clipboardLabel, activeActionData.clipboardText))
                        Toast.makeText(context, "Hasil estimasi disalin ke clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Slate200),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700)
                ) {
                    Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Salin Hasil",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = {
                        onDismiss()
                        onApplySearchQuery(activeActionData.searchQuery)
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandEmerald,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Icon(Icons.Outlined.Search, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Cari di POS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// KOMPONEN REUSABLE MODERN
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun EstimatorSegmentTab(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(9.dp),
        color = if (isSelected) Color.White else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, Slate200.copy(alpha = 0.8f)) else null,
        shadowElevation = if (isSelected) 0.5.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) BrandEmerald else Slate500,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Slate900 else Slate500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EstimatorSectionHeader(
    title: String,
    trailingBadge: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Slate500,
            letterSpacing = 0.8.sp
        )
        if (!trailingBadge.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = BrandMintBg,
                border = BorderStroke(1.dp, BrandMintBorder)
            ) {
                Text(
                    text = trailingBadge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandForest,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun EstimatorNumberField(
    label: String,
    value: String,
    unit: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Slate600
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent,
            border = BorderStroke(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) BrandEmerald else Slate200
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 14.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = "0",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        cursorBrush = SolidColor(BrandEmerald),
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate900
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused }
                    )
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Slate100
                ) {
                    Text(
                        text = unit,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate600,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EstimatorOptionCard(
    title: String,
    subtitle: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    centered: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(if (subtitle != null) 50.dp else 44.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) BrandEmerald else Slate200
        ),
        shadowElevation = 0.dp
    ) {
        if (centered) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) BrandEmerald else Slate700,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isSelected) BrandEmerald.copy(alpha = 0.85f) else Slate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 13.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) BrandEmerald else Slate700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = BrandEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EstimatorResultCard(
    heroSubtitle: String,
    heroValue: String,
    breakdownItems: List<Pair<String, String>>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Slate50,
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Hero Row
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
                        text = "ESTIMASI KEBUTUHAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = heroSubtitle,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BrandMintBg,
                    border = BorderStroke(1.dp, BrandMintBorder)
                ) {
                    Text(
                        text = heroValue,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandForest,
                        letterSpacing = (-0.3).sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            HorizontalDivider(color = Slate200.copy(alpha = 0.8f), thickness = 1.dp)

            // Bottom Breakdown Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                breakdownItems.forEach { (label, valText) ->
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
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
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
// TAB 1: KERAMIK & GRANIT
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun TileEstimatorBody(
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
    val areaNet = length * width
    val areaGross = areaNet * (1.0 + wastePercent / 100.0)
    val selectedPreset = presets[selectedPresetIndex]
    val boxesNeeded = if (selectedPreset.coveragePerBox > 0 && areaGross > 0) {
        ceil(areaGross / selectedPreset.coveragePerBox).toInt()
    } else 0

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // Section 1: Dimensi Ruangan
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EstimatorSectionHeader(
                title = "UKURAN RUANGAN",
                trailingBadge = "Luas: ${formatNum(areaNet)} m²"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EstimatorNumberField(
                    label = "Panjang",
                    value = lengthInput,
                    unit = "m",
                    onValueChange = onLengthChange,
                    modifier = Modifier.weight(1f)
                )
                EstimatorNumberField(
                    label = "Lebar",
                    value = widthInput,
                    unit = "m",
                    onValueChange = onWidthChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Section 2: Ukuran Ubin (3x2 Symmetric Grid)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EstimatorSectionHeader(title = "UKURAN KERAMIK / GRANIT")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.chunked(3).forEachIndexed { rowIndex, rowPresets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowPresets.forEachIndexed { colIndex, preset ->
                            val index = rowIndex * 3 + colIndex
                            EstimatorOptionCard(
                                title = preset.label,
                                subtitle = preset.coverageLabel,
                                isSelected = selectedPresetIndex == index,
                                onClick = { onSelectPreset(index) },
                                centered = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Cadangan Potongan (Waste)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EstimatorSectionHeader(title = "CADANGAN POTONGAN (WASTE)")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    0 to "0% Pas",
                    5 to "5% Standar",
                    10 to "10% Sudut"
                ).forEach { (pct, label) ->
                    EstimatorOptionCard(
                        title = label,
                        isSelected = wastePercent == pct,
                        onClick = { onSelectWaste(pct) },
                        centered = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Hasil Estimasi
        EstimatorResultCard(
            heroSubtitle = "${selectedPreset.label} • Waste $wastePercent%",
            heroValue = "$boxesNeeded Dus",
            breakdownItems = listOf(
                "Luas Bersih" to "${String.format(Locale.US, "%.2f", areaNet)} m²",
                "Total + Waste" to "${String.format(Locale.US, "%.2f", areaGross)} m²",
                "Cakupan/Dus" to selectedPreset.coverageLabel.replace("/dus", "")
            )
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// TAB 2: SEMEN & BATA
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun PlasterEstimatorBody(
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

    val cementSacks = if (wallArea > 0) {
        if (workType == 0) ceil(wallArea * 0.20).toInt() else ceil(wallArea * 0.23).toInt()
    } else 0
    val sandM3 = if (workType == 0) wallArea * 0.024 else wallArea * 0.043
    val bricksCount = if (workType == 1 && wallArea > 0) ceil(wallArea * 70).toInt() else 0

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // Section 1: Dimensi Dinding
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EstimatorSectionHeader(
                title = "DIMENSI DINDING",
                trailingBadge = "Luas: ${formatNum(wallArea)} m²"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EstimatorNumberField(
                    label = "Panjang",
                    value = lengthInput,
                    unit = "m",
                    onValueChange = onLengthChange,
                    modifier = Modifier.weight(1f)
                )
                EstimatorNumberField(
                    label = "Tinggi",
                    value = heightInput,
                    unit = "m",
                    onValueChange = onHeightChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Section 2: Jenis Pengerjaan
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EstimatorSectionHeader(title = "JENIS PENGERJAAN")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EstimatorOptionCard(
                    title = "Plesteran",
                    subtitle = "Tebal ±15 mm",
                    isSelected = workType == 0,
                    onClick = { onSelectWorkType(0) },
                    modifier = Modifier.weight(1f)
                )
                EstimatorOptionCard(
                    title = "Pasang Bata",
                    subtitle = "Bata merah 1:5",
                    isSelected = workType == 1,
                    onClick = { onSelectWorkType(1) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Hasil Estimasi
        val breakdown = buildList<Pair<String, String>> {
            add("Pasir Pasang" to "${String.format(Locale.US, "%.2f", sandM3)} m³")
            if (workType == 1) {
                add("Bata Merah" to "$bricksCount Pcs")
            } else {
                add("Acian / Plamir" to "Opsional")
            }
            add("Luas Bidang" to "${String.format(Locale.US, "%.1f", wallArea)} m²")
        }

        EstimatorResultCard(
            heroSubtitle = if (workType == 0) "Semen 50kg (Plesteran 1 sisi)" else "Semen 50kg (Pasangan Bata)",
            heroValue = "$cementSacks Sak",
            breakdownItems = breakdown
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// TAB 3: CAT DINDING
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun PaintEstimatorBody(
    areaInput: String,
    onAreaChange: (String) -> Unit,
    coatsCount: Int,
    onSelectCoats: (Int) -> Unit
) {
    val area = areaInput.parseDecimalInput()
    val totalCoverageArea = area * coatsCount
    val litersNeeded = if (totalCoverageArea > 0) totalCoverageArea / 10.0 else 0.0
    val gallonsNeeded = if (litersNeeded > 0) ceil(litersNeeded / 2.5).toInt() else 0
    val pailsNeeded = if (litersNeeded > 0) ceil(litersNeeded / 20.0).toInt() else 0

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // Section 1: Luas Permukaan + Quick Area Presets
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EstimatorSectionHeader(
                title = "LUAS PERMUKAAN CAT",
                trailingBadge = "Daya sebar ~10 m²/L"
            )
            EstimatorNumberField(
                label = "Total Luas Dinding",
                value = areaInput,
                unit = "m²",
                onValueChange = onAreaChange,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("20", "50", "100", "150").forEach { presetArea ->
                    val isSelected = areaInput.trim() == presetArea
                    Surface(
                        onClick = { onAreaChange(presetArea) },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) BrandMintBg else Color.Transparent,
                        border = BorderStroke(1.dp, if (isSelected) BrandEmerald else Slate200)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$presetArea m²",
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) BrandEmerald else Slate600
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Jumlah Lapisan
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EstimatorSectionHeader(title = "JUMLAH LAPISAN CAT")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(1, "1 Lapis", "Refresh"),
                    Triple(2, "2 Lapis", "Standar"),
                    Triple(3, "3 Lapis", "Warna Gelap")
                ).forEach { (coats, title, sub) ->
                    EstimatorOptionCard(
                        title = title,
                        subtitle = sub,
                        isSelected = coatsCount == coats,
                        onClick = { onSelectCoats(coats) },
                        centered = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Hasil Estimasi
        EstimatorResultCard(
            heroSubtitle = "Kemasan Galon 2.5 Liter ($coatsCount Lapis)",
            heroValue = "$gallonsNeeded Galon",
            breakdownItems = listOf(
                "Total Cairan" to "${String.format(Locale.US, "%.1f", litersNeeded)} Liter",
                "Atau Pail 20L" to "$pailsNeeded Pail",
                "Total Sapuan" to "${String.format(Locale.US, "%.0f", totalCoverageArea)} m²"
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
    val label: String,
    val coverageLabel: String,
    val coveragePerBox: Double,
    val searchQuery: String
)

private data class EstimatorActionData(
    val clipboardLabel: String,
    val clipboardText: String,
    val searchQuery: String
)

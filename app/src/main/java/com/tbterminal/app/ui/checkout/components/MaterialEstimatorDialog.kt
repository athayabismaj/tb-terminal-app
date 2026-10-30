package com.tbterminal.app.ui.checkout.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.ceil

/**
 * Kalkulator Cepat Estimasi Kebutuhan Material Bangunan di Layar POS Kasir.
 * Membantu kasir menjawab pertanyaan tukang/pelanggan secara cepat:
 * 1. Estimasi Keramik / Lantai (Panjang x Lebar -> Dus + Waste 5-10%)
 * 2. Estimasi Plesteran Dinding & Pasangan Bata (m² -> Sak Semen & Pasir)
 * 3. Estimasi Cat Tembok (m² & Lapis -> Liter, Galon, Pail)
 */
@Composable
fun MaterialEstimatorDialog(
    onDismiss: () -> Unit,
    onApplySearchQuery: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Keramik & Granit", "Plester & Bata", "Cat Dinding")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Calculate,
                            contentDescription = null,
                            tint = Color(0xFF256B57),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Kalkulator Bahan Bangunan",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Tutup",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                // Tab Switcher
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFE2E8F0),
                    contentColor = Color(0xFF0F172A),
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) Color(0xFF256B57) else Color(0xFF64748B)
                                )
                            }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> TileEstimatorTab(context, onDismiss, onApplySearchQuery)
                    1 -> PlasterEstimatorTab(context, onDismiss, onApplySearchQuery)
                    2 -> PaintEstimatorTab(context, onDismiss, onApplySearchQuery)
                }
            }
        }
    }
}

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

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("1. Ukuran Ruangan (Panjang x Lebar)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = lengthInput,
                onValueChange = { lengthInput = it },
                label = { Text("Panjang (m)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )
            OutlinedTextField(
                value = widthInput,
                onValueChange = { widthInput = it },
                label = { Text("Lebar (m)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )
        }

        Text("2. Ukuran Ubin / Keramik", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tileSizes.take(3).forEachIndexed { index, preset ->
                val isSelected = selectedPresetIndex == index
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedPresetIndex = index },
                    label = { Text(preset.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
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
                val isSelected = selectedPresetIndex == index
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedPresetIndex = index },
                    label = { Text(preset.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text("3. Cadangan Potongan (Waste)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(5 to "5% (Standar)", 10 to "10% (Pola/Banyak Sudut)", 0 to "0% (Pas)").forEach { (pct, label) ->
                val isSelected = wastePercent == pct
                FilterChip(
                    selected = isSelected,
                    onClick = { wastePercent = pct },
                    label = { Text(label, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HorizontalDivider(color = Color(0xFFCBD5E1), modifier = Modifier.padding(vertical = 4.dp))

        // Hasil Perhitungan
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Luas Ruangan Bersih", fontSize = 12.sp, color = Color(0xFF047857))
                    Text(String.format(java.util.Locale.US, "%.2f m²", areaNet), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Luas + Cadangan ($wastePercent%)", fontSize = 12.sp, color = Color(0xFF047857))
                    Text(String.format(java.util.Locale.US, "%.2f m²", areaGross), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                }
                HorizontalDivider(color = Color(0xFFA7F3D0), modifier = Modifier.padding(vertical = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Kebutuhan Keramik", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                    Text(
                        text = "$boxesNeeded DUS",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF047857)
                    )
                }
            }
        }

        // Tombol Aksi
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    val summary = "Estimasi Keramik: Ruangan ${length}x${width}m (${String.format(java.util.Locale.US, "%.2f", areaNet)}m²) butuh $boxesNeeded Dus (${tileSizes[selectedPresetIndex].label})."
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Estimasi Keramik", summary))
                    Toast.makeText(context, "Hasil estimasi disalin ke clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Salin Hasil", fontSize = 12.sp)
            }
            Button(
                onClick = {
                    val query = tileSizes[selectedPresetIndex].searchQuery
                    onDismiss()
                    onApplySearchQuery(query)
                },
                modifier = Modifier.weight(1.3f).height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF256B57))
            ) {
                Icon(Icons.Outlined.Search, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Cari di Katalog", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

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

    // Standar SNI:
    // Plesteran 1 sisi tebal 15mm: ~0.20 sak semen (50kg) & ~0.024 m³ pasir per m²
    // Pasangan Bata Merah (1:4): ~0.23 sak semen (50kg), ~0.043 m³ pasir, ~70 bata per m²
    val cementSacks = if (workType == 0) ceil(wallArea * 0.20).toInt() else ceil(wallArea * 0.23).toInt()
    val sandM3 = if (workType == 0) wallArea * 0.024 else wallArea * 0.043
    val bricksCount = if (workType == 1) ceil(wallArea * 70).toInt() else 0

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("1. Dimensi Dinding (Panjang x Tinggi)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = wallLengthInput,
                onValueChange = { wallLengthInput = it },
                label = { Text("Panjang Dinding (m)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )
            OutlinedTextField(
                value = wallHeightInput,
                onValueChange = { wallHeightInput = it },
                label = { Text("Tinggi Dinding (m)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )
        }

        Text("2. Jenis Pengerjaan", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = workType == 0,
                onClick = { workType = 0 },
                label = { Text("Plesteran (Tebal 15mm)", fontSize = 11.sp) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = workType == 1,
                onClick = { workType = 1 },
                label = { Text("Pasang Bata Merah", fontSize = 11.sp) },
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = Color(0xFFCBD5E1), modifier = Modifier.padding(vertical = 4.dp))

        // Hasil Perhitungan
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Luas Bidang Dinding", fontSize = 12.sp, color = Color(0xFF92400E))
                    Text(String.format(java.util.Locale.US, "%.2f m²", wallArea), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                }
                HorizontalDivider(color = Color(0xFFFDE68A), modifier = Modifier.padding(vertical = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Kebutuhan Semen (50kg)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F))
                    Text("$cementSacks SAK", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB45309))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Kebutuhan Pasir Pasang", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F))
                    Text(String.format(java.util.Locale.US, "%.2f m³", sandM3), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                }
                if (workType == 1) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Kebutuhan Bata Merah", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F))
                        Text("$bricksCount PCS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                    }
                }
            }
        }

        // Tombol Aksi
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    val summary = "Estimasi Dinding ${length}x${height}m (${String.format(java.util.Locale.US, "%.2f", wallArea)}m²): Butuh $cementSacks Sak Semen, ${String.format(java.util.Locale.US, "%.2f", sandM3)}m³ Pasir."
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Estimasi Plesteran", summary))
                    Toast.makeText(context, "Hasil estimasi disalin ke clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Salin Hasil", fontSize = 12.sp)
            }
            Button(
                onClick = {
                    onDismiss()
                    onApplySearchQuery("Semen")
                },
                modifier = Modifier.weight(1.3f).height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309))
            ) {
                Icon(Icons.Outlined.Search, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Cari Semen di POS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

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
    // Daya sebar standar cat tembok: ~10 m² per liter per lapis
    val litersNeeded = if (totalCoverageArea > 0) totalCoverageArea / 10.0 else 0.0
    // Ukuran kemasan umum: Galon (2.5 Liter / 5 Kg), Pail (20 Liter / 25 Kg)
    val gallonsNeeded = ceil(litersNeeded / 2.5).toInt()
    val pailsNeeded = ceil(litersNeeded / 20.0).toInt()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("1. Luas Permukaan yang Dicat", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
        OutlinedTextField(
            value = paintAreaInput,
            onValueChange = { paintAreaInput = it },
            label = { Text("Total Luas Bidang Dinding (m²)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Text("2. Jumlah Lapisan Cat", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(1 to "1 Lapis", 2 to "2 Lapis (Standar)", 3 to "3 Lapis (Warna Gelap)").forEach { (coats, label) ->
                val isSelected = coatsCount == coats
                FilterChip(
                    selected = isSelected,
                    onClick = { coatsCount = coats },
                    label = { Text(label, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HorizontalDivider(color = Color(0xFFCBD5E1), modifier = Modifier.padding(vertical = 4.dp))

        // Hasil Perhitungan Cat
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Luas Sapuan ($coatsCount Lapis)", fontSize = 12.sp, color = Color(0xFF1E40AF))
                    Text(String.format(java.util.Locale.US, "%.1f m²", totalCoverageArea), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Kebutuhan Cairan", fontSize = 12.sp, color = Color(0xFF1E40AF))
                    Text(String.format(java.util.Locale.US, "%.1f Liter", litersNeeded), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1D4ED8))
                }
                HorizontalDivider(color = Color(0xFFBFDBFE), modifier = Modifier.padding(vertical = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Kemasan Galon (2.5 Liter)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                    Text("$gallonsNeeded GALON", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1D4ED8))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Atau Kemasan Pail (20 Liter)", fontSize = 13.sp, color = Color(0xFF1E3A8A))
                    Text("$pailsNeeded PAIL", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                }
            }
        }

        // Tombol Aksi
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    val summary = "Estimasi Cat Dinding (${String.format(java.util.Locale.US, "%.1f", area)}m², $coatsCount Lapis): Butuh ${String.format(java.util.Locale.US, "%.1f", litersNeeded)} Liter (Setara $gallonsNeeded Galon 2.5L atau $pailsNeeded Pail 20L)."
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Estimasi Cat", summary))
                    Toast.makeText(context, "Hasil estimasi disalin ke clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Salin Hasil", fontSize = 12.sp)
            }
            Button(
                onClick = {
                    onDismiss()
                    onApplySearchQuery("Cat Tembok")
                },
                modifier = Modifier.weight(1.3f).height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8))
            ) {
                Icon(Icons.Outlined.Search, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Cari Cat di POS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class TilePreset(
    val label: String,
    val coveragePerBox: Double,
    val searchQuery: String
)

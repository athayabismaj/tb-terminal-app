package com.tbterminal.app.ui.salestransactions

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tbterminal.app.TbTerminalApplication
import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.receiptNumber
import com.tbterminal.app.data.printer.DeliveryOrderFormatter
import com.tbterminal.app.data.printer.PrinterResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.RoundingMode

/**
 * Dialog Pengisian & Cetak Surat Jalan (Delivery Order) Standar Industri
 * Khusus Ritel & Toko Bahan Bangunan.
 *
 * Menyediakan:
 * 1. Formulir Interaktif Pengisian Alamat, Armada, No Plat, Sopir, dan Catatan Lapangan.
 * 2. Pratinjau Langsung (Live Preview) slip Surat Jalan saat kasir mengetik.
 * 3. Tiga Aksi Eksekusi: Cetak PDF Resmi (A5/A4), Cetak Thermal (ESC/POS), dan Kirim WhatsApp.
 */
@Composable
fun DeliveryOrderDialog(
    transaction: CashTransactionDetail,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPrintingThermal by remember { mutableStateOf(false) }
    var showWhatsAppConfirm by remember { mutableStateOf(false) }

    // Parse info pengiriman awal dari catatan transaksi
    val initialParsed = remember(transaction.notes) {
        DeliveryOrderFormatter.parseDeliveryInfo(transaction.notes)
    }

    // Tab aktif: 0 = Formulir Pengisian, 1 = Pratinjau Surat Jalan
    var activeTab by remember {
        mutableStateOf(if (initialParsed.address.isNullOrBlank()) 0 else 1)
    }

    // State form pengisian yang dapat diedit langsung oleh kasir
    var recipientName by remember(transaction) {
        mutableStateOf(transaction.customerName ?: "")
    }
    var recipientPhone by remember(initialParsed) {
        mutableStateOf(initialParsed.recipientPhone ?: "")
    }
    var deliveryAddress by remember(initialParsed) {
        mutableStateOf(initialParsed.address ?: "")
    }
    var courierName by remember(initialParsed) {
        mutableStateOf(initialParsed.courier ?: "Pick-up Toko")
    }
    var vehiclePlate by remember(initialParsed) {
        mutableStateOf(initialParsed.vehiclePlate ?: "")
    }
    var deliveryFeeText by remember(initialParsed) {
        mutableStateOf(initialParsed.deliveryFeeText ?: "")
    }
    var specialInstructions by remember(initialParsed) {
        mutableStateOf(initialParsed.specialInstructions ?: "")
    }

    // Objek data terkini yang disinkronkan ke printer & WhatsApp
    val currentDeliveryData = remember(
        recipientName,
        recipientPhone,
        deliveryAddress,
        courierName,
        vehiclePlate,
        deliveryFeeText,
        specialInstructions
    ) {
        DeliveryOrderFormatter.DeliveryOrderData(
            recipientName = recipientName.trim(),
            recipientPhone = recipientPhone.trim(),
            address = deliveryAddress.trim(),
            courier = courierName.trim(),
            vehiclePlate = vehiclePlate.trim(),
            deliveryFeeText = deliveryFeeText.trim(),
            specialInstructions = specialInstructions.trim()
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Header Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFFE0F2FE), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocalShipping,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Surat Jalan & Pengiriman",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "No. Nota: ${transaction.receiptNumber()}",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFE2E8F0), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Tutup",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 2. Segmented Tab Switcher (Formulir vs Pratinjau)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Tab 0: Formulir Pengisian
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .background(
                                color = if (activeTab == 0) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { activeTab = 0 },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = if (activeTab == 0) Color(0xFF0284C7) else Color(0xFF64748B),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Isi / Edit Data",
                                fontSize = 12.sp,
                                fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (activeTab == 0) Color(0xFF0F172A) else Color(0xFF64748B)
                            )
                        }
                    }

                    // Tab 1: Pratinjau Surat Jalan
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .background(
                                color = if (activeTab == 1) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { activeTab = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Visibility,
                                contentDescription = null,
                                tint = if (activeTab == 1) Color(0xFF0284C7) else Color(0xFF64748B),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Pratinjau Surat Jalan",
                                fontSize = 12.sp,
                                fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (activeTab == 1) Color(0xFF0F172A) else Color(0xFF64748B)
                            )
                        }
                    }
                }

                // 3. Konten Utama (Scrollable)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (activeTab == 0) {
                        // TAB 0: FORMULIR PENGISIAN STANDAR INDUSTRI
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Bagian A: Penerima & Alamat
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "1. TUJUAN & PENERIMA BARANG",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0284C7)
                                    )

                                    OutlinedTextField(
                                        value = recipientName,
                                        onValueChange = { recipientName = it },
                                        label = { Text("Nama Penerima / Kontak Lapangan", fontSize = 12.sp) },
                                        placeholder = { Text("Contoh: Pak Mandor Budi") },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Person, null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                        },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF0284C7),
                                            unfocusedBorderColor = Color(0xFFCBD5E1)
                                        )
                                    )

                                    OutlinedTextField(
                                        value = recipientPhone,
                                        onValueChange = { recipientPhone = it },
                                        label = { Text("Nomor Telepon / WhatsApp", fontSize = 12.sp) },
                                        placeholder = { Text("Contoh: 081234567890") },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Phone, null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                        },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF0284C7),
                                            unfocusedBorderColor = Color(0xFFCBD5E1)
                                        )
                                    )

                                    OutlinedTextField(
                                        value = deliveryAddress,
                                        onValueChange = { deliveryAddress = it },
                                        label = { Text("Alamat Pengiriman Lengkap", fontSize = 12.sp) },
                                        placeholder = { Text("Jalan, RT/RW, Desa, Patokan proyek / dekat masjid...") },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.LocationOn, null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                        },
                                        minLines = 2,
                                        maxLines = 4,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF0284C7),
                                            unfocusedBorderColor = Color(0xFFCBD5E1)
                                        )
                                    )
                                }
                            }

                            // Bagian B: Armada & Ekspedisi
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "2. ARMADA PENGANGKUT & SOPIR",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0284C7)
                                    )

                                    // Quick Chips Armada
                                    Text("Pilihan Cepat Armada Toko:", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("Pick-up Toko", "Truk Engkel", "Motor Viar", "Ambil Sendiri").forEach { fleet ->
                                            val isSelected = courierName.contains(fleet, ignoreCase = true)
                                            Surface(
                                                color = if (isSelected) Color(0xFFE0F2FE) else Color(0xFFF1F5F9),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF0284C7) else Color(0xFFE2E8F0)),
                                                modifier = Modifier.clickable { courierName = fleet }
                                            ) {
                                                Text(
                                                    text = fleet,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) Color(0xFF0369A1) else Color(0xFF475569),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = courierName,
                                        onValueChange = { courierName = it },
                                        label = { Text("Armada / Jenis Kendaraan & Nama Sopir", fontSize = 12.sp) },
                                        placeholder = { Text("Contoh: Pick-up Gran Max - Pak Budi") },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.LocalShipping, null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                        },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF0284C7),
                                            unfocusedBorderColor = Color(0xFFCBD5E1)
                                        )
                                    )

                                    OutlinedTextField(
                                        value = vehiclePlate,
                                        onValueChange = { vehiclePlate = it },
                                        label = { Text("Nomor Polisi / Plat Kendaraan", fontSize = 12.sp) },
                                        placeholder = { Text("Contoh: L 1234 AB") },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Description, null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                        },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF0284C7),
                                            unfocusedBorderColor = Color(0xFFCBD5E1)
                                        )
                                    )
                                }
                            }

                            // Bagian C: Biaya & Catatan Lapangan
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "3. ONGKIR & INSTRUKSI LAPANGAN",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0284C7)
                                    )

                                    OutlinedTextField(
                                        value = deliveryFeeText,
                                        onValueChange = { deliveryFeeText = it },
                                        label = { Text("Biaya Pengiriman / Ongkir (Opsional)", fontSize = 12.sp) },
                                        placeholder = { Text("Contoh: Rp 50.000 / Gratis") },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Payments, null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                        },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF0284C7),
                                            unfocusedBorderColor = Color(0xFFCBD5E1)
                                        )
                                    )

                                    // Quick Chips Instruksi
                                    Text("Instruksi Cepat:", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("Hubungi sblm tiba", "Turunkan samping proyek", "Barang pecah belah").forEach { tip ->
                                            Surface(
                                                color = Color(0xFFF1F5F9),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                                modifier = Modifier.clickable {
                                                    specialInstructions = if (specialInstructions.isBlank()) tip else "$specialInstructions, $tip"
                                                }
                                            ) {
                                                Text(
                                                    text = "+ $tip",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF334155),
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = specialInstructions,
                                        onValueChange = { specialInstructions = it },
                                        label = { Text("Instruksi / Catatan Khusus Sopir & Bongkar", fontSize = 12.sp) },
                                        placeholder = { Text("Misal: Hubungi 30 menit sebelum sampai, hati-hati semen terkena hujan...") },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Edit, null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                        },
                                        minLines = 2,
                                        maxLines = 3,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF0284C7),
                                            unfocusedBorderColor = Color(0xFFCBD5E1)
                                        )
                                    )
                                }
                            }

                            // Tombol beralih ke preview
                            OutlinedButton(
                                onClick = { activeTab = 1 },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF0284C7))
                            ) {
                                Text("Lihat Pratinjau Surat Jalan →", color = Color(0xFF0284C7), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    } else {
                        // TAB 1: PRATINJAU SLIP SURAT JALAN (LIVE PREVIEW)
                        Card(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Header Surat Jalan
                                Text(
                                    text = "TB TERMINAL BAROKAH JAYA",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "*** SURAT JALAN / DELIVERY ORDER ***",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0284C7),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Bukti Resmi Serah Terima Barang Logistik",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )

                                HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

                                // Metadata Pengiriman
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("No. Surat Jalan", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text("SJ-${transaction.receiptNumber()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("No. Ref Faktur", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text(transaction.receiptNumber(), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Penerima", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text(
                                        text = currentDeliveryData.recipientName.ifBlank { "Pelanggan Umum" },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                                if (currentDeliveryData.recipientPhone.isNotBlank()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Telp / WA", fontSize = 11.sp, color = Color(0xFF64748B))
                                        Text(currentDeliveryData.recipientPhone, fontSize = 11.sp, color = Color(0xFF0F172A))
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Alamat Kirim", fontSize = 11.sp, color = Color(0xFF64748B), modifier = Modifier.width(85.dp))
                                    Text(
                                        text = currentDeliveryData.address.ifBlank { "Diambil di Toko / Belum diisi" },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color(0xFF0F172A),
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Armada / Supir", fontSize = 11.sp, color = Color(0xFF64748B))
                                    val courierDisplay = buildString {
                                        append(currentDeliveryData.courier.ifBlank { "Armada Toko" })
                                        if (currentDeliveryData.vehiclePlate.isNotBlank()) {
                                            append(" (${currentDeliveryData.vehiclePlate})")
                                        }
                                    }
                                    Text(courierDisplay, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                }

                                HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

                                // Tabel Daftar Barang
                                Text(
                                    text = "DAFTAR BARANG YANG DIKIRIM (CEK FISIK):",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155)
                                )

                                transaction.items.forEachIndexed { idx, item ->
                                    val qtyStr = if (item.quantity.stripTrailingZeros().scale() <= 0) {
                                        item.quantity.setScale(0, RoundingMode.HALF_UP).toPlainString()
                                    } else {
                                        item.quantity.stripTrailingZeros().toPlainString()
                                    }
                                    val unitStr = item.unitName?.takeIf { it.isNotBlank() } ?: "pcs"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "[✓]",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0284C7),
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                        Text(
                                            text = "$qtyStr $unitStr",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            modifier = Modifier.width(62.dp)
                                        )
                                        Text(
                                            text = item.productName,
                                            fontSize = 11.sp,
                                            color = Color(0xFF334155),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (currentDeliveryData.deliveryFeeText.isNotBlank()) {
                                    HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Biaya Pengiriman", fontSize = 11.sp, color = Color(0xFF64748B))
                                        Text(currentDeliveryData.deliveryFeeText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }
                                }

                                if (currentDeliveryData.specialInstructions.isNotBlank()) {
                                    HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))
                                    Text("Instruksi Khusus: ${currentDeliveryData.specialInstructions}", fontSize = 10.5.sp, color = Color(0xFF0369A1))
                                }

                                HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 6.dp))

                                // Kolom 3 Tanda Tangan: Kasir Toko, Sopir (Gudang & Armada), Penerima
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Kasir (Toko)", fontSize = 9.sp, color = Color(0xFF64748B))
                                        Spacer(Modifier.height(26.dp))
                                        Text("( ${transaction.cashierName ?: "Kasir"} )", fontSize = 9.5.sp, color = Color(0xFF0F172A))
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Sopir (Gudang/Armada)", fontSize = 9.sp, color = Color(0xFF64748B))
                                        Spacer(Modifier.height(26.dp))
                                        Text(
                                            text = if (currentDeliveryData.courier.isNotBlank() && !currentDeliveryData.courier.contains("Toko", true)) {
                                                "( ${currentDeliveryData.courier.take(12)} )"
                                            } else {
                                                "( ............... )"
                                            },
                                            fontSize = 9.5.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Penerima / Proyek", fontSize = 9.sp, color = Color(0xFF64748B))
                                        Spacer(Modifier.height(26.dp))
                                        Text(
                                            text = if (currentDeliveryData.recipientName.isNotBlank() && currentDeliveryData.recipientName != "Pelanggan Umum") {
                                                "( ${currentDeliveryData.recipientName.take(12)} )"
                                            } else {
                                                "( ............... )"
                                            },
                                            fontSize = 9.5.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Barang telah diterima dalam kondisi baik & cukup.",
                                    fontSize = 9.5.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // 4. Panel Tombol Aksi Bawah (Selalu Terlihat)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tombol Utama: Cetak PDF Dokumen Resmi Surat Jalan (A5/A4)
                    Button(
                        onClick = {
                            DeliveryOrderFormatter.printDeliveryOrderPdf(
                                context = context,
                                transaction = transaction,
                                deliveryData = currentDeliveryData
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Icon(Icons.Outlined.Description, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cetak PDF Surat Jalan (Resmi)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Baris Aksi Sekunder: Cetak Thermal & Kirim WA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Cetak Thermal ESC/POS
                        OutlinedButton(
                            onClick = {
                                if (isPrintingThermal) return@OutlinedButton
                                isPrintingThermal = true
                                scope.launch {
                                    val app = context.applicationContext as? TbTerminalApplication
                                    val printerManager = app?.appContainer?.thermalPrinterManager
                                    if (printerManager == null) {
                                        Toast.makeText(context, "Layanan printer belum siap", Toast.LENGTH_SHORT).show()
                                        isPrintingThermal = false
                                        return@launch
                                    }
                                    val paperSize = withContext(Dispatchers.IO) { printerManager.getPaperSize() }
                                    val bytes = DeliveryOrderFormatter.buildDeliveryOrderBytes(
                                        transaction = transaction,
                                        storeName = "TB TERMINAL",
                                        paperSize = paperSize,
                                        deliveryData = currentDeliveryData
                                    )
                                    val result = printerManager.printCustomBytes(context, bytes)
                                    isPrintingThermal = false
                                    when (result) {
                                        is PrinterResult.Success -> {
                                            Toast.makeText(context, "Surat Jalan berhasil dicetak ke printer!", Toast.LENGTH_SHORT).show()
                                        }
                                        is PrinterResult.Error -> {
                                            Toast.makeText(context, "Gagal mencetak: ${result.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            enabled = !isPrintingThermal,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                        ) {
                            if (isPrintingThermal) {
                                CircularProgressIndicator(color = Color(0xFF0284C7), modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Outlined.Print, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Cetak Thermal", color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Kirim WhatsApp
                        Button(
                            onClick = { showWhatsAppConfirm = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Kirim WA", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showWhatsAppConfirm) {
        val waText = remember(currentDeliveryData) {
            DeliveryOrderFormatter.buildDeliveryOrderWhatsAppText(
                transaction = transaction,
                storeName = "TB TERMINAL",
                deliveryData = currentDeliveryData
            )
        }
        WhatsAppReceiptDialog(
            transaction = transaction,
            onDismissRequest = { showWhatsAppConfirm = false },
            onSend = { phone ->
                showWhatsAppConfirm = false
                val targetPhone = phone.ifBlank { currentDeliveryData.recipientPhone }
                val formattedPhone = WhatsAppReceiptHelper.formatPhoneNumber(targetPhone)
                try {
                    val encodedText = java.net.URLEncoder.encode(waText, "UTF-8")
                    val uri = if (formattedPhone.isNotBlank()) {
                        android.net.Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedText")
                    } else {
                        android.net.Uri.parse("https://api.whatsapp.com/send?text=$encodedText")
                    }
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(context, "Aplikasi WhatsApp tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

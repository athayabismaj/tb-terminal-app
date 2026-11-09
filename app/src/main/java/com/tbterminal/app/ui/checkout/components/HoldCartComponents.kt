package com.tbterminal.app.ui.checkout.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.checkout.HeldCart
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BrandGreen = Color(0xFF10533C)
private val BrandGreenLight = Color(0xFFE8F5E9)
private val BrandAmber = Color(0xFFD97706)
private val BrandAmberLight = Color(0xFFFEF3C7)
private val SlateText = Color(0xFF0F172A)
private val SlateMuted = Color(0xFF64748B)
private val SlateBorder = Color(0xFFE2E8F0)
private val ErrorRed = Color(0xFFEF4444)

private fun formatRupiah(amount: java.math.BigDecimal): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount).replace(" ", "")
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.forLanguageTag("id-ID"))
    return sdf.format(Date(timestamp))
}

/**
 * Bottom Sheet Tunda / Parkir Pesanan dengan gaya visual konsisten QuickAddCustomerSheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoldCartDialog(
    itemCount: Int,
    defaultNote: String,
    onDismiss: () -> Unit,
    onConfirm: (note: String) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var noteInput by remember(defaultNote) { mutableStateOf(defaultNote) }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        scrimColor = Color(0xFF0F172A).copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar (Tanpa ikon di samping judul sesuai pedoman minimalis)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tunda / Parkir Pesanan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateText
                    )
                    Text(
                        text = "Simpan keranjang sementara ($itemCount barang dalam antrean)",
                        fontSize = 12.sp,
                        color = SlateMuted
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Tutup",
                        tint = SlateMuted
                    )
                }
            }

            HorizontalDivider(color = SlateBorder, thickness = 1.dp)

            // Info Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BrandAmberLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.BookmarkAdd,
                            contentDescription = null,
                            tint = BrandAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pesanan Disimpan Sementara",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlateText
                        )
                        Text(
                            text = "Keranjang saat ini akan disimpan agar kasir dapat melayani pelanggan lain. Panggil kembali lewat tombol Parkir.",
                            fontSize = 11.5.sp,
                            color = SlateMuted
                        )
                    }
                }
            }

            // Error banner if any
            if (!validationError.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFEF2F2))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = validationError!!,
                        color = ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Field Label / Catatan Pesanan
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Label / Catatan Pesanan *",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateText
                )

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = {
                        noteInput = it
                        validationError = null
                    },
                    placeholder = {
                        Text("Contoh: Bpk. Bambang (Semen)", color = SlateMuted, fontSize = 13.sp)
                    },
                    trailingIcon = {
                        if (noteInput.isNotBlank()) {
                            IconButton(onClick = { noteInput = ""; validationError = null }) {
                                Icon(
                                    Icons.Outlined.Cancel,
                                    contentDescription = "Hapus input",
                                    tint = SlateMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = SlateBorder,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        val trimmed = noteInput.trim().ifBlank { "Pesanan Parkir" }
                        onConfirm(trimmed)
                    })
                )
            }

            // Quick Shortcut Preset Chips
            val presets = listOf("Pesanan Pelanggan", "Tunggu Pembayaran", "Ambil Barang", "Proyek Baru")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { preset ->
                    val isSelected = noteInput == preset
                    Surface(
                        onClick = {
                            noteInput = preset
                            validationError = null
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) BrandGreenLight else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isSelected) BrandGreen else SlateBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preset,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) BrandGreen else SlateText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Button(
                onClick = {
                    val trimmed = noteInput.trim().ifBlank { "Pesanan Parkir" }
                    onConfirm(trimmed)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) {
                Icon(
                    Icons.Outlined.BookmarkAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "SIMPAN & PARKIR PESANAN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                Text(
                    text = "Batal",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

/**
 * Bottom sheet daftar pesanan terparkir untuk dipulihkan (resume) atau dihapus.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeldCartsSheet(
    heldCarts: List<HeldCart>,
    onDismiss: () -> Unit,
    onResumeCart: (heldCartId: String) -> Unit,
    onDeleteCart: (heldCartId: String) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        scrimColor = Color(0xFF0F172A).copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header (Tanpa ikon di samping judul sesuai konsistensi pop-up kasir)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pesanan Terparkir",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateText
                    )
                    Text(
                        text = "${heldCarts.size} transaksi tertunda",
                        fontSize = 12.sp,
                        color = SlateMuted
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Tutup", tint = SlateMuted)
                }
            }

            HorizontalDivider(color = SlateBorder, thickness = 1.dp)

            if (heldCarts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.ShoppingCart,
                            contentDescription = null,
                            tint = SlateMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            "Tidak ada pesanan yang sedang diparkir.",
                            color = SlateMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(heldCarts, key = { it.id }) { held ->
                        HeldCartCard(
                            heldCart = held,
                            onResume = { onResumeCart(held.id) },
                            onDelete = { onDeleteCart(held.id) }
                        )
                    }
                }
            }

            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                Text(
                    text = "Tutup",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun HeldCartCard(
    heldCart: HeldCart,
    onResume: () -> Unit,
    onDelete: () -> Unit,
) {
    val totalQty = heldCart.items.sumOf { it.quantity }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = heldCart.note,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = SlateText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE2E8F0)
                    ) {
                        Text(
                            text = formatTime(heldCart.createdAt),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlateMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Hapus",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Ringkasan item
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$totalQty barang (${heldCart.items.size} SKU)",
                    fontSize = 13.sp,
                    color = SlateMuted
                )
                Text(
                    text = formatRupiah(heldCart.finalTotal),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandGreen
                )
            }

            if (heldCart.customer != null || heldCart.isDeliveryEnabled) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (heldCart.customer != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.PersonOutline, null, tint = SlateMuted, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(heldCart.customer.name, fontSize = 11.sp, color = SlateMuted)
                        }
                    }
                    if (heldCart.isDeliveryEnabled) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LocalShipping, null, tint = BrandGreen, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Kirim", fontSize = 11.sp, color = BrandGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Tombol Pulihkan
            Button(
                onClick = onResume,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) {
                Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Muat ke Keranjang", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

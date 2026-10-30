package com.tbterminal.app.ui.checkout.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.checkout.HeldCart
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BrandGreen = Color(0xFF006948)
private val BrandAmber = Color(0xFFD97706)
private val BrandAmberLight = Color(0xFFFEF3C7)
private val SlateText = Color(0xFF0F172A)
private val SlateMuted = Color(0xFF64748B)
private val SlateBorder = Color(0xFFE2E8F0)

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
 * Dialog konfirmasi parkir pesanan dengan input label / catatan nama pelanggan.
 */
@Composable
fun HoldCartDialog(
    itemCount: Int,
    defaultNote: String,
    onDismiss: () -> Unit,
    onConfirm: (note: String) -> Unit,
) {
    var noteInput by remember { mutableStateOf(defaultNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BrandAmberLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.BookmarkAdd,
                    contentDescription = null,
                    tint = BrandAmber,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                "Parkir Pesanan Ini?",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = SlateText
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Simpan keranjang sementara ($itemCount barang). Anda dapat melayani transaksi lain dan memulihkan pesanan ini nanti.",
                    fontSize = 13.sp,
                    color = SlateMuted
                )
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Label / Catatan pesanan") },
                    placeholder = { Text("Contoh: Bpk. Bambang (Semen)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = SlateBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(noteInput) },
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Parkir Pesanan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = SlateMuted)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
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
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BrandAmberLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.PauseCircleOutline,
                            contentDescription = null,
                            tint = BrandAmber,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Pesanan Terparkir",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateText
                        )
                        Text(
                            "${heldCarts.size} transaksi tertunda",
                            fontSize = 12.sp,
                            color = SlateMuted
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Tutup", tint = SlateMuted)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = SlateBorder)

            if (heldCarts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Tidak ada pesanan yang sedang diparkir.",
                        color = SlateMuted,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
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
                    .height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) {
                Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Muat ke Keranjang", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

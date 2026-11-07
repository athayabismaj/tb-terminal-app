package com.tbterminal.app.ui.checkout.components

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.checkout.CartItem
import com.tbterminal.app.ui.checkout.PaymentMethod
import com.tbterminal.app.ui.checkout.displayName
import com.tbterminal.app.ui.checkout.estimateDiscountAmount
import com.tbterminal.app.data.model.DiscountType
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val CartSurface = Color.White
private val CartSurfaceContainerLow = Color(0xFFF0FDF4)
private val CartOnSurface = Color(0xFF0F172A)
private val CartOnSurfaceVariant = Color(0xFF64748B)
private val CartOutlineVariant = Color(0xFFE2E8F0)
private val CartOutline = Color(0xFF94A3B8)
private val CartPrimary = Color(0xFF10533C)
private val CartPrimaryContainer = Color(0xFF10533C)
private val CartOnPrimary = Color(0xFFFFFFFF)
private val CartTertiary = Color(0xFFB45309)
private val CartError = Color(0xFFDC2626)

@Composable
fun CartItemCard(
    item: CartItem,
    sku: String,
    unitName: String,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    onDiscountClick: () -> Unit,
    onToggleUnit: ((Boolean) -> Unit)? = null,
    onRemoveDiscount: (() -> Unit)? = null,
) {
    val lineGross = item.unitPrice.multiply(item.quantity.toBigDecimal())
    val discountAmount = estimateDiscountAmount(lineGross, item.discountRequest)
    val hasDiscount = item.discountRequest != null && discountAmount > BigDecimal.ZERO
    val lineNet = lineGross.subtract(discountAmount).coerceAtLeast(BigDecimal.ZERO)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── BARIS 1: Info Produk & Subtotal Harga ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Sisi Kiri: Avatar Inisial & Info Produk
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Kotak Inisial Produk (40dp x 40dp, rounded-xl 12dp)
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = item.productName.take(2).uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasDiscount) Color(0xFF92400E) else Color(0xFF10533C)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = item.productName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "@ ${formatRupiah(item.unitPrice)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                            if (unitName.isNotBlank()) {
                                Text(
                                    text = "/ $unitName",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            if (sku.isNotBlank()) {
                                Text(
                                    text = "• SKU: $sku",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                // Sisi Kanan: Subtotal Nominal Produk (Dengan harga coret jika ada diskon)
                Column(horizontalAlignment = Alignment.End) {
                    if (hasDiscount) {
                        Text(
                            text = formatRupiah(lineGross),
                            fontSize = 11.5.sp,
                            color = Color(0xFF94A3B8),
                            style = TextStyle(textDecoration = TextDecoration.LineThrough)
                        )
                    }
                    Text(
                        text = formatRupiah(if (hasDiscount) lineNet else lineGross),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.3).sp
                    )
                }
            }

            // ── BARIS 2: Modifiers / Diskon (Kiri) + Stepper Qty (Kanan) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sisi Kiri: Opsi Satuan Alternatif & Diskon Item
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Tombol Ganti Satuan jika ada secondary unit
                    if (item.hasSecondaryUnit && onToggleUnit != null) {
                        Surface(
                            onClick = { onToggleUnit(!item.isSecondaryUnit) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (item.isSecondaryUnit) Color(0xFFF0FDF4) else Color.White,
                            border = BorderStroke(1.dp, if (item.isSecondaryUnit) Color(0xFFBBF7D0) else Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SyncAlt,
                                    contentDescription = null,
                                    tint = if (item.isSecondaryUnit) Color(0xFF10533C) else Color(0xFF64748B),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (item.isSecondaryUnit) (item.secondaryUnitName ?: "Satuan 2") else (item.baseUnitName.ifBlank { "Utama" }),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.isSecondaryUnit) Color(0xFF10533C) else Color(0xFF475569)
                                )
                            }
                        }
                    }

                    // Modifiers Diskon
                    val request = item.discountRequest
                    if (request == null) {
                        Row(
                            modifier = Modifier
                                .clickable { onDiscountClick() }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocalOffer,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "+ Diskon",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 0.5.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LocalOffer,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = when (request.type) {
                                        DiscountType.PERCENTAGE -> "Diskon ${request.value.stripTrailingZeros().toPlainString()}%"
                                        DiscountType.FIXED_AMOUNT -> "Diskon ${formatRupiah(request.value)}"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.clickable { onDiscountClick() }
                                )
                                if (onRemoveDiscount != null) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Hapus diskon",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier
                                            .size(13.dp)
                                            .clickable { onRemoveDiscount() }
                                    )
                                }
                            }
                        }
                    }
                }

                // Sisi Kanan: Quantity Stepper (flex items-center gap-1 bg-slate-50 p-0.5 rounded-xl border border-slate-200)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(2.dp)
                    ) {
                        // Tombol Kurang / Hapus
                        Surface(
                            onClick = if (item.quantity <= 1) onRemove else onDecrease,
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 0.5.dp,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (item.quantity <= 1) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Hapus Item",
                                        tint = Color(0xFFE11D48),
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Remove,
                                        contentDescription = "Kurang Qty",
                                        tint = Color(0xFF334155),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // Angka Kuantitas
                        Box(
                            modifier = Modifier.width(30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.quantity.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                textAlign = TextAlign.Center
                            )
                        }

                        // Tombol Tambah (Hijau Forest #10533C)
                        Surface(
                            onClick = onIncrease,
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10533C),
                            shadowElevation = 0.5.dp,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = "Tambah Qty",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartCalculationRow(
    label: String,
    value: String,
    isDiscount: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 13.5.sp,
            color = Color(0xFF64748B)
        )
        Text(
            value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDiscount) Color(0xFFB45309) else Color(0xFF0F172A)
        )
    }
}

@Composable
fun PaymentMethodSelector(
    selectedMethod: PaymentMethod,
    onSelectPayment: (PaymentMethod) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "METODE PEMBAYARAN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 0.8.sp
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFF0FDF4),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0))
            ) {
                Text(
                    text = selectedMethod.displayName(),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF004528),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Baris 1: Pembayaran Langsung (Tunai, QRIS, Transfer)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(PaymentMethod.TUNAI, PaymentMethod.QRIS, PaymentMethod.TRANSFER).forEach { method ->
                val isSelected = method == selectedMethod
                Surface(
                    onClick = { onSelectPayment(method) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFF004528) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF004528) else Color(0xFFE2E8F0)
                    ),
                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = when (method) {
                                PaymentMethod.TUNAI -> Icons.Outlined.Payments
                                PaymentMethod.QRIS -> Icons.Outlined.QrCode2
                                PaymentMethod.TRANSFER -> Icons.Outlined.AccountBalance
                                else -> Icons.Outlined.Payments
                            },
                            contentDescription = method.displayName(),
                            tint = if (isSelected) Color.White else Color(0xFF475569),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = method.displayName(),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF334155),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Baris 2: Piutang & Bertahap (Hutang / Tempo & DP Proyek)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(PaymentMethod.HUTANG, PaymentMethod.DP).forEach { method ->
                val isSelected = method == selectedMethod
                val label = if (method == PaymentMethod.HUTANG) "Hutang / Tempo" else "DP Proyek"
                Surface(
                    onClick = { onSelectPayment(method) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFF004528) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF004528) else Color(0xFFE2E8F0)
                    ),
                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = when (method) {
                                PaymentMethod.HUTANG -> Icons.Outlined.Receipt
                                PaymentMethod.DP -> Icons.Outlined.AccountBalanceWallet
                                else -> Icons.Outlined.Receipt
                            },
                            contentDescription = label,
                            tint = if (isSelected) Color.White else Color(0xFF475569),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF334155),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CartEmptyState(
    onBackToPos: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        CartSurfaceContainerLow,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.ShoppingCart,
                    contentDescription = null,
                    tint = CartOutline,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Keranjang Kosong",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = CartOnSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Tambahkan produk dari katalog untuk memulai transaksi",
                fontSize = 15.sp,
                color = CartOnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onBackToPos,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CartPrimary
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Mulai Belanja",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

private fun formatRupiah(amount: BigDecimal): String {
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))
    return "Rp ${formatter.format(amount)}"
}

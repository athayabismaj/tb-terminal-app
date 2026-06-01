package com.tbterminal.app.ui.checkout.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.checkout.CartItem
import com.tbterminal.app.ui.checkout.PaymentMethod
import com.tbterminal.app.ui.checkout.displayName
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val CartSurface = Color(0xFFFFFFFF)
private val CartSurfaceContainerLow = Color(0xFFEFF4FF)
private val CartOnSurface = Color(0xFF121C2A)
private val CartOnSurfaceVariant = Color(0xFF3D4A42)
private val CartOutlineVariant = Color(0xFFBCCAC0)
private val CartOutline = Color(0xFF6D7A72)
private val CartPrimary = Color(0xFF006948)
private val CartPrimaryContainer = Color(0xFF00855D)
private val CartOnPrimary = Color(0xFFFFFFFF)
private val CartTertiary = Color(0xFF9B3E3B)
private val CartError = Color(0xFFBA1A1A)

@Composable
fun CartItemCard(
    item: CartItem,
    sku: String,
    unitName: String,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    val subtotal = item.unitPrice.multiply(item.quantity.toBigDecimal())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CartSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CartOutlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(CartSurfaceContainerLow, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    item.productName.take(2).uppercase(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = CartPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.productName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CartOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row {
                    if (unitName.isNotBlank()) {
                        Text(
                            unitName,
                            fontSize = 13.sp,
                            color = CartOnSurfaceVariant
                        )
                    }
                    if (sku.isNotBlank()) {
                        if (unitName.isNotBlank()) {
                            Text(
                                " • ",
                                fontSize = 13.sp,
                                color = CartOnSurfaceVariant
                            )
                        }
                        Text(
                            "SKU: $sku",
                            fontSize = 13.sp,
                            color = CartOnSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.quantity <= 1) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .size(36.dp)
                            .border(1.dp, CartError.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Hapus",
                            tint = CartError,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onDecrease,
                        modifier = Modifier
                            .size(36.dp)
                            .border(1.dp, CartOutlineVariant, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            Icons.Outlined.Remove,
                            contentDescription = "Kurang",
                            tint = CartOnSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = item.quantity.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CartOnSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(44.dp)
                )

                IconButton(
                    onClick = onIncrease,
                    modifier = Modifier
                        .size(36.dp)
                        .background(CartPrimaryContainer, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "Tambah",
                        tint = CartOnPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(130.dp)
            ) {
                Text(
                    "@ ${formatRupiah(item.unitPrice)}",
                    fontSize = 11.sp,
                    color = CartOnSurfaceVariant
                )
                Text(
                    formatRupiah(subtotal),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CartOnSurface
                )
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
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 15.sp,
            color = CartOnSurfaceVariant
        )
        Text(
            value,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDiscount) CartTertiary else CartOnSurface
        )
    }
}

@Composable
fun PaymentMethodSelector(
    selectedMethod: PaymentMethod,
    onSelectPayment: (PaymentMethod) -> Unit
) {
    Column {
        Text(
            "Metode Pembayaran",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = CartOnSurfaceVariant,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PaymentMethod.entries.forEach { method ->
                val isSelected = method == selectedMethod
                Surface(
                    onClick = { onSelectPayment(method) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) CartPrimary else CartSurface,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color.Transparent else CartOutlineVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Icon(
                            when (method) {
                                PaymentMethod.TUNAI -> Icons.Outlined.Payments
                                PaymentMethod.TRANSFER -> Icons.Outlined.AccountBalance
                                PaymentMethod.QRIS -> Icons.Outlined.QrCode2
                                PaymentMethod.HUTANG -> Icons.Outlined.Receipt
                                PaymentMethod.DP -> Icons.Outlined.AccountBalanceWallet
                            },
                            contentDescription = method.displayName(),
                            tint = if (isSelected) CartOnPrimary else CartOnSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            method.displayName(),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) CartOnPrimary else CartOnSurfaceVariant,
                            textAlign = TextAlign.Center,
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
    val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
    return "Rp ${formatter.format(amount)}"
}

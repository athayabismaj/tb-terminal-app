package com.tbterminal.app.ui.checkout.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditScore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.Customer
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Kartu Inspector Profil Kredit & Status Piutang Pelanggan di Layar POS / Keranjang.
 * Memberi sinyal keamanan bagi kasir sebelum memberikan transaksi non-tunai bertipe Piutang (Bon/Tempo).
 */
@Composable
fun CustomerCreditInspectorCard(
    customer: Customer,
    currentBillTotal: BigDecimal = BigDecimal.ZERO,
    modifier: Modifier = Modifier
) {
    val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply { groupingSeparator = '.' }
    val moneyFormatter = DecimalFormat("#,##0", symbols)
    fun formatMoney(amount: BigDecimal): String = "Rp " + moneyFormatter.format(amount)

    val hasCreditFacility = customer.creditLimit > BigDecimal.ZERO
    val isOverLimit = hasCreditFacility && currentBillTotal > customer.creditLimit

    val containerColor = when {
        !hasCreditFacility -> Color(0xFFF8FAFC)
        isOverLimit -> Color(0xFFFEF2F2)
        else -> Color(0xFFF0FDF4)
    }

    val borderColor = when {
        !hasCreditFacility -> Color(0xFFCBD5E1)
        isOverLimit -> Color(0xFFFECACA)
        else -> Color(0xFFBBF7D0)
    }

    val titleColor = when {
        !hasCreditFacility -> Color(0xFF475569)
        isOverLimit -> Color(0xFFDC2626)
        else -> Color(0xFF166534)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Nama Pelanggan & Status Fasilitas Kredit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (hasCreditFacility) Icons.Outlined.CreditScore else Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        tint = titleColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = customer.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    if (customer.isContractor) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE0E7FF),
                            border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                        ) {
                            Text(
                                text = "Kontraktor",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3730A3),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Badge Plafon
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (hasCreditFacility) (if (isOverLimit) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)) else Color(0xFFE2E8F0)
                ) {
                    Text(
                        text = if (!hasCreditFacility) "Hanya Tunai" else (if (isOverLimit) "Overlimit" else "Kredit Aktif"),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = borderColor)

            // Info Plafon & Termin
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Plafon Kredit", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text(
                        text = if (hasCreditFacility) formatMoney(customer.creditLimit) else "Rp 0 (Non-Kredit)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasCreditFacility) Color(0xFF0F172A) else Color(0xFF64748B)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Termin Jatuh Tempo", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text(
                        text = if (customer.paymentTermDays > 0) "${customer.paymentTermDays} Hari" else "Langsung (0 Hari)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            // Peringatan Keamanan Transaksi
            if (isOverLimit) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Total tagihan (${formatMoney(currentBillTotal)}) melampaui plafon kredit pelanggan!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }
            } else if (!hasCreditFacility) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Pelanggan ini belum memiliki persetujuan limit kredit (Tempo/Bon).",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

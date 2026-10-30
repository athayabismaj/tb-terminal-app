package com.tbterminal.app.ui.checkout.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal

private val BrandGreen = Color(0xFF006948)
private val SlateText = Color(0xFF0F172A)
private val SlateMuted = Color(0xFF64748B)
private val SlateBorder = Color(0xFFE2E8F0)
private val ContainerBg = Color(0xFFF8FAFC)

/**
 * Komponen Pengiriman / Ekspedisi Toko Bangunan pada Ringkasan Keranjang.
 */
@Composable
fun DeliverySectionCard(
    isDeliveryEnabled: Boolean,
    deliveryFeeInput: String,
    deliveryAddress: String,
    deliveryCourier: String,
    onToggleDelivery: (Boolean) -> Unit,
    onDeliveryFeeChanged: (String) -> Unit,
    onDeliveryAddressChanged: (String) -> Unit,
    onDeliveryCourierChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDeliveryEnabled) Color(0xFFF0FDF4) else ContainerBg),
        border = BorderStroke(1.dp, if (isDeliveryEnabled) BrandGreen.copy(alpha = 0.3f) else SlateBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = if (isDeliveryEnabled) BrandGreen.copy(alpha = 0.15f) else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocalShipping,
                            contentDescription = null,
                            tint = if (isDeliveryEnabled) BrandGreen else SlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Pengiriman Armada",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateText
                        )
                        Text(
                            text = if (isDeliveryEnabled) "Diantar ke alamat pembeli" else "Bawa pulang sendiri",
                            fontSize = 11.sp,
                            color = SlateMuted
                        )
                    }
                }

                Switch(
                    checked = isDeliveryEnabled,
                    onCheckedChange = onToggleDelivery,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BrandGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFCBD5E1)
                    )
                )
            }

            AnimatedVisibility(
                visible = isDeliveryEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Input Ongkir
                    OutlinedTextField(
                        value = deliveryFeeInput,
                        onValueChange = onDeliveryFeeChanged,
                        label = { Text("Biaya Pengiriman (Ongkir)") },
                        placeholder = { Text("Contoh: 50000") },
                        leadingIcon = {
                            Text(
                                "Rp",
                                color = BrandGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = BrandGreen,
                            unfocusedBorderColor = SlateBorder
                        )
                    )

                    // Input Alamat
                    OutlinedTextField(
                        value = deliveryAddress,
                        onValueChange = onDeliveryAddressChanged,
                        label = { Text("Alamat Pengiriman") },
                        placeholder = { Text("Nama jalan, patokan, atau proyek...") },
                        leadingIcon = {
                            Icon(Icons.Outlined.LocationOn, null, tint = SlateMuted, modifier = Modifier.size(18.dp))
                        },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = BrandGreen,
                            unfocusedBorderColor = SlateBorder
                        )
                    )

                    // Input Driver / Armada
                    OutlinedTextField(
                        value = deliveryCourier,
                        onValueChange = onDeliveryCourierChanged,
                        label = { Text("Armada / Nama Supir (Opsional)") },
                        placeholder = { Text("Contoh: Pick-up Engkel (Pak Joko)") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Badge, null, tint = SlateMuted, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = BrandGreen,
                            unfocusedBorderColor = SlateBorder
                        )
                    )
                }
            }
        }
    }
}

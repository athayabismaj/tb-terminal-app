package com.tbterminal.app.ui.checkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.data.model.DiscountType
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val BrandGreen = Color(0xFF10533C)
private val BrandGreenLight = Color(0xFFE8F5E9)
private val TextDark = Color(0xFF0F172A)
private val TextMuted = Color(0xFF64748B)
private val BorderColor = Color(0xFFE2E8F0)
private val ErrorRed = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiscountInputDialog(
    title: String,
    baseAmount: BigDecimal,
    initialValue: CheckoutDiscount?,
    onDismiss: () -> Unit,
    onApply: (CheckoutDiscount?) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var type by remember(initialValue) { mutableStateOf(initialValue?.type ?: DiscountType.PERCENTAGE) }
    var valueInput by remember(initialValue) {
        mutableStateOf(initialValue?.value?.stripTrailingZeros()?.toPlainString().orEmpty())
    }
    var error by remember { mutableStateOf<String?>(null) }

    fun apply() {
        val cleaned = valueInput.trim().replace(',', '.')
        if (cleaned.isBlank()) {
            error = "Masukkan nilai diskon yang valid."
            return
        }
        val value = cleaned.toBigDecimalOrNull()
        if (value == null) {
            error = "Masukkan nilai diskon yang valid."
            return
        }
        val discount = CheckoutDiscount(type, value)
        val validation = validateCheckoutDiscount(baseAmount, discount)
        if (validation != null) {
            error = validation
            return
        }
        onApply(discount.takeUnless { it.value.compareTo(BigDecimal.ZERO) == 0 })
    }

    // Live preview calculation
    val parsedValue = valueInput.trim().replace(',', '.').toBigDecimalOrNull()
    val previewCut: BigDecimal? = if (parsedValue != null && parsedValue > BigDecimal.ZERO) {
        if (type == DiscountType.PERCENTAGE) {
            baseAmount.multiply(parsedValue).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        } else {
            parsedValue
        }
    } else null
    val previewFinal: BigDecimal? = previewCut?.let {
        baseAmount.subtract(it).coerceAtLeast(BigDecimal.ZERO)
    }

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
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    val subtitle = if (title.contains("Nota", ignoreCase = true) || title.contains("Transaksi", ignoreCase = true)) {
                        "Atur potongan harga untuk total belanja nota kasir"
                    } else {
                        "Atur potongan harga khusus untuk baris item ini"
                    }
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Tutup",
                        tint = TextMuted
                    )
                }
            }

            HorizontalDivider(color = BorderColor, thickness = 1.dp)

            // Base Amount & Preview Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dasar Nilai Belanja",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        Text(
                            text = formatRupiah(baseAmount),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }

                    if (previewCut != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Potongan Diskon:",
                                fontSize = 12.sp,
                                color = BrandGreen
                            )
                            Text(
                                text = "- ${formatRupiah(previewCut)}",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreen
                            )
                        }

                        HorizontalDivider(
                            color = BorderColor.copy(alpha = 0.6f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Setelah Diskon:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDark
                            )
                            Text(
                                text = formatRupiah(previewFinal ?: baseAmount),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandGreen
                            )
                        }
                    }
                }
            }

            // Error Banner (if any)
            if (!error.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFEF2F2))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = error!!,
                        color = ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Jenis Diskon (Segmented Control Tabs)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Jenis Diskon *",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isPercent = type == DiscountType.PERCENTAGE
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isPercent) BrandGreen else Color.Transparent)
                            .clickable {
                                type = DiscountType.PERCENTAGE
                                error = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Persentase (%)",
                            fontSize = 13.sp,
                            fontWeight = if (isPercent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isPercent) Color.White else TextDark
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isPercent) BrandGreen else Color.Transparent)
                            .clickable {
                                type = DiscountType.FIXED_AMOUNT
                                error = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nominal Rupiah (Rp)",
                            fontSize = 13.sp,
                            fontWeight = if (!isPercent) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isPercent) Color.White else TextDark
                        )
                    }
                }
            }

            // Field Nilai Diskon
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val inputLabel = if (type == DiscountType.PERCENTAGE) "Besaran Persen Diskon (%) *" else "Nominal Diskon (Rp) *"
                Text(
                    text = inputLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )

                OutlinedTextField(
                    value = valueInput,
                    onValueChange = { input ->
                        valueInput = input.filter { char -> char.isDigit() || char == '.' || char == ',' }
                        error = null
                    },
                    placeholder = {
                        val ph = if (type == DiscountType.PERCENTAGE) "Contoh: 10 (untuk 10%)" else "Contoh: 15000 (untuk Rp 15.000)"
                        Text(ph, color = TextMuted, fontSize = 13.sp)
                    },
                    prefix = {
                        Text(
                            text = if (type == DiscountType.PERCENTAGE) "% " else "Rp ",
                            fontWeight = FontWeight.Bold,
                            color = BrandGreen,
                            fontSize = 14.sp
                        )
                    },
                    trailingIcon = {
                        if (valueInput.isNotBlank()) {
                            IconButton(onClick = { valueInput = ""; error = null }) {
                                Icon(
                                    Icons.Outlined.Cancel,
                                    contentDescription = "Hapus input",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { apply() })
                )
            }

            // Quick Shortcut Presets
            val presets = if (type == DiscountType.PERCENTAGE) {
                listOf("5", "10", "15", "20")
            } else {
                when {
                    baseAmount >= BigDecimal(100_000) -> listOf("10000", "25000", "50000", "100000")
                    baseAmount >= BigDecimal(50_000) -> listOf("5000", "10000", "15000", "20000")
                    else -> listOf("2000", "5000", "10000", "15000")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    val isSelected = valueInput == preset
                    Surface(
                        onClick = {
                            valueInput = preset
                            error = null
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) BrandGreenLight else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isSelected) BrandGreen else BorderColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val presetLabel = if (type == DiscountType.PERCENTAGE) {
                                "$preset%"
                            } else {
                                val num = preset.toLongOrNull() ?: 0L
                                "Rp ${DecimalFormat("#,##0", DecimalFormatSymbols(Locale.forLanguageTag("id-ID"))).format(num)}"
                            }
                            Text(
                                text = presetLabel,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) BrandGreen else TextDark
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Button(
                onClick = ::apply,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "TERAPKAN DISKON",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            if (initialValue != null) {
                OutlinedButton(
                    onClick = { onApply(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFDAD6)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Hapus Diskon",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ErrorRed
                    )
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
                    text = "Batal",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

package com.tbterminal.app.ui.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.data.model.DiscountType
import java.math.BigDecimal

@Composable
internal fun DiscountInputDialog(
    title: String,
    baseAmount: BigDecimal,
    initialValue: CheckoutDiscount?,
    onDismiss: () -> Unit,
    onApply: (CheckoutDiscount?) -> Unit,
) {
    var type by remember(initialValue) { mutableStateOf(initialValue?.type ?: DiscountType.PERCENTAGE) }
    var valueInput by remember(initialValue) {
        mutableStateOf(initialValue?.value?.stripTrailingZeros()?.toPlainString().orEmpty())
    }
    var error by remember { mutableStateOf<String?>(null) }

    fun apply() {
        val value = valueInput.trim().replace(',', '.').toBigDecimalOrNull()
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

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Diskon nominal berlaku untuk total baris item, bukan per satuan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = type == DiscountType.PERCENTAGE,
                        onClick = {
                            type = DiscountType.PERCENTAGE
                            error = null
                        },
                        label = { Text("Persen") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = type == DiscountType.FIXED_AMOUNT,
                        onClick = {
                            type = DiscountType.FIXED_AMOUNT
                            error = null
                        },
                        label = { Text("Nominal") },
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = valueInput,
                    onValueChange = { input ->
                        valueInput = input.filter { char -> char.isDigit() || char == '.' || char == ',' }
                        error = null
                    },
                    label = { Text(if (type == DiscountType.PERCENTAGE) "Persentase" else "Nominal diskon") },
                    prefix = { Text(if (type == DiscountType.PERCENTAGE) "% " else "Rp ") },
                    supportingText = error?.let { message ->
                        { Text(message, color = MaterialTheme.colorScheme.error) }
                    },
                    isError = error != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                )
            }
        },
        confirmButton = { Button(onClick = ::apply) { Text("Terapkan") } },
        dismissButton = {
            Row {
                if (initialValue != null) {
                    TextButton(onClick = { onApply(null) }) { Text("Hapus diskon") }
                }
                TextButton(onClick = onDismiss) { Text("Batal") }
            }
        },
    )
}

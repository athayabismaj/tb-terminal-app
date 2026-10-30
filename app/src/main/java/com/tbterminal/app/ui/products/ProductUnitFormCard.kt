package com.tbterminal.app.ui.products

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BrandGreen = Color(0xFF256B57)
private val TextPrimary = Color(0xFF0F172A)
private val TextSecondary = Color(0xFF64748B)
private val BorderSlate200 = Color(0xFFE2E8F0)
private val BorderSlate300 = Color(0xFFCBD5E1)

@Composable
internal fun ProductUnitFormCard(
    modifier: Modifier,
    uiState: ProductUnitUiState,
    onNameChanged: (String) -> Unit,
    onSymbolChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
) {
    val isErrorMessage = uiState.message?.let {
        it.contains("gagal", ignoreCase = true) ||
        it.contains("tidak", ignoreCase = true) ||
        it.contains("kosong", ignoreCase = true)
    } == true
    val isSymbolError = isErrorMessage && uiState.message?.contains("simbol", ignoreCase = true) == true
    val isNameError = isErrorMessage && !isSymbolError

    Surface(
        modifier = modifier.testTag("unit-form"),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSlate200),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandGreen.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Straighten,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = BrandGreen,
                    )
                }
                Column {
                    Text(
                        text = if (uiState.editingUnit == null) "Satuan baru" else "Ubah satuan",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Gunakan nama dan simbol yang mudah dikenali.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                    )
                }
            }

            UnitInput(
                label = "Nama satuan",
                value = uiState.nameInput,
                placeholder = "Contoh: Kilogram",
                tag = "unit-name",
                onChange = onNameChanged,
                isError = isNameError,
                supportingText = if (isNameError) uiState.message else null,
            )

            UnitInput(
                label = "Simbol satuan",
                value = uiState.symbolInput,
                placeholder = "Contoh: kg",
                tag = "unit-symbol",
                onChange = onSymbolChanged,
                isError = isSymbolError,
                supportingText = if (isSymbolError) uiState.message else null,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onCancelEdit,
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderSlate300),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                ) {
                    Text("Batal", color = Color(0xFF334155), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = onSave,
                    enabled = !uiState.isSaving,
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("unit-save"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isSaving) "Menyimpan…" else "Simpan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun UnitInput(
    label: String,
    value: String,
    placeholder: String,
    tag: String,
    onChange: (String) -> Unit,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            color = if (isError) Color(0xFFDC2626) else Color(0xFF475569),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag),
            placeholder = { Text(placeholder, fontSize = 14.sp, color = Color(0xFF94A3B8)) },
            singleLine = true,
            isError = isError,
            supportingText = supportingText?.let { text ->
                {
                    Text(
                        text = text,
                        color = Color(0xFFDC2626),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isError) Color(0xFFDC2626) else BrandGreen,
                unfocusedBorderColor = if (isError) Color(0xFFDC2626) else BorderSlate300,
                focusedContainerColor = if (isError) Color(0xFFFFF5F5) else Color.White,
                unfocusedContainerColor = if (isError) Color(0xFFFFF5F5) else Color.White,
                cursorColor = if (isError) Color(0xFFDC2626) else BrandGreen,
                errorBorderColor = Color(0xFFDC2626),
                errorContainerColor = Color(0xFFFFF5F5),
            ),
            shape = RoundedCornerShape(12.dp),
        )
    }
}

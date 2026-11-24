package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.ui.incominggoods.IncomingPaymentMethod
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.BrandSoftText
import com.tbterminal.app.ui.theme.FieldErrorColor
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.OwnerBorder
import com.tbterminal.app.ui.theme.OwnerDivider
import com.tbterminal.app.ui.theme.SurfaceWhite
import java.math.BigDecimal

internal val BarangMasukAvailableDot = Color(0xFF1F9D63)
internal val BarangMasukAmberInk = Color(0xFF9A6A00)

private val ownerPaymentMethods = listOf(
    IncomingPaymentMethod.CASH to "Tunai",
    IncomingPaymentMethod.TRANSFER to "Transfer",
    IncomingPaymentMethod.QRIS to "QRIS",
    IncomingPaymentMethod.DEBT to "Hutang"
)

@Composable
internal fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = ownerTextStyle(
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .padding(bottom = 16.dp)
            .cssLineBoxHeight(20.dp)
            .semantics { heading() }
    )
}

@Composable
internal fun SelectField(
    label: String,
    value: String?,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    testTag: String = ""
) {
    val isError = errorText != null
    val borderWidth = if (isError) 1.5.dp else 1.dp
    val borderColor = if (isError) FieldErrorColor else OwnerBorder
    val horizontalPad = if (isError) 15.5.dp else 16.dp
    val displayText = value?.takeIf { it.isNotBlank() }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Ink
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .cssLineBoxHeight(20.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceWhite)
                .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onClick
                )
                .padding(horizontal = horizontalPad)
                .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = displayText ?: placeholder,
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (displayText != null) Ink else FormPlaceholderColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .cssLineBoxHeight(20.dp)
            )

            OwnerVectorIcon(
                imageVector = OwnerIcons.ChevronDown,
                tint = Muted,
                size = 20.dp
            )
        }

        if (errorText != null) {
            Text(
                text = errorText,
                style = ownerTextStyle(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = FieldErrorColor
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
internal fun MoneyField(
    label: String,
    digits: String,
    onDigitsChange: (String) -> Unit,
    placeholder: String = "0",
    modifier: Modifier = Modifier,
    errorText: String? = null,
    labelEndAction: (@Composable () -> Unit)? = null,
    testTag: String = ""
) {
    FormField(
        label = label,
        value = digits,
        onValueChange = { raw -> onDigitsChange(raw.filter(Char::isDigit)) },
        placeholder = placeholder,
        prefixText = "Rp",
        errorText = errorText,
        showErrorIcon = false,
        helperTopPadding = 8.dp,
        tabularNumbers = true,
        visualTransformation = ThousandSeparatorVisualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        labelEndAction = labelEndAction,
        testTag = testTag,
        modifier = modifier
    )
}

@Composable
internal fun DateReadOnlyField(
    label: String,
    displayValue: String?,
    placeholder: String,
    helperText: String?,
    errorText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val isError = errorText != null
    val borderWidth = if (isError) 1.5.dp else 1.dp
    val borderColor = if (isError) FieldErrorColor else OwnerBorder
    val startPad = if (isError) 15.5.dp else 16.dp
    val endPad = if (isError) 3.5.dp else 4.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Ink
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .cssLineBoxHeight(20.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceWhite)
                .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onClick
                )
                .padding(start = startPad, end = endPad)
                .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayValue?.takeIf { it.isNotBlank() } ?: placeholder,
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (!displayValue.isNullOrBlank()) Ink else FormPlaceholderColor,
                    tabularNumbers = true
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .cssLineBoxHeight(20.dp)
            )

            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                OwnerVectorIcon(
                    imageVector = OwnerIcons.CalendarRange,
                    tint = Muted,
                    size = 24.dp
                )
            }
        }

        if (errorText != null) {
            Text(
                text = errorText,
                style = ownerTextStyle(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = FieldErrorColor
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
        } else if (helperText != null) {
            Text(
                text = helperText,
                style = ownerTextStyle(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Muted
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
internal fun ProductMetaInfoBlock(
    product: ProductStock,
    showPhoneMetrics: Boolean,
    modifier: Modifier = Modifier
) {
    val isAvailable = product.quantity > BigDecimal.ZERO
    val statusColor = if (isAvailable) Brand else FieldErrorColor
    val dotColor = if (isAvailable) BarangMasukAvailableDot else FieldErrorColor
    val skuText = product.sku?.takeIf { it.isNotBlank() } ?: "-"
    val categoryText = product.categoryName.ifBlank { "Umum" }
    val unitLabel = product.unitName?.takeIf { it.isNotBlank() } ?: "Pieces"

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$skuText \u00B7 $categoryText",
                style = ownerTextStyle(
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = Muted
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .cssLineBoxHeight(20.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Text(
                    text = if (isAvailable) "Tersedia" else "Habis",
                    style = ownerTextStyle(
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    ),
                    maxLines = 1,
                    modifier = Modifier.cssLineBoxHeight(20.dp)
                )
            }
        }

        if (showPhoneMetrics) {
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Stok sekarang",
                        style = ownerTextStyle(
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = Muted
                        ),
                        maxLines = 1,
                        modifier = Modifier.cssLineBoxHeight(16.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${formatStockWhole(product.quantity)} $unitLabel",
                        style = ownerTextStyle(
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Ink,
                            tabularNumbers = true
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .cssLineBoxHeight(20.dp)
                            .testTag("barang-masuk-current-stock")
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .drawBehind {
                            val strokePx = 1.dp.toPx()
                            drawLine(
                                color = OwnerBorder,
                                start = Offset(strokePx / 2f, 0f),
                                end = Offset(strokePx / 2f, size.height),
                                strokeWidth = strokePx
                            )
                        }
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = "HPP saat ini",
                        style = ownerTextStyle(
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = Muted
                        ),
                        maxLines = 1,
                        modifier = Modifier.cssLineBoxHeight(16.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatRupiahWhole(product.priceBuy),
                        style = ownerTextStyle(
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Ink,
                            tabularNumbers = true
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .cssLineBoxHeight(20.dp)
                            .testTag("barang-masuk-current-hpp")
                    )
                }
            }
        }
    }
}

@Composable
internal fun PaymentMethodChips(
    selected: IncomingPaymentMethod,
    isTablet: Boolean,
    onSelect: (IncomingPaymentMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().selectableGroup()) {
        Text(
            text = "Metode pembayaran",
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Ink
            ),
            maxLines = 1,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .cssLineBoxHeight(20.dp)
        )

        if (isTablet) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PaymentChipRow(
                    methods = ownerPaymentMethods.take(2),
                    selected = selected,
                    onSelect = onSelect
                )
                PaymentChipRow(
                    methods = ownerPaymentMethods.drop(2),
                    selected = selected,
                    onSelect = onSelect
                )
            }
        } else {
            PaymentChipRow(
                methods = ownerPaymentMethods,
                selected = selected,
                onSelect = onSelect
            )
        }
    }
}

@Composable
private fun PaymentChipRow(
    methods: List<Pair<IncomingPaymentMethod, String>>,
    selected: IncomingPaymentMethod,
    onSelect: (IncomingPaymentMethod) -> Unit
) {
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            methods.forEach { (method, label) ->
                PaymentMethodChipItem(
                    label = label,
                    selected = method == selected,
                    onClick = { onSelect(method) },
                    testTag = "barang-masuk-method-${method.apiValue}"
                )
            }
        }
    ) { measurables, constraints ->
        val gapPx = 8.dp.roundToPx()
        val chipVisualHeightPx = 40.dp.roundToPx()
        val touchExtraVerticalPx = 4.dp.roundToPx()
        val touchHeightPx = chipVisualHeightPx + (touchExtraVerticalPx * 2)
        val totalWidth = constraints.maxWidth
        val count = measurables.size.coerceAtLeast(1)
        val colWidthPx = (totalWidth - (gapPx * (count - 1))).coerceAtLeast(0) / count

        var xCursor = 0
        val placeables = measurables.mapIndexed { index, measurable ->
            val widthPx = if (index == measurables.lastIndex) {
                (totalWidth - xCursor).coerceAtLeast(0)
            } else {
                colWidthPx
            }
            xCursor += widthPx + gapPx
            measurable.measure(Constraints.fixed(width = widthPx, height = touchHeightPx))
        }

        layout(width = totalWidth, height = chipVisualHeightPx) {
            var placeX = 0
            placeables.forEach { placeable ->
                placeable.placeRelative(x = placeX, y = -touchExtraVerticalPx)
                placeX += placeable.width + gapPx
            }
        }
    }
}

@Composable
private fun PaymentMethodChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val shape = RoundedCornerShape(20.dp)
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var chipWidthPx by remember { mutableIntStateOf(0) }
    val fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium

    Box(
        modifier = Modifier
            .height(48.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(shape)
                .background(SurfaceWhite)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) Brand else OwnerBorder,
                    shape = shape
                )
                .onSizeChanged { chipWidthPx = it.width }
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val horizontalPadPx = with(density) { 8.dp.toPx().toInt() }
            val availableTextWidthPx = (chipWidthPx - horizontalPadPx).coerceAtLeast(1)
            val textSize = remember(label, availableTextWidthPx, fontWeight, density.fontScale) {
                if (chipWidthPx <= 0) {
                    14.sp
                } else {
                    listOf(14f, 13f, 12f, 11f).firstOrNull { sizeSp ->
                        textMeasurer.measure(
                            text = label,
                            style = ownerTextStyle(
                                fontSize = sizeSp.sp,
                                lineHeight = 20.sp,
                                fontWeight = fontWeight,
                                color = Ink
                            ),
                            maxLines = 1,
                            softWrap = false
                        ).size.width <= availableTextWidthPx
                    }?.sp ?: 11.sp
                }
            }

            Text(
                text = label,
                style = ownerTextStyle(
                    fontSize = textSize,
                    lineHeight = if (textSize == 14.sp) 20.sp else (textSize.value * 1.4f).sp,
                    fontWeight = fontWeight,
                    color = if (selected) Brand else Ink
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun LabelValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    labelColor: Color = Muted,
    valueColor: Color = Ink,
    valueTestTag: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                color = labelColor
            ),
            maxLines = 1,
            modifier = Modifier.cssLineBoxHeight(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = ownerTextStyle(
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
                tabularNumbers = true
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f, fill = false)
                .cssLineBoxHeight(20.dp)
                .let { if (valueTestTag != null) it.testTag(valueTestTag) else it }
        )
    }
}

@Composable
internal fun StockImpact(
    calculation: OwnerBarangMasukCalculation,
    unitLabel: String,
    onGreenBackground: Boolean,
    modifier: Modifier = Modifier
) {
    val lineColor = if (onGreenBackground) Color.White.copy(alpha = 0.18f) else OwnerDivider
    val titleColor = if (onGreenBackground) BrandSoftText else Muted
    val labelColor = if (onGreenBackground) BrandSoftText else Muted
    val valueColor = if (onGreenBackground) OnBrand else Ink

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(lineColor)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Setelah barang masuk",
            style = ownerTextStyle(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor
            ),
            maxLines = 1,
            modifier = Modifier.cssLineBoxHeight(18.dp)
        )

        if (calculation.hasImpactPreview) {
            Spacer(modifier = Modifier.height(10.dp))
            LabelValueRow(
                label = "Stok",
                value = "${formatStockWhole(calculation.oldStock)} \u2192 ${formatStockWhole(calculation.newStock)} $unitLabel",
                labelColor = labelColor,
                valueColor = valueColor,
                valueTestTag = if (onGreenBackground) "barang-masuk-tablet-impact-stock" else "barang-masuk-phone-impact-stock"
            )
            Spacer(modifier = Modifier.height(8.dp))
            val cogsText = if (onGreenBackground) {
                formatRupiahWhole(calculation.newCogs)
            } else {
                "${formatRupiahWhole(calculation.oldCogs)} \u2192 ${formatRupiahWhole(calculation.newCogs)}"
            }
            LabelValueRow(
                label = "HPP rata-rata",
                value = cogsText,
                labelColor = labelColor,
                valueColor = valueColor,
                valueTestTag = if (onGreenBackground) "barang-masuk-tablet-impact-hpp" else "barang-masuk-phone-impact-hpp"
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Isi jumlah dan harga satuan untuk melihat perkiraan.",
                style = ownerTextStyle(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = labelColor
                )
            )
        }
    }
}

@Composable
internal fun FormFooter(
    isTablet: Boolean,
    enabled: Boolean,
    isSubmitting: Boolean,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val startPad = if (isTablet) 64.dp else 20.dp
    val endPad = if (isTablet) 32.dp else 20.dp
    val active = enabled && !isSubmitting
    val buttonShape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceWhite)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OwnerDivider)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isTablet) Modifier else Modifier.navigationBarsPadding())
                .padding(start = startPad, top = 12.dp, end = endPad, bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(buttonShape)
                    .background(if (active || isSubmitting) Brand else OwnerBorder)
                    .clickable(
                        enabled = active,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onSaveClick
                    )
                    .semantics { contentDescription = "Simpan barang masuk" }
                    .testTag("barang-masuk-submit"),
                contentAlignment = Alignment.Center
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .testTag("barang-masuk-submit-loading"),
                        color = OnBrand,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Simpan barang masuk",
                        style = ownerTextStyle(
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (active) OnBrand else FormPlaceholderColor
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
internal fun ProductPickerModal(
    products: List<ProductStock>,
    selectedProductId: String?,
    onSelect: (ProductStock) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(products, query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            products
        } else {
            products.filter {
                it.productName.contains(trimmed, ignoreCase = true) ||
                    (it.sku?.contains(trimmed, ignoreCase = true) == true) ||
                    it.categoryName.contains(trimmed, ignoreCase = true)
            }
        }
    }

    PickerSheetContainer(
        title = "Pilih produk",
        searchQuery = query,
        searchPlaceholder = "Cari nama produk atau SKU...",
        onSearchChange = { query = it },
        onDismiss = onDismiss,
        emptyText = "Produk tidak ditemukan",
        itemCount = filtered.size
    ) {
        itemsIndexed(filtered, key = { _, item -> item.productId }) { index, item ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(OwnerDivider)
                )
            }
            val unitLabel = item.unitName?.takeIf { it.isNotBlank() } ?: "Pieces"
            val isSelected = item.productId == selectedProductId
            PickerOptionRow(
                title = item.productName,
                subtitle = "${item.sku ?: "-"} \u00B7 Stok ${formatStockWhole(item.quantity)} $unitLabel",
                selected = isSelected,
                onClick = { onSelect(item) },
                testTag = "product-picker-item-${item.productId}"
            )
        }
    }
}

@Composable
internal fun SupplierPickerModal(
    suppliers: List<Supplier>,
    selectedSupplierId: String?,
    onSelect: (Supplier) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(suppliers, query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            suppliers
        } else {
            suppliers.filter {
                it.name.contains(trimmed, ignoreCase = true) ||
                    (it.phone?.contains(trimmed, ignoreCase = true) == true)
            }
        }
    }

    PickerSheetContainer(
        title = "Pilih supplier",
        searchQuery = query,
        searchPlaceholder = "Cari nama atau nomor supplier...",
        onSearchChange = { query = it },
        onDismiss = onDismiss,
        emptyText = "Supplier tidak ditemukan",
        itemCount = filtered.size
    ) {
        itemsIndexed(filtered, key = { _, item -> item.id }) { index, item ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(OwnerDivider)
                )
            }
            val termText = if (item.paymentTermDays > 0) "Termin ${item.paymentTermDays} hari" else "Tanpa termin"
            val isSelected = item.id == selectedSupplierId
            PickerOptionRow(
                title = item.name,
                subtitle = termText,
                selected = isSelected,
                onClick = { onSelect(item) },
                testTag = "supplier-picker-item-${item.id}"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerSheetContainer(
    title: String,
    searchQuery: String,
    searchPlaceholder: String,
    onSearchChange: (String) -> Unit,
    onDismiss: () -> Unit,
    emptyText: String,
    itemCount: Int,
    itemsContent: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    val config = LocalConfiguration.current
    val isTablet = config.screenWidthDp >= 840
    val density = LocalDensity.current
    val screenHeight = config.screenHeightDp.dp

    val body: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isTablet) 64.dp else 48.dp)
                    .padding(start = if (isTablet) 24.dp else 20.dp, end = if (isTablet) 12.dp else 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = ownerTextStyle(
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink
                    ),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = onDismiss
                        )
                        .semantics { contentDescription = "Tutup" },
                    contentAlignment = Alignment.Center
                ) {
                    OwnerVectorIcon(
                        imageVector = OwnerIcons.CloseX,
                        tint = Muted,
                        size = 24.dp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isTablet) 24.dp else 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceWhite)
                        .border(1.dp, OwnerBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OwnerVectorIcon(
                        imageVector = OwnerIcons.Search,
                        tint = Muted,
                        size = 20.dp
                    )
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = searchPlaceholder,
                                style = ownerTextStyle(
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = FormPlaceholderColor
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            singleLine = true,
                            textStyle = ownerTextStyle(
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Normal,
                                color = Ink
                            ),
                            cursorBrush = SolidColor(Brand),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    role = Role.Button,
                                    onClick = { onSearchChange("") }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            OwnerVectorIcon(
                                imageVector = OwnerIcons.CloseX,
                                tint = Muted,
                                size = 18.dp
                            )
                        }
                    }
                }
            }

            if (itemCount == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyText,
                        style = ownerTextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Normal,
                            color = Muted
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = if (isTablet) 420.dp else 460.dp)
                        .padding(horizontal = if (isTablet) 24.dp else 20.dp)
                ) {
                    itemsContent()
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    if (isTablet) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x7A14201B))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(520.dp)
                        .heightIn(max = (screenHeight - 48.dp).coerceAtLeast(320.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(SurfaceWhite)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                ) {
                    body()
                }
            }
        }
    } else {
        val statusBarTopDp = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
        val sheetHeight = (screenHeight - maxOf(56.dp, statusBarTopDp + 8.dp)).coerceAtLeast(320.dp)
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = SurfaceWhite,
            tonalElevation = 0.dp,
            scrimColor = Color(0x7A14201B),
            dragHandle = null,
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetHeight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFD5DAD7))
                    )
                }
                body()
            }
        }
    }
}

@Composable
private fun PickerOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = ownerTextStyle(
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) Brand else Ink
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = ownerTextStyle(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Muted,
                    tabularNumbers = true
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (selected) {
            OwnerVectorIcon(
                imageVector = OwnerIcons.CheckPlain,
                tint = Brand,
                size = 20.dp
            )
        }
    }
}

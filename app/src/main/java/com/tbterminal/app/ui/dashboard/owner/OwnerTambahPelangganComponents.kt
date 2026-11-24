package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.FieldBorder
import com.tbterminal.app.ui.theme.FieldErrorColor
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.OwnerBorder
import com.tbterminal.app.ui.theme.OwnerDivider
import com.tbterminal.app.ui.theme.SurfaceWhite

internal val FormPlaceholderColor = Color(0xFF8A938E)

@Immutable
internal data class RadioCardOption<T>(
    val value: T,
    val title: String,
    val subtitle: String,
    val testTag: String
)

internal fun validateOwnerCustomerName(rawName: String): String? {
    val trimmed = rawName.trim()
    return when {
        trimmed.isEmpty() -> "Nama pelanggan wajib diisi."
        trimmed.length < 2 -> "Nama pelanggan minimal 2 karakter."
        trimmed.length > 120 -> "Nama maksimal 120 karakter."
        else -> null
    }
}

internal fun validateOwnerCustomerPhone(rawPhone: String): String? {
    if (rawPhone.isEmpty()) return null
    val valid = rawPhone.all(Char::isDigit) &&
        rawPhone.length in 10..13 &&
        (rawPhone.startsWith("08") || rawPhone.startsWith("62"))
    return if (valid) null else "Nomor HP tidak valid."
}

internal fun formatThousandsDots(digits: String): String {
    if (digits.isEmpty()) return ""
    val sb = StringBuilder(digits.length + digits.length / 3)
    var count = 0
    for (i in digits.lastIndex downTo 0) {
        if (count > 0 && count % 3 == 0) {
            sb.append('.')
        }
        sb.append(digits[i])
        count++
    }
    return sb.reverse().toString()
}

internal object ThousandSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val formatted = formatThousandsDots(raw)
        val n = raw.length
        val origToTrans = IntArray(n + 1)
        val transToOrig = IntArray(formatted.length + 1)
        var origIdx = 0
        for (tIdx in formatted.indices) {
            transToOrig[tIdx] = origIdx
            if (formatted[tIdx] != '.') {
                origToTrans[origIdx] = tIdx
                origIdx++
            }
        }
        origToTrans[n] = formatted.length
        transToOrig[formatted.length] = n

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return origToTrans[offset.coerceIn(0, n)]
            }

            override fun transformedToOriginal(offset: Int): Int {
                return transToOrig[offset.coerceIn(0, formatted.length)]
            }
        }
        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

@Composable
internal fun TambahPelangganTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backTestTag: String = "tambah-pelanggan-back-btn"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier.size(width = 36.dp, height = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .wrapContentSize(align = Alignment.CenterEnd, unbounded = true)
                    .size(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onBack
                    )
                    .semantics { contentDescription = "Kembali" }
                    .testTag(backTestTag),
                contentAlignment = Alignment.Center
            ) {
                OwnerVectorIcon(
                    imageVector = OwnerIcons.ChevronLeft,
                    tint = OnBrand,
                    size = 24.dp
                )
            }
        }

        Text(
            text = title,
            style = ownerTextStyle(
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnBrand,
                letterSpacing = (-0.01).em
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .cssLineBoxHeight(22.dp)
                .semantics { heading() }
        )
    }
}

@Composable
internal fun <T> RadioCardGroup(
    options: List<RadioCardOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.forEach { option ->
            RadioCard(
                title = option.title,
                subtitle = option.subtitle,
                selected = option.value == selected,
                enabled = enabled,
                onClick = { onSelect(option.value) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag(option.testTag)
            )
        }
    }
}

@Composable
internal fun RadioCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderWidth = if (selected) 1.5.dp else 1.dp
    val borderColor = if (selected) Brand else OwnerBorder
    val horizontalPad = if (selected) 12.5.dp else 13.dp
    val verticalPad = if (selected) 14.5.dp else 15.dp

    Row(
        modifier = modifier
            .heightIn(min = 76.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceWhite)
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .selectable(
                selected = selected,
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = horizontalPad, vertical = verticalPad),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(SurfaceWhite)
                .border(
                    width = if (selected) 6.dp else 1.5.dp,
                    color = if (selected) Brand else FieldBorder,
                    shape = CircleShape
                )
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = ownerTextStyle(
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                ),
                modifier = Modifier.cssLineBoxHeight(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = ownerTextStyle(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Muted
                )
            )
        }
    }
}

@Composable
internal fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    optional: Boolean = false,
    prefixText: String? = null,
    suffixText: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    fieldHeight: Dp = 56.dp,
    multilineTopPadding: Dp = 16.dp,
    tabularNumbers: Boolean = false,
    forceFocusedBorder: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onBlur: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    labelEndAction: (@Composable () -> Unit)? = null,
    helperTopPadding: Dp = 6.dp,
    showErrorIcon: Boolean = true,
    testTag: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var hadFocus by remember { mutableStateOf(false) }

    val isError = errorText != null
    val isThickBorder = isError || isFocused || forceFocusedBorder
    val borderWidth = if (isThickBorder) 1.5.dp else 1.dp
    val borderColor = when {
        isError -> FieldErrorColor
        isFocused || forceFocusedBorder -> Brand
        else -> OwnerBorder
    }
    val horizontalPad = if (isThickBorder) 15.5.dp else 16.dp
    val topPad = if (singleLine) 0.dp else multilineTopPadding

    val labelAnnotated = remember(label, optional) {
        buildAnnotatedString {
            append(label)
            if (optional) {
                append(" ")
                withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = Muted)) {
                    append("(opsional)")
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (labelEndAction != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = labelAnnotated,
                    style = ownerTextStyle(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Ink
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .cssLineBoxHeight(20.dp)
                )
                labelEndAction()
            }
        } else {
            Text(
                text = labelAnnotated,
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
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            textStyle = ownerTextStyle(
                fontSize = 16.sp,
                lineHeight = if (singleLine) 20.sp else 22.sp,
                fontWeight = FontWeight.Normal,
                color = Ink,
                tabularNumbers = tabularNumbers
            ),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(if (isError) FieldErrorColor else Brand),
            modifier = Modifier
                .fillMaxWidth()
                .height(fieldHeight)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        hadFocus = true
                    } else if (hadFocus) {
                        onBlur?.invoke()
                    }
                }
                .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it },
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceWhite)
                        .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
                        .padding(
                            start = horizontalPad,
                            top = topPad,
                            end = horizontalPad,
                            bottom = if (singleLine) 0.dp else 12.dp
                        ),
                    verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top
                ) {
                    if (prefixText != null) {
                        Text(
                            text = prefixText,
                            style = ownerTextStyle(
                                fontSize = 16.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Normal,
                                color = Muted
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = ownerTextStyle(
                                    fontSize = 16.sp,
                                    lineHeight = if (singleLine) 20.sp else 22.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = FormPlaceholderColor,
                                    tabularNumbers = tabularNumbers
                                ),
                                maxLines = if (singleLine) 1 else 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }

                    if (suffixText != null) {
                        Text(
                            text = suffixText,
                            style = ownerTextStyle(
                                fontSize = 16.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Normal,
                                color = Muted
                            ),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    trailingContent?.invoke()
                }
            }
        )

        if (errorText != null) {
            Spacer(modifier = Modifier.height(helperTopPadding))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(if (testTag.isNotEmpty()) "$testTag-error" else ""),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showErrorIcon) {
                    OwnerVectorIcon(
                        imageVector = OwnerIcons.ErrorCircle,
                        tint = FieldErrorColor,
                        size = 16.dp
                    )
                }
                Text(
                    text = errorText,
                    style = ownerTextStyle(
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = FieldErrorColor
                    )
                )
            }
        } else if (helperText != null) {
            Text(
                text = helperText,
                style = ownerTextStyle(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Muted
                ),
                modifier = Modifier.padding(top = helperTopPadding)
            )
        }
    }
}

@Composable
internal fun SaveCustomerButton(
    enabled: Boolean,
    isSaving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Simpan pelanggan",
    buttonTestTag: String = "tambah-pelanggan-save",
    loadingTestTag: String = "tambah-pelanggan-loading"
) {
    val active = enabled && !isSaving
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (active || isSaving) Brand else OwnerBorder)
            .clickable(
                enabled = active,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = label }
            .testTag(buttonTestTag),
        contentAlignment = Alignment.Center
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(20.dp)
                    .testTag(loadingTestTag),
                color = OnBrand,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = label,
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) OnBrand else FormPlaceholderColor
                ),
                modifier = Modifier.cssLineBoxHeight(20.dp)
            )
        }
    }
}

@Composable
internal fun StickyActionBar(
    enabled: Boolean,
    isSaving: Boolean,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonLabel: String = "Simpan pelanggan",
    buttonTestTag: String = "tambah-pelanggan-save",
    loadingTestTag: String = "tambah-pelanggan-loading"
) {
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
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp)
        ) {
            SaveCustomerButton(
                enabled = enabled,
                isSaving = isSaving,
                onClick = onSaveClick,
                label = buttonLabel,
                buttonTestTag = buttonTestTag,
                loadingTestTag = loadingTestTag
            )
        }
    }
}

@Composable
internal fun CustomerFormSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("customer-form-skeleton"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkeletonBox(
                modifier = Modifier.weight(1f).height(76.dp),
                shape = RoundedCornerShape(14.dp)
            )
            SkeletonBox(
                modifier = Modifier.weight(1f).height(76.dp),
                shape = RoundedCornerShape(14.dp)
            )
        }
        repeat(3) {
            SkeletonBox(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }
        SkeletonBox(
            modifier = Modifier.fillMaxWidth().height(72.dp),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
internal fun DiscardCustomerDialog(
    onContinueEditing: () -> Unit,
    onDiscard: () -> Unit,
    title: String = "Buang data pelanggan?",
    message: String = "Data yang sudah diisi akan hilang.",
    dialogTestTag: String = "discard-customer-dialog",
    continueTestTag: String = "discard-customer-continue",
    discardTestTag: String = "discard-customer-confirm"
) {
    Dialog(onDismissRequest = onContinueEditing) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceWhite)
                .padding(24.dp)
                .testTag(dialogTestTag)
        ) {
            Text(
                text = title,
                style = ownerTextStyle(
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = ownerTextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = Muted
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = onContinueEditing
                        )
                        .padding(horizontal = 16.dp)
                        .testTag(continueTestTag),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Lanjut mengisi",
                        style = ownerTextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Brand
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FieldErrorColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = onDiscard
                        )
                        .padding(horizontal = 20.dp)
                        .testTag(discardTestTag),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Buang",
                        style = ownerTextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnBrand
                        )
                    )
                }
            }
        }
    }
}

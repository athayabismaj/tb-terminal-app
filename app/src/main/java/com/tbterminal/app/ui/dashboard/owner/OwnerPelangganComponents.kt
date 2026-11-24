package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.ui.customers.CustomerListUiState
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.BrandSoftText
import com.tbterminal.app.ui.theme.ChipBg
import com.tbterminal.app.ui.theme.FieldErrorColor
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.OwnerBorder
import com.tbterminal.app.ui.theme.Placeholder
import com.tbterminal.app.ui.theme.SurfaceWhite
import java.math.BigDecimal
import java.util.Locale

internal val CustomerActiveStatusDot = Color(0xFF1F9D63)

internal enum class CustomerSortOrder(val label: String) {
    NAME_ASC("Nama A\u2013Z"),
    NAME_DESC("Nama Z\u2013A"),
    CREDIT_DESC("Limit tertinggi")
}

internal enum class PelangganFooterStatus {
    NONE,
    LOADING,
    END_OF_LIST,
    ERROR
}

@Immutable
internal data class PelangganSummaryUiState(
    val totalCustomers: Long = 0L,
    val generalCount: Int = 0,
    val contractorCount: Int = 0
)

internal fun CustomerListUiState.toPelangganSummary(): PelangganSummaryUiState {
    // TODO: Gunakan angka ringkasan agregat seluruh halaman saat CustomerRepository/CustomerListUiState menyediakannya (saat ini dihitung dari halaman aktif).
    return PelangganSummaryUiState(
        totalCustomers = totalCustomers,
        generalCount = customers.count { !it.isContractor },
        contractorCount = contractorCount
    )
}

/**
 * Header top bar halaman Pelanggan: tinggi 48dp, tombol kembali chevron (area sentuh 48x48dp, margin kiri -12dp),
 * judul "Pelanggan" 20/600, serta ikon Cari dan Filter 24dp putih (area sentuh 48x48dp, Filter margin kanan -12dp).
 */
@Composable
internal fun SecondaryHeader(
    title: String,
    isFiltered: Boolean,
    onBack: () -> Unit,
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
    backTestTag: String = "pelanggan-back-btn",
    searchTestTag: String = "customer-toggle-search",
    filterTestTag: String = "customer-open-filters",
    searchContentDescription: String = "Cari pelanggan",
    filterContentDescription: String = "Filter pelanggan",
    showFilterAction: Boolean = true
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

        val searchButton: @Composable (Modifier) -> Unit = { buttonModifier ->
            Box(
                modifier = buttonModifier
                    .size(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onSearchClick
                    )
                    .semantics { contentDescription = searchContentDescription }
                    .testTag(searchTestTag),
                contentAlignment = Alignment.Center
            ) {
                OwnerVectorIcon(
                    imageVector = OwnerIcons.Search,
                    tint = OnBrand,
                    size = 24.dp
                )
            }
        }

        if (showFilterAction) {
            searchButton(Modifier)

            Box(
                modifier = Modifier.size(width = 36.dp, height = 48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .wrapContentSize(align = Alignment.CenterStart, unbounded = true)
                        .size(48.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = onFilterClick
                        )
                        .semantics { contentDescription = filterContentDescription }
                        .testTag(filterTestTag),
                    contentAlignment = Alignment.Center
                ) {
                    OwnerVectorIcon(
                        imageVector = OwnerIcons.FilterSliders,
                        tint = OnBrand,
                        size = 24.dp
                    )
                    if (isFiltered) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 10.dp, end = 10.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF2C14E))
                                .border(1.5.dp, Brand, CircleShape)
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.size(width = 36.dp, height = 48.dp)
            ) {
                searchButton(Modifier.wrapContentSize(align = Alignment.CenterStart, unbounded = true))
            }
        }
    }
}

/**
 * Mode pencarian di baris judul header hijau: tombol kembali menutup pencarian, kolom putih
 * tinggi 48dp radius 12dp dengan ikon cari 20dp dan tombol hapus saat ada teks.
 */
@Composable
internal fun SearchTopBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = true,
    placeholder: String = "Cari nama atau nomor HP...",
    backTestTag: String = "customer-search-back",
    inputTestTag: String = "customer-search-input",
    clearTestTag: String = "customer-search-clear"
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

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
                        onClick = onCloseSearch
                    )
                    .semantics { contentDescription = "Tutup pencarian" }
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

        Row(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceWhite)
                .padding(start = 14.dp, end = if (query.isNotEmpty()) 4.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OwnerVectorIcon(
                imageVector = OwnerIcons.Search,
                tint = Muted,
                size = 20.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = ownerTextStyle(
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Normal,
                            color = Placeholder
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChanged,
                    singleLine = true,
                    textStyle = ownerTextStyle(
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Ink
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { keyboardController?.hide() }
                    ),
                    cursorBrush = SolidColor(Brand),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag(inputTestTag)
                )
            }

            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 48.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = { onQueryChanged("") }
                        )
                        .semantics { contentDescription = "Hapus pencarian" }
                        .testTag(clearTestTag),
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
}

/**
 * Ringkasan hero di header hijau: Total pelanggan + angka 40/700 + satuan "orang" sejajar baseline,
 * garis pemisah putih 18%, lalu 2 kolom metrik (Umum, Kontraktor).
 */
@Composable
internal fun SummaryStats(
    summary: PelangganSummaryUiState,
    modifier: Modifier = Modifier,
    isTablet: Boolean = false
) {
    val lineColor = Color.White.copy(alpha = 0.18f)
    val countFontSize = if (isTablet) 48.sp else 40.sp
    val countLineHeight = if (isTablet) 54.sp else 46.sp
    val countBoxHeight = if (isTablet) 54.dp else 46.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Total pelanggan",
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                color = BrandSoftText
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.cssLineBoxHeight(20.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = summary.totalCustomers.toString(),
                style = ownerTextStyle(
                    fontSize = countFontSize,
                    lineHeight = countLineHeight,
                    fontWeight = FontWeight.Bold,
                    color = OnBrand,
                    letterSpacing = (-0.03).em,
                    tabularNumbers = true
                ),
                maxLines = 1,
                modifier = Modifier
                    .cssLineBoxHeight(countBoxHeight)
                    .alignByBaseline()
                    .testTag("pelanggan-total-count")
            )

            Text(
                text = "orang",
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = BrandSoftText
                ),
                maxLines = 1,
                modifier = Modifier
                    .cssLineBoxHeight(20.dp)
                    .alignByBaseline()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(lineColor)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            PelangganStatColumn(
                label = "Umum",
                value = "${summary.generalCount} orang",
                modifier = Modifier.weight(1f)
            )
            PelangganStatColumn(
                label = "Kontraktor",
                value = "${summary.contractorCount} orang",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .ownerHeroLeftDivider(lineColor)
            )
        }
    }
}

@Composable
internal fun PelangganStatColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = ownerTextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal,
                color = BrandSoftText
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.cssLineBoxHeight(16.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = ownerTextStyle(
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnBrand,
                tabularNumbers = true
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.cssLineBoxHeight(20.dp)
        )
    }
}

/**
 * Keadaan kosong / pencarian tanpa hasil sesuai HTML:
 * lingkaran 80dp + ikon 36dp, judul 18/600 (20dp bawah ikon), keterangan 14 muted (8dp bawah judul),
 * tombol "Tambah pelanggan" tinggi 56dp radius 16dp (24dp bawah keterangan), padding bawah 48dp.
 */
@Composable
internal fun EmptyState(
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
    title: String,
    description: String,
    showAddButton: Boolean,
    onAddCustomerClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonTestTag: String = "customer-add",
    buttonLabel: String = "Tambah pelanggan"
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(iconContainerColor),
            contentAlignment = Alignment.Center
        ) {
            OwnerVectorIcon(
                imageVector = icon,
                tint = iconTint,
                size = 36.dp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
            style = ownerTextStyle(
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                color = Muted
            ),
            textAlign = TextAlign.Center
        )

        if (showAddButton) {
            Spacer(modifier = Modifier.height(24.dp))

            AddPelangganPrimaryButton(
                onClick = onAddCustomerClick,
                testTag = buttonTestTag,
                label = buttonLabel
            )
        }
    }
}

/**
 * Tombol utama "Tambah pelanggan": lebar penuh, tinggi 56dp, radius 16dp, latar brand,
 * ikon plus 22dp + teks 16/600 putih.
 */
@Composable
internal fun AddPelangganPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "customer-add",
    label: String = "Tambah pelanggan"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brand)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = label }
            .testTag(testTag),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OwnerVectorIcon(
            imageVector = OwnerIcons.Plus,
            tint = OnBrand,
            size = 22.dp
        )
        Text(
            text = label,
            style = ownerTextStyle(
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnBrand
            ),
            maxLines = 1
        )
    }
}

internal fun formatPelangganInitials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase(Locale.ROOT)
        words.size == 1 -> words[0].take(1).uppercase(Locale.ROOT)
        else -> "P"
    }
}

internal fun formatCustomerPhoneDisplay(phone: String?): String {
    val trimmed = phone?.trim().orEmpty()
    if (trimmed.isEmpty()) return "Tanpa nomor HP"
    val digits = trimmed.filter(Char::isDigit)
    val looksNumeric = trimmed.all { it.isDigit() || it in setOf('-', ' ', '+', '(', ')') }
    return if (looksNumeric && digits.length >= 8) {
        digits.chunked(4).joinToString("-")
    } else {
        trimmed
    }
}

@Composable
internal fun StatusAvatar(
    name: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val statusText = if (isActive) "$name, aktif" else "$name, nonaktif"
    Box(
        modifier = modifier
            .size(40.dp)
            .semantics(mergeDescendants = true) { contentDescription = statusText }
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceWhite)
                .border(1.dp, OwnerBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formatPelangganInitials(name),
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isActive) Brand else Muted
                ),
                maxLines = 1,
                modifier = Modifier.clearAndSetSemantics { }
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 1.dp, y = 1.dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(if (isActive) CustomerActiveStatusDot else FieldErrorColor)
                .border(2.dp, SurfaceWhite, CircleShape)
        )
    }
}

@Composable
internal fun CustomerRow(
    customer: Customer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeLabel = if (customer.isContractor) "Kontraktor" else "Umum"
    val phoneLabel = formatCustomerPhoneDisplay(customer.phone)
    val hasCreditLimit = customer.creditLimit > BigDecimal.ZERO

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(vertical = 16.dp)
            .testTag("customer-row-${customer.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusAvatar(
            name = customer.name,
            isActive = customer.isActive
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = customer.name,
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (customer.isActive) Ink else Muted
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.cssLineBoxHeight(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$typeLabel \u00B7 $phoneLabel",
                style = ownerTextStyle(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Muted,
                    tabularNumbers = true
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.cssLineBoxHeight(18.dp)
            )
        }

        if (hasCreditLimit) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatRupiahCompact(customer.creditLimit.toDouble()),
                    style = ownerTextStyle(
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                        tabularNumbers = true
                    ),
                    maxLines = 1,
                    modifier = Modifier.cssLineBoxHeight(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Limit \u00B7 ${customer.paymentTermDays} hari",
                    style = ownerTextStyle(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Muted,
                        tabularNumbers = true
                    ),
                    maxLines = 1,
                    modifier = Modifier.cssLineBoxHeight(16.dp)
                )
            }
        }

        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            OwnerVectorIcon(
                imageVector = OwnerIcons.ChevronRight,
                tint = Muted,
                size = 20.dp
            )
        }
    }
}

@Composable
internal fun ListFooterState(
    status: PelangganFooterStatus,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    loadingText: String = "Memuat pelanggan lainnya\u2026",
    endText: String = "Semua pelanggan sudah ditampilkan",
    footerTestTag: String = "pelanggan-list-footer",
    retryTestTag: String = "pelanggan-footer-retry"
) {
    if (status == PelangganFooterStatus.NONE) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag(footerTestTag),
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            PelangganFooterStatus.LOADING -> {
                val transition = rememberInfiniteTransition(label = "pelangganFooterSpinner")
                val rotation by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 900, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "pelangganFooterRotation"
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(20.dp)) {
                        val strokePx = 2.5.dp.toPx()
                        val diameter = size.minDimension - strokePx
                        val topLeft = Offset(strokePx / 2f, strokePx / 2f)
                        val arcSize = Size(diameter, diameter)
                        drawArc(
                            color = OwnerBorder,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx)
                        )
                        drawArc(
                            color = Brand,
                            startAngle = -90f + rotation,
                            sweepAngle = 90f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = loadingText,
                        style = ownerTextStyle(
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = Muted
                        )
                    )
                }
            }

            PelangganFooterStatus.END_OF_LIST -> {
                Text(
                    text = endText,
                    style = ownerTextStyle(
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = Muted
                    ),
                    textAlign = TextAlign.Center
                )
            }

            PelangganFooterStatus.ERROR -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gagal memuat",
                        style = ownerTextStyle(
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = Muted
                        )
                    )
                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Button,
                                onClick = onRetry
                            )
                            .padding(horizontal = 8.dp)
                            .testTag(retryTestTag),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Coba lagi",
                            style = ownerTextStyle(
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Brand
                            )
                        )
                    }
                }
            }

            PelangganFooterStatus.NONE -> Unit
        }
    }
}

internal fun emptyStateSpecFor(
    searchQuery: String,
    isFiltered: Boolean
): PelangganEmptySpec {
    val trimmedQuery = searchQuery.trim()
    return when {
        trimmedQuery.isNotEmpty() -> PelangganEmptySpec(
            icon = OwnerIcons.Search,
            iconContainerColor = ChipBg,
            iconTint = Muted,
            title = "\"$trimmedQuery\" tidak ditemukan",
            description = "Periksa ejaan atau cari dengan nomor HP.",
            isSearchNotFound = true
        )
        isFiltered -> PelangganEmptySpec(
            icon = OwnerIcons.Search,
            iconContainerColor = ChipBg,
            iconTint = Muted,
            title = "Pelanggan tidak ditemukan",
            description = "Ubah pilihan filter atau tambahkan pelanggan baru.",
            isSearchNotFound = true
        )
        else -> PelangganEmptySpec(
            icon = OwnerIcons.CustomersEmptyGroup,
            iconContainerColor = SurfaceWhite,
            iconTint = Brand,
            title = "Belum ada pelanggan",
            description = "Tambahkan pelanggan untuk mencatat piutang, limit kredit, dan riwayat belanja.",
            isSearchNotFound = false
        )
    }
}

@Immutable
internal data class PelangganEmptySpec(
    val icon: ImageVector,
    val iconContainerColor: Color,
    val iconTint: Color,
    val title: String,
    val description: String,
    val isSearchNotFound: Boolean
)

internal object OwnerCustomerToastBridge {
    private var pendingMessage: String? = null

    fun setPendingSuccess(message: String) {
        pendingMessage = message
    }

    fun consumePendingSuccess(): String? {
        val value = pendingMessage
        pendingMessage = null
        return value
    }
}

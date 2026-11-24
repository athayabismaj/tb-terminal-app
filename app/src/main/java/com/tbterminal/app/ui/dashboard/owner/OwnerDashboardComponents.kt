package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.BrandSoftText
import com.tbterminal.app.ui.theme.ChipBg
import com.tbterminal.app.ui.theme.FieldBorder
import com.tbterminal.app.ui.theme.FieldErrorColor
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.KawanSans
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.OwnerAmberBg
import com.tbterminal.app.ui.theme.OwnerAmberInk
import com.tbterminal.app.ui.theme.OwnerBarColor
import com.tbterminal.app.ui.theme.OwnerBorder
import com.tbterminal.app.ui.theme.OwnerDivider
import com.tbterminal.app.ui.theme.OwnerNavBorder
import com.tbterminal.app.ui.theme.OwnerOkBg
import com.tbterminal.app.ui.theme.OwnerSyncDot
import com.tbterminal.app.ui.theme.SurfaceWhite
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun ownerTextStyle(
    fontSize: TextUnit,
    lineHeight: TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Ink,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle = FontStyle.Normal,
    tabularNumbers: Boolean = false
): TextStyle = TextStyle(
    fontFamily = KawanSans,
    fontSize = fontSize,
    fontWeight = fontWeight,
    fontStyle = fontStyle,
    lineHeight = lineHeight,
    color = color,
    letterSpacing = letterSpacing,
    fontFeatureSettings = if (tabularNumbers) "tnum" else null,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    )
)

@Composable
internal fun OwnerVectorIcon(
    imageVector: ImageVector,
    tint: Color,
    size: Dp,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Image(
        painter = rememberVectorPainter(imageVector),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size)
    )
}

@Composable
internal fun OwnerNoRippleClickable(
    onClick: () -> Unit,
    role: Role = Role.Button,
    enabled: Boolean = true
): Modifier = Modifier.clickable(
    enabled = enabled,
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    role = role,
    onClick = onClick
)

internal fun normalizeOwnerDisplayName(rawName: String): String {
    val trimmed = rawName.trim()
    return if (
        trimmed.isEmpty() ||
        trimmed.equals("owner", ignoreCase = true) ||
        trimmed.equals("pemilik", ignoreCase = true)
    ) {
        "Pemilik Toko"
    } else {
        trimmed
    }
}

/**
 * Row sapaan, pemilih periode, dan avatar akun dengan titik status sinkronisasi.
 * Tinggi baris 48dp; tombol avatar berukuran sentuh 48x48dp dengan lingkaran 40dp
 * sejajar tepi kanan konten.
 */
@Composable
internal fun OwnerGreetingRow(
    userName: String,
    synced: Boolean,
    selectedPeriod: Period,
    customRange: Pair<Long, Long>?,
    periodDropdownExpanded: Boolean,
    onOpenPeriodSelector: () -> Unit,
    periodDropdownFocusRequester: FocusRequester,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OwnerHeaderTitleRow(
        title = "Halo, $userName",
        userName = userName,
        synced = synced,
        onProfileClick = onProfileClick,
        modifier = modifier,
        trailingContent = {
            OwnerPeriodDropdown(
                selectedPeriod = selectedPeriod,
                customRange = customRange,
                expanded = periodDropdownExpanded,
                onClick = onOpenPeriodSelector,
                modifier = Modifier.focusRequester(periodDropdownFocusRequester)
            )
        }
    )
}

/**
 * Row judul header hub Owner dan avatar akun dengan titik status sinkronisasi.
 * Tinggi baris 48dp; mendukung aksi tambahan di kiri avatar (seperti kapsul dropdown).
 */
@Composable
internal fun OwnerHeaderTitleRow(
    title: String,
    userName: String,
    synced: Boolean,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleTestTag: String? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
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
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .semantics { heading() }
                    .then(if (titleTestTag != null) Modifier.testTag(titleTestTag) else Modifier)
            )
        }

        if (trailingContent != null) {
            trailingContent()
        }

        OwnerProfileAvatarButton(
            userName = userName,
            synced = synced,
            onClick = onProfileClick
        )
    }
}

/**
 * Tombol avatar akun seragam untuk semua halaman utama Owner: lingkaran 40dp transparan
 * dengan border putih 1dp alpha 0.28, inisial 16sp/SemiBold putih, titik sync 12dp,
 * dan area sentuh 48x48dp sejajar tepi kanan konten.
 */
@Composable
internal fun OwnerProfileAvatarButton(
    userName: String,
    synced: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initial = remember(userName) {
        userName.trim().firstOrNull()?.uppercase() ?: "P"
    }
    val syncLabel = if (synced) "tersinkron" else "offline"

    Box(
        modifier = modifier.size(width = 44.dp, height = 48.dp)
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize(align = Alignment.CenterStart, unbounded = true)
                .size(48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onClick
                )
                .semantics { contentDescription = "Akun $userName, $syncLabel" }
                .testTag("dashboard-owner-avatar-btn"),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(40.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.28f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        style = ownerTextStyle(
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnBrand
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (synced) OwnerSyncDot else FieldBorder)
                        .border(width = 2.dp, color = Brand, shape = CircleShape)
                        .testTag("owner-sync-dot")
                )
            }
        }
    }
}

/**
 * Label "Penjualan bersih" dengan tombol mata 48x48dp bermargin vertikal negatif (-14dp)
 * supaya tinggi barisnya tetap 20dp sesuai CSS.
 */
@Composable
internal fun OwnerNetSalesLabelRow(
    balanceHidden: Boolean,
    onToggleHidden: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Penjualan bersih"
) {
    Row(
        modifier = modifier.height(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                color = BrandSoftText
            )
        )

        Box(
            modifier = Modifier.size(width = 48.dp, height = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onToggleHidden
                    )
                    .semantics {
                        contentDescription = if (balanceHidden) "Tampilkan nominal" else "Sembunyikan nominal"
                    }
                    .testTag("owner-dashboard-toggle-privacy"),
                contentAlignment = Alignment.Center
            ) {
                OwnerVectorIcon(
                    imageVector = if (balanceHidden) OwnerIcons.EyeOff else OwnerIcons.Eye,
                    tint = BrandSoftText,
                    size = 20.dp
                )
            }
        }
    }
}

@Composable
internal fun OwnerDeltaChip(
    deltaPercent: Double,
    period: Period,
    modifier: Modifier = Modifier
) {
    val isPositive = deltaPercent >= 0.0
    val formattedPct = remember(deltaPercent) {
        val absPct = abs(deltaPercent)
        if (absPct % 1.0 == 0.0) "${absPct.roundToInt()}%" else "${String.format(java.util.Locale.US, "%.1f", absPct)}%"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.20f))
            .padding(start = 8.dp, top = 4.dp, end = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OwnerVectorIcon(
            imageVector = if (isPositive) OwnerIcons.TrendUp else OwnerIcons.TrendDown,
            tint = OnBrand,
            size = 14.dp
        )
        Text(
            text = "$formattedPct ${periodComparisonLabel(period)}",
            style = ownerTextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                color = OnBrand,
                tabularNumbers = true
            )
        )
    }
}

private val SelectorScrimColor = Ink.copy(alpha = 0.40f)
private val SelectorHandleColor = Color(0xFFD5DAD7)

/**
 * Kapsul dropdown seragam di header hijau: tinggi visual 36dp (area sentuh 48dp),
 * border putih 1dp (alpha 0.32 tertutup / 0.70 terbuka), latar transparan tanpa bayangan.
 */
@Composable
internal fun OwnerHeaderDropdownCapsule(
    label: String,
    expanded: Boolean,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderAlpha = if (expanded) 0.70f else 0.32f

    Box(
        modifier = Modifier.height(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = modifier
                .requiredHeight(48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.DropdownList,
                    onClick = onClick
                )
                .semantics {
                    this.contentDescription = contentDescription
                    stateDescription = if (expanded) "Terbuka" else "Tertutup"
                }
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = borderAlpha),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(start = 14.dp, end = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = ownerTextStyle(
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnBrand
                    ),
                    maxLines = 1,
                    softWrap = false
                )
                OwnerVectorIcon(
                    imageVector = OwnerIcons.ChevronDown,
                    tint = BrandSoftText,
                    size = 18.dp
                )
            }
        }
    }
}

internal data class OwnerSelectorOptionSpec<T>(
    val value: T,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconSize: Dp = 20.dp,
    val unselectedIconTint: Color = Ink,
    val testTag: String
)

/**
 * Dialog/sheet pemilih opsi bersama untuk hub Owner (bottom sheet di ponsel,
 * dialog tengah 480dp di tablet) dengan scrim #14201B alpha 0.40.
 */
@Composable
internal fun <T> OwnerSelectorDialog(
    title: String,
    options: List<OwnerSelectorOptionSpec<T>>,
    selectedValue: T,
    isTablet: Boolean,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    scrimTestTag: String,
    sheetTestTag: String,
    handleTestTag: String
) {
    val panelFocusRequester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        LaunchedEffect(Unit) {
            runCatching { panelFocusRequester.requestFocus() }
        }

        OwnerSelectorFrame(
            title = title,
            options = options,
            selectedValue = selectedValue,
            isTablet = isTablet,
            onSelect = onSelect,
            onDismiss = onDismiss,
            panelFocusRequester = panelFocusRequester,
            scrimTestTag = scrimTestTag,
            sheetTestTag = sheetTestTag,
            handleTestTag = handleTestTag
        )
    }
}

@Composable
internal fun <T> OwnerSelectorFrame(
    title: String,
    options: List<OwnerSelectorOptionSpec<T>>,
    selectedValue: T,
    isTablet: Boolean,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    scrimTestTag: String,
    sheetTestTag: String,
    handleTestTag: String,
    panelFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SelectorScrimColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onDismiss
                )
                .semantics { contentDescription = "Tutup $title" }
                .testTag(scrimTestTag)
        )

        val baseModifier = if (isTablet) {
            Modifier
                .align(Alignment.Center)
                .width(480.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceWhite)
                .pointerInput(Unit) { detectTapGestures { } }
        } else {
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(SurfaceWhite)
                .pointerInput(Unit) { detectTapGestures { } }
                .navigationBarsPadding()
        }
        val contentPaddingModifier = if (isTablet) {
            Modifier.padding(start = 24.dp, top = 28.dp, end = 24.dp, bottom = 24.dp)
        } else {
            Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 28.dp)
        }

        Column(
            modifier = baseModifier
                .then(if (panelFocusRequester != null) Modifier.focusRequester(panelFocusRequester) else Modifier)
                .focusable()
                .semantics { contentDescription = title }
                .testTag(sheetTestTag)
                .then(contentPaddingModifier)
        ) {
            if (!isTablet) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(SelectorHandleColor)
                        .testTag(handleTestTag)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            Text(
                text = title,
                style = ownerTextStyle(
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                ),
                modifier = Modifier.semantics { heading() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                options.forEach { spec ->
                    OwnerSelectorOptionRow(
                        spec = spec,
                        selected = spec.value == selectedValue,
                        onClick = { onSelect(spec.value) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun <T> OwnerSelectorOptionRow(
    spec: OwnerSelectorOptionSpec<T>,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleColor = if (selected) Brand else Ink
    val iconTint = if (selected) Brand else spec.unselectedIconTint

    val borderModifier = if (selected) {
        Modifier
            .border(width = 1.dp, color = Brand, shape = RoundedCornerShape(16.dp))
            .drawBehind {
                val strokePx = 1.dp.toPx()
                val halfStroke = strokePx * 1.5f
                val radiusPx = (16.dp.toPx() - halfStroke).coerceAtLeast(0f)
                drawRoundRect(
                    color = Brand,
                    topLeft = Offset(halfStroke, halfStroke),
                    size = Size(size.width - halfStroke * 2f, size.height - halfStroke * 2f),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = strokePx)
                )
            }
    } else {
        Modifier.border(width = 1.dp, color = OwnerBorder, shape = RoundedCornerShape(16.dp))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceWhite)
            .then(borderModifier)
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(spec.testTag),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(width = 1.dp, color = OwnerBorder, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            OwnerVectorIcon(
                imageVector = spec.icon,
                tint = iconTint,
                size = spec.iconSize
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = spec.title,
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = titleColor
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = spec.description,
                style = ownerTextStyle(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Muted
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (selected) {
            OwnerVectorIcon(
                imageVector = OwnerIcons.CheckPlain,
                tint = Brand,
                size = 22.dp
            )
        }
    }
}

internal fun Period.displayLabel(customRange: Pair<Long, Long>? = null): String = when (this) {
    Period.HARI -> "Hari"
    Period.MINGGU -> "Minggu"
    Period.BULAN -> "Bulan"
    Period.KUSTOM -> formatCustomRangeChipLabel(customRange)
}

@Composable
internal fun OwnerPeriodDropdown(
    selectedPeriod: Period,
    customRange: Pair<Long, Long>?,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeLabel = selectedPeriod.displayLabel(customRange)
    OwnerHeaderDropdownCapsule(
        label = activeLabel,
        expanded = expanded,
        contentDescription = "Periode: $activeLabel",
        testTag = "owner-period-dropdown",
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
internal fun buildPeriodSelectorOptions(
    selectedPeriod: Period,
    customRange: Pair<Long, Long>?
): List<OwnerSelectorOptionSpec<Period>> {
    val customDesc = if (selectedPeriod == Period.KUSTOM && customRange != null) {
        formatCustomRangeChipLabel(customRange)
    } else {
        "Pilih rentang tanggal"
    }
    return listOf(
        OwnerSelectorOptionSpec(
            value = Period.HARI,
            title = "Hari",
            description = "Ringkasan penjualan hari ini",
            icon = OwnerIcons.ActivityClock,
            testTag = "owner-period-HARI"
        ),
        OwnerSelectorOptionSpec(
            value = Period.MINGGU,
            title = "Minggu",
            description = "Ringkasan 7 hari terakhir",
            icon = OwnerIcons.Chart,
            testTag = "owner-period-MINGGU"
        ),
        OwnerSelectorOptionSpec(
            value = Period.BULAN,
            title = "Bulan",
            description = "Ringkasan 30 hari terakhir",
            icon = OwnerIcons.CalendarRange,
            testTag = "owner-period-BULAN"
        ),
        OwnerSelectorOptionSpec(
            value = Period.KUSTOM,
            title = "Kustom",
            description = customDesc,
            icon = OwnerIcons.FilterSliders,
            unselectedIconTint = Brand,
            testTag = "owner-period-KUSTOM"
        )
    )
}

@Composable
internal fun OwnerPeriodSelector(
    selectedPeriod: Period,
    customRange: Pair<Long, Long>?,
    isTablet: Boolean,
    onSelect: (Period) -> Unit,
    onDismiss: () -> Unit
) {
    OwnerSelectorDialog(
        title = "Pilih periode",
        options = buildPeriodSelectorOptions(selectedPeriod, customRange),
        selectedValue = selectedPeriod,
        isTablet = isTablet,
        onSelect = onSelect,
        onDismiss = onDismiss,
        scrimTestTag = "owner-period-selector-scrim",
        sheetTestTag = "owner-period-selector-sheet",
        handleTestTag = "owner-period-selector-handle"
    )
}

@Composable
internal fun OwnerPeriodSelectorFrame(
    selectedPeriod: Period,
    customRange: Pair<Long, Long>?,
    isTablet: Boolean,
    onSelect: (Period) -> Unit,
    onDismiss: () -> Unit
) {
    OwnerSelectorFrame(
        title = "Pilih periode",
        options = buildPeriodSelectorOptions(selectedPeriod, customRange),
        selectedValue = selectedPeriod,
        isTablet = isTablet,
        onSelect = onSelect,
        onDismiss = onDismiss,
        scrimTestTag = "owner-period-selector-scrim",
        sheetTestTag = "owner-period-selector-sheet",
        handleTestTag = "owner-period-selector-handle"
    )
}

@Composable
internal fun OwnerHeroSummaryStatsRow(
    transactionCount: Long,
    estimatedProfit: Double,
    cashIn: Double,
    balanceHidden: Boolean,
    isLoading: Boolean,
    isTablet: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.18f))
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OwnerHeroStatItem(
                label = "Transaksi",
                value = if (isLoading) "—" else transactionCount.toString(),
                modifier = Modifier.weight(1f)
            )
            OwnerHeroStatItem(
                label = "Estimasi laba",
                value = when {
                    isLoading -> "—"
                    balanceHidden -> "Rp ••••••"
                    else -> formatRupiahCompact(estimatedProfit)
                },
                modifier = Modifier.weight(1f)
            )
            OwnerHeroStatItem(
                label = "Kas masuk",
                value = when {
                    isLoading -> "—"
                    balanceHidden -> "Rp ••••••"
                    else -> formatRupiahCompact(cashIn)
                },
                valueFontStyle = if (!isTablet && !balanceHidden && !isLoading) FontStyle.Italic else FontStyle.Normal,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun OwnerHeroStatItem(
    label: String,
    value: String,
    valueFontStyle: FontStyle = FontStyle.Normal,
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
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = ownerTextStyle(
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnBrand,
                fontStyle = valueFontStyle,
                tabularNumbers = true
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun OwnerQuickActionsRow(
    isTablet: Boolean,
    onQuickAction: (QuickActionType) -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        Triple(QuickActionType.CATAT_BELI, "Catat beli", OwnerIcons.Truck),
        Triple(QuickActionType.CEK_STOK, "Cek stok", OwnerIcons.Box),
        Triple(QuickActionType.LAPORAN, "Laporan", OwnerIcons.Chart)
    )
    val boxHeight = if (isTablet) 72.dp else 64.dp
    val labelSize = if (isTablet) 14.sp else 13.sp
    val labelLineHeight = if (isTablet) 20.sp else 16.sp

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        actions.forEach { (type, label, icon) ->
            val isPrimary = type == QuickActionType.CATAT_BELI
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = { onQuickAction(type) }
                    )
                    .semantics { contentDescription = label }
                    .testTag("owner-qa-${type.name}"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(boxHeight)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isPrimary) Brand else ChipBg),
                    contentAlignment = Alignment.Center
                ) {
                    OwnerVectorIcon(
                        imageVector = icon,
                        tint = if (isPrimary) OnBrand else Ink,
                        size = 24.dp
                    )
                }

                Text(
                    text = label,
                    style = ownerTextStyle(
                        fontSize = labelSize,
                        lineHeight = labelLineHeight,
                        fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isPrimary) Brand else Ink
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun OwnerAttentionTilesRow(
    receivables: Double,
    stockStatus: StockStatus,
    balanceHidden: Boolean,
    isLoading: Boolean,
    onTileClick: (DashboardTileType) -> Unit,
    modifier: Modifier = Modifier
) {
    val isStockSafe = stockStatus is StockStatus.Safe
    val criticalCount = (stockStatus as? StockStatus.Critical)?.count ?: 0L

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OwnerAttentionTile(
            chipBg = OwnerAmberBg,
            chipTint = OwnerAmberInk,
            icon = OwnerIcons.Wallet,
            label = "Piutang aktif",
            value = when {
                isLoading -> ""
                balanceHidden -> "Rp ••••••"
                else -> formatRupiahSpaced(receivables)
            },
            valueColor = Ink,
            isLoading = isLoading,
            onClick = { onTileClick(DashboardTileType.PIUTANG_AKTIF) },
            modifier = Modifier
                .weight(1f)
                .testTag("owner-kpi-receivables")
        )

        OwnerAttentionTile(
            chipBg = if (isStockSafe) OwnerOkBg else Color(0xFFFDE8EC),
            chipTint = if (isStockSafe) Brand else FieldErrorColor,
            icon = if (isStockSafe) OwnerIcons.CheckCircle else OwnerIcons.AlertCircle,
            label = "Stok kritis",
            value = when {
                isLoading -> ""
                isStockSafe -> "Aman"
                else -> "$criticalCount produk"
            },
            valueColor = if (isStockSafe) Brand else FieldErrorColor,
            isLoading = isLoading,
            onClick = { onTileClick(DashboardTileType.STOK_KRITIS) },
            modifier = Modifier
                .weight(1f)
                .testTag("owner-kpi-stock")
        )
    }
}

@Composable
private fun OwnerAttentionTile(
    chipBg: Color,
    chipTint: Color,
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceWhite)
            .border(BorderStroke(1.dp, OwnerBorder), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = "$label: $value" }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(chipBg),
                contentAlignment = Alignment.Center
            ) {
                OwnerVectorIcon(
                    imageVector = icon,
                    tint = chipTint,
                    size = 18.dp
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SurfaceWhite)
                    .border(BorderStroke(1.dp, OwnerBorder), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                OwnerVectorIcon(
                    imageVector = OwnerIcons.ChevronRight,
                    tint = Muted,
                    size = 16.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = label,
            style = ownerTextStyle(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
                color = Muted
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ChipBg)
            )
        } else {
            Text(
                text = value,
                style = ownerTextStyle(
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = valueColor,
                    letterSpacing = (-0.01).em,
                    tabularNumbers = true
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Grafik batang 5 hari (digambar sendiri tanpa library).
 * Mendukung mode terang di ponsel dan mode di atas panel hijau di tablet,
 * serta state kosong (batang rata 4dp) dan loading (#F3F5F3 / translucent).
 */
@Composable
internal fun OwnerSalesTrendChart(
    chart: List<DayPoint>,
    balanceHidden: Boolean,
    isLoading: Boolean,
    onGreenBackground: Boolean,
    modifier: Modifier = Modifier
) {
    val plotHeight = if (onGreenBackground) 200.dp else 132.dp
    val maxBarHeight = if (onGreenBackground) 164.dp else 104.dp
    val baselineColor = if (onGreenBackground) Color.White.copy(alpha = 0.25f) else OwnerNavBorder
    val titleColor = if (onGreenBackground) OnBrand else Ink
    val subtitleColor = if (onGreenBackground) BrandSoftText else Muted
    val inactiveBarColor = when {
        isLoading && onGreenBackground -> Color.White.copy(alpha = 0.20f)
        isLoading -> ChipBg
        onGreenBackground -> Color.White.copy(alpha = 0.28f)
        else -> OwnerBarColor
    }
    val activeBarColor = if (onGreenBackground) SurfaceWhite else Brand

    val displayPoints = remember(chart) {
        if (chart.isEmpty()) {
            listOf("H-4", "H-3", "H-2", "H-1", "Hari ini").mapIndexed { idx, label ->
                DayPoint(dateLabel = label, revenue = 0.0, isToday = idx == 4)
            }
        } else {
            chart.takeLast(5)
        }
    }

    val hasAnyRevenue = remember(chart) { chart.any { it.revenue > 0.0 } }
    val totalPeriodRevenue = remember(chart) { chart.sumOf { it.revenue } }
    val maxRevenue = remember(displayPoints) {
        displayPoints.maxOfOrNull { it.revenue }?.takeIf { it > 0.0 } ?: 1.0
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = if (onGreenBackground) Alignment.Bottom else Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tren penjualan",
                    style = ownerTextStyle(
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "5 hari terakhir",
                    style = ownerTextStyle(
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = subtitleColor
                    )
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (onGreenBackground) Color.White.copy(alpha = 0.20f) else ChipBg)
                )
            } else {
                Text(
                    text = if (balanceHidden) "Rp ••••••" else formatRupiahCompact(totalPeriodRevenue),
                    style = ownerTextStyle(
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor,
                        tabularNumbers = true
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Area plot batang dengan garis dasar 1dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(plotHeight)
                .drawBehind {
                    val strokePx = 1.dp.toPx()
                    drawLine(
                        color = baselineColor,
                        start = Offset(0f, size.height - strokePx / 2f),
                        end = Offset(size.width, size.height - strokePx / 2f),
                        strokeWidth = strokePx
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            displayPoints.forEach { point ->
                val barHeight = when {
                    isLoading -> (maxBarHeight * 0.45f)
                    !hasAnyRevenue || point.revenue <= 0.0 -> 4.dp
                    else -> {
                        val ratio = (point.revenue / maxRevenue).toFloat().coerceIn(0f, 1f)
                        (maxBarHeight * ratio).coerceAtLeast(4.dp)
                    }
                }
                val barColor = when {
                    isLoading -> inactiveBarColor
                    point.isToday && hasAnyRevenue -> activeBarColor
                    else -> inactiveBarColor
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!isLoading && point.isToday && hasAnyRevenue) {
                        Text(
                            text = if (balanceHidden) "•••" else formatChartBarCompact(point.revenue),
                            style = ownerTextStyle(
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = titleColor,
                                tabularNumbers = true
                            ),
                            maxLines = 1
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    ) {
                        drawRect(color = barColor, size = size)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Label tanggal di bawah garis dasar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            displayPoints.forEach { point ->
                Text(
                    text = point.dateLabel,
                    textAlign = TextAlign.Center,
                    style = ownerTextStyle(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = if (point.isToday) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (point.isToday) titleColor else subtitleColor
                    ),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
internal fun OwnerRecentTransactionsSection(
    recent: List<TransactionUi>,
    isTablet: Boolean,
    isLoading: Boolean,
    balanceHidden: Boolean,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transaksi terkini",
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                )
            )

            Box(
                modifier = Modifier.height(20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .requiredHeightIn(min = 48.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = onSeeAll
                        )
                        .padding(start = 16.dp, top = 14.dp, bottom = 14.dp)
                        .testTag("owner-see-all-transactions"),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = "Lihat semua",
                        style = ownerTextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Brand
                        )
                    )
                }
            }
        }

        when {
            isLoading -> {
                val skeletonCount = if (isTablet) 5 else 3
                repeat(skeletonCount) { index ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 52.dp)
                                .height(1.dp)
                                .background(OwnerDivider)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .heightIn(min = 64.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ChipBg)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(128.dp)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ChipBg)
                            )
                            Box(
                                modifier = Modifier
                                    .width(88.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ChipBg)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(78.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(ChipBg)
                        )
                    }
                }
            }

            recent.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OwnerVectorIcon(
                        imageVector = OwnerIcons.Receipt,
                        tint = Muted,
                        size = 24.dp
                    )
                    Text(
                        text = "Belum ada transaksi pada periode ini",
                        style = ownerTextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Ink
                        ),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Transaksi kasir yang selesai akan muncul otomatis di sini.",
                        style = ownerTextStyle(
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = Muted
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            else -> {
                val maxItems = if (isTablet) 5 else 3
                val visibleItems = recent.take(maxItems)
                visibleItems.forEachIndexed { index, item ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 52.dp)
                                .height(1.dp)
                                .background(OwnerDivider)
                        )
                    }

                    OwnerTransactionRow(
                        item = item,
                        avatarBackground = if (isTablet) com.tbterminal.app.ui.theme.BrandTint else SurfaceWhite,
                        balanceHidden = balanceHidden,
                        onClick = onSeeAll
                    )
                }
            }
        }
    }
}

@Composable
internal fun OwnerTransactionRow(
    item: TransactionUi,
    avatarBackground: Color,
    balanceHidden: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = OwnerIcons.Receipt,
    subtitleColor: Color = Muted,
    formattedAmount: String = if (balanceHidden) "Rp ••••••" else formatRupiahSpaced(item.amount),
    semanticsLabel: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (semanticsLabel != null) {
                    Modifier.semantics(mergeDescendants = true) {
                        contentDescription = semanticsLabel
                    }
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(vertical = 12.dp)
            .heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avatarBackground),
            contentAlignment = Alignment.Center
        ) {
            OwnerVectorIcon(
                imageVector = icon,
                tint = Brand,
                size = 20.dp
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Ink
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.subtitle,
                style = ownerTextStyle(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = subtitleColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = formattedAmount,
            style = ownerTextStyle(
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                tabularNumbers = true
            ),
            maxLines = 1
        )
    }
}

internal data class OwnerNavEntry(
    val destination: DashboardNavDestination,
    val label: String,
    val icon: ImageVector,
    val backofficeTag: String
)

internal val ownerNavEntries = listOf(
    OwnerNavEntry(DashboardNavDestination.BERANDA, "Beranda", OwnerIcons.Home, "nav-HOME"),
    OwnerNavEntry(DashboardNavDestination.TRANSAKSI, "Transaksi", OwnerIcons.Receipt, "nav-TRANSACTIONS"),
    OwnerNavEntry(DashboardNavDestination.KEUANGAN, "Keuangan", OwnerIcons.Wallet, "nav-FINANCE"),
    OwnerNavEntry(DashboardNavDestination.STOK, "Stok", OwnerIcons.Box, "nav-STOCK"),
    OwnerNavEntry(DashboardNavDestination.MENU, "Menu", OwnerIcons.MenuGrid, "nav-MENU")
)

/**
 * Menu bawah ponsel (tinggi 80dp, padding 8/8/16, border atas 1dp #E7ECE9).
 */
@Composable
internal fun OwnerPhoneBottomNav(
    activeDestination: DashboardNavDestination,
    onNavSelect: (DashboardNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(SurfaceWhite)
            .drawBehind {
                val strokePx = 1.dp.toPx()
                drawLine(
                    color = OwnerNavBorder,
                    start = Offset(0f, strokePx / 2f),
                    end = Offset(size.width, strokePx / 2f),
                    strokeWidth = strokePx
                )
            }
            .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        ownerNavEntries.forEach { entry ->
            val selected = entry.destination == activeDestination
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                        onClick = { onNavSelect(entry.destination) }
                    )
                    .testTag(entry.backofficeTag),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceWhite),
                    contentAlignment = Alignment.Center
                ) {
                    OwnerVectorIcon(
                        imageVector = entry.icon,
                        tint = if (selected) Brand else Muted,
                        size = 24.dp
                    )
                }

                Text(
                    text = entry.label,
                    style = ownerTextStyle(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) Brand else Muted
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Menu samping tablet (rail lebar 88dp, logo toko 40x40, 5 menu dengan penanda 56x32).
 */
@Composable
internal fun OwnerTabletNavRail(
    activeDestination: DashboardNavDestination,
    onNavSelect: (DashboardNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(88.dp)
            .fillMaxHeight()
            .background(SurfaceWhite)
            .padding(top = 40.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 24.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brand),
            contentAlignment = Alignment.Center
        ) {
            OwnerVectorIcon(
                imageVector = OwnerIcons.Storefront,
                tint = OnBrand,
                size = 22.dp
            )
        }

        ownerNavEntries.forEach { entry ->
            val selected = entry.destination == activeDestination
            Column(
                modifier = Modifier
                    .width(72.dp)
                    .heightIn(min = 64.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                        onClick = { onNavSelect(entry.destination) }
                    )
                    .testTag(entry.backofficeTag),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceWhite),
                    contentAlignment = Alignment.Center
                ) {
                    OwnerVectorIcon(
                        imageVector = entry.icon,
                        tint = if (selected) Brand else Muted,
                        size = 24.dp
                    )
                }

                Text(
                    text = entry.label,
                    style = ownerTextStyle(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) Brand else Muted
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

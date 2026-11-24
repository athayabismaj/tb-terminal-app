package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.OwnerDivider

@Immutable
internal data class OwnerPlainMenuItem(
    val tag: String,
    val title: String,
    val subtitle: String?,
    val icon: ImageVector,
    val iconTint: Color,
    val onClick: () -> Unit
)

@Immutable
internal data class OwnerPlainMenuGroup(
    val title: String,
    val items: List<OwnerPlainMenuItem>
)

internal fun Modifier.cssLineBoxHeight(height: Dp): Modifier = this
    .height(height)
    .wrapContentHeight(align = Alignment.CenterVertically, unbounded = true)

internal fun Modifier.ownerHeroLeftDivider(lineColor: Color): Modifier = this
    .drawBehind {
        val strokePx = 1.dp.toPx()
        drawLine(
            color = lineColor,
            start = Offset(strokePx / 2f, 0f),
            end = Offset(strokePx / 2f, size.height),
            strokeWidth = strokePx
        )
    }
    .padding(start = 16.dp)

@Composable
internal fun OwnerPlainMenuSection(
    group: OwnerPlainMenuGroup,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = group.title,
            style = ownerTextStyle(
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            ),
            modifier = Modifier
                .padding(bottom = 12.dp)
                .cssLineBoxHeight(20.dp)
                .semantics { heading() }
        )

        group.items.forEachIndexed { index, item ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 52.dp)
                        .height(1.dp)
                        .background(OwnerDivider)
                )
            }
            OwnerPlainMenuRow(item = item)
        }
    }
}

/**
 * Baris menu polos sesuai CSS `.mr`: tinggi minimal 72dp, padding vertikal 16dp, gap 12dp,
 * ikon polos 24dp di kotak 40x40dp tanpa latar/border, panah kanan polos 20dp di kotak 24x24dp.
 */
@Composable
internal fun OwnerPlainMenuRow(
    item: OwnerPlainMenuItem,
    modifier: Modifier = Modifier
) {
    val description = if (item.subtitle != null) "${item.title}, ${item.subtitle}" else item.title
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = item.onClick
            )
            .padding(vertical = 16.dp)
            .testTag(item.tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            OwnerVectorIcon(
                imageVector = item.icon,
                tint = item.iconTint,
                size = 24.dp
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
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.cssLineBoxHeight(20.dp)
            )
            if (item.subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
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

/**
 * Top bar halaman sekunder di atas header hijau: tombol kembali chevron tanpa ekor
 * (area sentuh 48x48dp dengan offset -12dp ke kiri) + judul 20/26 SemiBold.
 */
@Composable
internal fun SecondaryTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backTestTag: String = "owner-secondary-back-btn"
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
                .weight(1f, fill = false)
                .semantics { heading() }
        )
    }
}

/**
 * Baris info akun datar sesuai CSS referensi: tinggi min 56dp, padding vertikal 16dp, gap 12dp,
 * label kiri 14/20 Muted, nilai kanan 16/20 Medium (tnum opsional), dan tombol salin opsional
 * berarea sentuh 44x44dp dengan margin negatif (-12dp) agar tinggi baris tetap 56dp.
 */
@Composable
internal fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tabularValue: Boolean = true,
    onCopyClick: (() -> Unit)? = null,
    copyContentDescription: String = "Salin",
    copyTestTag: String = "info-row-copy-btn",
    valueTestTag: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                color = Muted
            ),
            maxLines = 1,
            modifier = Modifier.cssLineBoxHeight(20.dp)
        )

        Text(
            text = value,
            style = ownerTextStyle(
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Ink,
                tabularNumbers = tabularValue
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f)
                .cssLineBoxHeight(20.dp)
                .let { if (valueTestTag != null) it.testTag(valueTestTag) else it }
        )

        if (onCopyClick != null) {
            Box(
                modifier = Modifier.size(width = 32.dp, height = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .wrapContentSize(align = Alignment.CenterStart, unbounded = true)
                        .size(44.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = onCopyClick
                        )
                        .semantics { contentDescription = copyContentDescription }
                        .testTag(copyTestTag),
                    contentAlignment = Alignment.Center
                ) {
                    OwnerVectorIcon(
                        imageVector = OwnerIcons.Copy,
                        tint = Muted,
                        size = 18.dp
                    )
                }
            }
        }
    }
}

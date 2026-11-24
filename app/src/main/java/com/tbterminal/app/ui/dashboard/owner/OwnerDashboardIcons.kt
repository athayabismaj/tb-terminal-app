package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * SVG line icons converted directly from the Barokah Jaya Owner Dashboard HTML reference
 * (viewBox 24x24, stroke 1.8, round cap & join).
 */
internal object OwnerIcons {

    val Eye: ImageVector by lazy {
        strokeIcon(
            name = "OwnerEye",
            paths = listOf(
                "M2.5 12S6 6 12 6s9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6z",
                "M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0"
            )
        )
    }

    val EyeOff: ImageVector by lazy {
        strokeIcon(
            name = "OwnerEyeOff",
            paths = listOf(
                "M3 3l18 18",
                "M10.6 6.1A9.8 9.8 0 0 1 12 6c5 0 8.5 4 9.5 6a12 12 0 0 1-2.2 3",
                "M6.6 6.7C4.5 8 3.1 10.2 2.5 12c1 2 4.5 6 9.5 6 1.4 0 2.6-.3 3.7-.8",
                "M9.9 9.9a3 3 0 0 0 4.2 4.2"
            )
        )
    }

    val TrendUp: ImageVector by lazy {
        strokeIcon(
            name = "OwnerTrendUp",
            strokeWidth = 2.2f,
            paths = listOf(
                "M7 17L17 7",
                "M8 7h9v9"
            )
        )
    }

    val TrendDown: ImageVector by lazy {
        strokeIcon(
            name = "OwnerTrendDown",
            strokeWidth = 2.2f,
            paths = listOf(
                "M7 7L17 17",
                "M8 17h9V8"
            )
        )
    }

    val Cart: ImageVector by lazy {
        strokeIcon(
            name = "OwnerCart",
            paths = listOf(
                "M7.7 20a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0 -2.6 0",
                "M15.7 20a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0 -2.6 0",
                "M3 4h2.5l2 11h10l1.8-8H6.5"
            )
        )
    }

    val Truck: ImageVector by lazy {
        strokeIcon(
            name = "OwnerTruck",
            paths = listOf(
                "M3 6h11v10H3z",
                "M14 10h4l3 3v3h-7z",
                "M5.2 18a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0 -3.6 0",
                "M15.2 18a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0 -3.6 0"
            )
        )
    }

    val Box: ImageVector by lazy {
        strokeIcon(
            name = "OwnerBox",
            paths = listOf(
                "M4 7h16v3H4z",
                "M5.5 10v9h13v-9",
                "M10 14h4"
            )
        )
    }

    val Chart: ImageVector by lazy {
        strokeIcon(
            name = "OwnerChart",
            paths = listOf(
                "M6 20v-8",
                "M12 20V5",
                "M18 20v-9"
            )
        )
    }

    val Wallet: ImageVector by lazy {
        strokeIcon(
            name = "OwnerWallet",
            paths = listOf(
                "M4 7h14a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z",
                "M4 7l11-3v3",
                "M15 13.5a1 1 0 1 0 2 0a1 1 0 1 0 -2 0"
            )
        )
    }

    val CheckCircle: ImageVector by lazy {
        strokeIcon(
            name = "OwnerCheckCircle",
            paths = listOf(
                "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0",
                "M8 12.5l2.7 2.7L16 9.5"
            )
        )
    }

    val AlertCircle: ImageVector by lazy {
        strokeIcon(
            name = "OwnerAlertCircle",
            paths = listOf(
                "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0",
                "M12 8v5",
                "M12 16h0.01"
            )
        )
    }

    val ChevronRight: ImageVector by lazy {
        strokeIcon(
            name = "OwnerChevronRight",
            paths = listOf("M9 6l6 6-6 6")
        )
    }

    val Receipt: ImageVector by lazy {
        strokeIcon(
            name = "OwnerReceipt",
            paths = listOf(
                "M6 3h12v18l-3-2-3 2-3-2-3 2z",
                "M9 8h6",
                "M9 12h6"
            )
        )
    }

    val Home: ImageVector by lazy {
        strokeIcon(
            name = "OwnerHome",
            paths = listOf(
                "M4 11l8-7 8 7",
                "M6 10v10h12V10"
            )
        )
    }

    val MenuGrid: ImageVector by lazy {
        strokeIcon(
            name = "OwnerMenuGrid",
            paths = listOf(
                roundedRectPath(4f, 4f, 7f, 7f, 1.5f),
                roundedRectPath(13f, 4f, 7f, 7f, 1.5f),
                roundedRectPath(4f, 13f, 7f, 7f, 1.5f),
                roundedRectPath(13f, 13f, 7f, 7f, 1.5f)
            )
        )
    }

    val Storefront: ImageVector by lazy {
        strokeIcon(
            name = "OwnerStorefront",
            paths = listOf(
                "M4 9l1.5-5h13L20 9",
                "M4 9c0 1.7 1.3 3 3 3s3-1.3 3-3c0 1.7 1.3 3 3 3s3-1.3 3-3c0 1.7 1.3 3 3 3",
                "M5 12v8h14v-8"
            )
        )
    }

    val Plus: ImageVector by lazy {
        strokeIcon(
            name = "OwnerPlus",
            paths = listOf(
                "M12 5v14",
                "M5 12h14"
            )
        )
    }

    val ClipboardCheck: ImageVector by lazy {
        strokeIcon(
            name = "OwnerClipboardCheck",
            paths = listOf(
                roundedRectPath(5f, 4f, 14f, 17f, 2.5f),
                "M9 4V3h6v1",
                "M9 13l2.2 2.2L15.5 11"
            )
        )
    }

    val Banknote: ImageVector by lazy {
        strokeIcon(
            name = "OwnerBanknote",
            paths = listOf(
                roundedRectPath(3f, 6f, 18f, 12f, 2.5f),
                "M9.5 12a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0",
                "M6.5 12h0.01",
                "M17.5 12h0.01"
            )
        )
    }

    val CreditCard: ImageVector by lazy {
        strokeIcon(
            name = "OwnerCreditCard",
            paths = listOf(
                roundedRectPath(3f, 5f, 18f, 14f, 2.5f),
                "M3 10h18",
                "M7 15h4"
            )
        )
    }

    val TrendLineUp: ImageVector by lazy {
        strokeIcon(
            name = "OwnerTrendLineUp",
            paths = listOf(
                "M3 17l6-6 4 4 8-8",
                "M15 7h6v6"
            )
        )
    }

    val PriceTag: ImageVector by lazy {
        strokeIcon(
            name = "OwnerPriceTag",
            paths = listOf(
                "M3 12V4h8l9 9-8 8z",
                "M6.2 8.5a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0 -2.6 0"
            )
        )
    }

    val StockCard: ImageVector by lazy {
        strokeIcon(
            name = "OwnerStockCard",
            paths = listOf(
                roundedRectPath(4f, 4f, 16f, 16f, 2.5f),
                "M8 9h8",
                "M8 13h8",
                "M8 17h5"
            )
        )
    }

    val SlidersAdjust: ImageVector by lazy {
        strokeIcon(
            name = "OwnerSlidersAdjust",
            paths = listOf(
                "M4 7h9",
                "M17 7h3",
                "M13 7a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
                "M4 17h3",
                "M11 17h9",
                "M7 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
                "M4 12h12",
                "M20 12h0.01"
            )
        )
    }

    val CategoryShapes: ImageVector by lazy {
        strokeIcon(
            name = "OwnerCategoryShapes",
            paths = listOf(
                "M12 3l4 6H8z",
                roundedRectPath(4f, 14f, 6f, 6f, 1f),
                "M14 17a3 3 0 1 0 6 0a3 3 0 1 0 -6 0"
            )
        )
    }

    val RulerUnits: ImageVector by lazy {
        strokeIcon(
            name = "OwnerRulerUnits",
            paths = listOf(
                roundedRectPath(3f, 8f, 18f, 8f, 1.5f),
                "M7 8v3",
                "M11 8v4",
                "M15 8v3",
                "M19 8v4"
            )
        )
    }

    val Shield: ImageVector by lazy {
        strokeIcon(
            name = "OwnerShield",
            paths = listOf(
                "M12 3l7 3v5.5c0 4.3 -3 7.8 -7 9.5 -4 -1.7 -7 -5.2 -7 -9.5V6z"
            )
        )
    }

    val Customers: ImageVector by lazy {
        strokeIcon(
            name = "OwnerCustomers",
            paths = listOf(
                "M6 8a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
                "M3.5 19c0.6 -3 2.8 -5 5.5 -5s4.9 2 5.5 5",
                "M15.5 5.3a3 3 0 0 1 0 5.4",
                "M17 14.3c1.9 0.6 3.1 2.3 3.5 4.7"
            )
        )
    }

    val SupplierTruck: ImageVector by lazy {
        strokeIcon(
            name = "OwnerSupplierTruck",
            paths = listOf(
                "M3 6h11v10H3z",
                "M14 9h4l3 3.5V16h-7",
                "M5.2 17.5a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0 -3.6 0",
                "M15.2 17.5a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0 -3.6 0"
            )
        )
    }

    val UserSingle: ImageVector by lazy {
        strokeIcon(
            name = "OwnerUserSingle",
            paths = listOf(
                "M8.5 8a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0 -7 0",
                "M5 20c0.8 -3.5 3.6 -6 7 -6s6.2 2.5 7 6"
            )
        )
    }

    val SyncArrows: ImageVector by lazy {
        strokeIcon(
            name = "OwnerSyncArrows",
            paths = listOf(
                "M20 12a8 8 0 0 1 -14 5.3",
                "M4 12a8 8 0 0 1 14 -5.3",
                "M18 3v4h-4",
                "M6 21v-4h4"
            )
        )
    }

    val BackupCloud: ImageVector by lazy {
        strokeIcon(
            name = "OwnerBackupCloud",
            paths = listOf(
                "M7 18a4.5 4.5 0 0 1 -0.6 -9A6 6 0 0 1 18 9.5a4 4 0 0 1 -0.5 8.5z",
                "M12 15v-5",
                "M9.5 12.5L12 10l2.5 2.5"
            )
        )
    }

    val ActivityClock: ImageVector by lazy {
        strokeIcon(
            name = "OwnerActivityClock",
            paths = listOf(
                "M4 12a8 8 0 1 0 2.4 -5.7",
                "M4 4v4h4",
                "M12 8v4l3 2"
            )
        )
    }

    val SecurityShieldCheck: ImageVector by lazy {
        strokeIcon(
            name = "OwnerSecurityShieldCheck",
            paths = listOf(
                "M12 3l7 3v5.5c0 4.3 -3 7.8 -7 9.5 -4 -1.7 -7 -5.2 -7 -9.5V6z",
                "M9 12l2 2 4 -4"
            )
        )
    }

    val ChevronLeft: ImageVector by lazy {
        strokeIcon(
            name = "OwnerChevronLeft",
            paths = listOf("M15 5l-7 7 7 7")
        )
    }

    val Copy: ImageVector by lazy {
        strokeIcon(
            name = "OwnerCopy",
            paths = listOf(
                roundedRectPath(8f, 8f, 12f, 12f, 2.5f),
                "M16 8V6a2 2 0 0 0 -2 -2H6a2 2 0 0 0 -2 2v8a2 2 0 0 0 2 2h2"
            )
        )
    }

    val LockClosed: ImageVector by lazy {
        strokeIcon(
            name = "OwnerLockClosed",
            paths = listOf(
                roundedRectPath(5f, 10f, 14f, 10f, 2.5f),
                "M8 10V7.5a4 4 0 0 1 8 0V10",
                "M12 14.5v2"
            )
        )
    }

    val PinDots: ImageVector by lazy {
        strokeIcon(
            name = "OwnerPinDots",
            paths = listOf(
                roundedRectPath(3f, 6f, 18f, 12f, 2.5f),
                "M7.5 12h0.01",
                "M12 12h0.01",
                "M16.5 12h0.01"
            )
        )
    }

    val Logout: ImageVector by lazy {
        strokeIcon(
            name = "OwnerLogout",
            paths = listOf(
                "M10 4H6a2 2 0 0 0 -2 2v12a2 2 0 0 0 2 2h4",
                "M15 8l4 4 -4 4",
                "M19 12H9"
            )
        )
    }

    val CloseX: ImageVector by lazy {
        strokeIcon(
            name = "OwnerCloseX",
            paths = listOf(
                "M6 6l12 12",
                "M18 6L6 18"
            )
        )
    }

    val EmptyCircle: ImageVector by lazy {
        strokeIcon(
            name = "OwnerEmptyCircle",
            paths = listOf(
                "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0"
            )
        )
    }

    val ErrorCircle: ImageVector by lazy {
        strokeIcon(
            name = "OwnerErrorCircle",
            paths = listOf(
                "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0",
                "M12 7.5v5.5",
                "M12 16.5h0.01"
            )
        )
    }

    val BackspaceKey: ImageVector by lazy {
        strokeIcon(
            name = "OwnerBackspaceKey",
            paths = listOf(
                "M9 5h11a1 1 0 0 1 1 1v12a1 1 0 0 1 -1 1H9l-6 -7z",
                "M12.5 9.5l5 5",
                "M17.5 9.5l-5 5"
            )
        )
    }

    val Search: ImageVector by lazy {
        strokeIcon(
            name = "OwnerSearch",
            paths = listOf(
                "M4.5 11a6.5 6.5 0 1 0 13 0a6.5 6.5 0 1 0 -13 0",
                "M16 16l4 4"
            )
        )
    }

    val FilterSliders: ImageVector by lazy {
        strokeIcon(
            name = "OwnerFilterSliders",
            paths = listOf(
                "M4 7h9",
                "M17 7h3",
                "M13 7a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
                "M4 17h3",
                "M11 17h9",
                "M7 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"
            )
        )
    }

    val CustomersEmptyGroup: ImageVector by lazy {
        strokeIcon(
            name = "OwnerCustomersEmptyGroup",
            strokeWidth = 1.6f,
            paths = listOf(
                "M9 8.5a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
                "M6.5 19c0.6 -3 2.8 -5 5.5 -5s4.9 2 5.5 5",
                "M5.6 11a2.5 2.5 0 0 1 0.4 -5",
                "M2.5 17.5c0.3 -1.9 1.4 -3.2 3 -3.7",
                "M18.4 11a2.5 2.5 0 0 0 -0.4 -5",
                "M21.5 17.5c-0.3 -1.9 -1.4 -3.2 -3 -3.7"
            )
        )
    }

    val CheckPlain: ImageVector by lazy {
        strokeIcon(
            name = "OwnerCheckPlain",
            paths = listOf("M5 12.5l4.5 4.5L19 7.5")
        )
    }

    val EditPencil: ImageVector by lazy {
        strokeIcon(
            name = "OwnerEditPencil",
            paths = listOf(
                "M12 20h9",
                "M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4Z"
            )
        )
    }

    val UserSlash: ImageVector by lazy {
        strokeIcon(
            name = "OwnerUserSlash",
            paths = listOf(
                "M8.5 7.5a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0 -7 0",
                "M5 19c0.8 -3 3.2 -4.5 6.5 -4.5",
                "M15 15l5 5",
                "M20 15l-5 5"
            )
        )
    }

    val DocumentReport: ImageVector by lazy {
        strokeIcon(
            name = "OwnerDocumentReport",
            paths = listOf(
                "M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z",
                "M14 3v5h5",
                "M9 13h6",
                "M9 17h6"
            )
        )
    }

    val SearchNotFound: ImageVector by lazy {
        strokeIcon(
            name = "OwnerSearchNotFound",
            strokeWidth = 1.6f,
            paths = listOf(
                "M4.5 11a6.5 6.5 0 1 0 13 0a6.5 6.5 0 1 0 -13 0",
                "M16 16l4 4",
                "M9 9l4 4",
                "M13 9l-4 4"
            )
        )
    }

    val CalendarRange: ImageVector by lazy {
        strokeIcon(
            name = "OwnerCalendarRange",
            paths = listOf(
                roundedRectPath(4f, 5f, 16f, 15f, 2.5f),
                "M4 10h16",
                "M8 3v4",
                "M16 3v4"
            )
        )
    }

    val ChevronDown: ImageVector by lazy {
        strokeIcon(
            name = "OwnerChevronDown",
            paths = listOf("M6 9l6 6 6-6")
        )
    }

    val Trash: ImageVector by lazy {
        strokeIcon(
            name = "OwnerTrash",
            paths = listOf(
                "M3 6h18",
                "M8 6V4a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v2",
                "M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6",
                "M10 11v6",
                "M14 11v6"
            )
        )
    }

    private fun roundedRectPath(x: Float, y: Float, w: Float, h: Float, r: Float): String {
        val right = x + w
        val bottom = y + h
        return "M${x + r},$y L${right - r},$y A$r,$r 0 0,1 $right,${y + r} " +
            "L$right,${bottom - r} A$r,$r 0 0,1 ${right - r},$bottom " +
            "L${x + r},$bottom A$r,$r 0 0,1 $x,${bottom - r} " +
            "L$x,${y + r} A$r,$r 0 0,1 ${x + r},$y Z"
    }

    private fun strokeIcon(
        name: String,
        paths: List<String>,
        strokeWidth: Float = 1.8f
    ): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        val brush = SolidColor(Color.Black)
        paths.forEach { d ->
            builder.addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                stroke = brush,
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
        return builder.build()
    }
}

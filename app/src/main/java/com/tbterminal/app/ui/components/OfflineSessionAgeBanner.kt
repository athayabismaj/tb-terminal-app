package com.tbterminal.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.sync.OfflineSessionAgeAlert
import com.tbterminal.app.data.sync.OfflineSessionAgeSeverity
import com.tbterminal.app.ui.theme.TbterminalappTheme

@Composable
fun OfflineSessionAgeBanner(
    alert: OfflineSessionAgeAlert,
    modifier: Modifier = Modifier
) {
    val isCritical = alert.severity == OfflineSessionAgeSeverity.CRITICAL
    val containerColor = if (isCritical) Color(0xFFFEF2F2) else Color(0xFFFFFBEB)
    val borderColor = if (isCritical) Color(0xFFFCA5A5) else Color(0xFFFCD34D)
    val iconTint = if (isCritical) Color(0xFFDC2626) else Color(0xFFD97706)
    val titleColor = if (isCritical) Color(0xFF991B1B) else Color(0xFF92400E)
    val bodyColor = if (isCritical) Color(0xFF7F1D1D) else Color(0xFF78350F)
    val tag = if (isCritical) {
        "offline-session-age-critical-banner"
    } else {
        "offline-session-age-warning-banner"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (isCritical) Icons.Outlined.ErrorOutline else Icons.Outlined.Warning,
                contentDescription = if (isCritical) "Peringatan keras sesi offline" else "Peringatan sesi offline",
                tint = iconTint,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = alert.title,
                    color = titleColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 18.sp
                )
                Text(
                    text = alert.message,
                    color = bodyColor,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                if (alert.affectedSessionCount > 1) {
                    Text(
                        text = "Terdapat ${alert.affectedSessionCount} sesi offline yang perlu segera disinkronkan.",
                        color = titleColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewOfflineSessionAgeBannerWarning() {
    TbterminalappTheme {
        OfflineSessionAgeBanner(
            alert = OfflineSessionAgeAlert(
                severity = OfflineSessionAgeSeverity.WARNING,
                ageHours = 50,
                remainingHours = 22,
                title = "Sesi offline sudah berjalan 50 jam",
                message = "Batas sinkronisasi sesi offline adalah 72 jam (tersisa sekitar 22 jam). Segera sambungkan internet agar transaksi bisa tersinkron."
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewOfflineSessionAgeBannerCritical() {
    TbterminalappTheme {
        OfflineSessionAgeBanner(
            alert = OfflineSessionAgeAlert(
                severity = OfflineSessionAgeSeverity.CRITICAL,
                ageHours = 68,
                remainingHours = 4,
                title = "Peringatan keras: sesi offline mendekati batas 72 jam",
                message = "Sesi offline sudah berjalan 68 jam (tersisa sekitar 4 jam). Segera sambungkan internet agar transaksi bisa tersinkron.",
                affectedSessionCount = 2
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

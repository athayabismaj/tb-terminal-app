package com.tbterminal.app.ui.dashboard.cashier

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.local.database.DevicePreferences
import com.tbterminal.app.data.sync.BackendStatus
import com.tbterminal.app.data.sync.OfflineConnectionStatus
import com.tbterminal.app.data.sync.OfflineStatus
import com.tbterminal.app.ui.cashier.session.components.CashierBukaShiftView
import com.tbterminal.app.ui.theme.TbterminalappTheme

@Preview(name = "Fase 1 - Shift Belum Dibuka (Buka Shift Screen)", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PreviewCashierBukaShiftScreen() {
    TbterminalappTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8FAF9)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CashierBukaShiftView(
                    cashierName = "Budi Santoso",
                    terminalName = "Terminal Barokah Jaya",
                    openingCashInput = "200000",
                    onOpeningCashChanged = {},
                    onOpenSession = {},
                    isSubmitting = false
                )
            }
        }
    }
}

@Preview(name = "Fase 1 - Tab Kas Header (Shift Aktif + Online + Printer Siap)", showBackground = true, widthDp = 390)
@Composable
fun PreviewCashierKasHeaderActiveOnline() {
    TbterminalappTheme {
        Column(
            modifier = Modifier
                .background(Color(0xFFF8FAF9))
                .padding(bottom = 16.dp)
        ) {
            CashierKasTopBar(
                userName = "Budi Santoso",
                role = "Kasir",
                title = "Kas Harian & Shift",
                subtitle = "Terminal Barokah Jaya",
                offlineStatus = OfflineStatus(
                    isInternetOnline = true,
                    backendStatus = BackendStatus.CONNECTED,
                    connectionStatus = OfflineConnectionStatus.ONLINE,
                    pendingSyncCount = 0
                ),
                devicePreferences = DevicePreferences(
                    printerBluetoothAddress = "00:11:22:33:44:55",
                    printerBluetoothName = "RP-58",
                    paperSize = "58mm"
                ),
                onAccountClick = {},
                onLockClick = {}
            )
        }
    }
}

@Preview(name = "Fase 1 - Tab Kas Header (Offline dengan Antrean Sync + Printer Belum Diatur)", showBackground = true, widthDp = 390)
@Composable
fun PreviewCashierKasHeaderOfflineSync() {
    TbterminalappTheme {
        Column(
            modifier = Modifier
                .background(Color(0xFFF8FAF9))
                .padding(bottom = 16.dp)
        ) {
            CashierKasTopBar(
                userName = "Budi Santoso",
                role = "Kasir",
                title = "Kas Harian & Shift",
                subtitle = "Terminal Barokah Jaya",
                offlineStatus = OfflineStatus(
                    isInternetOnline = false,
                    backendStatus = BackendStatus.UNREACHABLE,
                    connectionStatus = OfflineConnectionStatus.OFFLINE,
                    pendingSyncCount = 5
                ),
                devicePreferences = DevicePreferences(
                    printerBluetoothAddress = "",
                    printerBluetoothName = "",
                    paperSize = "58mm"
                ),
                onAccountClick = {},
                onLockClick = {}
            )
        }
    }
}

@Preview(name = "Fase 1 - Bottom Navigation 4 Tab", showBackground = true, widthDp = 390)
@Composable
fun PreviewCashierBottomNavigation4Tabs() {
    TbterminalappTheme {
        CashierBottomNavigation(
            activeDestination = CashierDestination.Pos,
            onDashboardClick = {},
            onPosClick = {},
            onCashSessionClick = {},
            onTransactionHistoryClick = {},
            onReceivablesClick = {}
        )
    }
}

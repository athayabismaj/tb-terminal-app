package com.tbterminal.app.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tbterminal.app.ui.theme.TbterminalappTheme

@Preview(name = "Transaksi - Phone", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Transaksi - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun TransactionsHubPreview() {
    HubPreview(BackofficeSection.TRANSACTIONS)
}

@Preview(name = "Keuangan - Phone", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Keuangan - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun FinanceHubPreview() {
    HubPreview(BackofficeSection.FINANCE)
}

@Preview(name = "Stok - Phone", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Stok - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun StockHubPreview() {
    HubPreview(BackofficeSection.STOCK)
}

@Composable
private fun HubPreview(section: BackofficeSection) {
    TbterminalappTheme {
        BackofficeHubScreen(
            name = "Pemilik Toko",
            role = "OWNER",
            section = section,
            onNavigate = {},
            onLogout = {},
        )
    }
}

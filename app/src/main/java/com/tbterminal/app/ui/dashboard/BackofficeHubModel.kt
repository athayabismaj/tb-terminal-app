package com.tbterminal.app.ui.dashboard

import androidx.annotation.StringRes
import com.tbterminal.app.R
import com.tbterminal.app.navigation.AppAccessPolicy
import com.tbterminal.app.navigation.AppCapability
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

internal enum class BackofficeTransactionTab(@param:StringRes val labelRes: Int) {
    SALES(R.string.hub_tab_sales),
    PURCHASES(R.string.hub_tab_purchases),
}

internal enum class BackofficeHubAction(
    @param:StringRes val titleRes: Int,
    val capability: AppCapability,
    val destination: AdminDestination,
) {
    SALES_HISTORY(R.string.hub_sales_history, AppCapability.ALL_TRANSACTIONS, AdminDestination.SalesTransactions),
    NEW_SALE(R.string.hub_new_sale, AppCapability.POS, AdminDestination.NewTransaction),
    PURCHASE_HISTORY(R.string.hub_purchase_history, AppCapability.PURCHASES, AdminDestination.PurchaseHistory),
    INCOMING_GOODS(R.string.hub_incoming_goods, AppCapability.PURCHASES, AdminDestination.IncomingGoods),
    RECEIVABLES(R.string.hub_receivables, AppCapability.RECEIVABLES, AdminDestination.Receivables),
    SUPPLIER_DEBTS(R.string.hub_supplier_debts, AppCapability.PAYABLES, AdminDestination.SupplierDebts),
    DAILY_CASH(R.string.hub_daily_cash, AppCapability.CASH_SESSIONS, AdminDestination.CashReconciliation),
    PRODUCTS_STOCK(R.string.hub_products_stock, AppCapability.PRODUCTS, AdminDestination.Products),
    ADJUST_STOCK(R.string.hub_adjust_stock, AppCapability.MANAGE_INVENTORY, AdminDestination.StockOpname),
    STOCK_CARD(R.string.hub_stock_card, AppCapability.STOCK, AdminDestination.StockReport),
    PRODUCT_PRICES(R.string.hub_product_prices, AppCapability.MANAGE_INVENTORY, AdminDestination.PriceManagement),
    CATEGORIES(R.string.hub_categories, AppCapability.MANAGE_INVENTORY, AdminDestination.ProductCategories),
    UNITS(R.string.hub_units, AppCapability.MANAGE_INVENTORY, AdminDestination.ProductUnits),
}

internal data class BackofficeHubGroup(
    @param:StringRes val titleRes: Int,
    val actions: List<BackofficeHubAction>,
)

internal data class BackofficeHubContent(
    val primaryAction: BackofficeHubAction? = null,
    val groups: List<BackofficeHubGroup> = emptyList(),
)

internal fun transactionHubContent(role: String?, tab: BackofficeTransactionTab): BackofficeHubContent {
    if (!AppAccessPolicy.can(role, AppCapability.BACKOFFICE)) return BackofficeHubContent()
    return when (tab) {
        BackofficeTransactionTab.SALES -> hubContent(
            role = role,
            primary = BackofficeHubAction.NEW_SALE,
            groups = listOf(
                BackofficeHubGroup(R.string.hub_group_sales, listOf(BackofficeHubAction.SALES_HISTORY)),
            ),
        )
        BackofficeTransactionTab.PURCHASES -> hubContent(
            role = role,
            primary = BackofficeHubAction.INCOMING_GOODS,
            groups = listOf(
                BackofficeHubGroup(R.string.hub_group_purchases, listOf(BackofficeHubAction.PURCHASE_HISTORY)),
            ),
        )
    }
}

internal fun financeHubContent(role: String?): BackofficeHubContent = hubContent(
    role = role,
    groups = listOf(
        BackofficeHubGroup(
            R.string.hub_group_finance,
            listOf(
                BackofficeHubAction.RECEIVABLES,
                BackofficeHubAction.SUPPLIER_DEBTS,
                BackofficeHubAction.DAILY_CASH,
            ),
        ),
    ),
)

internal fun stockHubContent(role: String?): BackofficeHubContent = hubContent(
    role = role,
    primary = BackofficeHubAction.INCOMING_GOODS,
    groups = listOf(
        BackofficeHubGroup(
            R.string.hub_group_stock,
            listOf(
                BackofficeHubAction.PRODUCTS_STOCK,
                BackofficeHubAction.ADJUST_STOCK,
                BackofficeHubAction.STOCK_CARD,
            ),
        ),
        BackofficeHubGroup(
            R.string.hub_group_product_settings,
            listOf(
                BackofficeHubAction.PRODUCT_PRICES,
                BackofficeHubAction.CATEGORIES,
                BackofficeHubAction.UNITS,
            ),
        ),
    ),
)

private fun hubContent(
    role: String?,
    primary: BackofficeHubAction? = null,
    groups: List<BackofficeHubGroup>,
): BackofficeHubContent {
    if (!AppAccessPolicy.can(role, AppCapability.BACKOFFICE)) return BackofficeHubContent()
    return BackofficeHubContent(
        primaryAction = primary?.takeIf { AppAccessPolicy.can(role, it.capability) },
        groups = groups.map { group ->
            group.copy(actions = group.actions.filter { AppAccessPolicy.can(role, it.capability) })
        }.filter { it.actions.isNotEmpty() },
    )
}

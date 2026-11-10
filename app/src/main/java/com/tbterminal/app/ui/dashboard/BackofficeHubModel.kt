package com.tbterminal.app.ui.dashboard

import androidx.annotation.StringRes
import com.tbterminal.app.R
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.navigation.AppAccessPolicy
import com.tbterminal.app.navigation.AppCapability
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

internal enum class BackofficeTransactionTab(@param:StringRes val labelRes: Int) {
    SALES(R.string.hub_tab_sales),
    PURCHASES(R.string.hub_tab_purchases),
}

internal enum class BackofficeHubAction(
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    val capability: AppCapability,
    val destination: AdminDestination,
) {
    SALES_HISTORY(
        R.string.hub_sales_history,
        R.string.hub_sales_history_description,
        AppCapability.ALL_TRANSACTIONS,
        AdminDestination.SalesTransactions,
    ),
    NEW_SALE(
        R.string.hub_new_sale,
        R.string.hub_new_sale_description,
        AppCapability.POS,
        AdminDestination.NewTransaction,
    ),
    RECEIVABLE_PAYMENTS(
        R.string.hub_receivable_payments,
        R.string.hub_receivable_payments_description,
        AppCapability.RECEIVABLE_PAYMENTS,
        AdminDestination.ReceivablePayments,
    ),
    OPERATIONAL_AUDIT(
        R.string.hub_operational_audit,
        R.string.hub_operational_audit_description,
        AppCapability.AUDIT,
        AdminDestination.OperationalAudit,
    ),
    PURCHASE_HISTORY(
        R.string.hub_purchase_history,
        R.string.hub_purchase_history_description,
        AppCapability.PURCHASES,
        AdminDestination.PurchaseHistory,
    ),
    INCOMING_GOODS(
        R.string.hub_incoming_goods,
        R.string.hub_incoming_goods_description,
        AppCapability.PURCHASES,
        AdminDestination.IncomingGoods,
    ),
    SUPPLIERS(
        R.string.hub_suppliers,
        R.string.hub_suppliers_description,
        AppCapability.SUPPLIERS,
        AdminDestination.Suppliers,
    ),
    RECEIVABLES(
        R.string.hub_receivables,
        R.string.hub_receivables_description,
        AppCapability.RECEIVABLES,
        AdminDestination.Receivables,
    ),
    SUPPLIER_DEBTS(
        R.string.hub_supplier_debts,
        R.string.hub_supplier_debts_description,
        AppCapability.PAYABLES,
        AdminDestination.SupplierDebts,
    ),
    DAILY_CASH(
        R.string.hub_daily_cash,
        R.string.hub_daily_cash_description,
        AppCapability.CASH_SESSIONS,
        AdminDestination.CashReconciliation,
    ),
    CASH_EXPENSES(
        R.string.hub_cash_expenses,
        R.string.hub_cash_expenses_description,
        AppCapability.CASH_SESSIONS,
        AdminDestination.CashExpenses,
    ),
    CASH_SESSION_HISTORY(
        R.string.hub_cash_session_history,
        R.string.hub_cash_session_history_description,
        AppCapability.CASH_SESSIONS,
        AdminDestination.CashSessionHistory,
    ),
    FINANCE_REPORTS(
        R.string.hub_finance_reports,
        R.string.hub_finance_reports_description,
        AppCapability.FINANCIAL_ANALYTICS,
        AdminDestination.Reports,
    ),
    ADD_PRODUCT(
        R.string.hub_add_product,
        R.string.hub_add_product_description,
        AppCapability.MANAGE_INVENTORY,
        AdminDestination.AddProduct,
    ),
    PRODUCTS_STOCK(
        R.string.hub_products_stock,
        R.string.hub_products_stock_description,
        AppCapability.PRODUCTS,
        AdminDestination.Products,
    ),
    ADJUST_STOCK(
        R.string.hub_adjust_stock,
        R.string.hub_adjust_stock_description,
        AppCapability.MANAGE_INVENTORY,
        AdminDestination.StockOpname,
    ),
    STOCK_CARD(
        R.string.hub_stock_card,
        R.string.hub_stock_card_description,
        AppCapability.STOCK,
        AdminDestination.StockReport,
    ),
    PRODUCT_PRICES(
        R.string.hub_product_prices,
        R.string.hub_product_prices_description,
        AppCapability.MANAGE_INVENTORY,
        AdminDestination.PriceManagement,
    ),
    CATEGORIES(
        R.string.hub_categories,
        R.string.hub_categories_description,
        AppCapability.MANAGE_INVENTORY,
        AdminDestination.ProductCategories,
    ),
    UNITS(
        R.string.hub_units,
        R.string.hub_units_description,
        AppCapability.MANAGE_INVENTORY,
        AdminDestination.ProductUnits,
    ),
}

internal data class BackofficeHubGroup(
    @param:StringRes val titleRes: Int,
    val actions: List<BackofficeHubAction>,
)

internal data class BackofficeHubContent(
    val primaryAction: BackofficeHubAction? = null,
    val secondaryQuickAction: BackofficeHubAction? = null,
    val groups: List<BackofficeHubGroup> = emptyList(),
)

internal fun transactionHubContent(role: String?, tab: BackofficeTransactionTab): BackofficeHubContent {
    if (!AppAccessPolicy.can(role, AppCapability.BACKOFFICE)) return BackofficeHubContent()
    val isOwner = isOwnerPersona(role)
    return when (tab) {
        BackofficeTransactionTab.SALES -> hubContent(
            role = role,
            primary = BackofficeHubAction.NEW_SALE,
            groups = listOf(
                BackofficeHubGroup(
                    R.string.hub_group_sales,
                    buildList {
                        add(BackofficeHubAction.SALES_HISTORY)
                        add(BackofficeHubAction.RECEIVABLE_PAYMENTS)
                        if (isOwner) {
                            add(BackofficeHubAction.OPERATIONAL_AUDIT)
                        }
                    },
                ),
            ),
        )
        BackofficeTransactionTab.PURCHASES -> hubContent(
            role = role,
            primary = BackofficeHubAction.INCOMING_GOODS,
            groups = listOf(
                BackofficeHubGroup(
                    R.string.hub_group_purchases,
                    listOf(
                        BackofficeHubAction.PURCHASE_HISTORY,
                        BackofficeHubAction.SUPPLIERS,
                        BackofficeHubAction.SUPPLIER_DEBTS,
                    ),
                ),
            ),
        )
    }
}

internal fun financeHubContent(role: String?): BackofficeHubContent = hubContent(
    role = role,
    groups = listOf(
        BackofficeHubGroup(
            R.string.hub_group_cash,
            listOf(
                BackofficeHubAction.DAILY_CASH,
                BackofficeHubAction.CASH_EXPENSES,
                BackofficeHubAction.CASH_SESSION_HISTORY,
            ),
        ),
        BackofficeHubGroup(
            R.string.hub_group_receivables_payables,
            listOf(
                BackofficeHubAction.RECEIVABLES,
                BackofficeHubAction.RECEIVABLE_PAYMENTS,
                BackofficeHubAction.SUPPLIER_DEBTS,
            ),
        ),
        BackofficeHubGroup(
            R.string.hub_group_finance_analytics,
            listOf(
                BackofficeHubAction.FINANCE_REPORTS,
            ),
        ),
    ),
)

internal fun stockHubContent(role: String?): BackofficeHubContent = hubContent(
    role = role,
    primary = BackofficeHubAction.INCOMING_GOODS,
    secondaryQuickAction = BackofficeHubAction.ADD_PRODUCT,
    groups = listOf(
        BackofficeHubGroup(
            R.string.hub_group_stock_catalog,
            listOf(
                BackofficeHubAction.PRODUCTS_STOCK,
                BackofficeHubAction.PRODUCT_PRICES,
            ),
        ),
        BackofficeHubGroup(
            R.string.hub_group_stock_audit,
            listOf(
                BackofficeHubAction.STOCK_CARD,
                BackofficeHubAction.ADJUST_STOCK,
            ),
        ),
        BackofficeHubGroup(
            R.string.hub_group_product_settings,
            listOf(
                BackofficeHubAction.CATEGORIES,
                BackofficeHubAction.UNITS,
            ),
        ),
    ),
)

internal data class ProductStockAggregate(
    val products: List<ProductStock>,
    val totalProducts: Int,
    val isPartial: Boolean = false,
)

internal suspend fun loadAllProductStocks(
    inventoryRepository: InventoryRepository,
    pageSize: Int = 100,
    maxPages: Int = 50,
): RepositoryResult<ProductStockAggregate> {
    var currentPage = 1
    var totalReported = 0L
    var totalPages = 1
    var isPartial = false
    val items = mutableListOf<ProductStock>()

    while (currentPage <= totalPages && currentPage <= maxPages) {
        when (val result = inventoryRepository.getProductStocks(page = currentPage, limit = pageSize)) {
            is RepositoryResult.Success -> {
                val pageData = result.data
                if (currentPage == 1) {
                    totalReported = pageData.total
                    totalPages = pageData.totalPages.coerceAtLeast(1)
                }
                if (pageData.data.isEmpty()) break
                items.addAll(pageData.data)
                currentPage++
            }
            is RepositoryResult.Error -> {
                if (items.isEmpty()) return result
                isPartial = true
                break
            }
            is RepositoryResult.Exception -> {
                if (items.isEmpty()) return result
                isPartial = true
                break
            }
        }
    }

    if (currentPage <= totalPages && currentPage > maxPages) {
        isPartial = true
    }

    val distinctProducts = items.distinctBy { it.productId }
    return RepositoryResult.Success(
        ProductStockAggregate(
            products = distinctProducts,
            totalProducts = totalReported.toInt().coerceAtLeast(distinctProducts.size),
            isPartial = isPartial,
        )
    )
}

private fun hubContent(
    role: String?,
    primary: BackofficeHubAction? = null,
    secondaryQuickAction: BackofficeHubAction? = null,
    groups: List<BackofficeHubGroup>,
): BackofficeHubContent {
    if (!AppAccessPolicy.can(role, AppCapability.BACKOFFICE)) return BackofficeHubContent()
    return BackofficeHubContent(
        primaryAction = primary?.takeIf { AppAccessPolicy.can(role, it.capability) },
        secondaryQuickAction = secondaryQuickAction?.takeIf { AppAccessPolicy.can(role, it.capability) },
        groups = groups.map { group ->
            group.copy(actions = group.actions.filter { AppAccessPolicy.can(role, it.capability) })
        }.filter { it.actions.isNotEmpty() },
    )
}

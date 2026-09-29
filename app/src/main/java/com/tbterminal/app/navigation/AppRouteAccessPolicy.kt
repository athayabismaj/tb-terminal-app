package com.tbterminal.app.navigation

internal enum class AppCapability {
    AUTH,
    DASHBOARD,
    BACKOFFICE,
    POS,
    PRODUCTS,
    STOCK,
    SUPPLIERS,
    PURCHASES,
    PAYABLES,
    CUSTOMERS,
    MANAGE_CUSTOMERS,
    MANAGE_INVENTORY,
    RECEIVABLES,
    RECEIVABLE_PAYMENTS,
    ADJUST_RECEIVABLES,
    REVERSE_RECEIVABLE_PAYMENTS,
    ALL_TRANSACTIONS,
    OWN_TRANSACTIONS,
    DIRECT_TRANSACTION_CORRECTION,
    APPROVED_TRANSACTION_CORRECTION,
    CASH_SESSIONS,
    REPORTS,
    FINANCIAL_ANALYTICS,
    AUDIT,
    USER_MANAGEMENT,
    SERVER_BACKUP,
    STORE_SETTINGS,
    SECURITY_SETTINGS,
    DEVICE_SETTINGS,
    SYNC,
    RETRY_SYNC,
    ACCOUNT,
}

internal enum class AppMenuFeature(val capability: AppCapability) {
    DASHBOARD(AppCapability.DASHBOARD),
    PRODUCTS(AppCapability.PRODUCTS),
    STOCK(AppCapability.STOCK),
    SUPPLIERS(AppCapability.SUPPLIERS),
    PURCHASES(AppCapability.PURCHASES),
    PAYABLES(AppCapability.PAYABLES),
    CUSTOMERS(AppCapability.CUSTOMERS),
    RECEIVABLES(AppCapability.RECEIVABLES),
    RECEIVABLE_PAYMENTS(AppCapability.RECEIVABLE_PAYMENTS),
    TRANSACTIONS(AppCapability.ALL_TRANSACTIONS),
    OWN_TRANSACTIONS(AppCapability.OWN_TRANSACTIONS),
    POS(AppCapability.POS),
    CASH_SESSIONS(AppCapability.CASH_SESSIONS),
    REPORTS(AppCapability.REPORTS),
    USER_MANAGEMENT(AppCapability.USER_MANAGEMENT),
    AUDIT(AppCapability.AUDIT),
    SERVER_BACKUP(AppCapability.SERVER_BACKUP),
    STORE_SETTINGS(AppCapability.STORE_SETTINGS),
    SECURITY_SETTINGS(AppCapability.SECURITY_SETTINGS),
    DEVICE_SETTINGS(AppCapability.DEVICE_SETTINGS),
    ACCOUNT(AppCapability.ACCOUNT),
}

/** Single source of truth for role capabilities and route access. */
internal object AppAccessPolicy {
    private const val ROLE_OWNER = "owner"
    private const val ROLE_ADMIN = "admin"
    private const val ROLE_CASHIER = "kasir"

    private val ownerCapabilities = AppCapability.entries.toSet()

    private val adminCapabilities = ownerCapabilities - setOf(
        AppCapability.USER_MANAGEMENT,
        AppCapability.SERVER_BACKUP,
        AppCapability.SECURITY_SETTINGS,
        AppCapability.FINANCIAL_ANALYTICS,
    )

    private val cashierCapabilities = setOf(
        AppCapability.AUTH,
        AppCapability.DASHBOARD,
        AppCapability.POS,
        AppCapability.CUSTOMERS,
        AppCapability.RECEIVABLES,
        AppCapability.RECEIVABLE_PAYMENTS,
        AppCapability.OWN_TRANSACTIONS,
        AppCapability.CASH_SESSIONS,
        AppCapability.DEVICE_SETTINGS,
        AppCapability.ACCOUNT,
        AppCapability.APPROVED_TRANSACTION_CORRECTION,
    )

    fun can(role: String?, capability: AppCapability): Boolean {
        val capabilities = when (role?.trim()?.lowercase()) {
            ROLE_OWNER -> ownerCapabilities
            ROLE_ADMIN -> adminCapabilities
            ROLE_CASHIER -> cashierCapabilities
            else -> emptySet()
        }
        return capability in capabilities
    }

    fun visibleMenuFeatures(role: String?): List<AppMenuFeature> =
        AppMenuFeature.entries.filter { feature -> can(role, feature.capability) }
}

internal object AppRouteAccessPolicy {
    fun isAllowed(route: String?, role: String?): Boolean {
        val capability = requiredCapability(route) ?: return false
        return capability == AppCapability.AUTH || AppAccessPolicy.can(role, capability)
    }

    internal fun requiredCapability(route: String?): AppCapability? {
        val value = route?.substringBefore('?')?.lowercase() ?: return null
        return when {
            value == AppRoute.Login.route || value == AppRoute.Pin.route -> AppCapability.AUTH
            value == AppRoute.Dashboard.route -> AppCapability.DASHBOARD
            value in setOf(
                AppRoute.BackofficeTransactions.route,
                AppRoute.BackofficeFinance.route,
                AppRoute.BackofficeStock.route,
                AppRoute.BackofficeMore.route,
            ) -> AppCapability.BACKOFFICE

            value == AppRoute.CashierPos.route || value == AppRoute.CashierCart.route -> AppCapability.POS
            value == AppRoute.CashierCashSession.route -> AppCapability.CASH_SESSIONS
            value == AppRoute.CashierStockCheck.route -> AppCapability.STOCK
            value == AppRoute.CashierTransactionHistory.route ||
                value.matchesRoute(AppRoute.CashierReceiptDetail.route) -> AppCapability.OWN_TRANSACTIONS

            value == AppRoute.Products.route || value.matchesRoute(AppRoute.ProductDetail.route) ->
                AppCapability.PRODUCTS
            value == AppRoute.AddProduct.route || value.matchesRoute(AppRoute.EditProduct.route) ||
                value == AppRoute.ProductCategories.route || value == AppRoute.ProductUnits.route ||
                value == AppRoute.PriceManagement.route -> AppCapability.MANAGE_INVENTORY

            value == AppRoute.StockReport.route -> AppCapability.STOCK
            value == AppRoute.StockOpname.route || value.matchesRoute(AppRoute.StockOpnameForm.route) ->
                AppCapability.MANAGE_INVENTORY
            value == AppRoute.IncomingGoods.route || value.matchesRoute(AppRoute.IncomingGoodsForm.route) ||
                value == AppRoute.PurchaseHistory.route -> AppCapability.PURCHASES
            value == AppRoute.Suppliers.route || value == AppRoute.SupplierForm.route -> AppCapability.SUPPLIERS
            value == AppRoute.SupplierDebts.route -> AppCapability.PAYABLES

            value == AppRoute.Customers.route || value.matchesRoute(AppRoute.CustomerDetail.route) ->
                AppCapability.CUSTOMERS
            value == AppRoute.AddCustomer.route || value.matchesRoute(AppRoute.EditCustomer.route) ->
                AppCapability.MANAGE_CUSTOMERS
            value == AppRoute.Receivables.route -> AppCapability.RECEIVABLES
            value == AppRoute.ReceivablePayments.route -> AppCapability.RECEIVABLE_PAYMENTS

            value == AppRoute.SalesTransactions.route || value.matchesRoute(AppRoute.AdminReceiptDetail.route) ->
                AppCapability.ALL_TRANSACTIONS
            value == AppRoute.CashSessionHistory.route || value == AppRoute.CashReconciliation.route ||
                value.matchesRoute(AppRoute.CashReconciliationDetail.route) ||
                value == AppRoute.CashExpenses.route -> AppCapability.CASH_SESSIONS

            value == AppRoute.Reports.route || value == AppRoute.OfflineReports.route -> AppCapability.REPORTS
            value == AppRoute.OperationalAudit.route -> AppCapability.AUDIT
            value == AppRoute.SyncCenter.route -> AppCapability.SYNC
            value == AppRoute.BackupRestore.route -> AppCapability.SERVER_BACKUP

            value == AppRoute.UserManagement.route || value == AppRoute.AddUser.route ||
                value.matchesRoute(AppRoute.EditUser.route) || value.matchesRoute(AppRoute.ChangeUserPassword.route) ||
                value.matchesRoute(AppRoute.ChangeUserPin.route) -> AppCapability.USER_MANAGEMENT
            value == AppRoute.SecurityLog.route -> AppCapability.SECURITY_SETTINGS
            value == AppRoute.AdminSettings.route -> AppCapability.STORE_SETTINGS
            value == AppRoute.CashierSettings.route -> AppCapability.DEVICE_SETTINGS
            value == AppRoute.AdminProfile.route || value == AppRoute.AdminEditProfile.route ||
                value == AppRoute.CashierProfile.route || value == AppRoute.CashierEditProfile.route -> AppCapability.ACCOUNT
            else -> null
        }
    }

    private fun String.matchesRoute(template: String): Boolean {
        val templateParts = template.substringBefore('?').lowercase().split('/')
        val routeParts = split('/')
        return routeParts.size == templateParts.size && routeParts.zip(templateParts).all { (actual, expected) ->
            expected.startsWith('{') || actual == expected
        }
    }
}

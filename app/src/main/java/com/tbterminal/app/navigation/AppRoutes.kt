package com.tbterminal.app.navigation

import com.tbterminal.app.data.session.SessionManager

sealed interface AppRoute {
    data object AdminReceiptDetail : AppRoute {
        override val route = "admin/sales/receipt/{transactionId}"
        fun createRoute(transactionId: String) = "admin/sales/receipt/$transactionId"
    }
    val route: String

    data object Login : AppRoute {
        override val route = "login"
    }

    data object Pin : AppRoute {
        override val route = "pin"
    }

    data object Dashboard : AppRoute {
        override val route = "dashboard"
    }

    data object BackofficeTransactions : AppRoute {
        override val route = "backoffice/transactions"
    }

    data object BackofficeFinance : AppRoute {
        override val route = "backoffice/finance"
    }

    data object BackofficeStock : AppRoute {
        override val route = "backoffice/stock"
    }

    data object BackofficeMore : AppRoute {
        override val route = "backoffice/more"
    }

    data object CashReconciliation : AppRoute {
        override val route = "admin/cash-reconciliation"
    }

    data object SalesTransactions : AppRoute {
        override val route = "admin/sales-transactions"
    }

    data object Reports : AppRoute {
        override val route = "admin/reports"
    }

    data object OfflineReports : AppRoute {
        override val route = "admin/reports/local"
    }

    data object StockReport : AppRoute {
        override val route = "admin/reports/stock"
    }

    data object PriceManagement : AppRoute {
        override val route = "admin/price-management"
    }

    data object OperationalAudit : AppRoute {
        override val route = "admin/operational-audit"
    }

    data object SyncCenter : AppRoute {
        override val route = "admin/sync-center"
    }

    data object BackupRestore : AppRoute {
        override val route = "admin/backup-restore"
    }

    data object CashierProfile : AppRoute { override val route = "cashier_profile" }
    data object CashierEditProfile : AppRoute { override val route = "cashier_profile/edit" }
    data object CashierSettings : AppRoute { override val route = "cashier_settings" }
    
    data object AdminProfile : AppRoute { override val route = "admin/profile" }
    data object AdminEditProfile : AppRoute { override val route = "admin/profile/edit" }
    data object AdminSettings : AppRoute {
        override val route = "admin/settings"
    }

    data object AdminStoreSettings : AppRoute {
        override val route = "admin/settings/store"
    }

    data object AdminDeviceSettings : AppRoute {
        override val route = "admin/settings/device"
    }

    data object CashierPos : AppRoute {
        override val route = "cashier/pos"
    }

    data object CashierCashSession : AppRoute {
        override val route = "cashier/cash-session"
    }

    data object CashierCart : AppRoute {
        override val route = "cashier/cart"
    }

    data object CashierPayment : AppRoute {
        override val route = "cashier/payment"
    }

    data object CashierStockCheck : AppRoute {
        override val route = "cashier/stock-check"
    }

    data object CashierTransactionHistory : AppRoute {
        override val route = "cashier/transactions"
    }

    data object CashierReceiptDetail : AppRoute {
        const val TRANSACTION_ID_ARG = "transactionId"
        override val route = "cashier/transactions/{$TRANSACTION_ID_ARG}"

        fun createRoute(transactionId: String): String {
            return "cashier/transactions/$transactionId"
        }
    }

    data object UserManagement : AppRoute {
        override val route = "user-management"
    }

    data object SecurityLog : AppRoute {
        override val route = "security-log"
    }

    data object Products : AppRoute {
        override val route = "admin/products"
    }

    data object AddProduct : AppRoute {
        override val route = "admin/products/add"
    }

    data object EditProduct : AppRoute {
        const val PRODUCT_ID_ARG = "productId"
        override val route = "admin/products/edit/{$PRODUCT_ID_ARG}"

        fun createRoute(productId: String): String {
            return "admin/products/edit/$productId"
        }
    }

    data object ProductDetail : AppRoute {
        const val PRODUCT_ID_ARG = "productId"
        override val route = "admin/products/detail/{$PRODUCT_ID_ARG}"

        fun createRoute(productId: String): String {
            return "admin/products/detail/$productId"
        }
    }

    data object ProductCategories : AppRoute {
        override val route = "admin/products/categories"
    }

    data object ProductUnits : AppRoute {
        override val route = "admin/products/units"
    }

    data object StockOpname : AppRoute {
        override val route = "admin/stock-opname"
    }

    data object StockOpnameForm : AppRoute {
        const val PRODUCT_ID_ARG = "productId"
        override val route = "admin/stock-opname/form?$PRODUCT_ID_ARG={$PRODUCT_ID_ARG}"

        fun createRoute(productId: String? = null): String {
            return if (productId.isNullOrBlank()) {
                "admin/stock-opname/form"
            } else {
                "admin/stock-opname/form?$PRODUCT_ID_ARG=$productId"
            }
        }
    }

    data object IncomingGoods : AppRoute {
        override val route = "admin/incoming-goods"
    }

    data object Suppliers : AppRoute {
        override val route = "admin/suppliers"
    }

    data object SupplierForm : AppRoute {
        override val route = "admin/suppliers/form"
    }

    data object PurchaseHistory : AppRoute {
        override val route = "admin/purchases"
    }

    data object IncomingGoodsForm : AppRoute {
        const val PRODUCT_ID_ARG = "productId"
        override val route = "admin/incoming-goods/form?$PRODUCT_ID_ARG={$PRODUCT_ID_ARG}"

        fun createRoute(productId: String? = null): String {
            return if (productId.isNullOrBlank()) {
                "admin/incoming-goods/form"
            } else {
                "admin/incoming-goods/form?$PRODUCT_ID_ARG=$productId"
            }
        }
    }

    data object SupplierDebts : AppRoute {
        override val route = "admin/supplier-debts"
    }

    data object SupplierDebtDetail : AppRoute {
        const val SUPPLIER_ID_ARG = "supplierId"
        override val route = "admin/supplier-debts/supplier/{$SUPPLIER_ID_ARG}"

        fun createRoute(supplierId: String): String {
            return "admin/supplier-debts/supplier/$supplierId"
        }
    }
    data object OpeningSupplierDebtForm : AppRoute {
        const val SUPPLIER_ID_ARG = "supplierId"
        override val route = "admin/supplier-debts/opening-balance?$SUPPLIER_ID_ARG={$SUPPLIER_ID_ARG}"

        fun createRoute(supplierId: String? = null): String {
            return if (supplierId.isNullOrBlank()) {
                "admin/supplier-debts/opening-balance"
            } else {
                "admin/supplier-debts/opening-balance?$SUPPLIER_ID_ARG=$supplierId"
            }
        }
    }

    data object AdjustmentSupplierDebtForm : AppRoute {
        const val SUPPLIER_ID_ARG = "supplierId"
        override val route = "admin/supplier-debts/adjustment?$SUPPLIER_ID_ARG={$SUPPLIER_ID_ARG}"

        fun createRoute(supplierId: String? = null): String {
            return if (supplierId.isNullOrBlank()) {
                "admin/supplier-debts/adjustment"
            } else {
                "admin/supplier-debts/adjustment?$SUPPLIER_ID_ARG=$supplierId"
            }
        }
    }


    data object CashSessionHistory : AppRoute {
        override val route = "admin/cash-sessions"
    }

    data object CashReconciliationDetail : AppRoute {
        const val SESSION_ID_ARG = "sessionId"
        override val route = "admin/cash-reconciliation/detail?$SESSION_ID_ARG={$SESSION_ID_ARG}"

        fun createRoute(sessionId: String? = null): String {
            return if (sessionId.isNullOrBlank()) {
                "admin/cash-reconciliation/detail"
            } else {
                "admin/cash-reconciliation/detail?$SESSION_ID_ARG=$sessionId"
            }
        }
    }

    data object CashExpenses : AppRoute {
        override val route = "admin/cash-expenses"
    }

    data object Receivables : AppRoute {
        override val route = "admin/receivables"
    }

    data object OpeningReceivableForm : AppRoute {
        const val CUSTOMER_ID_ARG = "customerId"
        override val route = "admin/receivables/opening-balance?$CUSTOMER_ID_ARG={$CUSTOMER_ID_ARG}"

        fun createRoute(customerId: String? = null): String {
            return if (customerId.isNullOrBlank()) {
                "admin/receivables/opening-balance"
            } else {
                "admin/receivables/opening-balance?$CUSTOMER_ID_ARG=$customerId"
            }
        }
    }

    data object AdjustmentReceivableForm : AppRoute {
        const val CUSTOMER_ID_ARG = "customerId"
        override val route = "admin/receivables/adjustment?$CUSTOMER_ID_ARG={$CUSTOMER_ID_ARG}"

        fun createRoute(customerId: String? = null): String {
            return if (customerId.isNullOrBlank()) {
                "admin/receivables/adjustment"
            } else {
                "admin/receivables/adjustment?$CUSTOMER_ID_ARG=$customerId"
            }
        }
    }

    data object ReceivableCustomerDetail : AppRoute {
        const val CUSTOMER_ID_ARG = "customerId"
        override val route = "admin/receivables/customer/{$CUSTOMER_ID_ARG}"

        fun createRoute(customerId: String): String {
            return "admin/receivables/customer/$customerId"
        }
    }

    data object ReceivablePayments : AppRoute {
        override val route = "admin/receivables/payments"
    }

    data object Customers : AppRoute {
        override val route = "admin/customers"
    }

    data object AddCustomer : AppRoute {
        override val route = "admin/customers/add"
    }

    data object EditCustomer : AppRoute {
        const val CUSTOMER_ID_ARG = "customerId"
        override val route = "admin/customers/edit/{$CUSTOMER_ID_ARG}"

        fun createRoute(customerId: String): String {
            return "admin/customers/edit/$customerId"
        }
    }

    data object CustomerDetail : AppRoute {
        const val CUSTOMER_ID_ARG = "customerId"
        override val route = "admin/customers/detail/{$CUSTOMER_ID_ARG}"

        fun createRoute(customerId: String): String {
            return "admin/customers/detail/$customerId"
        }
    }

    data object AddUser : AppRoute {
        override val route = "user-management/add"
    }

    data object EditUser : AppRoute {
        const val USER_ID_ARG = "userId"
        override val route = "user-management/edit/{$USER_ID_ARG}"

        fun createRoute(userId: String): String {
            return "user-management/edit/$userId"
        }
    }

    data object ChangeUserPassword : AppRoute {
        const val USER_ID_ARG = "userId"
        override val route = "user-management/edit/{$USER_ID_ARG}/password"

        fun createRoute(userId: String): String {
            return "user-management/edit/$userId/password"
        }
    }

    data object ChangeUserPin : AppRoute {
        const val USER_ID_ARG = "userId"
        override val route = "user-management/edit/{$USER_ID_ARG}/pin"

        fun createRoute(userId: String): String {
            return "user-management/edit/$userId/pin"
        }
    }
}


internal fun SessionManager.resolveStartDestination(): String {
    if (!hasAccessToken()) {
        return AppRoute.Login.route
    }

    if (readSessionUser() == null) {
        clearSession()
        return AppRoute.Login.route
    }

    return if (requiresPinUnlock()) {
        AppRoute.Pin.route
    } else {
        AppRoute.Dashboard.route
    }
}

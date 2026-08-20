package com.tbterminal.app.navigation

internal object AppRouteAccessPolicy {
    private const val ROLE_OWNER = "owner"
    private const val ROLE_ADMIN = "admin"
    private const val ROLE_CASHIER = "kasir"

    fun isAllowed(route: String?, role: String?): Boolean {
        if (route == AppRoute.Login.route || route == AppRoute.Pin.route) return true

        val normalizedRoute = route?.lowercase() ?: return false
        return when (role?.lowercase()) {
            ROLE_OWNER -> true
            ROLE_ADMIN -> !normalizedRoute.isOwnerOnlyRoute()
            ROLE_CASHIER -> normalizedRoute == AppRoute.Dashboard.route ||
                normalizedRoute.startsWith("cashier/") ||
                normalizedRoute == AppRoute.CashierProfile.route ||
                normalizedRoute == AppRoute.CashierSettings.route
            else -> false
        }
    }

    private fun String.isOwnerOnlyRoute(): Boolean {
        return startsWith(AppRoute.UserManagement.route) || this == AppRoute.SecurityLog.route
    }
}

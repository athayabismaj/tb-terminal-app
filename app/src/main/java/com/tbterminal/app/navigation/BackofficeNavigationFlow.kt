package com.tbterminal.app.navigation

import androidx.navigation.NavHostController
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.backofficePageTitle
import com.tbterminal.app.ui.dashboard.admin.backofficePageTitleRes
import com.tbterminal.app.ui.dashboard.admin.backofficeSection
import com.tbterminal.app.ui.dashboard.isOwnerPersona

/** Primary tabs reset to the dashboard; drill-down pages retain the page they were opened from. */
internal fun isBackofficePrimaryRoute(route: String?, role: String?): Boolean {
    if (!AppAccessPolicy.can(role, AppCapability.BACKOFFICE)) return false
    return route in listOf(
        AppRoute.Dashboard.route, AppRoute.BackofficeTransactions.route,
        AppRoute.BackofficeFinance.route, AppRoute.BackofficeStock.route,
        AppRoute.BackofficeMore.route,
    )
}

internal fun NavHostController.navigateBackofficePage(route: String, role: String?) {
    if (currentBackStackEntry?.destination?.route == route) return
    if (isBackofficePrimaryRoute(route, role)) {
        navigate(route) {
            popUpTo(AppRoute.Dashboard.route)
            launchSingleTop = true
        }
    } else {
        // Menu is a primary destination. Reuse the existing entry when returning from a child page.
        if (route == AppRoute.BackofficeMore.route && popBackStack(route, false)) return
        navigate(route) { launchSingleTop = true }
    }
}

/** A back-to-list action must not push a second copy of the list above its detail/form. */
internal fun NavHostController.returnToBackofficePage(route: String, role: String?) {
    if (isOwnerPersona(role) && popBackStack(route, false)) return
    navigate(route) { launchSingleTop = true }
}

internal fun backofficeRouteTitle(route: String?, role: String?): String? = when (route) {
    null -> null
    else -> AdminDestination.entries.firstOrNull { it.adminRouteOrNull(role) == route }?.backofficePageTitle()
}

internal fun backofficeRouteTitleRes(route: String?, role: String?): Int? {
    if (route == null) return null
    return AdminDestination.entries.firstOrNull { it.adminRouteOrNull(role) == route }?.backofficePageTitleRes()
}

internal fun ownerMenuParentRoute(route: String?, role: String?): String? {
    if (!isOwnerPersona(role) || route == null || route == AppRoute.BackofficeMore.route) return null
    val destination = AdminDestination.entries.firstOrNull { it.adminRouteOrNull(role) == route } ?: return null
    return if (destination.backofficeSection(role) == com.tbterminal.app.ui.dashboard.BackofficeSection.MENU) {
        AppRoute.BackofficeMore.route
    } else null
}

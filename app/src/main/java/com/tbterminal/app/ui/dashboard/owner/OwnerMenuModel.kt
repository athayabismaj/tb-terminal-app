package com.tbterminal.app.ui.dashboard.owner

import androidx.annotation.StringRes
import com.tbterminal.app.R
import com.tbterminal.app.navigation.AppAccessPolicy
import com.tbterminal.app.navigation.AppCapability
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.isOwnerPersona

internal enum class OwnerMenuAction(
    @param:StringRes val titleRes: Int,
    val capability: AppCapability,
    val destination: AdminDestination,
) {
    CUSTOMERS(R.string.owner_menu_customers, AppCapability.CUSTOMERS, AdminDestination.Customers),
    SUPPLIERS(R.string.owner_menu_suppliers, AppCapability.SUPPLIERS, AdminDestination.Suppliers),
    USERS(R.string.owner_menu_users, AppCapability.USER_MANAGEMENT, AdminDestination.UserManagement),
    REPORTS(R.string.owner_menu_reports, AppCapability.REPORTS, AdminDestination.Reports),
    DEVICE_REPORTS(R.string.owner_menu_device_reports, AppCapability.REPORTS, AdminDestination.LocalReports),
    BACKUP(R.string.owner_menu_backup, AppCapability.SERVER_BACKUP, AdminDestination.BackupRestore),
    SETTINGS(R.string.owner_menu_settings, AppCapability.STORE_SETTINGS, AdminDestination.Settings),
    SYNC(R.string.owner_menu_sync, AppCapability.SYNC, AdminDestination.SyncCenter),
    ACTIVITY(R.string.owner_menu_activity, AppCapability.AUDIT, AdminDestination.OperationalAudit),
    SECURITY_LOG(R.string.owner_menu_security_log, AppCapability.SECURITY_SETTINGS, AdminDestination.SecurityLog),
}

internal data class OwnerMenuGroup(
    @param:StringRes val titleRes: Int,
    val actions: List<OwnerMenuAction>,
)

internal fun ownerMenuGroups(role: String?): List<OwnerMenuGroup> {
    if (!isOwnerPersona(role) || !AppAccessPolicy.can(role, AppCapability.BACKOFFICE)) return emptyList()
    return listOf(
        OwnerMenuGroup(
            R.string.owner_menu_group_store,
            listOf(OwnerMenuAction.CUSTOMERS, OwnerMenuAction.SUPPLIERS, OwnerMenuAction.USERS),
        ),
        OwnerMenuGroup(
            R.string.owner_menu_group_reports,
            listOf(OwnerMenuAction.REPORTS, OwnerMenuAction.DEVICE_REPORTS, OwnerMenuAction.BACKUP),
        ),
        OwnerMenuGroup(
            R.string.owner_menu_group_app,
            listOf(
                OwnerMenuAction.SETTINGS,
                OwnerMenuAction.SYNC,
                OwnerMenuAction.ACTIVITY,
                OwnerMenuAction.SECURITY_LOG,
            ),
        ),
    ).map { group ->
        group.copy(actions = group.actions.filter { AppAccessPolicy.can(role, it.capability) })
    }.filter { it.actions.isNotEmpty() }
}

internal fun ownerMenuColumns(groups: List<OwnerMenuGroup>, expanded: Boolean): List<List<OwnerMenuGroup>> {
    if (!expanded) return listOf(groups)
    return listOf(
        groups.filterIndexed { index, _ -> index % 2 == 0 },
        groups.filterIndexed { index, _ -> index % 2 == 1 },
    ).filter { it.isNotEmpty() }
}

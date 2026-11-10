package com.tbterminal.app.ui.dashboard.owner

import androidx.annotation.StringRes
import com.tbterminal.app.R
import com.tbterminal.app.navigation.AppAccessPolicy
import com.tbterminal.app.navigation.AppCapability
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.isOwnerPersona

internal enum class OwnerMenuAction(
    @param:StringRes val titleRes: Int,
    val subtitle: String,
    val capability: AppCapability,
    val destination: AdminDestination? = null,
) {
    CUSTOMERS(R.string.owner_menu_customers, "Kontak dan batas kredit", AppCapability.CUSTOMERS, AdminDestination.Customers),
    SUPPLIERS(R.string.owner_menu_suppliers, "Kontak supplier toko", AppCapability.SUPPLIERS, AdminDestination.Suppliers),
    USERS(R.string.owner_menu_users, "Kelola akun dan hak akses", AppCapability.USER_MANAGEMENT, AdminDestination.UserManagement),
    REPORTS(R.string.owner_menu_reports, "Ringkasan bisnis dan ekspor", AppCapability.FINANCIAL_ANALYTICS, AdminDestination.Reports),
    DEVICE_REPORTS(R.string.owner_menu_device_reports, "Data lokal di perangkat ini", AppCapability.REPORTS, AdminDestination.LocalReports),
    BACKUP(R.string.owner_menu_backup, "Cadangan data server & lokal", AppCapability.SERVER_BACKUP, AdminDestination.BackupRestore),
    STORE_SETTINGS(R.string.owner_menu_store_settings, "Nama, alamat, dan header struk", AppCapability.STORE_SETTINGS, AdminDestination.StoreSettings),
    DEVICE_SETTINGS(R.string.owner_menu_device_settings, "Printer thermal dan nota kasir", AppCapability.DEVICE_SETTINGS, AdminDestination.DeviceSettings),
    SETTINGS(R.string.owner_menu_settings, "Printer, toko, dan perangkat", AppCapability.DEVICE_SETTINGS, AdminDestination.Settings),
    SYNC(R.string.owner_menu_sync, "Antrean & status data offline", AppCapability.SYNC, AdminDestination.SyncCenter),
    ACTIVITY(R.string.owner_menu_activity, "Audit perubahan data operasional", AppCapability.AUDIT, AdminDestination.OperationalAudit),
    SECURITY_LOG(R.string.owner_menu_security_log, "Login, PIN, dan keamanan akun", AppCapability.SECURITY_SETTINGS, AdminDestination.SecurityLog),
    LOGOUT(R.string.profile_logout, "Akhiri sesi akun pemilik", AppCapability.ACCOUNT, null),
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
            listOf(OwnerMenuAction.REPORTS, OwnerMenuAction.DEVICE_REPORTS),
        ),
        OwnerMenuGroup(
            R.string.owner_menu_group_system,
            listOf(
                OwnerMenuAction.STORE_SETTINGS,
                OwnerMenuAction.DEVICE_SETTINGS,
                OwnerMenuAction.SYNC,
                OwnerMenuAction.BACKUP,
                OwnerMenuAction.ACTIVITY,
                OwnerMenuAction.SECURITY_LOG,
            ),
        ),
        OwnerMenuGroup(
            R.string.owner_menu_group_account,
            listOf(OwnerMenuAction.LOGOUT),
        ),
    ).map { group ->
        group.copy(actions = group.actions.filter { AppAccessPolicy.can(role, it.capability) })
    }.filter { it.actions.isNotEmpty() }
}

internal fun ownerMenuColumns(groups: List<OwnerMenuGroup>, expanded: Boolean): List<List<OwnerMenuGroup>> {
    if (!expanded) return listOf(groups)
    val columns = List(2) { mutableListOf<OwnerMenuGroup>() }
    groups.sortedByDescending { it.actions.size }.forEach { group ->
        columns.minBy { column -> column.sumOf { it.actions.size + 1 } }.add(group)
    }
    return columns.filter { it.isNotEmpty() }
}

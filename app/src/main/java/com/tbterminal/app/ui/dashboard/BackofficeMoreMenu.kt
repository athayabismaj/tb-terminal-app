package com.tbterminal.app.ui.dashboard

import com.tbterminal.app.navigation.AppAccessPolicy
import com.tbterminal.app.navigation.AppCapability
import com.tbterminal.app.ui.components.TbWindowWidthClass
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

/** One entry per destination; product configuration remains in the Stock tab. */
internal enum class MoreMenuAction(
    val title: String,
    val subtitle: String,
    val capability: AppCapability,
    val destination: AdminDestination? = null,
) {
    CUSTOMERS("Pelanggan", "Kontak dan batas kredit", AppCapability.CUSTOMERS, AdminDestination.Customers),
    SUPPLIERS("Supplier", "Kontak supplier toko", AppCapability.SUPPLIERS, AdminDestination.Suppliers),
    REPORTS("Laporan bisnis", "Ringkasan dan ekspor", AppCapability.REPORTS, AdminDestination.Reports),
    LOCAL_REPORTS("Laporan perangkat", "Data di perangkat ini", AppCapability.REPORTS, AdminDestination.LocalReports),
    USERS("Pengguna & akses", "Kelola akun dan akses", AppCapability.USER_MANAGEMENT),
    ACTIVITY("Riwayat aktivitas", "Perubahan data toko", AppCapability.AUDIT, AdminDestination.OperationalAudit),
    SECURITY_LOG("Log keamanan", "Login dan keamanan akun", AppCapability.SECURITY_SETTINGS),
    BACKUP("Cadangan data", "Data perangkat dan server", AppCapability.SERVER_BACKUP, AdminDestination.BackupRestore),
    SETTINGS("Pengaturan aplikasi", "Printer, toko, dan perangkat", AppCapability.STORE_SETTINGS, AdminDestination.Settings),
    SYNC("Sinkronisasi", "Status data offline", AppCapability.SYNC, AdminDestination.SyncCenter),
    PROFILE("Profil & keamanan", "Profil, password, dan PIN", AppCapability.ACCOUNT, AdminDestination.Profile),
    LOGOUT("Keluar", "Akhiri sesi akun", AppCapability.ACCOUNT),
}

internal data class MoreMenuSection(val title: String, val actions: List<MoreMenuAction>)

internal fun moreMenuSections(role: String): List<MoreMenuSection> {
    if (isOwnerPersona(role) || !AppAccessPolicy.can(role, AppCapability.BACKOFFICE)) return emptyList()
    return listOf(
        MoreMenuSection("Data toko", listOf(MoreMenuAction.CUSTOMERS, MoreMenuAction.SUPPLIERS)),
        MoreMenuSection("Laporan", listOf(MoreMenuAction.REPORTS, MoreMenuAction.LOCAL_REPORTS)),
        MoreMenuSection("Administrasi", listOf(
            MoreMenuAction.USERS, MoreMenuAction.ACTIVITY, MoreMenuAction.SECURITY_LOG, MoreMenuAction.BACKUP,
        )),
        MoreMenuSection("Perangkat", listOf(MoreMenuAction.SETTINGS, MoreMenuAction.SYNC)),
        MoreMenuSection("Akun", listOf(MoreMenuAction.PROFILE, MoreMenuAction.LOGOUT)),
    ).map { section ->
        section.copy(actions = section.actions.filter { AppAccessPolicy.can(role, it.capability) })
    }.filter { it.actions.isNotEmpty() }
}

internal fun moreMenuColumnCount(widthClass: TbWindowWidthClass): Int =
    if (widthClass == TbWindowWidthClass.Compact) 1 else 2

/** Balance whole groups, avoiding empty grid cells beside the longer administration group. */
internal fun moreMenuColumns(
    sections: List<MoreMenuSection>,
    widthClass: TbWindowWidthClass,
): List<List<MoreMenuSection>> {
    val columns = List(moreMenuColumnCount(widthClass)) { mutableListOf<MoreMenuSection>() }
    val orderedSections = if (columns.size > 1) sections.sortedByDescending { it.actions.size } else sections
    orderedSections.forEach { section ->
        columns.minBy { column -> column.sumOf { it.actions.size + 1 } }.add(section)
    }
    return columns
}

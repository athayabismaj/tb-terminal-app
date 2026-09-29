package com.tbterminal.app.ui.users

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tbterminal.app.ui.theme.TbterminalappTheme

@Preview(name = "Pengguna & akses - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun UserManagementPhonePreview() {
    UserManagementPreviewContent()
}

@Preview(name = "Pengguna & akses - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun UserManagementTabletPreview() {
    UserManagementPreviewContent()
}

@Composable
private fun UserManagementPreviewContent() {
    TbterminalappTheme {
        UserManagementContent(
            uiState = UserManagementUiState(
                staffMembers = previewStaff(),
                totalStaff = 3,
            ),
        )
    }
}

private fun previewStaff() = listOf(
    StaffMember(
        id = "owner-1",
        name = "Pemilik Toko",
        email = "owner@tbterminal.id",
        rawEmail = "owner@tbterminal.id",
        username = "owner",
        roleId = "role-owner",
        role = StaffRole.Owner,
        status = StaffStatus.Active,
        lastLogin = "Hari ini, 08.15",
        initials = "PT",
    ),
    StaffMember(
        id = "admin-1",
        name = "Admin Operasional",
        email = "admin@tbterminal.id",
        rawEmail = "admin@tbterminal.id",
        username = "admin",
        roleId = "role-admin",
        role = StaffRole.Admin,
        status = StaffStatus.Active,
        lastLogin = "Kemarin, 17.40",
        initials = "AO",
    ),
    StaffMember(
        id = "cashier-1",
        name = "Kasir Cadangan",
        email = "kasir@tbterminal.id",
        rawEmail = "kasir@tbterminal.id",
        username = "kasir2",
        roleId = "role-cashier",
        role = StaffRole.Kasir,
        status = StaffStatus.Inactive,
        lastLogin = "12 Sep 2026",
        initials = "KC",
    ),
)

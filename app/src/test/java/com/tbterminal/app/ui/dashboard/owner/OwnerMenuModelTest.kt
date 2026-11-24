package com.tbterminal.app.ui.dashboard.owner

import com.tbterminal.app.navigation.AppAccessPolicy
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerMenuModelTest {
    @Test
    fun `owner menu omits actions already in profile account sheet and keeps unique destinations`() {
        val groups = ownerMenuGroups("OWNER")
        val actions = groups.flatMap { it.actions }
        val destinations = actions.mapNotNull { it.destination }

        assertEquals(2, groups.size)
        assertEquals(7, actions.size)
        assertEquals(7, destinations.size)
        assertEquals(destinations.size, destinations.distinct().size)
        assertFalse(OwnerMenuAction.STORE_SETTINGS in actions)
        assertFalse(OwnerMenuAction.DEVICE_SETTINGS in actions)
        assertFalse(OwnerMenuAction.LOGOUT in actions)
        assertFalse(AdminDestination.Profile in destinations)
        assertFalse(AdminDestination.StoreSettings in destinations)
        assertFalse(AdminDestination.DeviceSettings in destinations)
        assertFalse(AdminDestination.Reports in destinations)
        assertFalse(AdminDestination.LocalReports in destinations)
    }

    @Test
    fun `owner persona is explicit and other roles never receive owner menu`() {
        assertEquals(ownerMenuGroups("OWNER"), ownerMenuGroups(" owner "))
        listOf("ADMIN", "KASIR", "", "unknown", null).forEach { role ->
            assertTrue(ownerMenuGroups(role).isEmpty())
        }
    }

    @Test
    fun `policy filters every owner action`() {
        val destinations = ownerMenuGroups("OWNER").flatMap { it.actions }.mapNotNull { it.destination }
        assertTrue(AdminDestination.UserManagement in destinations)
        assertTrue(AdminDestination.SecurityLog in destinations)
        assertTrue(AdminDestination.BackupRestore in destinations)
        assertFalse(destinations.contains(AdminDestination.Profile))
        assertTrue(ownerMenuGroups("OWNER").flatMap { it.actions }.all {
            AppAccessPolicy.can("OWNER", it.capability)
        })
    }

    @Test
    fun `adaptive columns retain each group exactly once`() {
        val groups = ownerMenuGroups("OWNER")
        assertEquals(listOf(groups), ownerMenuColumns(groups, expanded = false))
        val tablet = ownerMenuColumns(groups, expanded = true)
        assertEquals(2, tablet.size)
        assertEquals(groups.toSet(), tablet.flatten().toSet())
        assertEquals(groups.size, tablet.flatten().size)
    }
}

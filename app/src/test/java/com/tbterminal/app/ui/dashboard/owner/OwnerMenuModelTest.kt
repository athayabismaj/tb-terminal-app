package com.tbterminal.app.ui.dashboard.owner

import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.navigation.AppAccessPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerMenuModelTest {
    @Test
    fun `owner menu contains three clear groups and unique destinations`() {
        val groups = ownerMenuGroups("OWNER")
        val actions = groups.flatMap { it.actions }

        assertEquals(3, groups.size)
        assertEquals(10, actions.size)
        assertEquals(actions.size, actions.distinctBy { it.destination }.size)
        assertTrue(AdminDestination.Profile !in actions.map { it.destination })
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
        val destinations = ownerMenuGroups("OWNER").flatMap { it.actions }.map { it.destination }
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
        assertEquals(listOf(groups[0], groups[2]), tablet[0])
        assertEquals(listOf(groups[1]), tablet[1])
    }
}

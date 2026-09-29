package com.tbterminal.app.ui.dashboard

import com.tbterminal.app.ui.components.TbWindowWidthClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackofficeMoreMenuTest {
    @Test
    fun `owner uses dedicated menu instead of admin more content`() {
        assertTrue(moreMenuSections("OWNER").isEmpty())
        assertFalse(moreMenuSections("ADMIN").isEmpty())
    }

    @Test
    fun `admin does not gain owner only actions`() {
        val actions = moreMenuSections("ADMIN").flatMap { it.actions }
        assertFalse(MoreMenuAction.USERS in actions)
        assertFalse(MoreMenuAction.SECURITY_LOG in actions)
        assertFalse(MoreMenuAction.BACKUP in actions)
        assertTrue(MoreMenuAction.ACTIVITY in actions)
        assertTrue(MoreMenuAction.SETTINGS in actions)
        assertEquals(9, actions.size)
    }

    @Test
    fun `unknown roles and cashier do not receive backoffice menu`() {
        listOf("KASIR", "", "unknown").forEach { assertTrue(moreMenuSections(it).isEmpty()) }
    }

    @Test
    fun `admin menu remains adaptive without duplicating groups`() {
        val sections = moreMenuSections("ADMIN")
        assertEquals(1, moreMenuColumnCount(TbWindowWidthClass.Compact))
        assertEquals(2, moreMenuColumnCount(TbWindowWidthClass.Medium))
        assertEquals(2, moreMenuColumnCount(TbWindowWidthClass.Expanded))
        assertEquals(listOf(sections), moreMenuColumns(sections, TbWindowWidthClass.Compact))
        assertEquals(sections.toSet(), moreMenuColumns(sections, TbWindowWidthClass.Expanded).flatten().toSet())
    }
}

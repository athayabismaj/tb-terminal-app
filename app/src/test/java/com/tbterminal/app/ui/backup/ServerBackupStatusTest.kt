package com.tbterminal.app.ui.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerBackupStatusTest {
    @Test
    fun onlyFinalStatusesStopPolling() {
        listOf("PENDING", "QUEUED", "RUNNING", "VALIDATED").forEach {
            assertFalse(it.isTerminalBackupStatus())
        }
        assertTrue("SUCCEEDED".isTerminalBackupStatus())
        assertTrue("FAILED".isTerminalBackupStatus())
    }

    @Test
    fun serverRoleAndAmbiguousConfirmPolicyAreSafe() {
        assertTrue(canManageServerDatabaseBackup("owner"))
        assertFalse(canManageServerDatabaseBackup("ADMIN"))
        assertFalse(canManageServerDatabaseBackup("kasir"))
        assertTrue(ambiguousRestoreRecovery() == RestoreAmbiguousRecovery.POLL_STATUS_ONLY)
    }
}

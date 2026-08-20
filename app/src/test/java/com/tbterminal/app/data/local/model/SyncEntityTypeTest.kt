package com.tbterminal.app.data.local.model

import com.tbterminal.app.data.local.database.LocalConverters
import com.tbterminal.app.data.local.entity.SyncQueueEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SyncEntityTypeTest {
    @Test
    fun supportedOfflineTypesAreExplicitAndLimited() {
        assertEquals(
            setOf(SyncEntityType.TRANSACTION, SyncEntityType.CASH_SESSION, SyncEntityType.CASH_EXPENSE),
            SyncEntityType.supported
        )
    }

    @Test
    fun unsupportedTypeCannotCreateANewQueueItem() {
        assertThrows(IllegalArgumentException::class.java) {
            SyncQueueEntity(
                entityType = SyncEntityType.UNSUPPORTED,
                entityLocalId = 1,
                operation = SyncOperation.CREATE,
                payloadJson = "{}"
            )
        }
    }

    @Test
    fun legacyUnknownDatabaseValueIsQuarantined() {
        assertEquals(SyncEntityType.UNSUPPORTED, LocalConverters().toSyncEntityType("PRODUCT"))
    }
}

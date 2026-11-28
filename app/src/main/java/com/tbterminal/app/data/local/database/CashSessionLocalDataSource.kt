package com.tbterminal.app.data.local.database

import com.tbterminal.app.data.local.dao.CashExpenseDao
import com.tbterminal.app.data.local.dao.CashSessionDao
import com.tbterminal.app.data.local.dao.SyncQueueDao
import com.tbterminal.app.data.local.dao.TransactionDao
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.mapper.hasUnsyncedLocalChanges
import com.tbterminal.app.data.local.mapper.toLocalCashSessionEntity
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncOperation
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.sync.CashSessionAdoptionDecision
import com.tbterminal.app.data.sync.CashSessionAdoptionPolicy

class CashSessionLocalDataSource(
    private val cashSessionDao: CashSessionDao,
    private val transactionDao: TransactionDao? = null,
    private val cashExpenseDao: CashExpenseDao? = null,
    private val syncQueueDao: SyncQueueDao? = null,
) {
    suspend fun getCachedCashSessionByServerId(serverId: String): LocalCashSessionEntity? {
        return cashSessionDao.getByServerId(serverId)
    }

    suspend fun getLatestOpenMirroredSession(cashierUserId: String? = null): LocalCashSessionEntity? {
        val normalizedCashierUserId = cashierUserId?.trim()?.takeIf(String::isNotBlank)
        return if (normalizedCashierUserId != null) {
            cashSessionDao.getLatestMirroredByStatusAndCashier(
                status = CASH_SESSION_OPEN,
                cashierUserId = normalizedCashierUserId
            )
        } else {
            cashSessionDao.getLatestMirroredByStatus(CASH_SESSION_OPEN)
        }
    }

    suspend fun getLatestOpenSession(cashierUserId: String? = null): LocalCashSessionEntity? {
        val normalizedCashierUserId = cashierUserId?.trim()?.takeIf(String::isNotBlank)
        return if (normalizedCashierUserId != null) {
            cashSessionDao.getLatestByStatusAndCashier(
                status = CASH_SESSION_OPEN,
                cashierUserId = normalizedCashierUserId
            )
        } else {
            cashSessionDao.getLatestByStatus(CASH_SESSION_OPEN)
        }
    }

    suspend fun getUnsyncedOfflineSessions(): List<LocalCashSessionEntity> {
        return cashSessionDao.getUnsyncedOfflineSessions()
    }

    suspend fun mirrorRemoteSession(session: CashSession): LocalCashSessionEntity {
        val existing = cashSessionDao.getByServerId(session.id)
        if (existing == null &&
            session.status.equals(CASH_SESSION_OPEN, ignoreCase = true) &&
            transactionDao != null &&
            cashExpenseDao != null &&
            syncQueueDao != null
        ) {
            val localOpen = cashSessionDao.getLatestByStatusAndCashier(CASH_SESSION_OPEN, session.userId)
            if (localOpen != null && localOpen.serverId.isNullOrBlank()) {
                val openQueue = syncQueueDao.getLatestForEntityAndOperation(
                    entityLocalId = localOpen.localId,
                    entityType = SyncEntityType.CASH_SESSION,
                    operation = SyncOperation.CREATE,
                )
                if (CashSessionAdoptionPolicy.parseAdoptRemoteIfActive(openQueue?.payloadJson)) {
                    val decision = CashSessionAdoptionPolicy.evaluateAdoption(
                        localSession = localOpen,
                        remoteServerId = session.id,
                        remoteCashierUserId = session.userId,
                        remoteStatus = session.status,
                        remoteOpenedAtMillis = CashSessionAdoptionPolicy.parseIsoTimestampMillis(session.openedAt),
                        existingByServerId = null,
                        transactions = transactionDao.getByCashSession(localOpen.localId),
                        expenses = cashExpenseDao.getByCashSession(localOpen.localId),
                    )
                    if (decision == CashSessionAdoptionDecision.Eligible) {
                        val now = System.currentTimeMillis()
                        val nextSyncStatus = if (localOpen.status.equals("CLOSED", ignoreCase = true)) {
                            SyncStatus.PENDING
                        } else {
                            SyncStatus.SYNCED
                        }
                        cashSessionDao.markOpenSynced(
                            localId = localOpen.localId,
                            serverId = session.id,
                            syncStatus = nextSyncStatus,
                            syncedAt = now,
                            updatedAt = now,
                        )
                        transactionDao.updateCashSessionServerIdForLocalSession(
                            cashSessionLocalId = localOpen.localId,
                            serverId = session.id,
                            updatedAt = now,
                        )
                        cashExpenseDao.updateCashSessionServerIdForLocalSession(
                            cashSessionLocalId = localOpen.localId,
                            serverId = session.id,
                            updatedAt = now,
                        )
                        openQueue?.let { queue ->
                            syncQueueDao.updateStatus(
                                id = queue.id,
                                status = SyncStatus.SYNCED,
                                lastError = null,
                                lastErrorCode = null,
                                retryIncrement = 0,
                                updatedAt = now,
                            )
                        }
                        return cashSessionDao.getByLocalId(localOpen.localId)
                            ?: localOpen.copy(serverId = session.id, syncStatus = nextSyncStatus, syncedAt = now, updatedAt = now)
                    }
                }
            }
        }
        if (existing?.hasUnsyncedLocalChanges() == true) {
            return existing
        }
        val entity = session.toLocalCashSessionEntity(existing = existing)
        val localId = cashSessionDao.upsert(entity)
        return cashSessionDao.getByLocalId(entity.localId.takeIf { it > 0L } ?: localId)
            ?: entity.copy(localId = localId)
    }

    suspend fun reconcileNoActiveRemoteSession(
        cashierUserId: String? = null,
        now: Long = System.currentTimeMillis()
    ): Int {
        val normalizedCashierUserId = cashierUserId?.trim()?.takeIf(String::isNotBlank)
        return cashSessionDao.markMirroredOpenSessionsClosed(
            cashierUserId = normalizedCashierUserId,
            closedAt = now,
            updatedAt = now,
            syncedAt = now,
        )
    }

    private companion object {
        const val CASH_SESSION_OPEN = "OPEN"
    }
}

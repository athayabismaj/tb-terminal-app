package com.tbterminal.app.data.local.database

import com.tbterminal.app.data.local.dao.CashSessionDao
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.mapper.toLocalCashSessionEntity
import com.tbterminal.app.data.model.CashSession

class CashSessionLocalDataSource(
    private val cashSessionDao: CashSessionDao
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

    suspend fun mirrorRemoteSession(session: CashSession): LocalCashSessionEntity {
        val existing = cashSessionDao.getByServerId(session.id)
        val entity = session.toLocalCashSessionEntity(existing = existing)
        val localId = cashSessionDao.upsert(entity)
        return cashSessionDao.getByLocalId(entity.localId.takeIf { it > 0L } ?: localId)
            ?: entity.copy(localId = localId)
    }

    private companion object {
        const val CASH_SESSION_OPEN = "OPEN"
    }
}

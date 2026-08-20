package com.tbterminal.app.data.local.database

import com.tbterminal.app.data.local.dao.UnitDao
import com.tbterminal.app.data.local.entity.LocalUnitEntity

class UnitLocalDataSource(
    private val unitDao: UnitDao
) {
    suspend fun getCachedUnits(): List<LocalUnitEntity> {
        return unitDao.getCachedUnits()
    }

    suspend fun getCachedUnitPage(
        page: Int,
        limit: Int,
        search: String?
    ): CachedPage<LocalUnitEntity> {
        val safePage = page.coerceAtLeast(1)
        val safeLimit = limit.coerceAtLeast(1)
        val query = search?.trim()?.takeIf(String::isNotBlank)
        val total = unitDao.countCachedUnits(query)
        val data = unitDao.getCachedUnitPage(
            query = query,
            limit = safeLimit,
            offset = (safePage - 1) * safeLimit
        )
        return CachedPage(data, total, safePage, safeLimit)
    }

    suspend fun cacheUnits(units: List<LocalUnitEntity>) {
        if (units.isNotEmpty()) {
            unitDao.upsertUnits(units)
        }
    }

    suspend fun cacheUnit(unit: LocalUnitEntity) {
        unitDao.upsert(unit)
    }
}

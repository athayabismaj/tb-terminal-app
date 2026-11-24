package com.tbterminal.app.data.local.database

import com.tbterminal.app.data.local.dao.ProductDao
import com.tbterminal.app.data.local.entity.LocalProductEntity

class ProductLocalDataSource(
    private val productDao: ProductDao
) {
    suspend fun getCachedProducts(
        page: Int,
        limit: Int,
        search: String?
    ): CachedPage<LocalProductEntity> {
        val safePage = page.coerceAtLeast(1)
        val safeLimit = limit.coerceAtLeast(1)
        val query = search?.trim()?.takeIf(String::isNotBlank)
        val total = productDao.countCachedProducts(query)
        val data = productDao.getCachedProducts(
            query = query,
            limit = safeLimit,
            offset = (safePage - 1) * safeLimit
        )
        return CachedPage(data, total, safePage, safeLimit)
    }

    suspend fun getCachedProductByServerId(id: String): LocalProductEntity? {
        return productDao.getByServerId(id)
    }

    suspend fun cacheProducts(products: List<LocalProductEntity>) {
        if (products.isEmpty()) return
        val mergedProducts = products.map { product ->
            val existing = product.serverId?.let { serverId ->
                productDao.getByServerId(serverId)
            }
            mergeProductMetadata(incoming = product, existing = existing)
        }
        productDao.upsertProducts(mergedProducts)
    }

    suspend fun cacheProductStockSnapshots(products: List<LocalProductEntity>) {
        if (products.isEmpty()) return

        val mergedProducts = products.map { product ->
            val existing = product.serverId?.let { serverId ->
                productDao.getByServerId(serverId)
            }

            if (existing == null) {
                product
            } else {
                val resolvedSecondaryId = product.secondaryUnitServerId ?: existing.secondaryUnitServerId
                val resolvedSecondaryName = product.secondaryUnitName
                    ?: if (resolvedSecondaryId == existing.secondaryUnitServerId) existing.secondaryUnitName else null
                val resolvedSecondaryFactor = product.secondaryUnitFactor
                    ?: if (resolvedSecondaryId == existing.secondaryUnitServerId) existing.secondaryUnitFactor else null
                existing.copy(
                    name = product.name,
                    sku = product.sku ?: existing.sku,
                    unitServerId = product.unitServerId ?: existing.unitServerId,
                    unitName = product.unitName ?: existing.unitName,
                    secondaryUnitServerId = resolvedSecondaryId,
                    secondaryUnitName = resolvedSecondaryName,
                    secondaryUnitFactor = resolvedSecondaryFactor,
                    priceSell = product.priceSell,
                    priceBuy = product.priceBuy,
                    priceRetail = product.priceRetail,
                    priceContractor = product.priceContractor,
                    discount = product.discount,
                    stock = product.stock,
                    minimumStock = product.minimumStock,
                    photoFilename = product.photoFilename,
                    isActive = product.isActive,
                    syncStatus = product.syncStatus,
                    updatedAt = product.updatedAt,
                    deletedAt = product.deletedAt
                )
            }
        }

        productDao.upsertProducts(mergedProducts)
    }

    suspend fun cacheProduct(product: LocalProductEntity) {
        val existing = product.serverId?.let { serverId ->
            productDao.getByServerId(serverId)
        }
        productDao.upsert(mergeProductMetadata(incoming = product, existing = existing))
    }

    suspend fun applyUnitNames(
        unitNameByServerId: Map<String, String>,
        productServerIds: Collection<String>,
        now: Long = System.currentTimeMillis()
    ) {
        if (unitNameByServerId.isEmpty() || productServerIds.isEmpty()) return
        val updates = productServerIds.mapNotNull { serverId ->
            val existing = productDao.getByServerId(serverId) ?: return@mapNotNull null
            val resolvedUnitName = existing.unitName?.takeIf(String::isNotBlank)
                ?: existing.unitServerId?.let(unitNameByServerId::get)
            val resolvedSecondaryName = existing.secondaryUnitServerId?.let(unitNameByServerId::get)
                ?: existing.secondaryUnitName
            if (resolvedUnitName == existing.unitName && resolvedSecondaryName == existing.secondaryUnitName) {
                null
            } else {
                existing.copy(
                    unitName = resolvedUnitName,
                    secondaryUnitName = resolvedSecondaryName,
                    updatedAt = now
                )
            }
        }
        if (updates.isNotEmpty()) {
            productDao.upsertProducts(updates)
        }
    }

    suspend fun updateProductPhoto(serverId: String, photoUrl: String?) {
        val existing = productDao.getByServerId(serverId) ?: return
        productDao.upsert(existing.copy(photoFilename = photoUrl, updatedAt = System.currentTimeMillis()))
    }

    private fun mergeProductMetadata(
        incoming: LocalProductEntity,
        existing: LocalProductEntity?
    ): LocalProductEntity {
        if (existing == null) return incoming
        val nextSecondaryId = incoming.secondaryUnitServerId
        val nextSecondaryName = when {
            nextSecondaryId == null -> null
            !incoming.secondaryUnitName.isNullOrBlank() -> incoming.secondaryUnitName
            nextSecondaryId == existing.secondaryUnitServerId -> existing.secondaryUnitName
            else -> null
        }
        return existing.copy(
            name = incoming.name,
            sku = incoming.sku ?: existing.sku,
            categoryServerId = incoming.categoryServerId ?: existing.categoryServerId,
            unitServerId = incoming.unitServerId ?: existing.unitServerId,
            unitName = incoming.unitName ?: existing.unitName,
            secondaryUnitServerId = nextSecondaryId,
            secondaryUnitName = nextSecondaryName,
            secondaryUnitFactor = if (nextSecondaryId != null) incoming.secondaryUnitFactor else null,
            priceSell = incoming.priceSell,
            priceBuy = incoming.priceBuy,
            priceRetail = incoming.priceRetail,
            priceContractor = incoming.priceContractor,
            discount = incoming.discount,
            minimumStock = incoming.minimumStock,
            photoFilename = incoming.photoFilename ?: existing.photoFilename,
            isActive = incoming.isActive,
            syncStatus = incoming.syncStatus,
            updatedAt = incoming.updatedAt,
            deletedAt = incoming.deletedAt
        )
    }
}

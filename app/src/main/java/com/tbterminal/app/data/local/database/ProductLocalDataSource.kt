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
        if (products.isNotEmpty()) {
            productDao.upsertProducts(products)
        }
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
                existing.copy(
                    name = product.name,
                    sku = product.sku ?: existing.sku,
                    priceSell = product.priceSell,
                    priceBuy = product.priceBuy,
                    priceRetail = product.priceRetail,
                    priceContractor = product.priceContractor,
                    discount = product.discount,
                    stock = product.stock,
                    minimumStock = product.minimumStock,
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
        productDao.upsert(product)
    }
}

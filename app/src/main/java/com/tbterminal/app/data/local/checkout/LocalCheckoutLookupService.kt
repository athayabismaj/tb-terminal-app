package com.tbterminal.app.data.local.checkout

import com.tbterminal.app.data.local.dao.CashSessionDao
import com.tbterminal.app.data.local.database.CustomerLocalDataSource
import com.tbterminal.app.data.local.database.ProductLocalDataSource

class LocalCheckoutLookupService(
    private val productLocalDataSource: ProductLocalDataSource,
    private val customerLocalDataSource: CustomerLocalDataSource,
    private val cashSessionDao: CashSessionDao
) {
    suspend fun findProductByServerId(serverId: String): LocalCheckoutProductLookup? {
        val product = productLocalDataSource.getCachedProductByServerId(serverId)
            ?: return null

        return LocalCheckoutProductLookup(
            localId = product.localId,
            serverId = product.serverId,
            cogsAtTransaction = product.priceBuy,
            stock = product.stock
        )
    }

    suspend fun findCustomerByServerId(serverId: String): LocalCheckoutCustomerLookup? {
        val customer = customerLocalDataSource.getCachedCustomerByServerId(serverId)
            ?: return null

        return LocalCheckoutCustomerLookup(
            localId = customer.localId,
            serverId = customer.serverId
        )
    }

    suspend fun findCashSessionByServerId(serverId: String): LocalCheckoutCashSessionLookup? {
        val cashSession = cashSessionDao.getByServerId(serverId)
            ?: return null

        return LocalCheckoutCashSessionLookup(
            localId = cashSession.localId,
            serverId = cashSession.serverId,
            cashierUserId = cashSession.cashierUserId,
            status = cashSession.status
        )
    }
}

data class LocalCheckoutProductLookup(
    val localId: Long,
    val serverId: String?,
    val cogsAtTransaction: Double,
    val stock: Double
)

data class LocalCheckoutCustomerLookup(
    val localId: Long,
    val serverId: String?
)

data class LocalCheckoutCashSessionLookup(
    val localId: Long,
    val serverId: String?,
    val cashierUserId: String,
    val status: String
)

package com.tbterminal.app.data.local.database

import com.tbterminal.app.data.local.dao.CustomerDao
import com.tbterminal.app.data.local.entity.LocalCustomerEntity

class CustomerLocalDataSource(
    private val customerDao: CustomerDao
) {
    suspend fun getCachedCustomers(
        page: Int,
        limit: Int,
        search: String?
    ): CachedPage<LocalCustomerEntity> {
        val safePage = page.coerceAtLeast(1)
        val safeLimit = limit.coerceAtLeast(1)
        val query = search?.trim()?.takeIf(String::isNotBlank)
        val total = customerDao.countCachedCustomers(query)
        val data = customerDao.getCachedCustomers(
            query = query,
            limit = safeLimit,
            offset = (safePage - 1) * safeLimit
        )
        return CachedPage(data, total, safePage, safeLimit)
    }

    suspend fun getCachedCustomerByServerId(id: String): LocalCustomerEntity? {
        return customerDao.getByServerId(id)
    }

    suspend fun cacheCustomers(customers: List<LocalCustomerEntity>) {
        if (customers.isNotEmpty()) {
            customerDao.upsertCustomers(customers)
        }
    }

    suspend fun cacheCustomer(customer: LocalCustomerEntity) {
        customerDao.upsert(customer)
    }
}

package com.tbterminal.app.data.repository

import com.tbterminal.app.data.local.database.CustomerLocalDataSource
import com.tbterminal.app.data.local.mapper.toCustomer as toCachedCustomer
import com.tbterminal.app.data.local.mapper.toLocalCustomerEntity
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.model.CustomerCommand
import com.tbterminal.app.data.model.CustomerPage
import com.tbterminal.app.data.remote.CustomerRequestDto
import com.tbterminal.app.data.remote.CustomerResponseDto
import com.tbterminal.app.data.remote.PaginatedResponse
import com.tbterminal.app.data.remote.ReceivableApi
import com.tbterminal.app.data.remote.safeApiCall
import kotlinx.coroutines.CancellationException

interface CustomerRepository {
    suspend fun getCustomers(
        page: Int = 1,
        limit: Int = 10,
        search: String? = null
    ): RepositoryResult<CustomerPage>

    suspend fun getCustomerById(id: String): RepositoryResult<Customer>

    suspend fun createCustomer(command: CustomerCommand): RepositoryResult<Customer>

    suspend fun updateCustomer(
        id: String,
        command: CustomerCommand
    ): RepositoryResult<Customer>

    suspend fun deactivateCustomer(id: String): RepositoryResult<Unit>
}

class RemoteCustomerRepository(
    private val receivableApi: ReceivableApi,
    private val customerLocalDataSource: CustomerLocalDataSource? = null
) : CustomerRepository {
    override suspend fun getCustomers(
        page: Int,
        limit: Int,
        search: String?
    ): RepositoryResult<CustomerPage> {
        val result = safeApiCall {
            receivableApi.getCustomers(page = page, limit = limit, search = search?.takeIf(String::isNotBlank))
        }.toRepositoryResult { response ->
            val pageData = response.data
            if (!response.success || pageData == null) {
                RepositoryResult.Error(
                    code = response.code ?: "CUSTOMERS_FAILED",
                    message = response.message ?: response.error ?: "Pelanggan gagal dimuat."
                )
                } else {
                    cacheCustomers(pageData.data)
                    RepositoryResult.Success(pageData.toCustomerPage())
                }
            }

        return when (result) {
            is RepositoryResult.Success -> result
            is RepositoryResult.Error,
            is RepositoryResult.Exception -> fallbackCustomerPage(page, limit, search, result)
        }
    }

    override suspend fun getCustomerById(id: String): RepositoryResult<Customer> {
        val result = safeApiCall { receivableApi.getCustomerById(id) }
            .toRepositoryResult { response ->
                val customer = response.data
                if (!response.success || customer == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "CUSTOMER_DETAIL_FAILED",
                        message = response.message ?: response.error ?: "Detail pelanggan gagal dimuat."
                    )
                } else {
                    cacheCustomer(customer)
                    RepositoryResult.Success(customer.toCustomer())
                }
            }

        return when (result) {
            is RepositoryResult.Success -> result
            is RepositoryResult.Error,
            is RepositoryResult.Exception -> fallbackCustomer(id, result)
        }
    }

    override suspend fun createCustomer(command: CustomerCommand): RepositoryResult<Customer> {
        return safeApiCall { receivableApi.createCustomer(command.toRequestDto()) }
            .toRepositoryResult { response ->
                val customer = response.data
                if (!response.success || customer == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "CREATE_CUSTOMER_FAILED",
                        message = response.message ?: response.error ?: "Pelanggan gagal disimpan."
                    )
                } else {
                    cacheCustomer(customer)
                    RepositoryResult.Success(customer.toCustomer())
                }
            }
    }

    override suspend fun updateCustomer(
        id: String,
        command: CustomerCommand
    ): RepositoryResult<Customer> {
        return safeApiCall { receivableApi.updateCustomer(id, command.toRequestDto()) }
            .toRepositoryResult { response ->
                val customer = response.data
                if (!response.success || customer == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "UPDATE_CUSTOMER_FAILED",
                        message = response.message ?: response.error ?: "Pelanggan gagal diperbarui."
                    )
                } else {
                    cacheCustomer(customer)
                    RepositoryResult.Success(customer.toCustomer())
                }
            }
    }

    override suspend fun deactivateCustomer(id: String): RepositoryResult<Unit> {
        return safeApiCall { receivableApi.deactivateCustomer(id) }
            .toRepositoryResult { response ->
                if (!response.success) {
                    RepositoryResult.Error(
                        code = response.code ?: "DEACTIVATE_CUSTOMER_FAILED",
                        message = response.message ?: response.error ?: "Pelanggan gagal dinonaktifkan."
                    )
                } else {
                    RepositoryResult.Success(Unit)
                }
            }
    }

    private suspend fun cacheCustomer(customer: CustomerResponseDto) {
        cacheSafely {
            customerLocalDataSource?.cacheCustomer(customer.toLocalCustomerEntity())
        }
    }

    private suspend fun cacheCustomers(customers: List<CustomerResponseDto>) {
        cacheSafely {
            customerLocalDataSource?.cacheCustomers(customers.map(CustomerResponseDto::toLocalCustomerEntity))
        }
    }

    private suspend fun cacheSafely(block: suspend () -> Unit) {
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // Cache failure must not change the online-first repository behavior.
        }
    }

    private suspend fun fallbackCustomer(
        id: String,
        original: RepositoryResult<Customer>
    ): RepositoryResult<Customer> {
        if (!original.shouldFallbackToCustomerCache()) return original
        return readCacheOrOriginal(original) {
            customerLocalDataSource?.getCachedCustomerByServerId(id)?.toCachedCustomer()
                ?.let { RepositoryResult.Success(it) }
        }
    }

    private suspend fun fallbackCustomerPage(
        page: Int,
        limit: Int,
        search: String?,
        original: RepositoryResult<CustomerPage>
    ): RepositoryResult<CustomerPage> {
        if (!original.shouldFallbackToCustomerCache()) return original
        return readCacheOrOriginal(original) {
            val cached = customerLocalDataSource?.getCachedCustomers(page, limit, search)
                ?: return@readCacheOrOriginal null
            if (cached.data.isEmpty()) return@readCacheOrOriginal null
            RepositoryResult.Success(
                CustomerPage(
                    data = cached.data.map { it.toCachedCustomer() },
                    total = cached.total,
                    page = cached.page,
                    limit = cached.limit,
                    totalPages = cached.totalPages
                )
            )
        }
    }

    private suspend fun <T> readCacheOrOriginal(
        original: RepositoryResult<T>,
        block: suspend () -> RepositoryResult<T>?
    ): RepositoryResult<T> {
        return try {
            block() ?: original
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            original
        }
    }
}

private fun RepositoryResult<*>.shouldFallbackToCustomerCache(): Boolean {
    return when (this) {
        is RepositoryResult.Success -> false
        is RepositoryResult.Exception -> true
        is RepositoryResult.Error -> {
            val normalizedCode = code.uppercase()
            val normalizedMessage = message.uppercase()
            val isAuthError = normalizedCode.contains("401") ||
                normalizedCode.contains("403") ||
                normalizedCode.contains("UNAUTHORIZED") ||
                normalizedCode.contains("FORBIDDEN") ||
                normalizedMessage.contains("UNAUTHORIZED") ||
                normalizedMessage.contains("FORBIDDEN")
            !isAuthError && (
                normalizedCode.startsWith("HTTP_5") ||
                    normalizedCode == "EMPTY_BODY" ||
                    normalizedCode.contains("TIMEOUT") ||
                    normalizedCode.contains("NETWORK") ||
                    normalizedCode.contains("SERVER")
                )
        }
    }
}

private fun CustomerCommand.toRequestDto(): CustomerRequestDto {
    return CustomerRequestDto(
        name = name,
        phone = phone,
        address = address,
        isContractor = isContractor,
        creditLimit = creditLimit,
        paymentTermDays = paymentTermDays
    )
}

private fun PaginatedResponse<CustomerResponseDto>.toCustomerPage(): CustomerPage {
    return CustomerPage(
        data = data.map(CustomerResponseDto::toCustomer),
        total = total,
        page = page,
        limit = limit,
        totalPages = totalPages
    )
}

private fun CustomerResponseDto.toCustomer(): Customer {
    return Customer(
        id = id,
        name = name,
        phone = phone,
        address = address,
        isContractor = isContractor,
        creditLimit = creditLimit,
        paymentTermDays = paymentTermDays,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

package com.tbterminal.app.data.repository

import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.model.CustomerCommand
import com.tbterminal.app.data.model.CustomerPage
import com.tbterminal.app.data.remote.CustomerRequestDto
import com.tbterminal.app.data.remote.CustomerResponseDto
import com.tbterminal.app.data.remote.PaginatedResponse
import com.tbterminal.app.data.remote.ReceivableApi
import com.tbterminal.app.data.remote.safeApiCall

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
    private val receivableApi: ReceivableApi
) : CustomerRepository {
    override suspend fun getCustomers(
        page: Int,
        limit: Int,
        search: String?
    ): RepositoryResult<CustomerPage> {
        return safeApiCall {
            receivableApi.getCustomers(page = page, limit = limit, search = search?.takeIf(String::isNotBlank))
        }.toRepositoryResult { response ->
            val pageData = response.data
            if (!response.success || pageData == null) {
                RepositoryResult.Error(
                    code = response.code ?: "CUSTOMERS_FAILED",
                    message = response.message ?: response.error ?: "Pelanggan gagal dimuat."
                )
            } else {
                RepositoryResult.Success(pageData.toCustomerPage())
            }
        }
    }

    override suspend fun getCustomerById(id: String): RepositoryResult<Customer> {
        return safeApiCall { receivableApi.getCustomerById(id) }
            .toRepositoryResult { response ->
                val customer = response.data
                if (!response.success || customer == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "CUSTOMER_DETAIL_FAILED",
                        message = response.message ?: response.error ?: "Detail pelanggan gagal dimuat."
                    )
                } else {
                    RepositoryResult.Success(customer.toCustomer())
                }
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

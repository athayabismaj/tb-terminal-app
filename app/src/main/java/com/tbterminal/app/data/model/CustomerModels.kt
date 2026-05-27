package com.tbterminal.app.data.model

import java.math.BigDecimal

data class Customer(
    val id: String,
    val name: String,
    val phone: String?,
    val address: String?,
    val isContractor: Boolean,
    val creditLimit: BigDecimal,
    val paymentTermDays: Int,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
)

data class CustomerPage(
    val data: List<Customer>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

data class CustomerCommand(
    val name: String,
    val phone: String?,
    val address: String?,
    val isContractor: Boolean,
    val creditLimit: BigDecimal,
    val paymentTermDays: Int
)

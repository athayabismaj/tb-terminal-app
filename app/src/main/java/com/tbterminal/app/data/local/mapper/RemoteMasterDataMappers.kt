package com.tbterminal.app.data.local.mapper

import com.tbterminal.app.data.local.entity.LocalCategoryEntity
import com.tbterminal.app.data.local.entity.LocalCustomerEntity
import com.tbterminal.app.data.local.entity.LocalProductEntity
import com.tbterminal.app.data.local.entity.LocalUnitEntity
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.model.Product
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.data.model.ProductUnit
import com.tbterminal.app.data.remote.CategoryResponseDto
import com.tbterminal.app.data.remote.CustomerResponseDto
import com.tbterminal.app.data.remote.ProductResponseDto
import com.tbterminal.app.data.remote.StockDetailResponseDto
import com.tbterminal.app.data.remote.UnitResponseDto
import java.math.BigDecimal

fun ProductResponseDto.toLocalProductEntity(now: Long = System.currentTimeMillis()): LocalProductEntity {
    return LocalProductEntity(
        serverId = id,
        clientGeneratedId = "server-product-$id",
        name = name,
        sku = sku,
        categoryServerId = categoryId,
        unitServerId = baseUnitId,
        priceSell = priceRetail.toSafeDouble(),
        priceBuy = priceBuy.toSafeDouble(),
        priceRetail = priceRetail.toSafeDouble(),
        priceContractor = priceContractor.toSafeDouble(),
        discount = discount.toSafeDouble(),
        minimumStock = minStock.toSafeDouble(),
        photoFilename = photoFilename,
        isActive = isActive,
        syncStatus = SyncStatus.SYNCED,
        updatedAt = now
    )
}

fun StockDetailResponseDto.toLocalProductEntity(now: Long = System.currentTimeMillis()): LocalProductEntity {
    return LocalProductEntity(
        serverId = productId,
        clientGeneratedId = "server-product-$productId",
        name = productName,
        sku = sku,
        priceSell = priceRetail.toSafeDouble(),
        priceBuy = priceBuy.toSafeDouble(),
        priceRetail = priceRetail.toSafeDouble(),
        priceContractor = priceContractor.toSafeDouble(),
        discount = discount.toSafeDouble(),
        stock = quantity.toSafeDouble(),
        minimumStock = minStock.toSafeDouble(),
        isActive = isActive,
        syncStatus = SyncStatus.SYNCED,
        updatedAt = now,
        deletedAt = null
    )
}

fun CategoryResponseDto.toLocalCategoryEntity(now: Long = System.currentTimeMillis()): LocalCategoryEntity {
    return LocalCategoryEntity(
        serverId = id,
        clientGeneratedId = "server-category-$id",
        name = name,
        syncStatus = SyncStatus.SYNCED,
        remoteCreatedAt = createdAt,
        updatedAt = now
    )
}

fun UnitResponseDto.toLocalUnitEntity(now: Long = System.currentTimeMillis()): LocalUnitEntity {
    return LocalUnitEntity(
        serverId = id,
        clientGeneratedId = "server-unit-$id",
        name = name,
        symbol = symbol,
        syncStatus = SyncStatus.SYNCED,
        remoteCreatedAt = createdAt,
        updatedAt = now
    )
}

fun CustomerResponseDto.toLocalCustomerEntity(now: Long = System.currentTimeMillis()): LocalCustomerEntity {
    return LocalCustomerEntity(
        serverId = id,
        clientGeneratedId = "server-customer-$id",
        name = name,
        phone = phone,
        address = address,
        isContractor = isContractor,
        creditAllowed = creditLimit > BigDecimal.ZERO || paymentTermDays > 0,
        creditLimit = creditLimit.toSafeDouble(),
        paymentTermDays = paymentTermDays,
        isActive = isActive,
        syncStatus = SyncStatus.SYNCED,
        remoteCreatedAt = createdAt,
        remoteUpdatedAt = updatedAt,
        updatedAt = now
    )
}

fun LocalProductEntity.toProduct(): Product {
    return Product(
        id = serverId.orEmpty(),
        categoryId = categoryServerId.orEmpty(),
        baseUnitId = unitServerId.orEmpty(),
        sku = sku.orEmpty(),
        name = name,
        priceBuy = priceBuy.toBigDecimal(),
        priceRetail = priceRetail.toBigDecimal(),
        priceContractor = priceContractor.toBigDecimal(),
        discount = discount.toBigDecimal(),
        minStock = minimumStock.toBigDecimal(),
        photoFilename = photoFilename,
        isActive = isActive
    )
}

fun LocalCategoryEntity.toProductCategory(): ProductCategory {
    return ProductCategory(
        id = serverId.orEmpty(),
        name = name,
        createdAt = remoteCreatedAt.orEmpty()
    )
}

fun LocalUnitEntity.toProductUnit(): ProductUnit {
    return ProductUnit(
        id = serverId.orEmpty(),
        name = name,
        symbol = symbol,
        createdAt = remoteCreatedAt.orEmpty()
    )
}

fun LocalCustomerEntity.toCustomer(): Customer {
    return Customer(
        id = serverId.orEmpty(),
        name = name,
        phone = phone,
        address = address,
        isContractor = isContractor,
        creditLimit = creditLimit.toBigDecimal(),
        paymentTermDays = paymentTermDays,
        isActive = isActive,
        createdAt = remoteCreatedAt.orEmpty(),
        updatedAt = remoteUpdatedAt.orEmpty()
    )
}

private fun BigDecimal.toSafeDouble(): Double {
    return runCatching { toDouble() }.getOrDefault(0.0)
}

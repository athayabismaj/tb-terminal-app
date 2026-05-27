package com.tbterminal.app.data.model

import java.math.BigDecimal

data class ProductCategory(
    val id: String,
    val name: String,
    val createdAt: String
)

data class ProductCategoryPage(
    val data: List<ProductCategory>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

data class ProductUnit(
    val id: String,
    val name: String,
    val symbol: String,
    val createdAt: String
)

data class ProductUnitPage(
    val data: List<ProductUnit>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

data class Product(
    val id: String,
    val categoryId: String,
    val baseUnitId: String,
    val sku: String,
    val name: String,
    val priceBuy: BigDecimal,
    val priceRetail: BigDecimal,
    val priceContractor: BigDecimal,
    val discount: BigDecimal,
    val minStock: BigDecimal,
    val photoFilename: String?,
    val isActive: Boolean
)

data class ProductStock(
    val productId: String,
    val sku: String,
    val productName: String,
    val categoryName: String,
    val unitName: String,
    val quantity: BigDecimal,
    val minStock: BigDecimal,
    val priceBuy: BigDecimal,
    val priceRetail: BigDecimal,
    val priceContractor: BigDecimal,
    val discount: BigDecimal,
    val isActive: Boolean
)

data class ProductStockPage(
    val data: List<ProductStock>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

data class StockAdjustment(
    val id: String,
    val productId: String,
    val sku: String,
    val productName: String,
    val categoryName: String,
    val unitName: String,
    val adjustmentType: String,
    val adjustmentTypeLabel: String,
    val qtyBefore: BigDecimal,
    val qtyAfter: BigDecimal,
    val difference: BigDecimal,
    val reason: String,
    val userId: String,
    val createdAt: String
)

data class StockAdjustmentPage(
    val data: List<StockAdjustment>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

data class ProductDetail(
    val product: Product,
    val stock: ProductStock?
)

data class CreateProductCommand(
    val categoryId: String,
    val baseUnitId: String,
    val sku: String,
    val name: String,
    val priceBuy: BigDecimal,
    val priceRetail: BigDecimal,
    val priceContractor: BigDecimal,
    val discount: BigDecimal,
    val minStock: BigDecimal,
    val photoFilename: String? = null
)

data class UpdateProductCommand(
    val categoryId: String,
    val baseUnitId: String,
    val name: String,
    val priceBuy: BigDecimal,
    val priceRetail: BigDecimal,
    val priceContractor: BigDecimal,
    val discount: BigDecimal,
    val minStock: BigDecimal,
    val photoFilename: String? = null
)

package com.tbterminal.app.data.repository

import com.tbterminal.app.data.model.Product
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.data.model.ProductCategoryPage
import com.tbterminal.app.data.model.ProductPage
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.ProductStockPage
import com.tbterminal.app.data.model.ProductUnit
import com.tbterminal.app.data.model.ProductUnitPage
import com.tbterminal.app.data.model.StockAdjustment
import com.tbterminal.app.data.model.StockAdjustmentPage
import com.tbterminal.app.data.remote.CategoryResponseDto
import com.tbterminal.app.data.remote.PaginatedResponse
import com.tbterminal.app.data.remote.ProductResponseDto
import com.tbterminal.app.data.remote.StockAdjustmentResponseDto
import com.tbterminal.app.data.remote.StockDetailResponseDto
import com.tbterminal.app.data.remote.UnitResponseDto

internal fun PaginatedResponse<StockDetailResponseDto>.toProductStockPage(): ProductStockPage {
    return ProductStockPage(data.map(StockDetailResponseDto::toProductStock), total, page, limit, totalPages)
}

internal fun PaginatedResponse<StockAdjustmentResponseDto>.toStockAdjustmentPage(): StockAdjustmentPage {
    return StockAdjustmentPage(data.map(StockAdjustmentResponseDto::toStockAdjustment), total, page, limit, totalPages)
}

internal fun PaginatedResponse<CategoryResponseDto>.toProductCategoryPage(): ProductCategoryPage {
    return ProductCategoryPage(data.map(CategoryResponseDto::toProductCategory), total, page, limit, totalPages)
}

internal fun PaginatedResponse<UnitResponseDto>.toProductUnitPage(): ProductUnitPage {
    return ProductUnitPage(data.map(UnitResponseDto::toProductUnit), total, page, limit, totalPages)
}

internal fun ProductResponseDto.toProduct(): Product {
    return Product(
        id = id,
        categoryId = categoryId,
        baseUnitId = baseUnitId,
        sku = sku,
        name = name,
        priceBuy = priceBuy,
        priceRetail = priceRetail,
        priceContractor = priceContractor,
        discount = discount,
        minStock = minStock,
        photoFilename = photoFilename,
        isActive = isActive,
        secondaryUnitId = secondaryUnitId,
        secondaryUnitFactor = secondaryUnitFactor?.toBigDecimalOrNull()
    )
}

internal fun StockDetailResponseDto.toProductStock(): ProductStock {
    return ProductStock(productId, sku, productName, categoryName, unitName, quantity, minStock, priceBuy, priceRetail, priceContractor, discount, isActive)
}

internal fun StockAdjustmentResponseDto.toStockAdjustment(): StockAdjustment {
    return StockAdjustment(
        id = id,
        productId = productId,
        sku = sku,
        productName = productName,
        categoryName = categoryName,
        unitName = unitName,
        adjustmentType = adjustmentType,
        adjustmentTypeLabel = adjustmentTypeLabel,
        qtyBefore = qtyBefore,
        qtyAfter = qtyAfter,
        difference = difference,
        reason = reason,
        userId = userId,
        source = source,
        occurredOn = occurredOn,
        createdAt = createdAt
    )
}

internal fun CategoryResponseDto.toProductCategory(): ProductCategory {
    return ProductCategory(id, name, createdAt)
}

internal fun UnitResponseDto.toProductUnit(): ProductUnit {
    return ProductUnit(id, name, symbol, createdAt)
}


internal fun PaginatedResponse<ProductResponseDto>.toProductPage(): ProductPage {
    return ProductPage(
        data = data.map { it.toProduct() },
        total = total,
        page = page,
        limit = limit,
        totalPages = totalPages
    )
}

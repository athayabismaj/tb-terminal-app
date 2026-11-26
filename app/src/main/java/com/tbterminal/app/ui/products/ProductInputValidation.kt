package com.tbterminal.app.ui.products

import com.tbterminal.app.ui.common.parseIdrDecimalInput
import com.tbterminal.app.ui.common.parseWholeRupiahInput
import java.math.BigDecimal

private val SKU_PATTERN = Regex("^[A-Z0-9][A-Z0-9._-]{0,49}$")
private val MAX_PRICE = BigDecimal("9999999999999.99")
private val MAX_STOCK = BigDecimal("99999999.99")
private val MAX_UNIT_CONVERSION_FACTOR = BigDecimal("999999.9999")

internal fun normalizeProductSku(value: String): String = value.trim().uppercase()

internal fun validateProductFormInput(input: ProductFormInput, isEditMode: Boolean): String? {
    val sku = normalizeProductSku(input.sku)
    val priceBuy = parseIdrDecimalInput(input.priceBuy)
    val priceRetail = parseWholeRupiahInput(input.priceRetail)
    val priceContractor = parseWholeRupiahInput(input.priceContractor)
    val minStock = input.minStock.toProductDecimalOrNull()
    val secondaryUnitFactor = input.secondaryUnitFactor.toProductDecimalOrNull()
    return when {
        input.name.isBlank() -> "Nama produk tidak boleh kosong."
        input.name.trim().length > 200 -> "Nama produk maksimal 200 karakter."
        !isEditMode && sku.isBlank() -> "SKU produk tidak boleh kosong."
        !isEditMode && !SKU_PATTERN.matches(sku) -> "SKU hanya boleh berisi huruf, angka, titik, garis bawah, atau tanda hubung."
        input.categoryId.isBlank() -> "Kategori wajib dipilih."
        input.baseUnitId.isBlank() -> "Satuan wajib dipilih."
        input.usesSecondaryUnit && input.secondaryUnitId.isBlank() -> "Satuan kedua wajib dipilih."
        input.usesSecondaryUnit && input.secondaryUnitId == input.baseUnitId -> "Satuan kedua harus berbeda dari satuan utama."
        input.usesSecondaryUnit && secondaryUnitFactor == null -> "Faktor konversi harus berupa angka valid."
        input.usesSecondaryUnit && secondaryUnitFactor?.let { it <= BigDecimal.ZERO } == true -> "Faktor konversi harus lebih dari nol."
        input.usesSecondaryUnit && secondaryUnitFactor?.let { it > MAX_UNIT_CONVERSION_FACTOR } == true -> "Faktor konversi melebihi batas."
        input.usesSecondaryUnit && secondaryUnitFactor?.let { it.scale() > 4 } == true -> "Faktor konversi maksimal 4 angka desimal."
        priceBuy == null -> "Harga beli harus berupa angka valid."
        priceRetail == null -> "Harga retail harus berupa rupiah bulat tanpa desimal."
        priceContractor == null -> "Harga kontraktor harus berupa rupiah bulat tanpa desimal."
        minStock == null -> "Stok minimum harus berupa angka valid."
        priceBuy < BigDecimal.ZERO -> "Harga beli tidak boleh negatif."
        priceRetail < BigDecimal.ZERO -> "Harga retail tidak boleh negatif."
        priceContractor < BigDecimal.ZERO -> "Harga kontraktor tidak boleh negatif."
        minStock < BigDecimal.ZERO -> "Stok minimum tidak boleh negatif."
        priceBuy > MAX_PRICE || priceRetail > MAX_PRICE || priceContractor > MAX_PRICE -> "Harga melebihi batas."
        minStock > MAX_STOCK -> "Stok minimum melebihi batas."
        priceBuy.scale() > 2 -> "Harga beli maksimal 2 angka desimal."
        minStock.scale() > 2 -> "Stok minimum maksimal 2 angka desimal."
        else -> null
    }
}

internal fun String.toProductDecimalOrNull(): BigDecimal? = trim()
    .replace(",", ".")
    .takeIf(String::isNotBlank)
    ?.runCatching { toBigDecimal() }
    ?.getOrNull()

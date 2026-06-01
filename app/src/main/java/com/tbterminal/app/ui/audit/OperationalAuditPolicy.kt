package com.tbterminal.app.ui.audit

import com.tbterminal.app.data.model.AuditLogItem

enum class OperationalAuditType(
    val title: String,
    val module: String
) {
    ProductCreated("Produk dibuat", "Produk"),
    ProductUpdated("Produk diubah", "Produk"),
    ProductDisabled("Produk dinonaktifkan", "Produk"),
    PriceUpdated("Harga diubah", "Harga"),
    CategoryUnitUpdated("Kategori/satuan diubah", "Master produk"),
    StockOpname("Stok opname", "Stok"),
    StockCorrection("Koreksi stok", "Stok"),
    DamagedReturn("Retur/rusak", "Stok"),
    IncomingGoods("Barang masuk/restok", "Pembelian"),
    SupplierDebtPayment("Pembayaran utang supplier", "Utang"),
    CustomerReceivablePayment("Pembayaran piutang pelanggan", "Piutang"),
    CashSession("Buka/tutup kas harian", "Kas"),
    CancelRefund("Transaksi dibatalkan/refund", "Transaksi")
}

fun AuditLogItem.operationalAuditType(): OperationalAuditType? {
    val schema = schemaName.lowercase()
    val table = tableName.lowercase()
    val payload = "${oldData.orEmpty()} ${newData.orEmpty()}".lowercase()

    return when {
        schema == "inventory" && table == "products" && action == "INSERT" ->
            OperationalAuditType.ProductCreated

        schema == "inventory" && table == "products" && action == "DELETE" ->
            OperationalAuditType.ProductDisabled

        schema == "inventory" && table == "products_price" ->
            OperationalAuditType.PriceUpdated

        schema == "inventory" && table == "products" && payload.containsAny(priceFields) ->
            OperationalAuditType.PriceUpdated

        schema == "inventory" && table == "products" ->
            OperationalAuditType.ProductUpdated

        schema == "inventory" && table in setOf("categories", "units") ->
            OperationalAuditType.CategoryUnitUpdated

        schema == "inventory" && table == "stock_opname" ->
            OperationalAuditType.StockOpname

        schema == "inventory" && table == "stock_correction" ->
            OperationalAuditType.StockCorrection

        schema == "inventory" && table == "stock_damage" ->
            OperationalAuditType.DamagedReturn

        schema == "inventory" && table == "stock_adjustments" && payload.containsAny(opnameMarkers) ->
            OperationalAuditType.StockOpname

        schema == "inventory" && table == "stock_adjustments" && payload.containsAny(correctionMarkers) ->
            OperationalAuditType.StockCorrection

        schema == "inventory" && table == "stock_adjustments" && payload.containsAny(damageMarkers) ->
            OperationalAuditType.DamagedReturn

        schema == "inventory" && table == "stock_adjustments" ->
            OperationalAuditType.StockCorrection

        schema == "purchasing" && table in setOf("purchases", "purchase_items") ->
            OperationalAuditType.IncomingGoods

        schema == "purchasing" && table in setOf("supplier_payables", "supplier_payments") ->
            OperationalAuditType.SupplierDebtPayment

        schema == "receivable" && table in setOf("receivables", "receivable_payments") ->
            OperationalAuditType.CustomerReceivablePayment

        schema == "sales" && table == "cash_sessions" ->
            OperationalAuditType.CashSession

        schema == "sales" && table == "transactions" && payload.containsAny(cancelRefundMarkers) ->
            OperationalAuditType.CancelRefund

        else -> null
    }
}

fun AuditLogItem.isOperationalAudit(): Boolean {
    return operationalAuditType() != null
}

fun AuditLogItem.operationalTitle(): String {
    return operationalAuditType()?.title ?: activityLabel
}

fun AuditLogItem.operationalModule(): String {
    return operationalAuditType()?.module ?: "${schemaName}.${tableName}"
}

private fun String.containsAny(markers: Set<String>): Boolean {
    return markers.any(::contains)
}

private val priceFields = setOf(
    "price_buy",
    "pricebuy",
    "price_retail",
    "priceretail",
    "price_contractor",
    "pricecontractor",
    "discount"
)

private val opnameMarkers = setOf("opname")
private val correctionMarkers = setOf("correction", "koreksi", "adjustment")
private val damageMarkers = setOf("damage", "damaged", "retur", "return", "rusak")
private val cancelRefundMarkers = setOf("cancel", "cancelled", "batal", "refund", "refunded")

package com.tbterminal.app.data.numbering

import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class DocumentNumberType(
    val prefix: String,
    val label: String
) {
    IncomingGoods("BM", "Barang Masuk"),
    Customer("PLG", "Pelanggan"),
    Sales("PJ", "Penjualan"),
    Supplier("SUP", "Supplier"),
    StockOpname("OPN", "Stok Opname"),
    Return("RET", "Retur")
}

interface DocumentNumberGenerator {
    fun generate(type: DocumentNumberType, date: LocalDate = LocalDate.now()): String
}

class DailyDocumentNumberGenerator : DocumentNumberGenerator {
    private val counters = mutableMapOf<DocumentNumberType, CounterState>()

    @Synchronized
    override fun generate(type: DocumentNumberType, date: LocalDate): String {
        val dateKey = date.format(DATE_FORMATTER)
        val current = counters[type]
        val nextSequence = if (current?.dateKey == dateKey) current.sequence + 1 else 1

        counters[type] = CounterState(dateKey = dateKey, sequence = nextSequence)

        return "${type.prefix}-$dateKey-${nextSequence.toString().padStart(SEQUENCE_DIGITS, '0')}"
    }

    private data class CounterState(
        val dateKey: String,
        val sequence: Int
    )

    private companion object {
        const val SEQUENCE_DIGITS = 3
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}

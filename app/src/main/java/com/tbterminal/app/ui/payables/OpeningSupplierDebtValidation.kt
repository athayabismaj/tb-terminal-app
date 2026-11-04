package com.tbterminal.app.ui.payables

import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException

internal data class OpeningSupplierDebtInput(
    val amount: BigDecimal,
    val debtDate: LocalDate,
    val dueDate: LocalDate
)

internal fun validateOpeningSupplierDebtInput(
    supplierId: String,
    amountInput: String,
    debtDateInput: String,
    dueDateInput: String,
    today: LocalDate = LocalDate.now()
): Result<OpeningSupplierDebtInput> = runCatching {
    require(supplierId.isNotBlank()) { "Supplier aktif wajib dipilih." }
    val amount = amountInput.trim().replace(',', '.').toBigDecimalOrNull()
        ?: error("Nominal hutang wajib diisi dengan angka valid.")
    require(amount > BigDecimal.ZERO) { "Nominal hutang harus lebih dari nol." }
    require(amount.scale() <= 2) { "Nominal hutang maksimal dua angka desimal." }
    val debtDate = parseOpeningSupplierDebtDate(debtDateInput, "Tanggal hutang")
    val dueDate = parseOpeningSupplierDebtDate(dueDateInput, "Jatuh tempo")
    require(debtDate <= today) { "Tanggal hutang tidak boleh di masa depan." }
    require(dueDate >= debtDate) { "Jatuh tempo tidak boleh sebelum tanggal hutang." }
    OpeningSupplierDebtInput(amount, debtDate, dueDate)
}

private fun parseOpeningSupplierDebtDate(value: String, label: String): LocalDate {
    return try {
        LocalDate.parse(value.trim())
    } catch (_: DateTimeParseException) {
        throw IllegalArgumentException("$label harus berformat yyyy-MM-dd.")
    }
}

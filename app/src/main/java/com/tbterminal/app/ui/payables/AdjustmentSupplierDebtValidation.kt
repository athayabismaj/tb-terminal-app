package com.tbterminal.app.ui.payables

import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException

internal data class AdjustmentSupplierDebtInput(
    val amount: BigDecimal,
    val debtDate: LocalDate,
    val dueDate: LocalDate,
    val reference: String,
    val reason: String
)

internal fun validateAdjustmentSupplierDebtInput(
    supplierId: String,
    activeSupplierIds: Set<String>,
    amountInput: String,
    debtDateInput: String,
    dueDateInput: String,
    referenceInput: String,
    reasonInput: String,
    today: LocalDate = LocalDate.now()
): Result<AdjustmentSupplierDebtInput> = runCatching {
    require(supplierId.isNotBlank() && (activeSupplierIds.isEmpty() || supplierId in activeSupplierIds)) {
        "Supplier aktif wajib dipilih."
    }
    val amount = amountInput.trim().replace(',', '.').toBigDecimalOrNull()
        ?: error("Nominal penyesuaian wajib diisi dengan angka valid.")
    require(amount > BigDecimal.ZERO) { "Nominal penyesuaian harus lebih dari nol." }
    require(amount.scale() <= 2) { "Nominal penyesuaian maksimal dua angka desimal." }
    val debtDate = parseAdjustmentSupplierDebtDate(debtDateInput, "Tanggal penyesuaian")
    val dueDate = parseAdjustmentSupplierDebtDate(dueDateInput, "Jatuh tempo")
    require(debtDate <= today) { "Tanggal penyesuaian tidak boleh di masa depan." }
    require(dueDate >= debtDate) { "Jatuh tempo tidak boleh sebelum tanggal penyesuaian." }
    val reference = referenceInput.trim()
    val reason = reasonInput.trim()
    require(reference.isNotBlank()) { "Referensi penyesuaian wajib diisi." }
    require(reference.length <= 100) { "Referensi maksimal 100 karakter." }
    require(reason.isNotBlank()) { "Alasan penyesuaian wajib diisi." }
    require(reason.length <= 1000) { "Alasan maksimal 1000 karakter." }
    AdjustmentSupplierDebtInput(amount, debtDate, dueDate, reference, reason)
}

private fun parseAdjustmentSupplierDebtDate(value: String, label: String): LocalDate = try {
    LocalDate.parse(value.trim())
} catch (_: DateTimeParseException) {
    throw IllegalArgumentException("$label harus berformat yyyy-MM-dd.")
}

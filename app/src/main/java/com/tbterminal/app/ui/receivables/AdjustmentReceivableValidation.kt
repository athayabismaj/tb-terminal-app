package com.tbterminal.app.ui.receivables

import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException

internal data class AdjustmentReceivableInput(
    val amount: BigDecimal,
    val debtDate: LocalDate,
    val dueDate: LocalDate,
    val reference: String,
    val reason: String
)

internal fun validateAdjustmentReceivableInput(
    customerId: String,
    activeCustomerIds: Set<String>,
    amountInput: String,
    debtDateInput: String,
    dueDateInput: String,
    referenceInput: String,
    reasonInput: String,
    today: LocalDate = LocalDate.now()
): Result<AdjustmentReceivableInput> = runCatching {
    require(customerId.isNotBlank() && customerId in activeCustomerIds) { "Pelanggan aktif wajib dipilih." }
    val amount = amountInput.trim().replace(',', '.').toBigDecimalOrNull()
        ?: error("Nominal adjustment wajib diisi dengan angka valid.")
    require(amount > BigDecimal.ZERO) { "Nominal adjustment harus lebih dari nol." }
    require(amount.scale() <= 2) { "Nominal adjustment maksimal dua angka desimal." }
    val debtDate = parseAdjustmentDate(debtDateInput, "Tanggal adjustment")
    val dueDate = parseAdjustmentDate(dueDateInput, "Jatuh tempo")
    require(debtDate <= today) { "Tanggal adjustment tidak boleh di masa depan." }
    require(dueDate >= debtDate) { "Jatuh tempo tidak boleh sebelum tanggal adjustment." }
    val reference = referenceInput.trim()
    val reason = reasonInput.trim()
    require(reference.isNotBlank()) { "Referensi adjustment wajib diisi." }
    require(reference.length <= 100) { "Referensi maksimal 100 karakter." }
    require(reason.isNotBlank()) { "Alasan adjustment wajib diisi." }
    require(reason.length <= 1000) { "Alasan maksimal 1000 karakter." }
    AdjustmentReceivableInput(amount, debtDate, dueDate, reference, reason)
}

private fun parseAdjustmentDate(value: String, label: String): LocalDate = try {
    LocalDate.parse(value.trim())
} catch (_: DateTimeParseException) {
    throw IllegalArgumentException("$label harus berformat yyyy-MM-dd.")
}

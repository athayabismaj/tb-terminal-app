package com.tbterminal.app.ui.receivables

import com.tbterminal.app.ui.common.parseWholeRupiahInput
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException

internal data class OpeningReceivableInput(
    val amount: BigDecimal,
    val debtDate: LocalDate,
    val dueDate: LocalDate
)

internal fun validateOpeningReceivableInput(
    customerId: String,
    amountInput: String,
    debtDateInput: String,
    dueDateInput: String,
    today: LocalDate = LocalDate.now()
): Result<OpeningReceivableInput> = runCatching {
    require(customerId.isNotBlank()) { "Pelanggan aktif wajib dipilih." }
    val amount = parseWholeRupiahInput(amountInput)
        ?: error("Nominal piutang wajib diisi dengan angka valid.")
    require(amount > BigDecimal.ZERO) { "Nominal piutang harus lebih dari nol." }
    require(amount.scale() <= 2) { "Nominal piutang maksimal dua angka desimal." }
    val debtDate = parseOpeningDate(debtDateInput, "Tanggal piutang")
    val dueDate = parseOpeningDate(dueDateInput, "Jatuh tempo")
    require(debtDate <= today) { "Tanggal piutang tidak boleh di masa depan." }
    require(dueDate >= debtDate) { "Jatuh tempo tidak boleh sebelum tanggal piutang." }
    OpeningReceivableInput(amount, debtDate, dueDate)
}

private fun parseOpeningDate(value: String, label: String): LocalDate {
    return try {
        LocalDate.parse(value.trim())
    } catch (_: DateTimeParseException) {
        throw IllegalArgumentException("$label harus berformat yyyy-MM-dd.")
    }
}

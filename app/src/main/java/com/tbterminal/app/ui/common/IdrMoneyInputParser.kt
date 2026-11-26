package com.tbterminal.app.ui.common

import java.math.BigDecimal

internal const val DEFAULT_MAX_RUPIAH_DIGITS = 12

private val PLAIN_INTEGER_REGEX = Regex("^\\d+$")
private val GROUPED_THOUSANDS_REGEX = Regex("^\\d{1,3}(\\.\\d{3})+$")

/**
 * Sanitizes typed money input for whole-rupiah fields while keeping dot thousand separators
 * and a single comma so comma-separated non-zero decimals (e.g. "1.500,50") remain visible
 * and are explicitly rejected upon submission rather than silently concatenated.
 */
internal fun sanitizeWholeRupiahInput(
    raw: String,
    maxIntegerDigits: Int = DEFAULT_MAX_RUPIAH_DIGITS,
): String {
    val result = StringBuilder(raw.length)
    var intDigits = 0
    var seenComma = false

    for (ch in raw) {
        when {
            ch.isDigit() -> {
                if (seenComma) {
                    result.append(ch)
                } else if (intDigits < maxIntegerDigits) {
                    result.append(ch)
                    intDigits += 1
                }
            }
            ch == '.' && !seenComma -> {
                result.append(ch)
            }
            ch == ',' && !seenComma -> {
                seenComma = true
                result.append(ch)
            }
        }
    }
    return result.toString()
}

/**
 * Parses whole-rupiah user input:
 * - "." acts strictly as a 3-digit Indonesian thousand separator ("20.000" -> 20000).
 * - "," followed only by zeros ("20000,00", "20.000,0") is ignored -> whole rupiah.
 * - "," followed by non-zero digits or nothing ("1.500,50", "20000,") returns null.
 * - Invalid dot groupings ("20.00", "99.99", "20.", ".500") return null.
 */
internal fun parseWholeRupiahInput(raw: String): BigDecimal? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null

    val isNegative = trimmed.startsWith("-")
    val body = if (isNegative) trimmed.substring(1).trim() else trimmed
    if (body.isEmpty()) return null

    val commaCount = body.count { it == ',' }
    if (commaCount > 1) return null

    val intPart = if (commaCount == 1) {
        val fracPart = body.substringAfter(',')
        if (fracPart.isEmpty() || !fracPart.all { it == '0' }) {
            return null
        }
        body.substringBefore(',')
    } else {
        body
    }

    if (!PLAIN_INTEGER_REGEX.matches(intPart) && !GROUPED_THOUSANDS_REGEX.matches(intPart)) {
        return null
    }

    val digits = intPart.filter(Char::isDigit)
    val parsed = (if (isNegative) "-$digits" else digits).toBigDecimalOrNull() ?: return null
    return if (parsed.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO else parsed
}

/**
 * Formats an authoritative monetary [BigDecimal] for text field pre-fill without ".00" noise.
 */
internal fun BigDecimal.toWholeRupiahInputText(): String {
    val normalized = stripTrailingZeros()
    return if (normalized.scale() <= 0) {
        normalized.setScale(0).toPlainString()
    } else {
        normalized.toPlainString().replace('.', ',')
    }
}

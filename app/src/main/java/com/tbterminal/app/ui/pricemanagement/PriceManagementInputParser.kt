package com.tbterminal.app.ui.pricemanagement

import com.tbterminal.app.ui.common.parseIdrDecimalInput
import com.tbterminal.app.ui.common.parseWholeRupiahInput
import java.math.BigDecimal

internal fun parseCatalogBuyPriceInput(raw: String): BigDecimal? {
    return parseIdrDecimalInput(raw)?.takeIf { it >= BigDecimal.ZERO }
}

internal fun parseCatalogWholeRupiahInput(raw: String): BigDecimal? {
    return parseWholeRupiahInput(raw)?.takeIf { it >= BigDecimal.ZERO }
}

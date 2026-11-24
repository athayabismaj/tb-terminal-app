package com.tbterminal.app.data.repository

import com.tbterminal.app.data.model.RefundDisposition
import java.math.BigDecimal
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionActionRequestMappingTest {
    @Test
    fun voidRequestCarriesScopedManagerApproval() {
        val request = buildVoidTransactionRequest("void-key-123", "  Salah input  ", "approval-1")
        assertEquals("void-key-123", request.idempotencyKey)
        assertEquals("Salah input", request.reason)
        assertEquals("approval-1", request.managerApprovalId)
    }

    @Test
    fun everyRefundDispositionUsesExactBackendValue() {
        RefundDisposition.entries.forEach { disposition ->
            val request = buildRefundTransactionRequest(
                idempotencyKey = "refund-key-123",
                reason = "  Barang dikembalikan  ",
                disposition = disposition,
                managerApprovalId = "approval-2",
            )
            assertEquals(disposition.apiValue, request.returnDisposition)
            assertEquals("Barang dikembalikan", request.reason)
            assertEquals("approval-2", request.managerApprovalId)
        }
    }

    @Test
    fun payDebtRequestCarriesIdempotencyKeyAndSerializesIt() {
        val request = buildPayDebtRequest(
            amount = BigDecimal("25000"),
            method = "TUNAI",
            idempotencyKey = "550e8400-e29b-41d4-a716-446655440000",
        )
        assertEquals(BigDecimal("25000"), request.amount)
        assertEquals("TUNAI", request.method)
        assertEquals("550e8400-e29b-41d4-a716-446655440000", request.idempotencyKey)

        val encoded = Json.encodeToString(request)
        assertTrue(encoded.contains("\"idempotencyKey\":\"550e8400-e29b-41d4-a716-446655440000\""))
    }
}

package com.tbterminal.app.ui.cashier.transactions

import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.ManagerApprovalAction
import com.tbterminal.app.data.model.ManagerApprovalGrant
import com.tbterminal.app.data.model.ManagerApprovalResourceType
import com.tbterminal.app.data.model.ManagerApprovalStatus
import java.math.BigDecimal
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionActionPolicyTest {
    @Test
    fun ownerAndAdminCanVoidAndRefundDirectly() {
        listOf("OWNER", "ADMIN").forEach { role ->
            val access = transactionActionAccess(role, "LUNAS", "PENJUALAN")
            assertTrue(access.canVoid)
            assertTrue(access.canRefund)
            assertFalse(access.requiresManagerApproval)
        }
    }

    @Test
    fun cashierCanUseActionsOnlyWithManagerApproval() {
        val access = transactionActionAccess("KASIR", "LUNAS", "PENJUALAN")
        assertTrue(access.canVoid)
        assertTrue(access.canRefund)
        assertTrue(access.requiresManagerApproval)
    }

    @Test
    fun terminalStatusesHideInvalidActions() {
        val voided = transactionActionAccess("OWNER", "VOIDED", "PENJUALAN")
        val refunded = transactionActionAccess("ADMIN", "REFUNDED", "PENJUALAN")
        assertFalse(voided.canRefund)
        assertFalse(refunded.canVoid)
        assertFalse(refunded.canRefund)
    }

    @Test
    fun approvalMustMatchActionAndTransaction() {
        val transaction = transaction()
        assertNotNull(
            validateActionApproval(
                ManagerApprovalAction.VOID_TRANSACTION,
                transaction,
                grant(ManagerApprovalAction.REFUND_TRANSACTION, transaction.id),
            ),
        )
        assertNotNull(
            validateActionApproval(
                ManagerApprovalAction.VOID_TRANSACTION,
                transaction,
                grant(ManagerApprovalAction.VOID_TRANSACTION, "other-transaction"),
            ),
        )
        assertNull(
            validateActionApproval(
                ManagerApprovalAction.VOID_TRANSACTION,
                transaction,
                grant(ManagerApprovalAction.VOID_TRANSACTION, transaction.id),
            ),
        )
    }

    @Test
    fun expiredApprovalRequiresNewApproval() {
        assertTrue(requiresNewActionApproval("MANAGER_APPROVAL_EXPIRED"))
        assertTrue(requiresNewActionApproval("MANAGER_APPROVAL_SCOPE_MISMATCH"))
        assertFalse(requiresNewActionApproval("TRANSACTION_NOT_REFUNDABLE"))
    }

    private fun transaction() = CashTransactionDetail(
        id = "transaction-1",
        receiptId = "TRX-1",
        sessionId = "session-1",
        customerId = null,
        customerName = null,
        type = "PENJUALAN",
        status = "LUNAS",
        total = BigDecimal("100000"),
        paidAmount = BigDecimal("100000"),
        amountTendered = BigDecimal("100000"),
        changeAmount = BigDecimal.ZERO,
        createdAt = "2026-09-01T00:00:00Z",
        items = emptyList(),
    )

    private fun grant(action: ManagerApprovalAction, resourceId: String) = ManagerApprovalGrant(
        approvalId = "approval-1",
        action = action,
        resourceType = ManagerApprovalResourceType.TRANSACTION,
        resourceId = resourceId,
        status = ManagerApprovalStatus.APPROVED,
        createdAt = "2026-09-01T00:00:00Z",
        expiresAt = "2026-09-01T00:05:00Z",
    )
}

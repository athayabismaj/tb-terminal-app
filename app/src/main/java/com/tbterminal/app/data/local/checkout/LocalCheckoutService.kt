package com.tbterminal.app.data.local.checkout

import androidx.room.withTransaction
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.LocalAuditLogEntity
import com.tbterminal.app.data.local.entity.LocalPaymentEntity
import com.tbterminal.app.data.local.entity.LocalReceivableEntity
import com.tbterminal.app.data.local.entity.LocalTransactionEntity
import com.tbterminal.app.data.local.entity.LocalTransactionItemEntity
import com.tbterminal.app.data.local.entity.SyncQueueEntity
import com.tbterminal.app.data.local.id.LocalIdGenerator
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncOperation
import com.tbterminal.app.data.local.model.SyncStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

class LocalCheckoutService(
    private val database: TbTerminalDatabase,
    private val deviceId: String? = null
) {
    suspend fun saveCheckout(command: LocalCheckoutCommand): LocalCheckoutResult {
        val validationError = validate(command)
        if (validationError != null) {
            return LocalCheckoutResult.Failed(validationError)
        }

        return runCatching {
            database.withTransaction {
                val now = System.currentTimeMillis()
                val remainingAmount = max(0.0, command.remainingAmount ?: (command.total - command.paidAmount))
                val transactionClientId = LocalIdGenerator.clientGeneratedId("TRX")
                val transactionCode = localTransactionCode(command.occurredAt)

                val transactionLocalId = database.transactionDao().upsert(
                    LocalTransactionEntity(
                        clientGeneratedId = transactionClientId,
                        deviceId = deviceId,
                        transactionCode = transactionCode,
                        customerLocalId = command.customerLocalId,
                        customerServerId = command.customerServerId,
                        cashSessionLocalId = command.cashSessionLocalId,
                        cashSessionServerId = command.cashSessionServerId,
                        cashierUserId = command.cashierUserId,
                        status = transactionStatus(remainingAmount),
                        subtotal = command.subtotal,
                        discount = command.discount,
                        total = command.total,
                        paidAmount = command.paidAmount,
                        remainingAmount = remainingAmount,
                        syncStatus = SyncStatus.PENDING,
                        createdAt = now,
                        updatedAt = now,
                        occurredAt = command.occurredAt
                    )
                )

                command.items.forEach { item ->
                    database.transactionItemDao().upsert(item.toEntity(transactionLocalId, now))
                    item.productLocalId?.let { productLocalId ->
                        database.productDao().decrementStock(
                            localId = productLocalId,
                            quantity = item.quantity,
                            status = SyncStatus.PENDING,
                            updatedAt = now
                        )
                    }
                }

                val paymentLocalId = if (command.paidAmount > 0.0) {
                    database.paymentDao().upsert(command.toPaymentEntity(transactionLocalId, now))
                } else {
                    null
                }

                val receivableLocalId = if (shouldCreateReceivable(command.paymentMethod, remainingAmount)) {
                    database.receivableDao().upsert(command.toReceivableEntity(transactionLocalId, remainingAmount, now))
                } else {
                    null
                }

                val syncQueueId = database.syncQueueDao().enqueue(
                    SyncQueueEntity(
                        entityType = SyncEntityType.TRANSACTION,
                        entityLocalId = transactionLocalId,
                        operation = SyncOperation.CREATE,
                        payloadJson = command.toSyncPayloadJson(
                            transactionLocalId = transactionLocalId,
                            transactionClientGeneratedId = transactionClientId,
                            transactionCode = transactionCode,
                            remainingAmount = remainingAmount
                        ),
                        status = SyncStatus.PENDING,
                        createdAt = now,
                        updatedAt = now
                    )
                )

                database.localAuditLogDao().upsert(
                    LocalAuditLogEntity(
                        clientGeneratedId = LocalIdGenerator.clientGeneratedId("AUDIT"),
                        deviceId = deviceId,
                        actorUserId = command.cashierUserId,
                        action = "INSERT",
                        tableName = "sales.transactions",
                        recordId = transactionClientId,
                        description = "Checkout lokal dibuat",
                        metadataJson = """{"transactionCode":"$transactionCode","total":${command.total}}""",
                        occurredAt = command.occurredAt,
                        syncStatus = SyncStatus.PENDING,
                        createdAt = now,
                        updatedAt = now
                    )
                )

                LocalCheckoutResult.Success(
                    transactionLocalId = transactionLocalId,
                    transactionClientGeneratedId = transactionClientId,
                    transactionCode = transactionCode,
                    paymentLocalId = paymentLocalId,
                    receivableLocalId = receivableLocalId,
                    syncQueueId = syncQueueId
                )
            }
        }.getOrElse { error ->
            LocalCheckoutResult.Failed(error.message ?: "Gagal menyimpan checkout lokal")
        }
    }

    private fun validate(command: LocalCheckoutCommand): String? {
        if (command.cashierUserId.isBlank()) return "Kasir wajib diisi"
        if (command.paymentMethod.isBlank()) return "Metode pembayaran wajib diisi"
        if (command.total < 0.0) return "Total transaksi tidak boleh negatif"
        if (command.subtotal < 0.0) return "Subtotal transaksi tidak boleh negatif"
        if (command.discount < 0.0) return "Diskon tidak boleh negatif"
        if (command.paidAmount < 0.0) return "Nominal bayar tidak boleh negatif"
        if (command.items.isEmpty()) return "Item transaksi wajib diisi"

        command.items.forEach { item ->
            if (item.productNameSnapshot.isBlank()) return "Nama produk wajib diisi"
            if (item.quantity <= 0.0) return "Qty item harus lebih dari 0"
            if (item.priceAtTransaction < 0.0) return "Harga item tidak boleh negatif"
            if (item.cogsAtTransaction < 0.0) return "HPP item tidak boleh negatif"
            if (item.discount < 0.0) return "Diskon item tidak boleh negatif"
            if (item.subtotal < 0.0) return "Subtotal item tidak boleh negatif"
        }

        return null
    }

    private fun LocalCheckoutItemCommand.toEntity(
        transactionLocalId: Long,
        now: Long
    ): LocalTransactionItemEntity {
        return LocalTransactionItemEntity(
            transactionLocalId = transactionLocalId,
            productLocalId = productLocalId,
            productServerId = productServerId,
            productNameSnapshot = productNameSnapshot,
            skuSnapshot = skuSnapshot,
            unitNameSnapshot = unitNameSnapshot,
            quantity = quantity,
            priceAtTransaction = priceAtTransaction,
            cogsAtTransaction = cogsAtTransaction,
            discount = discount,
            subtotal = subtotal,
            syncStatus = SyncStatus.PENDING,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun LocalCheckoutCommand.toPaymentEntity(
        transactionLocalId: Long,
        now: Long
    ): LocalPaymentEntity {
        return LocalPaymentEntity(
            clientGeneratedId = LocalIdGenerator.clientGeneratedId("PAY"),
            deviceId = deviceId,
            transactionLocalId = transactionLocalId,
            method = paymentMethod,
            amount = paidAmount,
            paidAt = occurredAt,
            syncStatus = SyncStatus.PENDING,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun LocalCheckoutCommand.toReceivableEntity(
        transactionLocalId: Long,
        remainingAmount: Double,
        now: Long
    ): LocalReceivableEntity {
        return LocalReceivableEntity(
            clientGeneratedId = LocalIdGenerator.clientGeneratedId("RCV"),
            deviceId = deviceId,
            transactionLocalId = transactionLocalId,
            customerLocalId = customerLocalId,
            customerServerId = customerServerId,
            totalAmount = total,
            paidAmount = paidAmount,
            remainingAmount = remainingAmount,
            status = if (remainingAmount <= 0.0) "PAID" else "UNPAID",
            syncStatus = SyncStatus.PENDING,
            createdAt = now,
            updatedAt = now,
            occurredAt = occurredAt
        )
    }

    private fun LocalCheckoutCommand.toSyncPayloadJson(
        transactionLocalId: Long,
        transactionClientGeneratedId: String,
        transactionCode: String,
        remainingAmount: Double
    ): String {
        val safeCustomerName = customerName?.replace("\"", "\\\"")
        val safeNote = note?.replace("\"", "\\\"")
        return buildString {
            append("{")
            append("\"transactionLocalId\":$transactionLocalId,")
            append("\"clientGeneratedId\":\"$transactionClientGeneratedId\",")
            append("\"transactionCode\":\"$transactionCode\",")
            append("\"cashierUserId\":\"$cashierUserId\",")
            append("\"cashSessionLocalId\":${cashSessionLocalId ?: "null"},")
            append("\"cashSessionServerId\":${cashSessionServerId?.quoted() ?: "null"},")
            append("\"customerLocalId\":${customerLocalId ?: "null"},")
            append("\"customerServerId\":${customerServerId?.quoted() ?: "null"},")
            append("\"customerName\":${safeCustomerName?.quoted() ?: "null"},")
            append("\"paymentMethod\":\"$paymentMethod\",")
            append("\"subtotal\":$subtotal,")
            append("\"discount\":$discount,")
            append("\"total\":$total,")
            append("\"paidAmount\":$paidAmount,")
            append("\"remainingAmount\":$remainingAmount,")
            append("\"occurredAt\":$occurredAt,")
            append("\"note\":${safeNote?.quoted() ?: "null"},")
            append("\"items\":[")
            items.forEachIndexed { index, item ->
                if (index > 0) append(",")
                append(item.toPayloadJson())
            }
            append("]")
            append("}")
        }
    }

    private fun LocalCheckoutItemCommand.toPayloadJson(): String {
        return buildString {
            append("{")
            append("\"productLocalId\":${productLocalId ?: "null"},")
            append("\"productServerId\":${productServerId?.quoted() ?: "null"},")
            append("\"productNameSnapshot\":${productNameSnapshot.replace("\"", "\\\"").quoted()},")
            append("\"skuSnapshot\":${skuSnapshot?.replace("\"", "\\\"")?.quoted() ?: "null"},")
            append("\"unitNameSnapshot\":${unitNameSnapshot?.replace("\"", "\\\"")?.quoted() ?: "null"},")
            append("\"quantity\":$quantity,")
            append("\"priceAtTransaction\":$priceAtTransaction,")
            append("\"cogsAtTransaction\":$cogsAtTransaction,")
            append("\"discount\":$discount,")
            append("\"subtotal\":$subtotal")
            append("}")
        }
    }

    private fun shouldCreateReceivable(paymentMethod: String, remainingAmount: Double): Boolean {
        val normalizedMethod = paymentMethod.trim().uppercase()
        return remainingAmount > 0.0 || normalizedMethod == "HUTANG" || normalizedMethod == "DP"
    }

    private fun transactionStatus(remainingAmount: Double): String {
        return if (remainingAmount > 0.0) "UNPAID" else "PAID"
    }

    private fun localTransactionCode(occurredAt: Long): String {
        val dateKey = Instant.ofEpochMilli(occurredAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DATE_FORMATTER)
        val suffix = LocalIdGenerator.clientGeneratedId("LCL")
            .substringAfter("-")
            .take(8)
            .uppercase()
        return "TRX-$dateKey-$suffix"
    }

    private fun String.quoted(): String = "\"$this\""

    private companion object {
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}

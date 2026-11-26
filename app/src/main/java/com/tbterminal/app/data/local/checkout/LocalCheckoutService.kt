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
import com.tbterminal.app.data.local.model.ScaledDecimal2
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncOperation
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.remote.BigDecimalStringSerializer
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
                val remainingAmount = (command.remainingAmount ?: command.total.subtract(command.paidAmount))
                    .coerceAtLeast(BigDecimal.ZERO)
                val remainingScaled = ScaledDecimal2.ofExact(remainingAmount)
                val totalScaled = ScaledDecimal2.ofExact(command.total)
                val paidScaled = ScaledDecimal2.ofExact(command.paidAmount)
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
                        status = transactionStatus(remainingScaled),
                        subtotal = ScaledDecimal2.ofExact(command.subtotal),
                        discount = ScaledDecimal2.ofExact(command.discount),
                        taxEnabled = command.taxEnabled,
                        taxMode = command.taxMode,
                        taxRate = ScaledDecimal2.ofExact(command.taxRate),
                        taxLabel = command.taxLabel,
                        taxableBase = ScaledDecimal2.ofExact(command.taxableBase),
                        taxAmount = ScaledDecimal2.ofExact(command.taxAmount),
                        roundingAmount = ScaledDecimal2.ofExact(command.roundingAmount),
                        total = totalScaled,
                        paidAmount = paidScaled,
                        remainingAmount = remainingScaled,
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
                            quantity = ScaledDecimal2.ofExact(item.resolvedBaseQuantity),
                            status = SyncStatus.PENDING,
                            updatedAt = now
                        )
                    }
                }

                val paymentLines = command.payments?.filter { it.amount > BigDecimal.ZERO }.orEmpty()
                val paymentLocalId = if (paymentLines.isNotEmpty()) {
                    var firstId: Long? = null
                    paymentLines.forEach { line ->
                        val id = database.paymentDao().upsert(
                            LocalPaymentEntity(
                                clientGeneratedId = LocalIdGenerator.clientGeneratedId("PAY"),
                                deviceId = deviceId,
                                transactionLocalId = transactionLocalId,
                                method = line.method,
                                amount = ScaledDecimal2.ofExact(line.amount),
                                reference = line.reference,
                                paidAt = command.occurredAt,
                                syncStatus = SyncStatus.PENDING,
                                createdAt = now,
                                updatedAt = now
                            )
                        )
                        if (firstId == null) firstId = id
                    }
                    firstId
                } else if (command.paidAmount > BigDecimal.ZERO) {
                    database.paymentDao().upsert(command.toPaymentEntity(transactionLocalId, now))
                } else {
                    null
                }

                val receivableLocalId = if (shouldCreateReceivable(command.paymentMethod, remainingScaled)) {
                    database.receivableDao().upsert(command.toReceivableEntity(transactionLocalId, remainingScaled, now))
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
                        metadataJson = buildJsonObject {
                            put("transactionCode", transactionCode)
                            put("total", command.total.setScale(2, RoundingMode.HALF_UP).toPlainString())
                        }.toString(),
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
        if (command.total < BigDecimal.ZERO) return "Total transaksi tidak boleh negatif"
        if (command.subtotal < BigDecimal.ZERO) return "Subtotal transaksi tidak boleh negatif"
        if (command.discount < BigDecimal.ZERO) return "Diskon tidak boleh negatif"
        if (command.paidAmount < BigDecimal.ZERO) return "Nominal bayar tidak boleh negatif"
        if (exceedsMaxBaseQuantityScale(command.subtotal) ||
            exceedsMaxBaseQuantityScale(command.discount) ||
            exceedsMaxBaseQuantityScale(command.taxRate) ||
            exceedsMaxBaseQuantityScale(command.taxableBase) ||
            exceedsMaxBaseQuantityScale(command.taxAmount) ||
            exceedsMaxBaseQuantityScale(command.roundingAmount) ||
            exceedsMaxBaseQuantityScale(command.total) ||
            exceedsMaxBaseQuantityScale(command.paidAmount) ||
            command.remainingAmount?.let(::exceedsMaxBaseQuantityScale) == true
        ) {
            return "Nominal transaksi lokal melebihi 2 angka desimal"
        }
        if (command.payments?.any { it.amount < BigDecimal.ZERO || exceedsMaxBaseQuantityScale(it.amount) } == true) {
            return "Nominal baris pembayaran tidak valid"
        }
        if (command.items.isEmpty()) return "Item transaksi wajib diisi"

        command.items.forEach { item ->
            if (item.productNameSnapshot.isBlank()) return "Nama produk wajib diisi"
            if (item.quantity <= BigDecimal.ZERO) return "Qty item harus lebih dari 0"
            if (exceedsMaxBaseQuantityScale(item.quantity)) {
                return "Qty item ${item.productNameSnapshot} melebihi 2 angka desimal"
            }
            if (item.resolvedBaseQuantity <= BigDecimal.ZERO) return "Qty satuan dasar harus lebih dari 0"
            if (item.resolvedConversionFactor <= BigDecimal.ZERO) return "Faktor konversi satuan harus lebih dari 0"
            if (item.resolvedConversionFactor.compareTo(BigDecimal.ONE) != 0 &&
                exceedsMaxBaseQuantityScale(item.resolvedBaseQuantity)
            ) {
                return "Jumlah satuan dasar ${item.productNameSnapshot} melebihi 2 angka desimal"
            }
            if (exceedsMaxBaseQuantityScale(item.resolvedBaseQuantity)) {
                return "Jumlah satuan dasar ${item.productNameSnapshot} melebihi 2 angka desimal"
            }
            if (item.priceAtTransaction < BigDecimal.ZERO) return "Harga item tidak boleh negatif"
            if (item.cogsAtTransaction < BigDecimal.ZERO) return "HPP item tidak boleh negatif"
            if (item.discount < BigDecimal.ZERO) return "Diskon item tidak boleh negatif"
            if (item.subtotal < BigDecimal.ZERO) return "Subtotal item tidak boleh negatif"
            if (exceedsMaxBaseQuantityScale(item.priceAtTransaction) ||
                exceedsMaxBaseQuantityScale(item.cogsAtTransaction) ||
                exceedsMaxBaseQuantityScale(item.discount) ||
                exceedsMaxBaseQuantityScale(item.subtotal)
            ) {
                return "Nominal item ${item.productNameSnapshot} melebihi 2 angka desimal"
            }
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
            unitServerId = unitServerId?.trim()?.takeIf(String::isNotBlank),
            quantity = ScaledDecimal2.ofExact(quantity),
            baseQuantity = resolvedBaseQuantity,
            conversionFactor = resolvedConversionFactor,
            priceAtTransaction = ScaledDecimal2.ofExact(priceAtTransaction),
            cogsAtTransaction = ScaledDecimal2.ofExact(cogsAtTransaction),
            discount = ScaledDecimal2.ofExact(discount),
            subtotal = ScaledDecimal2.ofExact(subtotal),
            note = note,
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
            amount = ScaledDecimal2.ofExact(paidAmount),
            paidAt = occurredAt,
            syncStatus = SyncStatus.PENDING,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun LocalCheckoutCommand.toReceivableEntity(
        transactionLocalId: Long,
        remainingAmount: ScaledDecimal2,
        now: Long
    ): LocalReceivableEntity {
        return LocalReceivableEntity(
            clientGeneratedId = LocalIdGenerator.clientGeneratedId("RCV"),
            deviceId = deviceId,
            transactionLocalId = transactionLocalId,
            customerLocalId = customerLocalId,
            customerServerId = customerServerId,
            totalAmount = ScaledDecimal2.ofExact(total),
            paidAmount = ScaledDecimal2.ofExact(paidAmount),
            remainingAmount = remainingAmount,
            status = if (!remainingAmount.isPositive) "PAID" else "UNPAID",
            syncStatus = SyncStatus.PENDING,
            createdAt = now,
            updatedAt = now,
            occurredAt = occurredAt
        )
    }

    internal fun LocalCheckoutCommand.toSyncPayloadJson(
        transactionLocalId: Long,
        transactionClientGeneratedId: String,
        transactionCode: String,
        remainingAmount: BigDecimal
    ): String {
        val payload = LocalCheckoutSyncPayload(
            transactionLocalId = transactionLocalId,
            clientGeneratedId = transactionClientGeneratedId,
            transactionCode = transactionCode,
            cashierUserId = cashierUserId,
            cashSessionLocalId = cashSessionLocalId,
            cashSessionServerId = cashSessionServerId,
            customerLocalId = customerLocalId,
            customerServerId = customerServerId,
            customerName = customerName,
            paymentMethod = paymentMethod,
            subtotal = subtotal.setScale(2, RoundingMode.HALF_UP),
            discount = discount.setScale(2, RoundingMode.HALF_UP),
            total = total.setScale(2, RoundingMode.HALF_UP),
            paidAmount = paidAmount.setScale(2, RoundingMode.HALF_UP),
            remainingAmount = remainingAmount.setScale(2, RoundingMode.HALF_UP),
            occurredAt = occurredAt,
            note = note,
            items = items.map { it.toPayloadItem() }
        )
        return SYNC_PAYLOAD_JSON.encodeToString(payload)
    }

    private fun LocalCheckoutItemCommand.toPayloadItem(): LocalCheckoutSyncItemPayload {
        return LocalCheckoutSyncItemPayload(
            productLocalId = productLocalId,
            productServerId = productServerId,
            productNameSnapshot = productNameSnapshot,
            skuSnapshot = skuSnapshot,
            unitNameSnapshot = unitNameSnapshot,
            unitServerId = unitServerId?.trim()?.takeIf(String::isNotBlank),
            quantity = quantity.setScale(2, RoundingMode.HALF_UP),
            baseQuantity = resolvedBaseQuantity,
            conversionFactor = resolvedConversionFactor,
            priceAtTransaction = priceAtTransaction.setScale(2, RoundingMode.HALF_UP),
            cogsAtTransaction = cogsAtTransaction.setScale(2, RoundingMode.HALF_UP),
            discount = discount.setScale(2, RoundingMode.HALF_UP),
            subtotal = subtotal.setScale(2, RoundingMode.HALF_UP)
        )
    }

    private fun shouldCreateReceivable(paymentMethod: String, remainingAmount: ScaledDecimal2): Boolean {
        val normalizedMethod = paymentMethod.trim().uppercase()
        return remainingAmount.isPositive || normalizedMethod == "HUTANG" || normalizedMethod == "DP"
    }

    private fun transactionStatus(remainingAmount: ScaledDecimal2): String {
        return if (remainingAmount.isPositive) "UNPAID" else "PAID"
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

    internal companion object {
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val SYNC_PAYLOAD_JSON: Json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}

@Serializable
internal data class LocalCheckoutSyncPayload(
    val transactionLocalId: Long,
    val clientGeneratedId: String,
    val transactionCode: String,
    val cashierUserId: String,
    val cashSessionLocalId: Long? = null,
    val cashSessionServerId: String? = null,
    val customerLocalId: Long? = null,
    val customerServerId: String? = null,
    val customerName: String? = null,
    val paymentMethod: String,
    @Serializable(with = BigDecimalStringSerializer::class) val subtotal: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val discount: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val total: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val paidAmount: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val remainingAmount: BigDecimal,
    val occurredAt: Long,
    val note: String? = null,
    val items: List<LocalCheckoutSyncItemPayload>
)

@Serializable
internal data class LocalCheckoutSyncItemPayload(
    val productLocalId: Long? = null,
    val productServerId: String? = null,
    val productNameSnapshot: String,
    val skuSnapshot: String? = null,
    val unitNameSnapshot: String? = null,
    val unitServerId: String? = null,
    @Serializable(with = BigDecimalStringSerializer::class) val quantity: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val baseQuantity: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val conversionFactor: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val priceAtTransaction: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val cogsAtTransaction: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val discount: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val subtotal: BigDecimal
)

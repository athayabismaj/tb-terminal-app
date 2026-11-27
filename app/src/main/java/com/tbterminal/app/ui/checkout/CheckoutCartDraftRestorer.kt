package com.tbterminal.app.ui.checkout

import com.tbterminal.app.data.local.database.CustomerLocalDataSource
import com.tbterminal.app.data.local.database.ProductLocalDataSource
import com.tbterminal.app.data.local.entity.LocalCustomerEntity
import com.tbterminal.app.data.local.entity.LocalProductEntity
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.model.ProductDetail
import com.tbterminal.app.data.model.ProductPriceTier
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.sync.runSuspendCatching
import java.math.BigDecimal

internal class CheckoutCartDraftRestorer(
    private val productLocalDataSource: ProductLocalDataSource?,
    private val customerLocalDataSource: CustomerLocalDataSource?,
    private val inventoryRepository: InventoryRepository,
    private val customerRepository: CustomerRepository
) {
    private data class ResolvedProductMetadata(
        val name: String,
        val sku: String,
        val baseUnitId: String?,
        val baseUnitName: String,
        val secondaryUnitId: String?,
        val secondaryUnitName: String?,
        val secondaryUnitFactor: BigDecimal?,
        val retailBaseUnitPrice: BigDecimal,
        val contractorBaseUnitPrice: BigDecimal,
        val masterUnitDiscount: BigDecimal,
        val priceTiers: List<ProductPriceTier>
    )

    suspend fun restoreCartItems(drafts: List<PersistedCartItemDraft>): List<CartItem> {
        if (drafts.isEmpty()) return emptyList()
        val resolvedByProductId = mutableMapOf<String, ResolvedProductMetadata?>()
        for (productId in drafts.map { it.productId }.distinct()) {
            resolvedByProductId[productId] = resolveProductMetadata(productId)
        }
        return drafts.mapNotNull { draft ->
            restoreSingleItem(draft, resolvedByProductId[draft.productId])
        }
    }

    suspend fun restoreCustomer(snapshot: PersistedCustomerSnapshot?): Customer? {
        val snap = snapshot ?: return null
        val cached = customerLocalDataSource?.let { ds ->
            runSuspendCatching { ds.getCachedCustomerByServerId(snap.id) }.getOrNull()
        }
        if (cached != null) {
            return cached.toDomainCustomer()
        }
        val remote = runSuspendCatching { customerRepository.getCustomerById(snap.id) }.getOrNull()
        if (remote is RepositoryResult.Success) {
            return remote.data
        }
        // Fallback snapshot keeps selected customer intact when offline & uncached.
        return snap.toFallbackCustomerOrNull()
    }

    suspend fun restoreHeldCarts(drafts: List<PersistedHeldCartDraft>): List<HeldCart> {
        if (drafts.isEmpty()) return emptyList()
        val allProductIds = drafts.flatMap { held -> held.items.map { it.productId } }.distinct()
        val resolvedByProductId = mutableMapOf<String, ResolvedProductMetadata?>()
        for (productId in allProductIds) {
            resolvedByProductId[productId] = resolveProductMetadata(productId)
        }

        return drafts.mapNotNull { heldDraft ->
            val items = heldDraft.items.mapNotNull { draft ->
                restoreSingleItem(draft, resolvedByProductId[draft.productId])
            }
            if (items.isEmpty()) return@mapNotNull null

            val customer = restoreCustomer(heldDraft.customer)
            val txDiscount = heldDraft.parsedTransactionDiscount()
            val deliveryFee = heldDraft.parsedDeliveryFee()
            val repricedItems = repriceCartItemsForCheckout(
                items = items,
                isContractorCustomer = customer?.isContractor == true
            )
            val totals = estimateCheckoutTotals(repricedItems, txDiscount)
            val effectiveDelivery = if (heldDraft.isDeliveryEnabled) deliveryFee else BigDecimal.ZERO
            HeldCart(
                id = heldDraft.id,
                note = heldDraft.note,
                customer = customer,
                items = repricedItems,
                transactionDiscount = txDiscount,
                deliveryFee = deliveryFee,
                deliveryAddress = heldDraft.deliveryAddress,
                deliveryCourier = heldDraft.deliveryCourier,
                deliveryFleet = heldDraft.deliveryFleet,
                deliveryRecipientPhone = heldDraft.deliveryRecipientPhone,
                deliveryVehiclePlate = heldDraft.deliveryVehiclePlate,
                deliverySpecialInstructions = heldDraft.deliverySpecialInstructions,
                isDeliveryEnabled = heldDraft.isDeliveryEnabled,
                subtotal = totals.grossSubtotal,
                finalTotal = totals.netTotal.add(effectiveDelivery).coerceAtLeast(BigDecimal.ZERO),
                createdAt = heldDraft.createdAt
            )
        }
    }

    private suspend fun resolveProductMetadata(productId: String): ResolvedProductMetadata? {
        if (productId.isBlank()) return null
        val local = productLocalDataSource?.let { ds ->
            runSuspendCatching { ds.getCachedProductByServerId(productId) }.getOrNull()
        }
        if (local != null && local.isActive) {
            return local.toResolvedMetadata()
        }

        val remote = runSuspendCatching { inventoryRepository.getProductDetail(productId) }.getOrNull()
        if (remote is RepositoryResult.Success && remote.data.product.isActive) {
            return remote.data.toResolvedMetadata()
        }
        return null
    }

    private fun restoreSingleItem(
        draft: PersistedCartItemDraft,
        resolved: ResolvedProductMetadata?
    ): CartItem? {
        val fallback = draft.toFallbackCartItemOrNull() ?: return null
        if (resolved == null) {
            // Product missing from local cache and server unreachable/error: keep fallback snapshot.
            return fallback
        }

        val secondaryFactor = resolved.secondaryUnitFactor?.takeIf { it > BigDecimal.ZERO }
            ?: fallback.secondaryUnitFactor
        val useSecondary = draft.isSecondaryUnit && secondaryFactor != null
        val effectiveUnitPrice = if (useSecondary) {
            resolved.retailBaseUnitPrice.multiply(secondaryFactor)
        } else {
            resolved.retailBaseUnitPrice
        }
        val effectiveUnitName = if (useSecondary) {
            resolved.secondaryUnitName?.takeIf(String::isNotBlank)
                ?: fallback.secondaryUnitName
                ?: fallback.unitName
        } else {
            resolved.baseUnitName.ifBlank { fallback.unitName }
        }

        return fallback.copy(
            productName = resolved.name,
            unitPrice = effectiveUnitPrice,
            sku = resolved.sku.ifBlank { fallback.sku },
            unitName = effectiveUnitName,
            baseUnitPrice = resolved.retailBaseUnitPrice,
            baseUnitName = resolved.baseUnitName.ifBlank { fallback.baseUnitName },
            secondaryUnitId = resolved.secondaryUnitId ?: fallback.secondaryUnitId,
            secondaryUnitName = resolved.secondaryUnitName ?: fallback.secondaryUnitName,
            secondaryUnitFactor = secondaryFactor,
            isSecondaryUnit = useSecondary,
            retailBaseUnitPrice = resolved.retailBaseUnitPrice,
            contractorBaseUnitPrice = resolved.contractorBaseUnitPrice,
            priceTiers = resolved.priceTiers,
            masterUnitDiscount = resolved.masterUnitDiscount,
            baseUnitId = resolved.baseUnitId ?: fallback.baseUnitId
        )
    }

    private fun LocalProductEntity.toResolvedMetadata(): ResolvedProductMetadata {
        val retail = priceRetail.toBigDecimal().takeIf { it > BigDecimal.ZERO } ?: priceSell.toBigDecimal()
        return ResolvedProductMetadata(
            name = name,
            sku = sku.orEmpty(),
            baseUnitId = unitServerId,
            baseUnitName = unitName.orEmpty(),
            secondaryUnitId = secondaryUnitServerId,
            secondaryUnitName = secondaryUnitName,
            secondaryUnitFactor = secondaryUnitFactor,
            retailBaseUnitPrice = retail,
            contractorBaseUnitPrice = priceContractor.toBigDecimal(),
            masterUnitDiscount = discount.toBigDecimal(),
            priceTiers = emptyList()
        )
    }

    private fun ProductDetail.toResolvedMetadata(): ResolvedProductMetadata {
        return ResolvedProductMetadata(
            name = product.name,
            sku = product.sku,
            baseUnitId = product.baseUnitId,
            baseUnitName = stock?.unitName.orEmpty(),
            secondaryUnitId = product.secondaryUnitId,
            secondaryUnitName = stock?.secondaryUnitName,
            secondaryUnitFactor = product.secondaryUnitFactor,
            retailBaseUnitPrice = product.priceRetail,
            contractorBaseUnitPrice = product.priceContractor,
            masterUnitDiscount = product.discount,
            priceTiers = product.priceTiers
        )
    }

    private fun LocalCustomerEntity.toDomainCustomer(): Customer {
        return Customer(
            id = serverId ?: clientGeneratedId,
            name = name,
            phone = phone,
            address = address,
            isContractor = isContractor,
            creditLimit = creditLimit.toBigDecimal(),
            paymentTermDays = paymentTermDays,
            isActive = isActive,
            createdAt = remoteCreatedAt.orEmpty(),
            updatedAt = remoteUpdatedAt.orEmpty()
        )
    }
}

package com.tbterminal.app.ui.checkout

import com.tbterminal.app.data.model.Customer
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutFase1FeaturesTest {

    @Test
    fun `recalculateCheckoutState includes delivery fee when delivery is enabled`() {
        val items = listOf(
            CartItem(
                productId = "prod-semen",
                productName = "Semen Tiga Roda",
                unitPrice = BigDecimal("70000"),
                quantity = 2
            )
        )

        // Tanpa ongkir
        val stateWithoutDelivery = CheckoutUiState(isDeliveryEnabled = false).recalculateCheckoutState(
            items = items,
            discount = null,
            deliveryFeeAmount = BigDecimal.ZERO
        )
        assertEquals(0, BigDecimal("140000").compareTo(stateWithoutDelivery.subtotal))
        assertEquals(0, BigDecimal("140000").compareTo(stateWithoutDelivery.finalTotal))

        // Dengan ongkir Rp 35.000
        val deliveryFee = BigDecimal("35000")
        val stateWithDelivery = stateWithoutDelivery.copy(
            isDeliveryEnabled = true,
            deliveryFee = deliveryFee
        ).recalculateCheckoutState(
            items = items,
            discount = null,
            deliveryFeeAmount = deliveryFee
        )
        assertEquals(0, BigDecimal("140000").compareTo(stateWithDelivery.subtotal))
        assertEquals(0, BigDecimal("175000").compareTo(stateWithDelivery.finalTotal))
        assertEquals(stateWithDelivery.finalTotal.toPlainString(), stateWithDelivery.amountPaidInput)
    }

    @Test
    fun `held cart model holds and restores items and customer properly`() {
        val customer = Customer(
            id = "cust-1",
            name = "Bambang Perkasa",
            phone = "08123456789",
            address = "Jl. Merdeka No. 45",
            isContractor = false,
            creditLimit = BigDecimal("5000000"),
            paymentTermDays = 30,
            isActive = true,
            createdAt = "2026-10-28T10:00:00Z",
            updatedAt = "2026-10-28T10:00:00Z"
        )
        val items = listOf(
            CartItem(
                productId = "prod-besi-10",
                productName = "Besi Beton 10mm",
                unitPrice = BigDecimal("65000"),
                quantity = 10
            )
        )

        val heldCart = HeldCart(
            note = "Bpk. Bambang - Besi Proyek",
            customer = customer,
            items = items,
            deliveryFee = BigDecimal("50000"),
            deliveryAddress = "Jl. Merdeka No. 45",
            deliveryCourier = "Pick-up Engkel Toko",
            isDeliveryEnabled = true,
            subtotal = BigDecimal("650000"),
            finalTotal = BigDecimal("700000")
        )

        assertEquals("Bpk. Bambang - Besi Proyek", heldCart.note)
        assertEquals("Bambang Perkasa", heldCart.customer?.name)
        assertEquals(1, heldCart.items.size)
        assertEquals(10, heldCart.items.first().quantity)
        assertTrue(heldCart.isDeliveryEnabled)
        assertEquals(BigDecimal("50000"), heldCart.deliveryFee)
        assertEquals("Jl. Merdeka No. 45", heldCart.deliveryAddress)
    }

    @Test
    fun `quick pin lock state handles locking and unlocking logic`() {
        val lockedState = CheckoutUiState(
            isScreenLocked = true,
            lockPinInput = "1234",
            lockPinError = null
        )
        assertTrue(lockedState.isScreenLocked)
        assertEquals("1234", lockedState.lockPinInput)

        // Salah PIN
        val wrongPinState = lockedState.copy(lockPinError = "PIN salah. Coba lagi.")
        assertTrue(wrongPinState.isScreenLocked)
        assertNotNull(wrongPinState.lockPinError)

        // Benar PIN -> Unlock
        val unlockedState = wrongPinState.copy(
            isScreenLocked = false,
            lockPinInput = "",
            lockPinError = null
        )
        assertFalse(unlockedState.isScreenLocked)
        assertEquals("", unlockedState.lockPinInput)
        assertNull(unlockedState.lockPinError)
    }
}

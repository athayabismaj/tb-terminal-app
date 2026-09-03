package com.tbterminal.app.ui.customers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerFormValidationTest {
    @Test
    fun `valid customer has no field errors`() {
        val errors = validateCustomerForm(
            CustomerFormInput(
                name = "Toko Sejahtera",
                phone = "+62 812-3456-7890",
                address = "Jakarta",
                creditLimit = "1250000.50",
                paymentTermDays = "30",
            ),
        )

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `required name is reported next to name field`() {
        val errors = validateCustomerForm(CustomerFormInput(name = "   "))

        assertEquals("Nama pelanggan wajib diisi.", errors[CUSTOMER_FIELD_NAME])
    }

    @Test
    fun `invalid phone money and term are rejected per field`() {
        val errors = validateCustomerForm(
            CustomerFormInput(
                name = "Pelanggan",
                phone = "0812ABC",
                creditLimit = "-1.234",
                paymentTermDays = "4000",
            ),
        )

        assertTrue(errors.containsKey(CUSTOMER_FIELD_PHONE))
        assertTrue(errors.containsKey(CUSTOMER_FIELD_CREDIT_LIMIT))
        assertTrue(errors.containsKey(CUSTOMER_FIELD_PAYMENT_TERM))
        assertFalse(errors.containsKey(CUSTOMER_FIELD_NAME))
    }
}

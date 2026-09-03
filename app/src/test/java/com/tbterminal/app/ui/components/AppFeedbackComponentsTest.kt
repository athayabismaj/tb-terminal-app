package com.tbterminal.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AppFeedbackComponentsTest {
    @Test
    fun confirmationRequiresActionTargetAndConsequence() {
        assertThrows(IllegalArgumentException::class.java) {
            AppConfirmationSpec("Nonaktifkan pelanggan", "", "Pelanggan tidak dapat dipilih.", "Nonaktifkan")
        }
    }

    @Test
    fun statusPresentationUsesConsistentLabelsAndTones() {
        assertEquals(StatusTone.SUCCESS, statusPresentation("lunas").tone)
        assertEquals(StatusTone.WARNING, statusPresentation("DP").tone)
        assertEquals(StatusTone.DANGER, statusPresentation("voided").tone)
        assertEquals(StatusTone.INFO, statusPresentation("USED").tone)
        assertEquals("REFUNDED", statusPresentation("refunded").label)
    }
}

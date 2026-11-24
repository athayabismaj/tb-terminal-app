package com.tbterminal.app.data.sync

import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncErrorClassifierTest {
    @Test
    fun allSixOfflineConflictCodesClassifyAsConflictByCodeEvenWithoutMessageKeywords() {
        val codes = listOf(
            "OFFLINE_PRICE_CONFLICT",
            "OFFLINE_ROUNDING_CONFLICT",
            "OFFLINE_TAX_CONFLICT",
            "OFFLINE_OCCURRED_AT_INVALID",
            "OFFLINE_OPENED_AT_INVALID",
            "OFFLINE_CLOSED_AT_INVALID",
        )

        val distinctTitles = mutableSetOf<String>()
        val distinctRecommendations = mutableSetOf<String>()

        codes.forEach { code ->
            val result = SyncErrorClassifier.classify(
                httpCode = 409,
                message = "Ditolak oleh server",
                errorCode = code,
            )
            assertEquals("Expected CONFLICT for $code", SyncErrorCategory.CONFLICT, result.category)
            assertNotNull("Expected title for $code", result.title)
            assertTrue("Title blank for $code", !result.title.isNullOrBlank())
            assertTrue("Recommendation blank for $code", result.recommendation.isNotBlank())
            assertFalse("Raw code leaked in title for $code", result.title!!.contains("OFFLINE_"))
            assertFalse("Raw code leaked in recommendation for $code", result.recommendation.contains("OFFLINE_"))

            distinctTitles += result.title!!
            distinctRecommendations += result.recommendation

            // Re-classifying from stored SyncQueueEntity (message + errorCode without httpCode) must yield the same result
            val stored = SyncErrorClassifier.classifyMessage(message = "Ditolak oleh server", errorCode = code)
            assertEquals(result, stored)
        }

        assertEquals(6, distinctTitles.size)
        assertEquals(6, distinctRecommendations.size)
    }

    @Test
    fun separates401Auth403And409Conflict422ValidationAndNetwork() {
        assertEquals(
            SyncErrorCategory.AUTH,
            SyncErrorClassifier.classify(httpCode = 401, message = "Unauthorized").category,
        )
        assertEquals(
            SyncErrorCategory.CONFLICT,
            SyncErrorClassifier.classify(httpCode = 403, message = "Forbidden").category,
        )
        assertEquals(
            SyncErrorCategory.CONFLICT,
            SyncErrorClassifier.classify(httpCode = 409, message = "Versi data berubah").category,
        )
        assertEquals(
            SyncErrorCategory.VALIDATION,
            SyncErrorClassifier.classify(httpCode = 422, message = "Field tidak lengkap").category,
        )
        assertEquals(
            SyncErrorCategory.NETWORK,
            SyncErrorClassifier.classify(throwable = UnknownHostException("offline")).category,
        )
    }
}

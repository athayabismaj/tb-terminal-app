package com.tbterminal.app.data.repository

import com.tbterminal.app.data.media.CoilImageLoaderFactory
import com.tbterminal.app.data.remote.InventoryApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class InventoryRepositoryPhotoTest {

    @Test
    fun `uploadProductPhoto sends multipart request and deleteProductPhoto handles 204 No Content`() = runTest {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"success":true,"data":{"productId":"prod-1","photoUrl":"https://res.cloudinary.com/demo/image/upload/v1/tbterminal/products/p1.jpg"}}""")
        )
        server.enqueue(MockResponse().setResponseCode(204))
        server.start()

        try {
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
            val api = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(OkHttpClient.Builder().build())
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(InventoryApi::class.java)
            val repo = RemoteInventoryRepository(inventoryApi = api)

            val uploadResult = repo.uploadProductPhoto("prod-1", byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))
            assertTrue(uploadResult is RepositoryResult.Success)
            assertEquals(
                "https://res.cloudinary.com/demo/image/upload/v1/tbterminal/products/p1.jpg",
                (uploadResult as RepositoryResult.Success).data
            )

            val recordedUpload = server.takeRequest()
            assertEquals("POST", recordedUpload.method)
            assertEquals("/api/inventory/products/prod-1/photo", recordedUpload.path)
            assertTrue(recordedUpload.getHeader("Content-Type").orEmpty().startsWith("multipart/form-data"))

            val deleteResult = repo.deleteProductPhoto("prod-1")
            assertTrue(deleteResult is RepositoryResult.Success)

            val recordedDelete = server.takeRequest()
            assertEquals("DELETE", recordedDelete.method)
            assertEquals("/api/inventory/products/prod-1/photo", recordedDelete.path)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `clean Coil OkHttpClient does not send Authorization header to image host`() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
        server.start()

        try {
            val client = CoilImageLoaderFactory.createCleanHttpClient()
            val response = client.newCall(Request.Builder().url(server.url("/image/upload/demo.jpg")).build()).execute()
            response.close()

            val recorded = server.takeRequest()
            assertNull(recorded.getHeader("Authorization"))
        } finally {
            server.shutdown()
        }
    }
}

package com.tbterminal.app.data.media

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ProductImageCompressorTest {

    @Test
    fun `scaledDimensions caps longest edge to maxDim preserving ratio`() {
        val (w1, h1) = AndroidProductImageCompressor.scaledDimensions(2400, 1600, 1280)
        assertTrue(w1 <= 1280 && h1 <= 1280)
        assertEquals(1280, w1)
        assertEquals(853, h1)

        val (w2, h2) = AndroidProductImageCompressor.scaledDimensions(800, 600, 1280)
        assertEquals(800, w2)
        assertEquals(600, h2)
    }

    @Test
    fun `compressForUpload produces JPEG bytes within max dimension`() = runTest {
        val app = RuntimeEnvironment.getApplication()
        val tempFile = File.createTempFile("oversized_test", ".png", app.cacheDir)
        try {
            val largeBitmap = Bitmap.createBitmap(2000, 1000, Bitmap.Config.ARGB_8888)
            largeBitmap.eraseColor(Color.GREEN)
            tempFile.outputStream().use { out ->
                largeBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            largeBitmap.recycle()

            val compressor = AndroidProductImageCompressor(app.contentResolver, maxDimensionPx = 1280, jpegQuality = 82)
            val result = compressor.compressForUpload(Uri.fromFile(tempFile))

            assertTrue("Expected compression success, got: ${result.exceptionOrNull()}", result.isSuccess)
            val compressed = result.getOrThrow()
            assertTrue(compressed.bytes.size >= 3)
            assertEquals(0xFF.toByte(), compressed.bytes[0])
            assertEquals(0xD8.toByte(), compressed.bytes[1])
            assertEquals(0xFF.toByte(), compressed.bytes[2])

            val decoded = BitmapFactory.decodeByteArray(compressed.bytes, 0, compressed.bytes.size)
            assertTrue(decoded.width <= 1280)
            assertTrue(decoded.height <= 1280)
            decoded.recycle()
        } finally {
            tempFile.delete()
        }
    }
}

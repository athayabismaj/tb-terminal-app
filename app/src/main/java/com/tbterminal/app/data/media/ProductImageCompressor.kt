package com.tbterminal.app.data.media

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

data class CompressedImage(
    val bytes: ByteArray,
    val mimeType: String = "image/jpeg",
    val fileName: String = "product.jpg"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CompressedImage) return false
        return bytes.contentEquals(other.bytes) && mimeType == other.mimeType && fileName == other.fileName
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + fileName.hashCode()
        return result
    }
}

interface ImageCompressor {
    suspend fun compressForUpload(uri: Uri): Result<CompressedImage>
}

class AndroidProductImageCompressor(
    private val contentResolver: ContentResolver,
    private val maxDimensionPx: Int = DEFAULT_MAX_DIMENSION_PX,
    private val jpegQuality: Int = DEFAULT_JPEG_QUALITY
) : ImageCompressor {

    override suspend fun compressForUpload(uri: Uri): Result<CompressedImage> = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeScaledBitmap(uri)
                ?: return@withContext Result.failure(IllegalArgumentException("Gagal membaca gambar yang dipilih"))

            try {
                val finalBitmap = ensureMaxDimension(bitmap, maxDimensionPx)
                try {
                    val out = ByteArrayOutputStream()
                    // Re-encode JPEG membuang metadata EXIF/GPS dari kamera secara otomatis.
                    val ok = finalBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
                    val encoded = out.toByteArray()
                    if (!ok || encoded.isEmpty()) {
                        return@withContext Result.failure(IllegalStateException("Gagal mengompresi foto produk"))
                    }
                    Result.success(CompressedImage(bytes = encoded))
                } finally {
                    if (finalBitmap !== bitmap) {
                        finalBitmap.recycle()
                    }
                }
            } finally {
                bitmap.recycle()
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Exception) {
            Result.failure(cause)
        }
    }

    private fun decodeScaledBitmap(uri: Uri): Bitmap? {
        return try {
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val (targetW, targetH) = scaledDimensions(info.size.width, info.size.height, maxDimensionPx)
                decoder.setTargetSize(targetW, targetH)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } catch (_: Throwable) {
            decodeWithBitmapFactory(uri)
        }
    }

    private fun decodeWithBitmapFactory(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimensionPx)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    companion object {
        const val DEFAULT_MAX_DIMENSION_PX = 1280
        const val DEFAULT_JPEG_QUALITY = 82

        internal fun scaledDimensions(width: Int, height: Int, maxDim: Int): Pair<Int, Int> {
            if (width <= 0 || height <= 0) return 1 to 1
            if (width <= maxDim && height <= maxDim) return width to height
            val ratio = min(maxDim.toFloat() / width, maxDim.toFloat() / height)
            val targetW = (width * ratio).toInt().coerceAtLeast(1)
            val targetH = (height * ratio).toInt().coerceAtLeast(1)
            return targetW to targetH
        }

        internal fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
            if (width <= 0 || height <= 0) return 1
            var sample = 1
            var longest = max(width, height)
            while (longest / 2 >= maxDim) {
                sample *= 2
                longest /= 2
            }
            return sample
        }

        private fun ensureMaxDimension(bitmap: Bitmap, maxDim: Int): Bitmap {
            val w = bitmap.width
            val h = bitmap.height
            if (w <= maxDim && h <= maxDim) return bitmap
            val (targetW, targetH) = scaledDimensions(w, h, maxDim)
            return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        }
    }
}

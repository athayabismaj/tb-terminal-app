package com.tbterminal.app.data.media

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

object CoilImageLoaderFactory {
    private const val DISK_CACHE_BYTES = 50L * 1024L * 1024L

    /**
     * OkHttpClient terpisah untuk Coil tanpa AuthInterceptor/JWT backend
     * agar token tidak pernah terkirim ke CDN pihak ketiga (Cloudinary).
     */
    fun createCleanHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun create(
        context: Context,
        okHttpClient: OkHttpClient = createCleanHttpClient()
    ): ImageLoader {
        val appContext = context.applicationContext
        return ImageLoader.Builder(appContext)
            .okHttpClient(okHttpClient)
            .diskCache {
                DiskCache.Builder()
                    .directory(File(appContext.cacheDir, "image_cache"))
                    .maxSizeBytes(DISK_CACHE_BYTES)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}

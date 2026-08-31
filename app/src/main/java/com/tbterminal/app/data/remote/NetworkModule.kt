package com.tbterminal.app.data.remote

import android.content.Context
import com.tbterminal.app.BuildConfig
import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.data.session.SharedPreferencesTokenStore
import com.tbterminal.app.data.session.TokenStore
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class NetworkModule(
    context: Context,
    baseUrl: String = BuildConfig.BASE_URL
) {
    val tokenStore: TokenStore = SharedPreferencesTokenStore(context.applicationContext)
    val sessionManager: SessionManager = SessionManager(tokenStore)

    private val json: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }

    private val normalizedBaseUrl = baseUrl.asRetrofitBaseUrl()

    private val refreshOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().applyStandardTimeouts().build()
    }

    private val refreshApi: TokenRefreshApi by lazy {
        Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(refreshOkHttpClient)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()
            .create(TokenRefreshApi::class.java)
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .applyStandardTimeouts()
            .addInterceptor(AuthInterceptor(tokenStore))
            .authenticator(RefreshTokenAuthenticator(tokenStore, sessionManager, refreshApi))
            .build()
    }

    private val healthOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(HEALTH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(HEALTH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(HEALTH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()
    }

    private val backupRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(
                okHttpClient.newBuilder()
                    .readTimeout(BACKUP_READ_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                    .writeTimeout(BACKUP_WRITE_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                    .callTimeout(BACKUP_CALL_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                    .retryOnConnectionFailure(false)
                    .build()
            )
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()
    }

    private val healthRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(healthOkHttpClient)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()
    }

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    init {
        sessionManager.configureRemoteLogout { accessToken ->
            val request = Request.Builder()
                .url("${normalizedBaseUrl}api/auth/logout")
                .header("Authorization", "Bearer $accessToken")
                .post(ByteArray(0).toRequestBody(null))
                .build()
            refreshOkHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) = Unit
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.close()
                }
            })
        }
    }

    val checkoutApi: CheckoutApi by lazy {
        retrofit.create(CheckoutApi::class.java)
    }

    val salesApi: SalesApi by lazy {
        retrofit.create(SalesApi::class.java)
    }

    val userApi: UserApi by lazy {
        retrofit.create(UserApi::class.java)
    }

    val securityApi: SecurityApi by lazy {
        retrofit.create(SecurityApi::class.java)
    }

    val inventoryApi: InventoryApi by lazy {
        retrofit.create(InventoryApi::class.java)
    }

    val purchasingApi: PurchasingApi by lazy {
        retrofit.create(PurchasingApi::class.java)
    }

    val receivableApi: ReceivableApi by lazy {
        retrofit.create(ReceivableApi::class.java)
    }

    val analyticsApi: AnalyticsApi by lazy {
        retrofit.create(AnalyticsApi::class.java)
    }

    val systemApi: com.tbterminal.app.data.network.SystemApi by lazy {
        retrofit.create(com.tbterminal.app.data.network.SystemApi::class.java)
    }

    val healthApi: HealthApi by lazy {
        healthRetrofit.create(HealthApi::class.java)
    }

    val databaseBackupApi: DatabaseBackupApi by lazy {
        backupRetrofit.create(DatabaseBackupApi::class.java)
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
        const val HEALTH_TIMEOUT_SECONDS = 2L
        const val CONNECT_TIMEOUT_SECONDS = 10L
        const val READ_TIMEOUT_SECONDS = 30L
        const val WRITE_TIMEOUT_SECONDS = 30L
        const val CALL_TIMEOUT_SECONDS = 45L
        const val BACKUP_READ_TIMEOUT_MINUTES = 10L
        const val BACKUP_WRITE_TIMEOUT_MINUTES = 10L
        const val BACKUP_CALL_TIMEOUT_MINUTES = 15L
    }

    private fun OkHttpClient.Builder.applyStandardTimeouts(): OkHttpClient.Builder {
        return connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }
}

private fun String.asRetrofitBaseUrl(): String {
    val normalized = if (endsWith("/")) this else "$this/"
    require(normalized.startsWith("http://") || normalized.startsWith("https://")) {
        "BASE_URL harus menggunakan skema http atau https"
    }
    return normalized
}

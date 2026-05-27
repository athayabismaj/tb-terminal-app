package com.tbterminal.app.data.remote

import android.content.Context
import com.tbterminal.app.BuildConfig
import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.data.session.SharedPreferencesTokenStore
import com.tbterminal.app.data.session.TokenStore
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class NetworkModule(
    context: Context,
    baseUrl: String = BuildConfig.BASE_URL
) {
    val tokenStore: TokenStore = SharedPreferencesTokenStore(context.applicationContext)
    val sessionManager: SessionManager = SessionManager(tokenStore)

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenStore))
        .addInterceptor(UnauthorizedInterceptor(sessionManager))
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl.asRetrofitBaseUrl())
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
        .build()

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
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

    private companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}

private fun String.asRetrofitBaseUrl(): String {
    return if (endsWith("/")) this else "$this/"
}

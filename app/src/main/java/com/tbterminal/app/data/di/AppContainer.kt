package com.tbterminal.app.data.di

import android.content.Context
import com.tbterminal.app.data.numbering.DailyDocumentNumberGenerator
import com.tbterminal.app.data.numbering.DocumentNumberGenerator
import com.tbterminal.app.data.remote.NetworkModule
import com.tbterminal.app.data.repository.AuthRepository
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.CheckoutRepository
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.RemoteAuthRepository
import com.tbterminal.app.data.repository.RemoteCashReconciliationRepository
import com.tbterminal.app.data.repository.RemoteCheckoutRepository
import com.tbterminal.app.data.repository.RemoteCustomerRepository
import com.tbterminal.app.data.repository.RemoteInventoryRepository
import com.tbterminal.app.data.repository.RemotePurchasingRepository
import com.tbterminal.app.data.repository.RemoteReceivableRepository
import com.tbterminal.app.data.repository.RemoteSecurityLogRepository
import com.tbterminal.app.data.repository.RemoteUserRepository
import com.tbterminal.app.data.repository.SecurityLogRepository
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.data.repository.ReceivableRepository
import com.tbterminal.app.data.repository.UserRepository
import com.tbterminal.app.data.repository.AnalyticsRepository
import com.tbterminal.app.data.repository.RemoteAnalyticsRepository
import com.tbterminal.app.data.session.SessionManager

interface AppContainer {
    val sessionManager: SessionManager
    val authRepository: AuthRepository
    val cashReconciliationRepository: CashReconciliationRepository
    val checkoutRepository: CheckoutRepository
    val userRepository: UserRepository
    val securityLogRepository: SecurityLogRepository
    val inventoryRepository: InventoryRepository
    val purchasingRepository: PurchasingRepository
    val customerRepository: CustomerRepository
    val receivableRepository: ReceivableRepository
    val documentNumberGenerator: DocumentNumberGenerator
    val analyticsRepository: AnalyticsRepository
    val systemRepository: com.tbterminal.app.data.repository.SystemRepository
}

class DefaultAppContainer(
    context: Context
) : AppContainer {
    private val networkModule = NetworkModule(context.applicationContext)

    override val sessionManager: SessionManager = networkModule.sessionManager

    override val documentNumberGenerator: DocumentNumberGenerator by lazy {
        DailyDocumentNumberGenerator()
    }

    override val authRepository: AuthRepository by lazy {
        RemoteAuthRepository(
            authApi = networkModule.authApi,
            sessionManager = networkModule.sessionManager
        )
    }

    override val checkoutRepository: CheckoutRepository by lazy {
        RemoteCheckoutRepository(networkModule.checkoutApi)
    }

    override val cashReconciliationRepository: CashReconciliationRepository by lazy {
        RemoteCashReconciliationRepository(networkModule.salesApi)
    }

    override val userRepository: UserRepository by lazy {
        RemoteUserRepository(networkModule.userApi)
    }

    override val securityLogRepository: SecurityLogRepository by lazy {
        RemoteSecurityLogRepository(networkModule.securityApi)
    }

    override val inventoryRepository: InventoryRepository by lazy {
        RemoteInventoryRepository(networkModule.inventoryApi)
    }

    override val purchasingRepository: PurchasingRepository by lazy {
        RemotePurchasingRepository(networkModule.purchasingApi)
    }

    override val customerRepository: CustomerRepository by lazy {
        RemoteCustomerRepository(networkModule.receivableApi)
    }

    override val receivableRepository: ReceivableRepository by lazy {
        RemoteReceivableRepository(networkModule.receivableApi)
    }

    override val analyticsRepository: AnalyticsRepository by lazy {
        RemoteAnalyticsRepository(networkModule.analyticsApi)
    }

    override val systemRepository: com.tbterminal.app.data.repository.SystemRepository by lazy {
        com.tbterminal.app.data.repository.SystemRepository(networkModule.systemApi)
    }
}

package com.tbterminal.app.data.di

import android.content.Context
import com.tbterminal.app.data.local.cashexpense.LocalCashExpenseService
import com.tbterminal.app.data.local.cashsession.LocalCashSessionService
import com.tbterminal.app.data.local.checkout.LocalCheckoutLookupService
import com.tbterminal.app.data.local.checkout.LocalCheckoutService
import com.tbterminal.app.data.local.database.CashSessionLocalDataSource
import com.tbterminal.app.data.local.database.CategoryLocalDataSource
import com.tbterminal.app.data.local.database.CustomerLocalDataSource
import com.tbterminal.app.data.local.database.LocalAppSettingsDataSource
import com.tbterminal.app.data.local.database.ProductLocalDataSource
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.database.UnitLocalDataSource
import com.tbterminal.app.data.local.id.DeviceIdProvider
import com.tbterminal.app.data.numbering.DailyDocumentNumberGenerator
import com.tbterminal.app.data.numbering.DocumentNumberGenerator
import com.tbterminal.app.data.remote.NetworkModule
import com.tbterminal.app.data.repository.AuthRepository
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.CheckoutRepository
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.LocalBackupRepository
import com.tbterminal.app.data.repository.OfflineDashboardRepository
import com.tbterminal.app.data.repository.OfflineReportRepository
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
import com.tbterminal.app.data.repository.ServerBackupRepository
import com.tbterminal.app.data.repository.SyncMonitoringRepository
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.data.repository.ReceivableRepository
import com.tbterminal.app.data.repository.UserRepository
import com.tbterminal.app.data.repository.AnalyticsRepository
import com.tbterminal.app.data.repository.RemoteAnalyticsRepository
import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.data.sync.BackendHealthMonitor
import com.tbterminal.app.data.sync.NetworkMonitor
import com.tbterminal.app.data.sync.OfflineCashSessionSyncService
import com.tbterminal.app.data.sync.OfflineCashExpenseSyncService
import com.tbterminal.app.data.sync.OfflineCheckoutSyncService
import com.tbterminal.app.data.sync.OfflineSyncScheduler
import com.tbterminal.app.data.sync.OfflineStatusRepository
import com.tbterminal.app.data.sync.SyncManager

interface AppContainer {
    val sessionManager: SessionManager
    val localDatabase: TbTerminalDatabase
    val localAppSettingsDataSource: LocalAppSettingsDataSource
    val productLocalDataSource: ProductLocalDataSource
    val categoryLocalDataSource: CategoryLocalDataSource
    val unitLocalDataSource: UnitLocalDataSource
    val customerLocalDataSource: CustomerLocalDataSource
    val cashSessionLocalDataSource: CashSessionLocalDataSource
    val localCashExpenseService: LocalCashExpenseService
    val localCashSessionService: LocalCashSessionService
    val localCheckoutLookupService: LocalCheckoutLookupService
    val localCheckoutService: LocalCheckoutService
    val offlineCashSessionSyncService: OfflineCashSessionSyncService
    val offlineCashExpenseSyncService: OfflineCashExpenseSyncService
    val offlineCheckoutSyncService: OfflineCheckoutSyncService
    val networkMonitor: NetworkMonitor
    val backendHealthMonitor: BackendHealthMonitor
    val offlineStatusRepository: OfflineStatusRepository
    val syncMonitoringRepository: SyncMonitoringRepository
    val offlineDashboardRepository: OfflineDashboardRepository
    val offlineReportRepository: OfflineReportRepository
    val localBackupRepository: LocalBackupRepository
    val serverBackupRepository: ServerBackupRepository
    val syncManager: SyncManager
    val offlineSyncScheduler: OfflineSyncScheduler
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
    private val appContext = context.applicationContext
    private val networkModule = NetworkModule(context.applicationContext)

    override val sessionManager: SessionManager = networkModule.sessionManager

    override val localDatabase: TbTerminalDatabase by lazy {
        TbTerminalDatabase.getInstance(appContext)
    }

    override val localAppSettingsDataSource: LocalAppSettingsDataSource by lazy {
        LocalAppSettingsDataSource(localDatabase.appSettingDao())
    }

    override val productLocalDataSource: ProductLocalDataSource by lazy {
        ProductLocalDataSource(localDatabase.productDao())
    }

    override val categoryLocalDataSource: CategoryLocalDataSource by lazy {
        CategoryLocalDataSource(localDatabase.categoryDao())
    }

    override val unitLocalDataSource: UnitLocalDataSource by lazy {
        UnitLocalDataSource(localDatabase.unitDao())
    }

    override val customerLocalDataSource: CustomerLocalDataSource by lazy {
        CustomerLocalDataSource(localDatabase.customerDao())
    }

    override val cashSessionLocalDataSource: CashSessionLocalDataSource by lazy {
        CashSessionLocalDataSource(localDatabase.cashSessionDao())
    }

    override val localCashExpenseService: LocalCashExpenseService by lazy {
        LocalCashExpenseService(
            database = localDatabase,
            deviceId = deviceIdProvider.deviceId
        )
    }

    override val localCashSessionService: LocalCashSessionService by lazy {
        LocalCashSessionService(
            database = localDatabase,
            deviceId = deviceIdProvider.deviceId
        )
    }

    override val localCheckoutLookupService: LocalCheckoutLookupService by lazy {
        LocalCheckoutLookupService(
            productLocalDataSource = productLocalDataSource,
            customerLocalDataSource = customerLocalDataSource,
            cashSessionDao = localDatabase.cashSessionDao()
        )
    }

    private val deviceIdProvider: DeviceIdProvider by lazy {
        DeviceIdProvider(appContext)
    }

    override val localCheckoutService: LocalCheckoutService by lazy {
        LocalCheckoutService(
            database = localDatabase,
            deviceId = deviceIdProvider.deviceId
        )
    }

    override val offlineCashSessionSyncService: OfflineCashSessionSyncService by lazy {
        OfflineCashSessionSyncService(
            database = localDatabase,
            salesApi = networkModule.salesApi,
            deviceId = deviceIdProvider.deviceId
        )
    }

    override val offlineCashExpenseSyncService: OfflineCashExpenseSyncService by lazy {
        OfflineCashExpenseSyncService(
            database = localDatabase,
            salesApi = networkModule.salesApi,
            deviceId = deviceIdProvider.deviceId,
            cashSessionSyncService = offlineCashSessionSyncService
        )
    }

    override val offlineCheckoutSyncService: OfflineCheckoutSyncService by lazy {
        OfflineCheckoutSyncService(
            database = localDatabase,
            salesApi = networkModule.salesApi,
            deviceId = deviceIdProvider.deviceId,
            cashSessionSyncService = offlineCashSessionSyncService,
            cashExpenseSyncService = offlineCashExpenseSyncService
        )
    }

    override val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(appContext)
    }

    override val backendHealthMonitor: BackendHealthMonitor by lazy {
        BackendHealthMonitor(
            healthApi = networkModule.healthApi,
            networkMonitor = networkMonitor
        )
    }

    override val offlineStatusRepository: OfflineStatusRepository by lazy {
        OfflineStatusRepository(
            networkMonitor = networkMonitor,
            backendHealthMonitor = backendHealthMonitor,
            localAppSettingsDataSource = localAppSettingsDataSource,
            syncQueueDao = localDatabase.syncQueueDao()
        )
    }

    override val syncMonitoringRepository: SyncMonitoringRepository by lazy {
        SyncMonitoringRepository(
            syncQueueDao = localDatabase.syncQueueDao(),
            offlineCashSessionSyncService = offlineCashSessionSyncService,
            offlineCheckoutSyncService = offlineCheckoutSyncService,
            offlineCashExpenseSyncService = offlineCashExpenseSyncService
        )
    }

    override val offlineDashboardRepository: OfflineDashboardRepository by lazy {
        OfflineDashboardRepository(
            offlineDashboardDao = localDatabase.offlineDashboardDao()
        )
    }

    override val offlineReportRepository: OfflineReportRepository by lazy {
        OfflineReportRepository(
            salesReportDao = localDatabase.offlineSalesReportDao(),
            cashReportDao = localDatabase.offlineCashReportDao(),
            receivableReportDao = localDatabase.offlineReceivableReportDao(),
            expenseReportDao = localDatabase.offlineExpenseReportDao()
        )
    }

    override val localBackupRepository: LocalBackupRepository by lazy {
        LocalBackupRepository(
            context = appContext,
            database = localDatabase,
            deviceIdProvider = deviceIdProvider
        )
    }

    override val serverBackupRepository: ServerBackupRepository by lazy {
        ServerBackupRepository(appContext.contentResolver, networkModule.databaseBackupApi)
    }

    override val syncManager: SyncManager by lazy {
        SyncManager(
            database = localDatabase,
            networkMonitor = networkMonitor,
            backendHealthMonitor = backendHealthMonitor,
            localAppSettingsDataSource = localAppSettingsDataSource,
            sessionManager = sessionManager,
            offlineCashSessionSyncService = offlineCashSessionSyncService,
            offlineCheckoutSyncService = offlineCheckoutSyncService,
            offlineCashExpenseSyncService = offlineCashExpenseSyncService
        )
    }

    override val offlineSyncScheduler: OfflineSyncScheduler by lazy {
        OfflineSyncScheduler(
            context = appContext,
            localAppSettingsDataSource = localAppSettingsDataSource,
            networkMonitor = networkMonitor
        )
    }

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
        RemoteCashReconciliationRepository(
            salesApi = networkModule.salesApi,
            backendHealthMonitor = backendHealthMonitor
        )
    }

    override val userRepository: UserRepository by lazy {
        RemoteUserRepository(networkModule.userApi)
    }

    override val securityLogRepository: SecurityLogRepository by lazy {
        RemoteSecurityLogRepository(networkModule.securityApi)
    }

    override val inventoryRepository: InventoryRepository by lazy {
        RemoteInventoryRepository(
            inventoryApi = networkModule.inventoryApi,
            productLocalDataSource = productLocalDataSource,
            categoryLocalDataSource = categoryLocalDataSource,
            unitLocalDataSource = unitLocalDataSource
        )
    }

    override val purchasingRepository: PurchasingRepository by lazy {
        RemotePurchasingRepository(networkModule.purchasingApi)
    }

    override val customerRepository: CustomerRepository by lazy {
        RemoteCustomerRepository(
            receivableApi = networkModule.receivableApi,
            customerLocalDataSource = customerLocalDataSource
        )
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

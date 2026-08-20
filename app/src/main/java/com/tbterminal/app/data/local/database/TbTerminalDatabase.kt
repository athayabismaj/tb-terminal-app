package com.tbterminal.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tbterminal.app.data.local.dao.AppSettingDao
import com.tbterminal.app.data.local.dao.CashExpenseDao
import com.tbterminal.app.data.local.dao.CashSessionDao
import com.tbterminal.app.data.local.dao.CategoryDao
import com.tbterminal.app.data.local.dao.CustomerDao
import com.tbterminal.app.data.local.dao.LocalAuditLogDao
import com.tbterminal.app.data.local.dao.OfflineDashboardDao
import com.tbterminal.app.data.local.dao.OfflineCashReportDao
import com.tbterminal.app.data.local.dao.OfflineExpenseReportDao
import com.tbterminal.app.data.local.dao.OfflineReceivableReportDao
import com.tbterminal.app.data.local.dao.OfflineSalesReportDao
import com.tbterminal.app.data.local.dao.PaymentDao
import com.tbterminal.app.data.local.dao.ProductDao
import com.tbterminal.app.data.local.dao.ReceivableDao
import com.tbterminal.app.data.local.dao.SyncQueueDao
import com.tbterminal.app.data.local.dao.TransactionDao
import com.tbterminal.app.data.local.dao.TransactionItemDao
import com.tbterminal.app.data.local.dao.UnitDao
import com.tbterminal.app.data.local.entity.AppSettingEntity
import com.tbterminal.app.data.local.entity.LocalAuditLogEntity
import com.tbterminal.app.data.local.entity.LocalCashExpenseEntity
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.entity.LocalCategoryEntity
import com.tbterminal.app.data.local.entity.LocalCustomerEntity
import com.tbterminal.app.data.local.entity.LocalPaymentEntity
import com.tbterminal.app.data.local.entity.LocalProductEntity
import com.tbterminal.app.data.local.entity.LocalReceivableEntity
import com.tbterminal.app.data.local.entity.LocalTransactionEntity
import com.tbterminal.app.data.local.entity.LocalTransactionItemEntity
import com.tbterminal.app.data.local.entity.LocalUnitEntity
import com.tbterminal.app.data.local.entity.SyncQueueEntity

@Database(
    entities = [
        LocalProductEntity::class,
        LocalCategoryEntity::class,
        LocalUnitEntity::class,
        LocalCustomerEntity::class,
        LocalTransactionEntity::class,
        LocalTransactionItemEntity::class,
        LocalPaymentEntity::class,
        LocalReceivableEntity::class,
        LocalCashSessionEntity::class,
        LocalCashExpenseEntity::class,
        LocalAuditLogEntity::class,
        SyncQueueEntity::class,
        AppSettingEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(LocalConverters::class)
abstract class TbTerminalDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun unitDao(): UnitDao
    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionItemDao(): TransactionItemDao
    abstract fun paymentDao(): PaymentDao
    abstract fun receivableDao(): ReceivableDao
    abstract fun cashSessionDao(): CashSessionDao
    abstract fun cashExpenseDao(): CashExpenseDao
    abstract fun localAuditLogDao(): LocalAuditLogDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun appSettingDao(): AppSettingDao
    abstract fun offlineDashboardDao(): OfflineDashboardDao
    abstract fun offlineSalesReportDao(): OfflineSalesReportDao
    abstract fun offlineCashReportDao(): OfflineCashReportDao
    abstract fun offlineReceivableReportDao(): OfflineReceivableReportDao
    abstract fun offlineExpenseReportDao(): OfflineExpenseReportDao

    companion object {
        const val DATABASE_NAME = "tb_terminal_local.db"

        @Volatile
        private var instance: TbTerminalDatabase? = null

        fun getInstance(context: Context): TbTerminalDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TbTerminalDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE local_products ADD COLUMN priceRetail REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE local_products ADD COLUMN priceContractor REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE local_products ADD COLUMN discount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE local_products ADD COLUMN photoFilename TEXT")
                db.execSQL("ALTER TABLE local_products ADD COLUMN remoteCreatedAt TEXT")
                db.execSQL("ALTER TABLE local_products ADD COLUMN remoteUpdatedAt TEXT")
                db.execSQL("ALTER TABLE local_categories ADD COLUMN remoteCreatedAt TEXT")
                db.execSQL("ALTER TABLE local_categories ADD COLUMN remoteUpdatedAt TEXT")
                db.execSQL("ALTER TABLE local_units ADD COLUMN remoteCreatedAt TEXT")
                db.execSQL("ALTER TABLE local_units ADD COLUMN remoteUpdatedAt TEXT")
                db.execSQL("ALTER TABLE local_customers ADD COLUMN isContractor INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE local_customers ADD COLUMN paymentTermDays INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE local_customers ADD COLUMN remoteCreatedAt TEXT")
                db.execSQL("ALTER TABLE local_customers ADD COLUMN remoteUpdatedAt TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createTransactionTables(db)
                createTransactionIndexes(db)
            }
        }

        private fun createTransactionTables(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS local_transactions (
                    localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    serverId TEXT,
                    clientGeneratedId TEXT NOT NULL,
                    deviceId TEXT,
                    transactionCode TEXT NOT NULL,
                    customerLocalId INTEGER,
                    customerServerId TEXT,
                    cashSessionLocalId INTEGER,
                    cashSessionServerId TEXT,
                    cashierUserId TEXT,
                    status TEXT NOT NULL,
                    subtotal REAL NOT NULL,
                    discount REAL NOT NULL,
                    total REAL NOT NULL,
                    paidAmount REAL NOT NULL,
                    remainingAmount REAL NOT NULL,
                    syncStatus TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    occurredAt INTEGER NOT NULL,
                    syncedAt INTEGER,
                    deletedAt INTEGER
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS local_transaction_items (
                    localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    transactionLocalId INTEGER NOT NULL,
                    transactionServerId TEXT,
                    productLocalId INTEGER,
                    productServerId TEXT,
                    productNameSnapshot TEXT NOT NULL,
                    skuSnapshot TEXT,
                    unitNameSnapshot TEXT,
                    quantity REAL NOT NULL,
                    priceAtTransaction REAL NOT NULL,
                    cogsAtTransaction REAL NOT NULL,
                    discount REAL NOT NULL,
                    subtotal REAL NOT NULL,
                    syncStatus TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncedAt INTEGER,
                    deletedAt INTEGER
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS local_payments (
                    localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    serverId TEXT,
                    clientGeneratedId TEXT NOT NULL,
                    deviceId TEXT,
                    transactionLocalId INTEGER NOT NULL,
                    transactionServerId TEXT,
                    method TEXT NOT NULL,
                    amount REAL NOT NULL,
                    paidAt INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncedAt INTEGER,
                    deletedAt INTEGER
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS local_receivables (
                    localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    serverId TEXT,
                    clientGeneratedId TEXT NOT NULL,
                    deviceId TEXT,
                    transactionLocalId INTEGER NOT NULL,
                    transactionServerId TEXT,
                    customerLocalId INTEGER,
                    customerServerId TEXT,
                    totalAmount REAL NOT NULL,
                    paidAmount REAL NOT NULL,
                    remainingAmount REAL NOT NULL,
                    status TEXT NOT NULL,
                    dueDate INTEGER,
                    syncStatus TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    occurredAt INTEGER NOT NULL,
                    syncedAt INTEGER,
                    deletedAt INTEGER
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS local_cash_sessions (
                    localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    serverId TEXT,
                    clientGeneratedId TEXT NOT NULL,
                    deviceId TEXT,
                    cashierUserId TEXT NOT NULL,
                    status TEXT NOT NULL,
                    openedAt INTEGER NOT NULL,
                    closedAt INTEGER,
                    startingCash REAL NOT NULL,
                    expectedCash REAL,
                    actualCash REAL,
                    difference REAL,
                    openingNote TEXT,
                    closingNote TEXT,
                    syncStatus TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncedAt INTEGER,
                    deletedAt INTEGER
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS local_cash_expenses (
                    localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    serverId TEXT,
                    clientGeneratedId TEXT NOT NULL,
                    deviceId TEXT,
                    cashSessionLocalId INTEGER,
                    cashSessionServerId TEXT,
                    category TEXT,
                    description TEXT,
                    amount REAL NOT NULL,
                    occurredAt INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncedAt INTEGER,
                    deletedAt INTEGER
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS local_audit_logs (
                    localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    serverId TEXT,
                    clientGeneratedId TEXT NOT NULL,
                    deviceId TEXT,
                    actorUserId TEXT,
                    action TEXT NOT NULL,
                    tableName TEXT NOT NULL,
                    recordId TEXT,
                    description TEXT,
                    metadataJson TEXT,
                    occurredAt INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncedAt INTEGER,
                    deletedAt INTEGER
                )
                """.trimIndent()
            )
        }

        private fun createTransactionIndexes(db: SupportSQLiteDatabase) {
            createIndex(db, "local_transactions", "serverId")
            createIndex(db, "local_transactions", "clientGeneratedId", unique = true)
            createIndex(db, "local_transactions", "transactionCode")
            createIndex(db, "local_transactions", "customerLocalId")
            createIndex(db, "local_transactions", "customerServerId")
            createIndex(db, "local_transactions", "cashSessionLocalId")
            createIndex(db, "local_transactions", "cashSessionServerId")
            createIndex(db, "local_transactions", "status")
            createIndex(db, "local_transactions", "occurredAt")
            createIndex(db, "local_transactions", "syncStatus")

            createIndex(db, "local_transaction_items", "transactionLocalId")
            createIndex(db, "local_transaction_items", "productLocalId")
            createIndex(db, "local_transaction_items", "productServerId")
            createIndex(db, "local_transaction_items", "syncStatus")

            createIndex(db, "local_payments", "serverId")
            createIndex(db, "local_payments", "clientGeneratedId", unique = true)
            createIndex(db, "local_payments", "transactionLocalId")
            createIndex(db, "local_payments", "transactionServerId")
            createIndex(db, "local_payments", "method")
            createIndex(db, "local_payments", "paidAt")
            createIndex(db, "local_payments", "syncStatus")

            createIndex(db, "local_receivables", "serverId")
            createIndex(db, "local_receivables", "clientGeneratedId", unique = true)
            createIndex(db, "local_receivables", "transactionLocalId")
            createIndex(db, "local_receivables", "transactionServerId")
            createIndex(db, "local_receivables", "customerLocalId")
            createIndex(db, "local_receivables", "customerServerId")
            createIndex(db, "local_receivables", "status")
            createIndex(db, "local_receivables", "dueDate")
            createIndex(db, "local_receivables", "syncStatus")

            createIndex(db, "local_cash_sessions", "serverId")
            createIndex(db, "local_cash_sessions", "clientGeneratedId", unique = true)
            createIndex(db, "local_cash_sessions", "cashierUserId")
            createIndex(db, "local_cash_sessions", "status")
            createIndex(db, "local_cash_sessions", "openedAt")
            createIndex(db, "local_cash_sessions", "closedAt")
            createIndex(db, "local_cash_sessions", "syncStatus")

            createIndex(db, "local_cash_expenses", "serverId")
            createIndex(db, "local_cash_expenses", "clientGeneratedId", unique = true)
            createIndex(db, "local_cash_expenses", "cashSessionLocalId")
            createIndex(db, "local_cash_expenses", "cashSessionServerId")
            createIndex(db, "local_cash_expenses", "category")
            createIndex(db, "local_cash_expenses", "occurredAt")
            createIndex(db, "local_cash_expenses", "syncStatus")

            createIndex(db, "local_audit_logs", "serverId")
            createIndex(db, "local_audit_logs", "clientGeneratedId", unique = true)
            createIndex(db, "local_audit_logs", "actorUserId")
            createIndex(db, "local_audit_logs", "action")
            createIndex(db, "local_audit_logs", "tableName")
            createIndex(db, "local_audit_logs", "recordId")
            createIndex(db, "local_audit_logs", "occurredAt")
            createIndex(db, "local_audit_logs", "syncStatus")
        }

        private fun createIndex(
            db: SupportSQLiteDatabase,
            tableName: String,
            columnName: String,
            unique: Boolean = false
        ) {
            val uniqueSql = if (unique) "UNIQUE " else ""
            db.execSQL(
                "CREATE ${uniqueSql}INDEX IF NOT EXISTS index_${tableName}_${columnName} " +
                    "ON $tableName ($columnName)"
            )
        }
    }
}

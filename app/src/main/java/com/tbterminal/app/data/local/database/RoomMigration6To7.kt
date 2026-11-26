package com.tbterminal.app.data.local.database

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tbterminal.app.data.local.model.ScaledDecimal2
import java.math.BigDecimal
import java.math.RoundingMode

internal object RoomMigration6To7 {
    private data class IndexSpec(
        val columnName: String,
        val unique: Boolean = false
    )

    private data class TableMigrationSpec(
        val tableName: String,
        val columns: List<String>,
        val scaledColumns: Set<String>,
        val createTempTableSql: String,
        val indices: List<IndexSpec>
    )

    fun migrate(db: SupportSQLiteDatabase) {
        TABLE_SPECS.forEach { spec ->
            migrateTable(db, spec)
        }
    }

    private fun migrateTable(db: SupportSQLiteDatabase, spec: TableMigrationSpec) {
        val tempTable = "${spec.tableName}_v7"
        db.execSQL("DROP TABLE IF EXISTS $tempTable")
        db.execSQL(spec.createTempTableSql)

        val columnCsv = spec.columns.joinToString(", ")
        val placeholders = spec.columns.joinToString(", ") { "?" }
        val insertStmt = db.compileStatement("INSERT INTO $tempTable ($columnCsv) VALUES ($placeholders)")

        try {
            db.query("SELECT $columnCsv FROM ${spec.tableName}").use { cursor ->
                while (cursor.moveToNext()) {
                    insertStmt.clearBindings()
                    spec.columns.forEachIndexed { index, columnName ->
                        val bindIndex = index + 1
                        val isScaled = columnName in spec.scaledColumns
                        when (cursor.getType(index)) {
                            Cursor.FIELD_TYPE_NULL -> insertStmt.bindNull(bindIndex)
                            Cursor.FIELD_TYPE_INTEGER -> {
                                val rawLong = cursor.getLong(index)
                                val value = if (isScaled) Math.multiplyExact(rawLong, 100L) else rawLong
                                insertStmt.bindLong(bindIndex, value)
                            }
                            Cursor.FIELD_TYPE_FLOAT -> {
                                val rawDouble = cursor.getDouble(index)
                                if (isScaled) {
                                    insertStmt.bindLong(
                                        bindIndex,
                                        ScaledDecimal2.fromLegacyRealHalfUp(rawDouble).minorUnits
                                    )
                                } else {
                                    insertStmt.bindDouble(bindIndex, rawDouble)
                                }
                            }
                            Cursor.FIELD_TYPE_STRING -> {
                                val rawText = cursor.getString(index)
                                if (isScaled) {
                                    val parsed = rawText.trim().toBigDecimalOrNull()
                                        ?: error("Nilai kolom '$columnName' pada ${spec.tableName} bukan angka valid: '$rawText'")
                                    insertStmt.bindLong(
                                        bindIndex,
                                        ScaledDecimal2.ofRounded(parsed, RoundingMode.HALF_UP).minorUnits
                                    )
                                } else {
                                    insertStmt.bindString(bindIndex, rawText)
                                }
                            }
                            Cursor.FIELD_TYPE_BLOB -> insertStmt.bindBlob(bindIndex, cursor.getBlob(index))
                        }
                    }
                    insertStmt.executeInsert()
                }
            }
        } finally {
            insertStmt.close()
        }

        db.execSQL("DROP TABLE ${spec.tableName}")
        db.execSQL("ALTER TABLE $tempTable RENAME TO ${spec.tableName}")
        spec.indices.forEach { index ->
            val uniqueSql = if (index.unique) "UNIQUE " else ""
            db.execSQL(
                "CREATE ${uniqueSql}INDEX IF NOT EXISTS index_${spec.tableName}_${index.columnName} " +
                    "ON ${spec.tableName} (${index.columnName})"
            )
        }
    }

    private val TABLE_SPECS = listOf(
        TableMigrationSpec(
            tableName = "local_transactions",
            columns = listOf(
                "localId", "serverId", "clientGeneratedId", "deviceId", "transactionCode",
                "customerLocalId", "customerServerId", "cashSessionLocalId", "cashSessionServerId",
                "cashierUserId", "status", "subtotal", "discount", "taxEnabled", "taxMode",
                "taxRate", "taxLabel", "taxableBase", "taxAmount", "roundingAmount", "total",
                "paidAmount", "remainingAmount", "syncStatus", "createdAt", "updatedAt",
                "occurredAt", "syncedAt", "deletedAt"
            ),
            scaledColumns = setOf(
                "subtotal", "discount", "taxRate", "taxableBase", "taxAmount",
                "roundingAmount", "total", "paidAmount", "remainingAmount"
            ),
            createTempTableSql = """
                CREATE TABLE local_transactions_v7 (
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
                    subtotal INTEGER NOT NULL,
                    discount INTEGER NOT NULL,
                    taxEnabled INTEGER NOT NULL,
                    taxMode TEXT,
                    taxRate INTEGER NOT NULL,
                    taxLabel TEXT,
                    taxableBase INTEGER NOT NULL,
                    taxAmount INTEGER NOT NULL,
                    roundingAmount INTEGER NOT NULL,
                    total INTEGER NOT NULL,
                    paidAmount INTEGER NOT NULL,
                    remainingAmount INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    occurredAt INTEGER NOT NULL,
                    syncedAt INTEGER,
                    deletedAt INTEGER
                )
            """.trimIndent(),
            indices = listOf(
                IndexSpec("serverId"),
                IndexSpec("clientGeneratedId", unique = true),
                IndexSpec("transactionCode"),
                IndexSpec("customerLocalId"),
                IndexSpec("customerServerId"),
                IndexSpec("cashSessionLocalId"),
                IndexSpec("cashSessionServerId"),
                IndexSpec("status"),
                IndexSpec("occurredAt"),
                IndexSpec("syncStatus")
            )
        ),
        TableMigrationSpec(
            tableName = "local_transaction_items",
            columns = listOf(
                "localId", "transactionLocalId", "transactionServerId", "productLocalId",
                "productServerId", "productNameSnapshot", "skuSnapshot", "unitNameSnapshot",
                "unitServerId", "quantity", "baseQuantity", "conversionFactor",
                "priceAtTransaction", "cogsAtTransaction", "discount", "subtotal", "note",
                "syncStatus", "createdAt", "updatedAt", "syncedAt", "deletedAt"
            ),
            scaledColumns = setOf(
                "quantity", "priceAtTransaction", "cogsAtTransaction", "discount", "subtotal"
            ),
            createTempTableSql = """
                CREATE TABLE local_transaction_items_v7 (
                    localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    transactionLocalId INTEGER NOT NULL,
                    transactionServerId TEXT,
                    productLocalId INTEGER,
                    productServerId TEXT,
                    productNameSnapshot TEXT NOT NULL,
                    skuSnapshot TEXT,
                    unitNameSnapshot TEXT,
                    unitServerId TEXT,
                    quantity INTEGER NOT NULL,
                    baseQuantity TEXT,
                    conversionFactor TEXT,
                    priceAtTransaction INTEGER NOT NULL,
                    cogsAtTransaction INTEGER NOT NULL,
                    discount INTEGER NOT NULL,
                    subtotal INTEGER NOT NULL,
                    note TEXT,
                    syncStatus TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncedAt INTEGER,
                    deletedAt INTEGER
                )
            """.trimIndent(),
            indices = listOf(
                IndexSpec("transactionLocalId"),
                IndexSpec("productLocalId"),
                IndexSpec("productServerId"),
                IndexSpec("syncStatus")
            )
        ),
        TableMigrationSpec(
            tableName = "local_payments",
            columns = listOf(
                "localId", "serverId", "clientGeneratedId", "deviceId", "transactionLocalId",
                "transactionServerId", "method", "amount", "reference", "paidAt",
                "syncStatus", "createdAt", "updatedAt", "syncedAt", "deletedAt"
            ),
            scaledColumns = setOf("amount"),
            createTempTableSql = """
                CREATE TABLE local_payments_v7 (
                    localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    serverId TEXT,
                    clientGeneratedId TEXT NOT NULL,
                    deviceId TEXT,
                    transactionLocalId INTEGER NOT NULL,
                    transactionServerId TEXT,
                    method TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    reference TEXT,
                    paidAt INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncedAt INTEGER,
                    deletedAt INTEGER
                )
            """.trimIndent(),
            indices = listOf(
                IndexSpec("serverId"),
                IndexSpec("clientGeneratedId", unique = true),
                IndexSpec("transactionLocalId"),
                IndexSpec("transactionServerId"),
                IndexSpec("method"),
                IndexSpec("paidAt"),
                IndexSpec("syncStatus")
            )
        ),
        TableMigrationSpec(
            tableName = "local_receivables",
            columns = listOf(
                "localId", "serverId", "clientGeneratedId", "deviceId", "transactionLocalId",
                "transactionServerId", "customerLocalId", "customerServerId", "totalAmount",
                "paidAmount", "remainingAmount", "status", "dueDate", "syncStatus",
                "createdAt", "updatedAt", "occurredAt", "syncedAt", "deletedAt"
            ),
            scaledColumns = setOf("totalAmount", "paidAmount", "remainingAmount"),
            createTempTableSql = """
                CREATE TABLE local_receivables_v7 (
                    localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    serverId TEXT,
                    clientGeneratedId TEXT NOT NULL,
                    deviceId TEXT,
                    transactionLocalId INTEGER NOT NULL,
                    transactionServerId TEXT,
                    customerLocalId INTEGER,
                    customerServerId TEXT,
                    totalAmount INTEGER NOT NULL,
                    paidAmount INTEGER NOT NULL,
                    remainingAmount INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    dueDate INTEGER,
                    syncStatus TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    occurredAt INTEGER NOT NULL,
                    syncedAt INTEGER,
                    deletedAt INTEGER
                )
            """.trimIndent(),
            indices = listOf(
                IndexSpec("serverId"),
                IndexSpec("clientGeneratedId", unique = true),
                IndexSpec("transactionLocalId"),
                IndexSpec("transactionServerId"),
                IndexSpec("customerLocalId"),
                IndexSpec("customerServerId"),
                IndexSpec("status"),
                IndexSpec("dueDate"),
                IndexSpec("syncStatus")
            )
        ),
        TableMigrationSpec(
            tableName = "local_cash_sessions",
            columns = listOf(
                "localId", "serverId", "clientGeneratedId", "deviceId", "cashierUserId",
                "status", "openedAt", "closedAt", "startingCash", "expectedCash",
                "actualCash", "difference", "openingNote", "closingNote", "syncStatus",
                "createdAt", "updatedAt", "syncedAt", "deletedAt"
            ),
            scaledColumns = setOf("startingCash", "expectedCash", "actualCash", "difference"),
            createTempTableSql = """
                CREATE TABLE local_cash_sessions_v7 (
                    localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    serverId TEXT,
                    clientGeneratedId TEXT NOT NULL,
                    deviceId TEXT,
                    cashierUserId TEXT NOT NULL,
                    status TEXT NOT NULL,
                    openedAt INTEGER NOT NULL,
                    closedAt INTEGER,
                    startingCash INTEGER NOT NULL,
                    expectedCash INTEGER,
                    actualCash INTEGER,
                    difference INTEGER,
                    openingNote TEXT,
                    closingNote TEXT,
                    syncStatus TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncedAt INTEGER,
                    deletedAt INTEGER
                )
            """.trimIndent(),
            indices = listOf(
                IndexSpec("serverId"),
                IndexSpec("clientGeneratedId", unique = true),
                IndexSpec("cashierUserId"),
                IndexSpec("status"),
                IndexSpec("openedAt"),
                IndexSpec("closedAt"),
                IndexSpec("syncStatus")
            )
        ),
        TableMigrationSpec(
            tableName = "local_cash_expenses",
            columns = listOf(
                "localId", "serverId", "clientGeneratedId", "deviceId", "cashSessionLocalId",
                "cashSessionServerId", "category", "description", "amount", "occurredAt",
                "syncStatus", "createdAt", "updatedAt", "syncedAt", "deletedAt"
            ),
            scaledColumns = setOf("amount"),
            createTempTableSql = """
                CREATE TABLE local_cash_expenses_v7 (
                    localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    serverId TEXT,
                    clientGeneratedId TEXT NOT NULL,
                    deviceId TEXT,
                    cashSessionLocalId INTEGER,
                    cashSessionServerId TEXT,
                    category TEXT,
                    description TEXT,
                    amount INTEGER NOT NULL,
                    occurredAt INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncedAt INTEGER,
                    deletedAt INTEGER
                )
            """.trimIndent(),
            indices = listOf(
                IndexSpec("serverId"),
                IndexSpec("clientGeneratedId", unique = true),
                IndexSpec("cashSessionLocalId"),
                IndexSpec("cashSessionServerId"),
                IndexSpec("category"),
                IndexSpec("occurredAt"),
                IndexSpec("syncStatus")
            )
        ),
        TableMigrationSpec(
            tableName = "local_products",
            columns = listOf(
                "localId", "serverId", "clientGeneratedId", "deviceId", "name", "sku",
                "barcode", "categoryLocalId", "categoryServerId", "unitLocalId", "unitServerId",
                "unitName", "secondaryUnitServerId", "secondaryUnitName", "secondaryUnitFactor",
                "priceSell", "priceBuy", "priceRetail", "priceContractor", "discount",
                "stock", "minimumStock", "photoFilename", "isActive", "syncStatus",
                "remoteCreatedAt", "remoteUpdatedAt", "createdAt", "updatedAt", "deletedAt"
            ),
            scaledColumns = setOf(
                "priceSell", "priceBuy", "priceRetail", "priceContractor",
                "discount", "stock", "minimumStock"
            ),
            createTempTableSql = """
                CREATE TABLE local_products_v7 (
                    localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    serverId TEXT,
                    clientGeneratedId TEXT NOT NULL,
                    deviceId TEXT,
                    name TEXT NOT NULL,
                    sku TEXT,
                    barcode TEXT,
                    categoryLocalId INTEGER,
                    categoryServerId TEXT,
                    unitLocalId INTEGER,
                    unitServerId TEXT,
                    unitName TEXT,
                    secondaryUnitServerId TEXT,
                    secondaryUnitName TEXT,
                    secondaryUnitFactor TEXT,
                    priceSell INTEGER NOT NULL,
                    priceBuy INTEGER NOT NULL,
                    priceRetail INTEGER NOT NULL,
                    priceContractor INTEGER NOT NULL,
                    discount INTEGER NOT NULL,
                    stock INTEGER NOT NULL,
                    minimumStock INTEGER NOT NULL,
                    photoFilename TEXT,
                    isActive INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL,
                    remoteCreatedAt TEXT,
                    remoteUpdatedAt TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    deletedAt INTEGER
                )
            """.trimIndent(),
            indices = listOf(
                IndexSpec("serverId"),
                IndexSpec("clientGeneratedId", unique = true),
                IndexSpec("categoryLocalId"),
                IndexSpec("unitLocalId")
            )
        ),
        TableMigrationSpec(
            tableName = "local_customers",
            columns = listOf(
                "localId", "serverId", "clientGeneratedId", "deviceId", "name", "phone",
                "address", "isContractor", "creditAllowed", "creditLimit", "paymentTermDays",
                "isActive", "syncStatus", "remoteCreatedAt", "remoteUpdatedAt",
                "createdAt", "updatedAt", "deletedAt"
            ),
            scaledColumns = setOf("creditLimit"),
            createTempTableSql = """
                CREATE TABLE local_customers_v7 (
                    localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    serverId TEXT,
                    clientGeneratedId TEXT NOT NULL,
                    deviceId TEXT,
                    name TEXT NOT NULL,
                    phone TEXT,
                    address TEXT,
                    isContractor INTEGER NOT NULL,
                    creditAllowed INTEGER NOT NULL,
                    creditLimit INTEGER NOT NULL,
                    paymentTermDays INTEGER NOT NULL,
                    isActive INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL,
                    remoteCreatedAt TEXT,
                    remoteUpdatedAt TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    deletedAt INTEGER
                )
            """.trimIndent(),
            indices = listOf(
                IndexSpec("serverId"),
                IndexSpec("clientGeneratedId", unique = true),
                IndexSpec("phone")
            )
        )
    )
}

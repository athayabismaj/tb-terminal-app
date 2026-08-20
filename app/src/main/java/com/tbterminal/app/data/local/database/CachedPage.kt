package com.tbterminal.app.data.local.database

data class CachedPage<T>(
    val data: List<T>,
    val total: Long,
    val page: Int,
    val limit: Int
) {
    val totalPages: Int
        get() = if (total <= 0L) 1 else ((total + limit - 1) / limit).toInt()
}

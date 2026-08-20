package com.tbterminal.app.data.local.database

import com.tbterminal.app.data.local.dao.CategoryDao
import com.tbterminal.app.data.local.entity.LocalCategoryEntity

class CategoryLocalDataSource(
    private val categoryDao: CategoryDao
) {
    suspend fun getCachedCategories(): List<LocalCategoryEntity> {
        return categoryDao.getCachedCategories()
    }

    suspend fun getCachedCategoryPage(
        page: Int,
        limit: Int,
        search: String?
    ): CachedPage<LocalCategoryEntity> {
        val safePage = page.coerceAtLeast(1)
        val safeLimit = limit.coerceAtLeast(1)
        val query = search?.trim()?.takeIf(String::isNotBlank)
        val total = categoryDao.countCachedCategories(query)
        val data = categoryDao.getCachedCategoryPage(
            query = query,
            limit = safeLimit,
            offset = (safePage - 1) * safeLimit
        )
        return CachedPage(data, total, safePage, safeLimit)
    }

    suspend fun cacheCategories(categories: List<LocalCategoryEntity>) {
        if (categories.isNotEmpty()) {
            categoryDao.upsertCategories(categories)
        }
    }

    suspend fun cacheCategory(category: LocalCategoryEntity) {
        categoryDao.upsert(category)
    }
}

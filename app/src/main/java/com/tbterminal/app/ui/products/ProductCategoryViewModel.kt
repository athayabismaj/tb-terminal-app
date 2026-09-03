package com.tbterminal.app.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.common.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductCategoryViewModel(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductCategoryUiState())
    val uiState: StateFlow<ProductCategoryUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        loadCategories()
    }

    fun loadCategories(page: Int = _uiState.value.page) {
        viewModelScope.launch {
            val state = _uiState.value
            val safePage = page.coerceAtLeast(1)
            _uiState.update { current ->
                current.copy(isLoading = true, page = safePage, message = null)
            }

            when (
                val result = inventoryRepository.getCategoryPage(
                    page = safePage,
                    limit = state.pageSize,
                    search = state.searchQuery.trim().takeIf(String::isNotBlank)
                )
            ) {
                is RepositoryResult.Success -> {
                    val pageData = result.data
                    _uiState.update { state ->
                        state.copy(
                            categories = pageData.data,
                            page = pageData.page,
                            totalPages = pageData.totalPages.coerceAtLeast(1),
                            totalCategories = pageData.total,
                            isLoading = false
                        )
                    }
                }
                is RepositoryResult.Error -> setMessage(result.message, loading = false)
                is RepositoryResult.Exception -> setMessage("Kategori gagal dimuat karena koneksi bermasalah.", loading = false)
            }
        }
    }

    fun refresh() = loadCategories(_uiState.value.page)

    fun onNameChanged(name: String) {
        _uiState.update { state -> state.copy(nameInput = name, message = null) }
    }

    fun onSearchChanged(query: String) {
        _uiState.update { state -> state.copy(searchQuery = query, page = 1, message = null) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            loadCategories(page = 1)
        }
    }

    fun nextPage() {
        val state = _uiState.value
        if (state.page < state.totalPages && !state.isLoading) {
            loadCategories(page = state.page + 1)
        }
    }

    fun previousPage() {
        val state = _uiState.value
        if (state.page > 1 && !state.isLoading) {
            loadCategories(page = state.page - 1)
        }
    }

    fun edit(category: ProductCategory) {
        _uiState.update { state ->
            state.copy(editingCategory = category, nameInput = category.name, message = null)
        }
    }

    fun cancelEdit() {
        _uiState.update { state ->
            state.copy(editingCategory = null, nameInput = "", message = null)
        }
    }

    fun save() {
        val state = _uiState.value
        val name = state.nameInput.trim()
        if (name.isBlank()) return setMessage("Nama kategori tidak boleh kosong.")
        if (name.length > 100) return setMessage("Nama kategori maksimal 100 karakter.")

        viewModelScope.launch {
            _uiState.update { current -> current.copy(isSaving = true, message = null) }
            val result = state.editingCategory?.let { category ->
                inventoryRepository.updateCategory(category.id, name)
            } ?: inventoryRepository.createCategory(name)

            handleSavedResult(result, "Kategori berhasil disimpan.")
        }
    }

    fun delete(category: ProductCategory) {
        viewModelScope.launch {
            _uiState.update { state -> state.copy(isSaving = true, message = null) }
            val result = inventoryRepository.deleteCategory(category.id)
            handleDeletedResult(result)
        }
    }

    private fun handleSavedResult(
        result: RepositoryResult<ProductCategory>,
        successMessage: String
    ) {
        when (result) {
            is RepositoryResult.Success -> {
                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        editingCategory = null,
                        nameInput = "",
                        message = successMessage
                    )
                }
                loadCategories(page = _uiState.value.page)
            }
            is RepositoryResult.Error -> setMessage(result.message)
            is RepositoryResult.Exception -> setMessage("Kategori gagal disimpan karena koneksi bermasalah.")
        }
    }

    private fun handleDeletedResult(result: RepositoryResult<Unit>) {
        when (result) {
            is RepositoryResult.Success -> {
                _uiState.update { state ->
                    state.copy(isSaving = false, message = "Kategori berhasil dihapus.")
                }
                loadCategories(page = _uiState.value.page)
            }
            is RepositoryResult.Error -> setMessage(result.message)
            is RepositoryResult.Exception -> setMessage("Kategori gagal dihapus karena koneksi bermasalah.")
        }
    }

    private fun setMessage(
        message: String,
        loading: Boolean = _uiState.value.isLoading
    ) {
        _uiState.update { state ->
            state.copy(isSaving = false, isLoading = loading, message = message)
        }
    }

    companion object {
        fun factory(inventoryRepository: InventoryRepository): ViewModelProvider.Factory {
            return viewModelFactory {
                ProductCategoryViewModel(inventoryRepository = inventoryRepository)
            }
        }
    }
}

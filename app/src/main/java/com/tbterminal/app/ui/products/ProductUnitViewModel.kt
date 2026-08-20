package com.tbterminal.app.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.ProductUnit
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

class ProductUnitViewModel(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductUnitUiState())
    val uiState: StateFlow<ProductUnitUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        loadUnits()
    }

    fun loadUnits(page: Int = _uiState.value.page) {
        viewModelScope.launch {
            val state = _uiState.value
            val safePage = page.coerceAtLeast(1)
            _uiState.update { current ->
                current.copy(isLoading = true, page = safePage, message = null)
            }

            when (
                val result = inventoryRepository.getUnitPage(
                    page = safePage,
                    limit = state.pageSize,
                    search = state.searchQuery.trim().takeIf(String::isNotBlank)
                )
            ) {
                is RepositoryResult.Success -> {
                    val pageData = result.data
                    _uiState.update { state ->
                        state.copy(
                            units = pageData.data,
                            page = pageData.page,
                            totalPages = pageData.totalPages.coerceAtLeast(1),
                            totalUnits = pageData.total,
                            isLoading = false
                        )
                    }
                }
                is RepositoryResult.Error -> setMessage(result.message, loading = false)
                is RepositoryResult.Exception -> setMessage("Satuan gagal dimuat karena koneksi bermasalah.", loading = false)
            }
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update { state -> state.copy(nameInput = name, message = null) }
    }

    fun onSymbolChanged(symbol: String) {
        _uiState.update { state -> state.copy(symbolInput = symbol, message = null) }
    }

    fun onSearchChanged(query: String) {
        _uiState.update { state -> state.copy(searchQuery = query, page = 1, message = null) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            loadUnits(page = 1)
        }
    }

    fun nextPage() {
        val state = _uiState.value
        if (state.page < state.totalPages && !state.isLoading) {
            loadUnits(page = state.page + 1)
        }
    }

    fun previousPage() {
        val state = _uiState.value
        if (state.page > 1 && !state.isLoading) {
            loadUnits(page = state.page - 1)
        }
    }

    fun edit(unit: ProductUnit) {
        _uiState.update { state ->
            state.copy(
                editingUnit = unit,
                nameInput = unit.name,
                symbolInput = unit.symbol,
                message = null
            )
        }
    }

    fun cancelEdit() {
        _uiState.update { state ->
            state.copy(editingUnit = null, nameInput = "", symbolInput = "", message = null)
        }
    }

    fun save() {
        val state = _uiState.value
        val name = state.nameInput.trim()
        val symbol = state.symbolInput.trim()

        when {
            name.isBlank() -> setMessage("Nama satuan tidak boleh kosong.")
            symbol.isBlank() -> setMessage("Simbol satuan tidak boleh kosong.")
            name.length > 50 -> setMessage("Nama satuan maksimal 50 karakter.")
            symbol.length > 20 -> setMessage("Simbol satuan maksimal 20 karakter.")
            else -> submit(name, symbol, state.editingUnit)
        }
    }

    fun delete(unit: ProductUnit) {
        viewModelScope.launch {
            _uiState.update { state -> state.copy(isSaving = true, message = null) }
            val result = inventoryRepository.deleteUnit(unit.id)
            handleDeletedResult(result)
        }
    }

    private fun submit(
        name: String,
        symbol: String,
        editingUnit: ProductUnit?
    ) {
        viewModelScope.launch {
            _uiState.update { state -> state.copy(isSaving = true, message = null) }
            val result = editingUnit?.let { unit ->
                inventoryRepository.updateUnit(unit.id, name, symbol)
            } ?: inventoryRepository.createUnit(name, symbol)

            handleSavedResult(result)
        }
    }

    private fun handleSavedResult(result: RepositoryResult<ProductUnit>) {
        when (result) {
            is RepositoryResult.Success -> {
                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        editingUnit = null,
                        nameInput = "",
                        symbolInput = "",
                        message = "Satuan berhasil disimpan."
                    )
                }
                loadUnits(page = _uiState.value.page)
            }
            is RepositoryResult.Error -> setMessage(result.message)
            is RepositoryResult.Exception -> setMessage("Satuan gagal disimpan karena koneksi bermasalah.")
        }
    }

    private fun handleDeletedResult(result: RepositoryResult<Unit>) {
        when (result) {
            is RepositoryResult.Success -> {
                _uiState.update { state ->
                    state.copy(isSaving = false, message = "Satuan berhasil dihapus.")
                }
                loadUnits(page = _uiState.value.page)
            }
            is RepositoryResult.Error -> setMessage(result.message)
            is RepositoryResult.Exception -> setMessage("Satuan gagal dihapus karena koneksi bermasalah.")
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
                ProductUnitViewModel(inventoryRepository = inventoryRepository)
            }
        }
    }
}

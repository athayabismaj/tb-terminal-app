package com.tbterminal.app.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.repository.LocalBackupRepository
import com.tbterminal.app.data.repository.LocalBackupResult
import com.tbterminal.app.data.repository.LocalBackupSummary
import com.tbterminal.app.data.repository.LocalRestorePreview
import com.tbterminal.app.data.repository.LocalRestorePreviewResult
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.repository.ServerBackupRepository
import com.tbterminal.app.data.remote.DatabaseBackupJobDto
import com.tbterminal.app.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class BackupRestoreViewModel(
    private val repository: LocalBackupRepository,
    private val serverRepository: ServerBackupRepository,
    role: String
) : ViewModel() {
    private val canManageServerBackup = canManageServerDatabaseBackup(role)
    private val _uiState = MutableStateFlow(BackupRestoreUiState(isLoading = true))
    val uiState: StateFlow<BackupRestoreUiState> = _uiState.asStateFlow()

    private var selectedRestoreUri: Uri? = null
    private var serverRestoreToken: String? = null

    init {
        refreshSummary()
        if (canManageServerBackup) refreshServerBackups()
    }

    fun refreshSummary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null, errorMessage = null) }
            runCatching { repository.getSummary() }
                .onSuccess { summary ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            summary = summary
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Gagal memuat ringkasan backup."
                        )
                    }
                }
        }
    }

    fun createBackup(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null, errorMessage = null) }
            when (val result = repository.createBackup(uri)) {
                is LocalBackupResult.Success -> {
                    _uiState.update { it.copy(isBusy = false, message = result.message) }
                }
                is LocalBackupResult.Failed -> {
                    _uiState.update { it.copy(isBusy = false, errorMessage = result.message) }
                }
            }
            refreshSummary()
        }
    }

    fun inspectRestoreFile(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            selectedRestoreUri = null
            _uiState.update {
                it.copy(
                    isBusy = true,
                    selectedRestorePreview = null,
                    message = null,
                    errorMessage = null
                )
            }
            when (val result = repository.inspectBackup(uri)) {
                is LocalRestorePreviewResult.Valid -> {
                    selectedRestoreUri = uri
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            selectedRestorePreview = result.preview
                        )
                    }
                }
                is LocalRestorePreviewResult.Invalid -> {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun confirmRestore() {
        val uri = selectedRestoreUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null, errorMessage = null) }
            when (val result = repository.stageRestore(uri)) {
                is LocalBackupResult.Success -> {
                    selectedRestoreUri = null
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            selectedRestorePreview = null,
                            message = result.message
                        )
                    }
                }
                is LocalBackupResult.Failed -> {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun dismissRestoreDialog() {
        selectedRestoreUri = null
        _uiState.update { it.copy(selectedRestorePreview = null) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }

    fun refreshServerBackups() {
        if (!canManageServerBackup) return
        viewModelScope.launch {
            _uiState.update { it.copy(isServerLoading = true) }
            when (val result = serverRepository.list()) {
                is RepositoryResult.Success -> _uiState.update { it.copy(isServerLoading = false, serverJobs = result.data) }
                is RepositoryResult.Error -> setServerError(result.message)
                is RepositoryResult.Exception -> setServerError("Daftar backup server gagal dimuat karena koneksi bermasalah.")
            }
        }
    }

    fun createServerBackup() {
        if (!canManageServerBackup || _uiState.value.isServerBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isServerBusy = true, errorMessage = null, message = null) }
            when (val result = serverRepository.create()) {
                is RepositoryResult.Success -> {
                    upsertServerJob(result.data)
                    _uiState.update { it.copy(isServerBusy = false, message = "Backup server masuk antrean.") }
                    pollServerJob(result.data.id)
                }
                is RepositoryResult.Error -> setServerError(result.message)
                is RepositoryResult.Exception -> {
                    _uiState.update { it.copy(isServerBusy = false, errorMessage = "Hasil permintaan backup belum pasti. Daftar status dimuat ulang.") }
                    refreshServerBackups()
                }
            }
        }
    }

    fun downloadServerBackup(job: DatabaseBackupJobDto, uri: Uri?) {
        if (!canManageServerBackup || uri == null || job.status != "SUCCEEDED") return
        viewModelScope.launch {
            _uiState.update { it.copy(isServerBusy = true, errorMessage = null, message = null) }
            when (val result = serverRepository.download(job, uri)) {
                is RepositoryResult.Success -> _uiState.update { it.copy(isServerBusy = false, message = "Backup server berhasil disimpan dan checksum sesuai.") }
                is RepositoryResult.Error -> setServerError(result.message)
                is RepositoryResult.Exception -> setServerError("Download backup terputus. File tujuan tidak boleh digunakan; unduh ulang secara manual.")
            }
        }
    }

    fun validateServerRestore(uri: Uri?) {
        if (!canManageServerBackup || uri == null || _uiState.value.isServerBusy) return
        viewModelScope.launch {
            serverRestoreToken = null
            _uiState.update { it.copy(isServerBusy = true, serverRestorePrompt = null, errorMessage = null, message = null) }
            when (val result = serverRepository.validateRestore(uri)) {
                is RepositoryResult.Success -> {
                    serverRestoreToken = result.data.confirmationToken
                    upsertServerJob(result.data.job)
                    _uiState.update {
                        it.copy(
                            isServerBusy = false,
                            serverRestorePrompt = ServerRestorePrompt(
                                jobId = result.data.job.id,
                                confirmationPhrase = result.data.confirmationPhrase,
                                expiresAt = result.data.expiresAt
                            ),
                            serverRestorePhraseInput = "",
                            serverRestoreAcknowledged = false
                        )
                    }
                }
                is RepositoryResult.Error -> setServerError(result.message)
                is RepositoryResult.Exception -> setServerError("Upload atau validasi restore terputus. Pilih file kembali; validasi tidak diulang otomatis.")
            }
        }
    }

    fun onServerRestorePhraseChanged(value: String) = _uiState.update { it.copy(serverRestorePhraseInput = value) }
    fun onServerRestoreAcknowledgedChanged(value: Boolean) = _uiState.update { it.copy(serverRestoreAcknowledged = value) }

    fun dismissServerRestore() {
        serverRestoreToken = null
        _uiState.update { it.copy(serverRestorePrompt = null, serverRestorePhraseInput = "", serverRestoreAcknowledged = false) }
    }

    fun confirmServerRestore() {
        val state = _uiState.value
        val prompt = state.serverRestorePrompt ?: return
        val token = serverRestoreToken ?: return setServerError("Token konfirmasi sudah tidak tersedia. Validasi file kembali.")
        if (state.serverRestorePhraseInput != prompt.confirmationPhrase || !state.serverRestoreAcknowledged) {
            return setServerError("Ketik frasa persis dan setujui downtime serta penimpaan database.")
        }
        if (state.isServerBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isServerBusy = true, errorMessage = null, message = null) }
            val result = serverRepository.confirmRestore(prompt.jobId, token, state.serverRestorePhraseInput, true)
            // Token satu kali selalu dibuang setelah attempt. Timeout tidak pernah memicu retry confirm.
            serverRestoreToken = null
            _uiState.update { it.copy(serverRestorePrompt = null, serverRestorePhraseInput = "", serverRestoreAcknowledged = false) }
            when (result) {
                is RepositoryResult.Success -> {
                    upsertServerJob(result.data)
                    _uiState.update { it.copy(isServerBusy = false, message = "Restore server masuk antrean. Jangan tutup aplikasi sampai status final terlihat.") }
                    pollServerJob(prompt.jobId)
                }
                is RepositoryResult.Error -> setServerError(result.message)
                is RepositoryResult.Exception -> {
                    _uiState.update { it.copy(isServerBusy = false, errorMessage = "Hasil konfirmasi restore ambigu. Konfirmasi tidak diulang; aplikasi hanya memeriksa status job.") }
                    when (ambiguousRestoreRecovery()) {
                        RestoreAmbiguousRecovery.POLL_STATUS_ONLY -> pollServerJob(prompt.jobId)
                    }
                }
            }
        }
    }

    private suspend fun pollServerJob(id: String) {
        repeat(SERVER_POLL_ATTEMPTS) {
            delay(SERVER_POLL_INTERVAL_MS)
            when (val result = serverRepository.get(id)) {
                is RepositoryResult.Success -> {
                    upsertServerJob(result.data)
                    if (result.data.status.isTerminalBackupStatus()) {
                        _uiState.update { state ->
                            state.copy(
                                isServerBusy = false,
                                message = if (result.data.status == "SUCCEEDED") "Job ${result.data.operation.lowercase()} selesai." else state.message,
                                errorMessage = result.data.errorMessage?.takeIf { result.data.status == "FAILED" } ?: state.errorMessage
                            )
                        }
                        return
                    }
                }
                else -> Unit
            }
        }
        _uiState.update { it.copy(isServerBusy = false, message = "Job masih berjalan. Gunakan Muat Ulang untuk melihat status terbaru.") }
    }

    private fun upsertServerJob(job: DatabaseBackupJobDto) = _uiState.update { state ->
        state.copy(serverJobs = listOf(job) + state.serverJobs.filterNot { it.id == job.id })
    }

    private fun setServerError(message: String) = _uiState.update {
        it.copy(isServerBusy = false, isServerLoading = false, errorMessage = message)
    }

    companion object {
        fun factory(
            repository: LocalBackupRepository,
            serverRepository: ServerBackupRepository,
            role: String
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                BackupRestoreViewModel(repository, serverRepository, role)
            }
        }
    }
}

data class BackupRestoreUiState(
    val isLoading: Boolean = false,
    val isBusy: Boolean = false,
    val summary: LocalBackupSummary = LocalBackupSummary(),
    val selectedRestorePreview: LocalRestorePreview? = null,
    val message: String? = null,
    val errorMessage: String? = null,
    val isServerLoading: Boolean = false,
    val isServerBusy: Boolean = false,
    val serverJobs: List<DatabaseBackupJobDto> = emptyList(),
    val serverRestorePrompt: ServerRestorePrompt? = null,
    val serverRestorePhraseInput: String = "",
    val serverRestoreAcknowledged: Boolean = false
)

data class ServerRestorePrompt(
    val jobId: String,
    val confirmationPhrase: String,
    val expiresAt: String
)

internal fun String.isTerminalBackupStatus(): Boolean = this in setOf("SUCCEEDED", "FAILED")

internal fun canManageServerDatabaseBackup(role: String): Boolean =
    role.trim().lowercase() in setOf("owner", "admin")

internal enum class RestoreAmbiguousRecovery { POLL_STATUS_ONLY }
internal fun ambiguousRestoreRecovery(): RestoreAmbiguousRecovery = RestoreAmbiguousRecovery.POLL_STATUS_ONLY

private const val SERVER_POLL_ATTEMPTS = 80
private const val SERVER_POLL_INTERVAL_MS = 1_500L

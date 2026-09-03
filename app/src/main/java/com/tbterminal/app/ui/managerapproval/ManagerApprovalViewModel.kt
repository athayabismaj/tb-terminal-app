package com.tbterminal.app.ui.managerapproval

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.CreateManagerApprovalCommand
import com.tbterminal.app.data.model.ManagerApprovalContext
import com.tbterminal.app.data.model.ManagerApprovalGrant
import com.tbterminal.app.data.model.ManagerApprovalStatus
import com.tbterminal.app.data.remote.NetworkResult
import com.tbterminal.app.data.repository.ManagerApprovalRepository
import com.tbterminal.app.ui.common.viewModelFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ManagerApprovalUiPhase {
    IDLE,
    SUBMITTING,
    SUCCESS,
    ERROR,
}

data class ManagerApprovalUiState(
    val approverUsername: String = "",
    val approverPin: String = "",
    val phase: ManagerApprovalUiPhase = ManagerApprovalUiPhase.IDLE,
    val errorMessage: String? = null,
    val grant: ManagerApprovalGrant? = null,
)

internal class ManagerApprovalSubmissionGuard {
    private val submitting = AtomicBoolean(false)

    fun tryStart(): Boolean = submitting.compareAndSet(false, true)
    fun finish() = submitting.set(false)
}

class ManagerApprovalViewModel internal constructor(
    private val context: ManagerApprovalContext,
    private val repository: ManagerApprovalRepository,
    private val submissionGuard: ManagerApprovalSubmissionGuard = ManagerApprovalSubmissionGuard(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(ManagerApprovalUiState())
    val uiState: StateFlow<ManagerApprovalUiState> = _uiState.asStateFlow()

    fun onUsernameChanged(value: String) {
        if (_uiState.value.phase == ManagerApprovalUiPhase.SUBMITTING) return
        _uiState.update {
            it.copy(
                approverUsername = value.take(80),
                errorMessage = null,
                phase = ManagerApprovalUiPhase.IDLE,
            )
        }
    }

    fun onPinChanged(value: String) {
        if (_uiState.value.phase == ManagerApprovalUiPhase.SUBMITTING) return
        _uiState.update {
            it.copy(
                approverPin = value.filter(Char::isDigit).take(6),
                errorMessage = null,
                phase = ManagerApprovalUiPhase.IDLE,
            )
        }
    }

    fun submit(): Boolean {
        if (!submissionGuard.tryStart()) return false

        val current = _uiState.value
        val validationError = validateManagerApprovalInput(
            username = current.approverUsername,
            pin = current.approverPin,
        )
        if (validationError != null) {
            submissionGuard.finish()
            _uiState.update {
                it.copy(phase = ManagerApprovalUiPhase.ERROR, errorMessage = validationError)
            }
            return false
        }

        val command = CreateManagerApprovalCommand(
            action = context.action,
            resourceType = context.resourceType,
            resourceId = context.resourceId,
            approverUsername = current.approverUsername.trim(),
            approverPin = current.approverPin,
        )

        // Credential hanya hidup pada command request di memory dan langsung dihapus dari UI state.
        _uiState.update {
            it.copy(
                approverPin = "",
                phase = ManagerApprovalUiPhase.SUBMITTING,
                errorMessage = null,
                grant = null,
            )
        }

        viewModelScope.launch {
            try {
                when (val result = repository.createApproval(command)) {
                    is NetworkResult.Success -> handleSuccess(result.data)
                    is NetworkResult.Error -> setError(managerApprovalErrorMessage(result.code, result.message))
                    is NetworkResult.Exception -> setError("Persetujuan gagal karena koneksi bermasalah.")
                }
            } finally {
                submissionGuard.finish()
            }
        }
        return true
    }

    fun acknowledgeSuccess() {
        _uiState.update {
            it.copy(
                approverPin = "",
                phase = ManagerApprovalUiPhase.IDLE,
                errorMessage = null,
                grant = null,
            )
        }
    }

    fun clearSensitiveData() {
        _uiState.value = ManagerApprovalUiState()
    }

    private fun handleSuccess(response: com.tbterminal.app.data.remote.ApiResponse<ManagerApprovalGrant>) {
        val grant = response.data
        val validGrant = response.success && grant != null &&
            grant.status == ManagerApprovalStatus.APPROVED &&
            grant.action == context.action &&
            grant.resourceType == context.resourceType &&
            grant.resourceId == context.resourceId

        if (!validGrant) {
            setError(response.message ?: response.error ?: "Respons persetujuan manager tidak valid.")
            return
        }
        _uiState.update {
            it.copy(
                approverPin = "",
                phase = ManagerApprovalUiPhase.SUCCESS,
                errorMessage = null,
                grant = grant,
            )
        }
    }

    private fun setError(message: String) {
        _uiState.update {
            it.copy(
                approverPin = "",
                phase = ManagerApprovalUiPhase.ERROR,
                errorMessage = message,
                grant = null,
            )
        }
    }

    companion object {
        fun factory(
            context: ManagerApprovalContext,
            repository: ManagerApprovalRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            ManagerApprovalViewModel(context, repository)
        }
    }
}

internal fun validateManagerApprovalInput(username: String, pin: String): String? = when {
    username.isBlank() -> "Akun manager wajib diisi."
    pin.length != 6 || !pin.all(Char::isDigit) -> "PIN manager harus terdiri dari 6 digit."
    else -> null
}

internal fun managerApprovalErrorMessage(code: String, fallback: String): String {
    val normalized = code.trim().uppercase()
    return when {
        normalized == "MANAGER_APPROVAL_INVALID" -> "PIN atau akun manager tidak valid."
        normalized == "MANAGER_APPROVER_FORBIDDEN" -> "Akun tersebut tidak dapat memberikan persetujuan manager."
        normalized == "MANAGER_APPROVAL_SELF_APPROVAL_FORBIDDEN" -> "Anda tidak dapat menyetujui permintaan sendiri."
        normalized == "MANAGER_APPROVAL_EXPIRED" -> "Persetujuan manager sudah kedaluwarsa. Silakan minta persetujuan baru."
        normalized == "MANAGER_APPROVAL_ALREADY_USED" -> "Persetujuan manager sudah pernah digunakan."
        normalized == "MANAGER_APPROVAL_SCOPE_MISMATCH" ||
            normalized == "MANAGER_APPROVAL_ACTION_MISMATCH" ||
            normalized == "MANAGER_APPROVAL_REQUESTER_MISMATCH" ->
            "Persetujuan tidak sesuai dengan tindakan yang diminta."
        normalized == "HTTP_429" || "RATE_LIMIT" in normalized || normalized == "TOO_MANY_REQUESTS" ->
            "Terlalu banyak percobaan. Coba kembali beberapa saat lagi."
        normalized.startsWith("NETWORK_") || normalized.startsWith("CONNECTION_") ->
            "Persetujuan gagal karena koneksi bermasalah."
        else -> fallback.ifBlank { "Persetujuan manager gagal diproses." }
    }
}

package com.tbterminal.app.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.ManagedUser
import com.tbterminal.app.data.model.UpdateUserCommand
import com.tbterminal.app.data.model.UserPage
import com.tbterminal.app.data.model.UserRole
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.repository.UserRepository
import com.tbterminal.app.data.validation.CredentialValidator
import com.tbterminal.app.ui.common.viewModelFactory
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserManagementUiState(
    val staffMembers: List<StaffMember> = emptyList(),
    val roles: List<UserRole> = emptyList(),
    val totalStaff: Long = 0,
    val isLoading: Boolean = false,
    val isLoadingRoles: Boolean = false,
    val isMutating: Boolean = false,
    val errorMessage: String? = null,
    val actionErrorMessage: String? = null,
    val actionSuccessMessage: String? = null
) {
    val activeStaff: Int
        get() = staffMembers.count { staff -> staff.status == StaffStatus.Active }

    val latestLogin: String
        get() = staffMembers
            .filter { staff -> staff.lastLoginEpochMillis != null }
            .maxByOrNull { staff -> staff.lastLoginEpochMillis ?: Long.MIN_VALUE }
            ?.lastLogin
            ?: NO_LOGIN_TEXT
}

class UserManagementViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        UserManagementUiState(
            isLoading = true,
            isLoadingRoles = true
        )
    )
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()

    init {
        loadRoles()
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isLoading = true, errorMessage = null)
            }

            when (val result = userRepository.getUsers()) {
                is RepositoryResult.Success -> handleUsers(result.data)
                is RepositoryResult.Error -> setLoadError(result.message)
                is RepositoryResult.Exception -> {
                    setLoadError("Koneksi ke server bermasalah. Coba muat ulang daftar pengguna.")
                }
            }
        }
    }

    fun refresh() = loadUsers()

    fun loadRoles() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isLoadingRoles = true, actionErrorMessage = null)
            }

            when (val result = userRepository.getRoles()) {
                is RepositoryResult.Success -> handleRoles(result.data)
                is RepositoryResult.Error -> setRoleError(result.message)
                is RepositoryResult.Exception -> {
                    setRoleError("Role gagal dimuat karena koneksi ke server bermasalah.")
                }
            }
        }
    }

    fun updateUser(
        staff: StaffMember,
        input: StaffEditInput
    ) {
        val passwordError = if (input.newPassword.isNotEmpty()) {
            CredentialValidator.validatePassword(
                password = input.newPassword,
                requiredMessage = "Password baru minimal ${CredentialValidator.PASSWORD_MIN_LENGTH} karakter.",
                minMessage = "Password baru minimal ${CredentialValidator.PASSWORD_MIN_LENGTH} karakter.",
                maxMessage = "Password baru maksimal ${CredentialValidator.PASSWORD_MAX_LENGTH} karakter."
            )
        } else null
        val pinError = if (input.newPin.isNotEmpty()) {
            CredentialValidator.validatePin(
                pin = input.newPin,
                requiredMessage = "PIN baru harus terdiri dari ${CredentialValidator.PIN_MIN_LENGTH} sampai ${CredentialValidator.PIN_MAX_LENGTH} digit angka.",
                nonDigitMessage = "PIN baru harus terdiri dari ${CredentialValidator.PIN_MIN_LENGTH} sampai ${CredentialValidator.PIN_MAX_LENGTH} digit angka.",
                lengthMessage = "PIN baru harus terdiri dari ${CredentialValidator.PIN_MIN_LENGTH} sampai ${CredentialValidator.PIN_MAX_LENGTH} digit angka."
            )
        } else null

        when {
            _uiState.value.isMutating -> return
            input.name.isBlank() -> setActionError("Nama lengkap tidak boleh kosong.")
            input.username.isBlank() -> setActionError("Username tidak boleh kosong.")
            input.roleId.isBlank() -> setActionError("Role pengguna wajib dipilih.")
            passwordError != null -> setActionError(passwordError)
            pinError != null -> setActionError(pinError)
            else -> submitUserUpdate(
                staff = staff,
                input = input
            )
        }
    }

    fun activateUser(staff: StaffMember) {
        updateUser(
            staff = staff,
            input = StaffEditInput(
                name = staff.name,
                username = staff.username,
                email = staff.rawEmail.orEmpty(),
                roleId = staff.roleId,
                isActive = true,
                newPassword = "",
                newPin = ""
            )
        )
    }

    fun deactivateUser(staff: StaffMember) {
        if (_uiState.value.isMutating) return

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isMutating = true,
                    actionErrorMessage = null,
                    actionSuccessMessage = null
                )
            }

            when (val result = userRepository.deactivateUser(staff.id)) {
                is RepositoryResult.Success -> handleDeactivatedUser(staff = staff)
                is RepositoryResult.Error -> setActionError(result.message)
                is RepositoryResult.Exception -> {
                    setActionError("User gagal dinonaktifkan karena koneksi ke server bermasalah.")
                }
            }
        }
    }

    fun clearActionMessages() {
        _uiState.update { state ->
            state.copy(actionErrorMessage = null, actionSuccessMessage = null)
        }
    }

    private fun submitUserUpdate(
        staff: StaffMember,
        input: StaffEditInput
    ) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isMutating = true,
                    actionErrorMessage = null,
                    actionSuccessMessage = null
                )
            }

            val command = UpdateUserCommand(
                name = input.name.trim(),
                username = input.username.trim(),
                isActive = input.isActive,
                roleId = input.roleId,
                email = input.email.trim().takeIf(String::isNotBlank),
                newPassword = input.newPassword.takeIf(String::isNotBlank),
                newPin = input.newPin.takeIf(String::isNotBlank)
            )

            when (val result = userRepository.updateUser(staff.id, command)) {
                is RepositoryResult.Success -> handleUpdatedUser(result.data)
                is RepositoryResult.Error -> setActionError(result.message)
                is RepositoryResult.Exception -> {
                    setActionError("User gagal diperbarui karena koneksi ke server bermasalah.")
                }
            }
        }
    }

    private fun handleRoles(roles: List<UserRole>) {
        val employeeRoles = roles.filterNot { role ->
            role.name.equals(OWNER_ROLE_NAME, ignoreCase = true)
        }

        _uiState.update { state ->
            state.copy(
                roles = employeeRoles,
                isLoadingRoles = false,
                actionErrorMessage = null
            )
        }
    }

    private fun handleUsers(page: UserPage) {
        val employeeStaff = page.data
            .filterNot { user -> user.roleName.equals(OWNER_ROLE_NAME, ignoreCase = true) }
            .map(ManagedUser::toStaffMember)

        _uiState.update { state ->
            state.copy(
                staffMembers = employeeStaff,
                totalStaff = employeeStaff.size.toLong(),
                isLoading = false,
                errorMessage = null
            )
        }
    }

    private fun handleUpdatedUser(user: ManagedUser) {
        val updatedStaff = user.toStaffMember()
        _uiState.update { state ->
            state.copy(
                staffMembers = state.staffMembers.map { staff ->
                    if (staff.id == updatedStaff.id) updatedStaff else staff
                },
                isMutating = false,
                actionErrorMessage = null,
                actionSuccessMessage = "User berhasil diperbarui."
            )
        }
    }

    private fun handleDeactivatedUser(staff: StaffMember) {
        _uiState.update { state ->
            state.copy(
                staffMembers = state.staffMembers.map { current ->
                    if (current.id == staff.id) {
                        current.copy(status = StaffStatus.Inactive)
                    } else {
                        current
                    }
                },
                isMutating = false,
                actionErrorMessage = null,
                actionSuccessMessage = "User berhasil dinonaktifkan."
            )
        }
    }

    private fun setLoadError(message: String) {
        _uiState.update { state ->
            state.copy(isLoading = false, errorMessage = message)
        }
    }

    private fun setRoleError(message: String) {
        _uiState.update { state ->
            state.copy(isLoadingRoles = false, actionErrorMessage = message)
        }
    }

    private fun setActionError(message: String) {
        _uiState.update { state ->
            state.copy(
                isMutating = false,
                actionErrorMessage = message,
                actionSuccessMessage = null
            )
        }
    }

    companion object {
        fun factory(userRepository: UserRepository): ViewModelProvider.Factory {
            return viewModelFactory {
                UserManagementViewModel(userRepository = userRepository)
            }
        }
    }
}

data class UserDetailUiState(
    val staff: StaffMember? = null,
    val isLoading: Boolean = true,
    val isMutating: Boolean = false,
    val errorMessage: String? = null,
    val actionErrorMessage: String? = null,
    val credentialErrorMessage: String? = null,
    val credentialSuccessMessage: String? = null,
    val isDeactivated: Boolean = false,
    val isReactivated: Boolean = false,
    val isNotFoundError: Boolean = false
)

class UserDetailViewModel(
    private val userRepository: UserRepository,
    private val userId: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserDetailUiState())
    val uiState: StateFlow<UserDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    fun loadDetail() {
        if (userId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "ID pengguna tidak valid.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = userRepository.getUsers(limit = USER_DETAIL_PAGE_LIMIT)) {
                is RepositoryResult.Success -> {
                    val user = result.data.data.firstOrNull { candidate ->
                        candidate.id == userId &&
                            !candidate.roleName.equals(OWNER_ROLE_NAME, ignoreCase = true)
                    }
                    if (user != null) {
                        _uiState.update {
                            it.copy(
                                staff = user.toStaffMember(),
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Pengguna tidak ditemukan."
                            )
                        }
                    }
                }
                is RepositoryResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Koneksi bermasalah. Detail pengguna gagal dimuat."
                        )
                    }
                }
            }
        }
    }

    fun deactivate() = performDeactivate(stayOnDetail = false)

    fun undoActivate() = performDeactivate(stayOnDetail = true)

    fun activate() {
        val current = _uiState.value.staff ?: return
        if (_uiState.value.isMutating || userId.isBlank()) return
        _uiState.update {
            it.copy(
                isMutating = true,
                isReactivated = false,
                actionErrorMessage = null,
                isNotFoundError = false
            )
        }
        viewModelScope.launch {
            val command = UpdateUserCommand(
                name = current.name,
                username = current.username,
                isActive = true,
                roleId = current.roleId,
                email = current.rawEmail
            )
            when (val result = userRepository.updateUser(userId, command)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            staff = result.data.toStaffMember(),
                            isMutating = false,
                            isReactivated = true,
                            actionErrorMessage = null,
                            isNotFoundError = false
                        )
                    }
                }
                is RepositoryResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            actionErrorMessage = result.message,
                            isNotFoundError = result.code == "HTTP_404"
                        )
                    }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            actionErrorMessage = "Pengguna gagal diaktifkan karena koneksi bermasalah.",
                            isNotFoundError = false
                        )
                    }
                }
            }
        }
    }

    private fun performDeactivate(stayOnDetail: Boolean) {
        if (_uiState.value.isMutating || userId.isBlank()) return
        _uiState.update {
            it.copy(
                isMutating = true,
                isReactivated = false,
                actionErrorMessage = null,
                isNotFoundError = false
            )
        }
        viewModelScope.launch {
            when (val result = userRepository.deactivateUser(userId)) {
                is RepositoryResult.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            staff = current.staff?.copy(status = StaffStatus.Inactive),
                            isMutating = false,
                            isDeactivated = !stayOnDetail,
                            actionErrorMessage = null,
                            isNotFoundError = false
                        )
                    }
                }
                is RepositoryResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            actionErrorMessage = result.message,
                            isNotFoundError = result.code == "HTTP_404"
                        )
                    }
                }
                is RepositoryResult.Exception -> {
                    val msg = if (stayOnDetail) {
                        "Pengguna gagal dinonaktifkan kembali karena koneksi bermasalah."
                    } else {
                        "Pengguna gagal dinonaktifkan karena koneksi bermasalah."
                    }
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            actionErrorMessage = msg,
                            isNotFoundError = false
                        )
                    }
                }
            }
        }
    }

    fun clearActionError() {
        _uiState.update { it.copy(actionErrorMessage = null, isNotFoundError = false) }
    }

    fun resetDeactivatedState() {
        _uiState.update { it.copy(isDeactivated = false) }
    }

    fun resetReactivatedState() {
        _uiState.update { it.copy(isReactivated = false) }
    }

    fun clearCredentialMessages() {
        _uiState.update { it.copy(credentialErrorMessage = null, credentialSuccessMessage = null) }
    }

    fun changePassword(newPassword: String, confirmPassword: String) {
        val current = _uiState.value.staff ?: return
        if (_uiState.value.isMutating || userId.isBlank()) return
        CredentialValidator.validatePassword(
            password = newPassword,
            requiredMessage = "Kata sandi baru minimal ${CredentialValidator.PASSWORD_MIN_LENGTH} karakter.",
            minMessage = "Kata sandi baru minimal ${CredentialValidator.PASSWORD_MIN_LENGTH} karakter.",
            maxMessage = "Kata sandi baru maksimal ${CredentialValidator.PASSWORD_MAX_LENGTH} karakter."
        )?.let { error ->
            _uiState.update { it.copy(credentialErrorMessage = error) }
            return
        }
        if (newPassword != confirmPassword) {
            _uiState.update { it.copy(credentialErrorMessage = "Kata sandi tidak sama.") }
            return
        }
        submitCredentialUpdate(
            current = current,
            newPassword = newPassword,
            newPin = null,
            successMessage = "Kata sandi berhasil diperbarui",
            connectionErrorMessage = "Kata sandi gagal diperbarui karena koneksi bermasalah."
        )
    }

    fun changePin(newPin: String, confirmPin: String) {
        val current = _uiState.value.staff ?: return
        if (_uiState.value.isMutating || userId.isBlank()) return
        if (!CredentialValidator.isValidPin(newPin)) {
            _uiState.update {
                it.copy(
                    credentialErrorMessage = "PIN baru harus terdiri dari ${CredentialValidator.PIN_MIN_LENGTH} sampai ${CredentialValidator.PIN_MAX_LENGTH} digit angka."
                )
            }
            return
        }
        if (newPin != confirmPin) {
            _uiState.update { it.copy(credentialErrorMessage = "PIN tidak sama.") }
            return
        }
        submitCredentialUpdate(
            current = current,
            newPassword = null,
            newPin = newPin,
            successMessage = "PIN transaksi berhasil diperbarui",
            connectionErrorMessage = "PIN gagal diperbarui karena koneksi bermasalah."
        )
    }

    private fun submitCredentialUpdate(
        current: StaffMember,
        newPassword: String?,
        newPin: String?,
        successMessage: String,
        connectionErrorMessage: String
    ) {
        _uiState.update {
            it.copy(
                isMutating = true,
                credentialErrorMessage = null,
                credentialSuccessMessage = null
            )
        }
        viewModelScope.launch {
            val command = UpdateUserCommand(
                name = current.name,
                username = current.username,
                isActive = current.status == StaffStatus.Active,
                roleId = current.roleId,
                email = current.rawEmail,
                newPassword = newPassword,
                newPin = newPin
            )
            when (val result = userRepository.updateUser(userId, command)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            staff = result.data.toStaffMember(),
                            isMutating = false,
                            credentialErrorMessage = null,
                            credentialSuccessMessage = successMessage
                        )
                    }
                }
                is RepositoryResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            credentialErrorMessage = result.message,
                            isNotFoundError = result.code == "HTTP_404"
                        )
                    }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            credentialErrorMessage = connectionErrorMessage
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val USER_DETAIL_PAGE_LIMIT = 200

        fun factory(
            userRepository: UserRepository,
            userId: String
        ): ViewModelProvider.Factory {
            return viewModelFactory { UserDetailViewModel(userRepository, userId) }
        }
    }
}

internal fun ManagedUser.toStaffMember(): StaffMember {
    val lastLoginInstant = lastLogin?.toServerInstantOrNull()

    return StaffMember(
        id = id,
        name = name,
        email = email?.takeIf(String::isNotBlank) ?: "Email belum diisi",
        rawEmail = email?.takeIf(String::isNotBlank),
        username = username,
        roleId = roleId,
        role = StaffRole.fromBackendName(roleName),
        status = if (isActive) StaffStatus.Active else StaffStatus.Inactive,
        lastLogin = lastLoginInstant?.toRelativeLoginText() ?: NO_LOGIN_TEXT,
        lastLoginEpochMillis = lastLoginInstant?.toEpochMilli(),
        initials = name.toInitials()
    )
}

private fun String.toServerInstantOrNull(): Instant? {
    return runCatching { Instant.parse(this) }.getOrNull()
}

private fun Instant.toRelativeLoginText(
    now: Instant = Instant.now()
): String {
    val duration = Duration.between(this, now)

    if (duration.isNegative || duration.seconds < MINUTE_SECONDS) {
        return "Baru saja"
    }

    val minutes = duration.toMinutes()
    if (minutes < HOUR_MINUTES) {
        return "$minutes menit lalu"
    }

    val hours = duration.toHours()
    if (hours < DAY_HOURS) {
        return "$hours jam lalu"
    }

    val days = duration.toDays()
    return "$days hari lalu"
}

private fun String.toInitials(): String {
    return trim()
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
        .take(2)
        .mapNotNull { part -> part.firstOrNull()?.uppercase() }
        .joinToString(separator = "")
        .ifBlank { "?" }
}

private const val NO_LOGIN_TEXT = "Belum pernah"
private const val MINUTE_SECONDS = 60
private const val HOUR_MINUTES = 60
private const val DAY_HOURS = 24
private const val OWNER_ROLE_NAME = "OWNER"

package com.tbterminal.app.ui.security

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.TabletAndroid
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.AuditLog
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.repository.SecurityLogRepository
import com.tbterminal.app.ui.common.viewModelFactory
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SecurityLogUiState(
    val logs: List<SecurityLogItem> = emptyList(),
    val visibleLogs: List<SecurityLogItem> = emptyList(),
    val searchQuery: String = "",
    val dateFilter: SecurityDateFilter = SecurityDateFilter.All,
    val activityFilter: SecurityActivityFilter = SecurityActivityFilter.All,
    val selectedLog: SecurityLogItem? = null,
    val totalLogs: Long = 0,
    val page: Int = 1,
    val limit: Int = DEFAULT_LIMIT,
    val totalPages: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val updateCount: Int
        get() = logs.count { log -> log.type == SecurityLogType.Update }

    val latestRelativeTime: String
        get() = logs.firstOrNull()?.relativeTime ?: "Belum ada"

    val latestCompactRelativeTime: String
        get() = logs.firstOrNull()?.compactRelativeTime ?: "Belum ada"
}

enum class SecurityLogType(
    val label: String,
    val color: Color,
    val background: Color
) {
    Insert("Tambah Data", SecurityPrimary, SecurityPrimaryLight),
    Update("Ubah Data", SecurityInfo, SecurityInfoLight),
    Delete("Nonaktifkan Data", SecuritySlate500, SecuritySlate100)
}

enum class SecurityDateFilter(
    val label: String,
    val queryValue: String?
) {
    All("Semua Tanggal", null),
    Today("Hari Ini", "today"),
    Last7Days("7 Hari Terakhir", "7d"),
    Last30Days("30 Hari Terakhir", "30d")
}

enum class SecurityActivityFilter(
    val label: String,
    val actionQuery: String?
) {
    All("Semua Aktivitas", null),
    Insert("Tambah Data", "INSERT"),
    Update("Ubah Data", "UPDATE"),
    Delete("Nonaktifkan Data", "DELETE")
}

data class SecurityLogItem(
    val id: String,
    val userName: String,
    val userRole: String,
    val type: SecurityLogType,
    val activityLabel: String,
    val ipAddress: String,
    val deviceIcon: ImageVector,
    val deviceName: String,
    val time: String,
    val relativeTime: String,
    val compactRelativeTime: String
)

class SecurityLogViewModel(
    private val securityLogRepository: SecurityLogRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SecurityLogUiState(isLoading = true))
    val uiState: StateFlow<SecurityLogUiState> = _uiState.asStateFlow()

    init {
        loadLogs()
    }

    fun loadLogs() {
        val current = _uiState.value
        loadLogs(page = current.page, dateFilter = current.dateFilter, activityFilter = current.activityFilter)
    }

    private fun loadLogs(
        page: Int,
        dateFilter: SecurityDateFilter,
        activityFilter: SecurityActivityFilter
    ) {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    page = page,
                    dateFilter = dateFilter,
                    activityFilter = activityFilter,
                    isLoading = true,
                    errorMessage = null
                )
            }

            when (val result = securityLogRepository.getAuditLogs(
                    page = page,
                    limit = current.limit,
                    action = activityFilter.actionQuery,
                    range = dateFilter.queryValue
                )) {
                is RepositoryResult.Success -> {
                    val page = result.data
                    val logs = page.data.map(AuditLog::toSecurityLogItem)
                    _uiState.update { state ->
                        val updated = state.copy(
                            logs = logs,
                            totalLogs = page.total,
                            page = page.page,
                            limit = page.limit,
                            totalPages = page.totalPages,
                            isLoading = false,
                            errorMessage = null
                        )
                        updated.copy(visibleLogs = updated.filteredLogs())
                    }
                }
                is RepositoryResult.Error -> setError(result.message)
                is RepositoryResult.Exception -> {
                    setError("Log keamanan gagal dimuat karena koneksi ke server bermasalah.")
                }
            }
        }
    }

    fun updateSearchQuery(value: String) {
        _uiState.update { state ->
            val updated = state.copy(searchQuery = value)
            updated.copy(visibleLogs = updated.filteredLogs())
        }
    }

    fun updateDateFilter(filter: SecurityDateFilter) {
        val current = _uiState.value
        loadLogs(page = 1, dateFilter = filter, activityFilter = current.activityFilter)
    }

    fun updateActivityFilter(filter: SecurityActivityFilter) {
        val current = _uiState.value
        loadLogs(page = 1, dateFilter = current.dateFilter, activityFilter = filter)
    }

    fun goToPage(page: Int) {
        val current = _uiState.value
        val safePage = page.coerceIn(1, current.totalPages.coerceAtLeast(1))
        if (safePage != current.page) {
            loadLogs(page = safePage, dateFilter = current.dateFilter, activityFilter = current.activityFilter)
        }
    }

    fun nextPage() {
        val current = _uiState.value
        goToPage(current.page + 1)
    }

    fun previousPage() {
        val current = _uiState.value
        goToPage(current.page - 1)
    }

    fun showLogDetail(log: SecurityLogItem) {
        _uiState.update { state -> state.copy(selectedLog = log) }
    }

    fun dismissLogDetail() {
        _uiState.update { state -> state.copy(selectedLog = null) }
    }

    private fun setError(message: String) {
        _uiState.update { state ->
            state.copy(isLoading = false, errorMessage = message)
        }
    }

    companion object {
        fun factory(securityLogRepository: SecurityLogRepository): ViewModelProvider.Factory {
            return viewModelFactory {
                SecurityLogViewModel(securityLogRepository = securityLogRepository)
            }
        }
    }
}

private fun SecurityLogUiState.filteredLogs(): List<SecurityLogItem> {
    if (searchQuery.isBlank()) return logs

    return logs.filter { log ->
        log.userName.contains(searchQuery, ignoreCase = true) ||
            log.userRole.contains(searchQuery, ignoreCase = true) ||
            log.activityLabel.contains(searchQuery, ignoreCase = true) ||
            log.ipAddress.contains(searchQuery, ignoreCase = true) ||
            log.deviceName.contains(searchQuery, ignoreCase = true)
    }
}

private fun AuditLog.toSecurityLogItem(): SecurityLogItem {
    val createdAtInstant = createdAt.toServerInstantOrNull()
    val type = action.toSecurityLogType()

    return SecurityLogItem(
        id = id,
        userName = actorName ?: "Sistem",
        userRole = actorRole?.uppercase() ?: "SYSTEM",
        type = type,
        activityLabel = activityLabel.ifBlank { type.label },
        ipAddress = ipAddress?.takeIf(String::isNotBlank) ?: "-",
        deviceIcon = if (ipAddress.isNullOrBlank()) Icons.Outlined.DesktopWindows else Icons.Outlined.TabletAndroid,
        deviceName = if (ipAddress.isNullOrBlank()) "Server" else "Client POS",
        time = createdAtInstant?.toClockText() ?: createdAt,
        relativeTime = createdAtInstant?.toRelativeText() ?: "-",
        compactRelativeTime = createdAtInstant?.toCompactRelativeText() ?: "-"
    )
}

private fun String.toSecurityLogType(): SecurityLogType {
    return when (uppercase()) {
        "INSERT" -> SecurityLogType.Insert
        "DELETE" -> SecurityLogType.Delete
        else -> SecurityLogType.Update
    }
}

private fun String.toServerInstantOrNull(): Instant? {
    return runCatching { Instant.parse(this) }.getOrNull()
}

private fun Instant.toClockText(): String {
    val localTime = atZone(java.time.ZoneId.systemDefault()).toLocalTime()
    return "%02d:%02d".format(localTime.hour, localTime.minute)
}

private fun Instant.toRelativeText(now: Instant = Instant.now()): String {
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

    return "${duration.toDays()} hari lalu"
}

private fun Instant.toCompactRelativeText(now: Instant = Instant.now()): String {
    val duration = Duration.between(this, now)

    if (duration.isNegative || duration.seconds < MINUTE_SECONDS) {
        return "Baru saja"
    }

    val minutes = duration.toMinutes()
    if (minutes < HOUR_MINUTES) {
        return "${minutes}m lalu"
    }

    val hours = duration.toHours()
    if (hours < DAY_HOURS) {
        return "${hours}j lalu"
    }

    return "${duration.toDays()}h lalu"
}

private const val DEFAULT_LIMIT = 50
private const val MINUTE_SECONDS = 60
private const val HOUR_MINUTES = 60
private const val DAY_HOURS = 24

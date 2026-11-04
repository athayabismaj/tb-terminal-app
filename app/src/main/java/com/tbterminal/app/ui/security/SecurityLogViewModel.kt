package com.tbterminal.app.ui.security

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.TabletAndroid
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.AuditLogItem
import com.tbterminal.app.data.remote.NetworkResult
import com.tbterminal.app.data.repository.SecurityLogRepository
import com.tbterminal.app.ui.common.viewModelFactory
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val WIB_ZONE = ZoneId.of("Asia/Jakarta")

data class SecurityLogUiState(
    val logs: List<SecurityLogItem> = emptyList(),
    val visibleLogs: List<SecurityLogItem> = emptyList(),
    val searchQuery: String = "",
    val dateFilter: SecurityDateFilter = SecurityDateFilter.Today,
    val selectedDate: String? = LocalDate.now(WIB_ZONE).toString(),
    val startDate: String? = LocalDate.now(WIB_ZONE).toString(),
    val endDate: String? = LocalDate.now(WIB_ZONE).toString(),
    val selectedPreset: String? = "Hari",
    val customDate: String? = null,
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

    fun historyShortDateLabel(): String {
        val start = startDate?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val end = (endDate ?: selectedDate)?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        if (start != null && end != null) {
            return if (start == end) {
                start.format(DateTimeFormatter.ofPattern("dd/MM"))
            } else {
                "${start.format(DateTimeFormatter.ofPattern("dd/MM"))} - ${end.format(DateTimeFormatter.ofPattern("dd/MM"))}"
            }
        }

        val raw = endDate ?: selectedDate ?: startDate
        if (raw.isNullOrBlank()) {
            return runCatching {
                LocalDate.now(WIB_ZONE).format(DateTimeFormatter.ofPattern("dd/MM"))
            }.getOrDefault("Tanggal")
        }
        return runCatching {
            LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("dd/MM"))
        }.getOrElse {
            raw.take(5)
        }
    }
}

enum class SecurityLogType(val label: String) {
    Insert("Tambah data"),
    Update("Ubah data"),
    Delete("Nonaktifkan data")
}

enum class SecurityDateFilter(
    val label: String,
    val queryValue: String?
) {
    All("Semua", null),
    Today("Hari", "today"),
    Last7Days("Minggu", "7d"),
    Last30Days("Bulan", "30d"),
    Custom("Kustom", null);

    companion object {
        val Week: SecurityDateFilter get() = Last7Days
        val Month: SecurityDateFilter get() = Last30Days
        val Day: SecurityDateFilter get() = Today
    }
}

enum class SecurityActivityFilter(
    val label: String,
    val actionQuery: String?
) {
    All("Semua aktivitas", null),
    Insert("Tambah data", "INSERT"),
    Update("Ubah data", "UPDATE"),
    Delete("Nonaktifkan data", "DELETE")
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
    val dateLabel: String = "-",
    val rawDateIso: String = "",
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

    fun loadLogs(page: Int = _uiState.value.page) {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    page = page,
                    isLoading = true,
                    errorMessage = null
                )
            }

            val todayStr = LocalDate.now(WIB_ZONE).toString()
            val isSingleToday = current.selectedPreset == "Hari" ||
                current.selectedPreset == "Hari ini" ||
                (current.selectedPreset == null && current.startDate == todayStr && current.endDate == todayStr)

            val rangeQuery = when {
                isSingleToday -> "today"
                current.selectedPreset in listOf("Minggu", "Minggu ini", "7 hari", "7 Hari") -> "7d"
                current.selectedPreset in listOf("Bulan", "Bulan ini", "30 hari", "30 Hari") -> "30d"
                current.selectedPreset == "Semua" -> null
                current.startDate != null || current.endDate != null -> null
                current.dateFilter == SecurityDateFilter.Custom -> null
                else -> current.dateFilter.queryValue
            }

            when (val result = securityLogRepository.getAuditLogs(
                page = page,
                limit = current.limit,
                action = current.activityFilter.actionQuery,
                range = rangeQuery,
                startDate = current.startDate,
                endDate = current.endDate
            )) {
                is NetworkResult.Success -> {
                    val pageData = result.data.data
                    if (pageData != null) {
                        val logs = pageData.data.map(AuditLogItem::toSecurityLogItem)
                        _uiState.update { state ->
                            val updated = state.copy(
                                logs = logs,
                                totalLogs = pageData.total,
                                page = pageData.page,
                                limit = pageData.limit,
                                totalPages = pageData.totalPages,
                                isLoading = false,
                                errorMessage = null
                            )
                            updated.copy(visibleLogs = updated.filteredLogs())
                        }
                    } else {
                        setError("Data log keamanan kosong.")
                    }
                }
                is NetworkResult.Error -> setError(result.message)
                is NetworkResult.Exception -> {
                    setError("Log keamanan gagal dimuat karena koneksi ke server bermasalah.")
                }
            }
        }
    }

    fun refresh() = loadLogs(page = _uiState.value.page)

    fun updateSearchQuery(value: String) {
        _uiState.update { state ->
            val updated = state.copy(searchQuery = value)
            updated.copy(visibleLogs = updated.filteredLogs())
        }
    }

    fun setDatePreset(preset: String) {
        val today = LocalDate.now(WIB_ZONE)
        val startDate = when (preset) {
            "Minggu", "Minggu ini", "7 hari", "7 Hari" -> today.with(DayOfWeek.MONDAY)
            "Bulan", "Bulan ini", "30 hari", "30 Hari" -> today.withDayOfMonth(1)
            "Semua" -> null
            else -> today
        }
        val endDate = if (preset == "Semua") null else today
        val filter = when (preset) {
            "Semua" -> SecurityDateFilter.All
            "Minggu", "Minggu ini", "7 hari", "7 Hari" -> SecurityDateFilter.Last7Days
            "Bulan", "Bulan ini", "30 hari", "30 Hari" -> SecurityDateFilter.Last30Days
            else -> SecurityDateFilter.Today
        }
        val normalizedPreset = when (preset) {
            "7 hari", "7 Hari", "Minggu ini" -> "Minggu"
            "30 hari", "30 Hari", "Bulan ini" -> "Bulan"
            "Hari ini" -> "Hari"
            else -> preset
        }
        setDateRange(startDate, endDate, normalizedPreset, filter)
    }

    fun setDate(date: String?) {
        val resolvedDate = date ?: LocalDate.now(WIB_ZONE).toString()
        val parsedDate = runCatching { LocalDate.parse(resolvedDate) }.getOrDefault(LocalDate.now(WIB_ZONE))
        val preset = if (resolvedDate == LocalDate.now(WIB_ZONE).toString()) "Hari" else null
        val filter = if (preset == "Hari") SecurityDateFilter.Today else SecurityDateFilter.Custom
        setDateRange(parsedDate, parsedDate, preset, filter)
    }

    fun setDateRange(
        startDate: LocalDate?,
        endDate: LocalDate?,
        preset: String?,
        filter: SecurityDateFilter = if (preset == null) SecurityDateFilter.Custom else when (preset) {
            "Semua" -> SecurityDateFilter.All
            "Minggu" -> SecurityDateFilter.Last7Days
            "Bulan" -> SecurityDateFilter.Last30Days
            else -> SecurityDateFilter.Today
        }
    ) {
        _uiState.update {
            it.copy(
                selectedDate = endDate?.toString(),
                startDate = startDate?.toString(),
                endDate = endDate?.toString(),
                selectedPreset = preset,
                dateFilter = filter,
                customDate = if (preset == null || filter == SecurityDateFilter.Custom) startDate?.toString() else null,
                page = 1
            )
        }
        loadLogs(page = 1)
    }

    fun setDateRange(startDateStr: String?, endDateStr: String?, preset: String?) {
        val start = startDateStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val end = endDateStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        setDateRange(start, end, preset)
    }

    fun updateDateFilter(filter: SecurityDateFilter, customDate: String? = null) {
        when (filter) {
            SecurityDateFilter.All -> setDatePreset("Semua")
            SecurityDateFilter.Today -> setDatePreset("Hari")
            SecurityDateFilter.Last7Days -> setDatePreset("Minggu")
            SecurityDateFilter.Last30Days -> setDatePreset("Bulan")
            SecurityDateFilter.Custom -> {
                if (customDate != null) {
                    setDate(customDate)
                } else {
                    val state = _uiState.value
                    val start = state.startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    val end = state.endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    setDateRange(start, end, null, SecurityDateFilter.Custom)
                }
            }
        }
    }

    fun updateActivityFilter(filter: SecurityActivityFilter) {
        _uiState.update { it.copy(activityFilter = filter, page = 1) }
        loadLogs(page = 1)
    }

    fun applyFilters(
        startDateStr: String?,
        endDateStr: String?,
        preset: String?,
        activityFilter: SecurityActivityFilter
    ) {
        val start = startDateStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val end = endDateStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val filter = if (preset == null) SecurityDateFilter.Custom else when (preset) {
            "Semua" -> SecurityDateFilter.All
            "Minggu" -> SecurityDateFilter.Last7Days
            "Bulan" -> SecurityDateFilter.Last30Days
            else -> SecurityDateFilter.Today
        }
        _uiState.update {
            it.copy(
                selectedDate = end?.toString(),
                startDate = start?.toString(),
                endDate = end?.toString(),
                selectedPreset = preset,
                dateFilter = filter,
                customDate = if (preset == null || filter == SecurityDateFilter.Custom) start?.toString() else null,
                activityFilter = activityFilter,
                page = 1
            )
        }
        loadLogs(page = 1)
    }

    fun goToPage(page: Int) {
        val current = _uiState.value
        val safePage = page.coerceIn(1, current.totalPages.coerceAtLeast(1))
        if (safePage != current.page) {
            loadLogs(page = safePage)
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

    fun clearError() {
        _uiState.update { state -> state.copy(errorMessage = null) }
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
    var filtered = logs

    val start = startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val end = endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    if (start != null || end != null) {
        filtered = filtered.filter { log ->
            val logDate = log.rawDateIso.takeIf { it.isNotBlank() }?.let {
                runCatching { LocalDate.parse(it) }.getOrNull()
            } ?: return@filter true

            if (start != null && logDate.isBefore(start)) return@filter false
            if (end != null && logDate.isAfter(end)) return@filter false
            true
        }
    }

    if (searchQuery.isBlank()) return filtered

    return filtered.filter { log ->
        log.userName.contains(searchQuery, ignoreCase = true) ||
            log.userRole.contains(searchQuery, ignoreCase = true) ||
            log.activityLabel.contains(searchQuery, ignoreCase = true) ||
            log.ipAddress.contains(searchQuery, ignoreCase = true) ||
            log.deviceName.contains(searchQuery, ignoreCase = true)
    }
}

private fun AuditLogItem.toSecurityLogItem(): SecurityLogItem {
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
        dateLabel = createdAtInstant?.toDateText() ?: "-",
        rawDateIso = createdAtInstant?.atZone(WIB_ZONE)?.toLocalDate()?.toString() ?: "",
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
    return runCatching { OffsetDateTime.parse(this).toInstant() }
        .getOrElse { runCatching { Instant.parse(this) }.getOrNull() }
}

private fun Instant.toClockText(): String {
    val localTime = atZone(WIB_ZONE).toLocalTime()
    return "%02d:%02d".format(localTime.hour, localTime.minute)
}

private fun Instant.toDateText(): String {
    return atZone(WIB_ZONE).format(SECURITY_DATE_FORMATTER)
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

private val SECURITY_DATE_FORMATTER = DateTimeFormatter.ofPattern(
    "dd MMM yyyy",
    Locale.forLanguageTag("id-ID"),
)
private const val DEFAULT_LIMIT = 10
private const val MINUTE_SECONDS = 60
private const val HOUR_MINUTES = 60
private const val DAY_HOURS = 24

package com.tbterminal.app.data.remote

/** One source of truth for transport errors shown to users. Business error codes stay available to ViewModels. */
internal object UserFacingErrorMapper {
    fun message(
        httpStatus: Int? = null,
        code: String? = null,
        backendMessage: String? = null,
        retryAfterSeconds: Long? = null,
    ): String {
        val normalizedCode = code?.trim()?.uppercase().orEmpty()
        val resolvedStatus = httpStatus ?: normalizedCode
            .removePrefix("HTTP_")
            .toIntOrNull()
        val waitDuration = retryAfterSeconds?.takeIf { it > 0L }?.let(::formatRetryWaitDuration)

        return when {
            normalizedCode == "NETWORK_TIMEOUT" -> "Request terlalu lama. Periksa koneksi lalu coba lagi."
            normalizedCode in NETWORK_CODES -> "Koneksi bermasalah. Periksa jaringan dan status server."
            normalizedCode in INVALID_RESPONSE_CODES -> "Respons server tidak dapat diproses. Silakan coba lagi."
            normalizedCode == "TOO_MANY_ATTEMPTS" -> {
                if (waitDuration != null) {
                    "Terlalu banyak percobaan gagal. Coba lagi dalam $waitDuration."
                } else {
                    "Terlalu banyak percobaan gagal. Tunggu sebentar lalu coba lagi."
                }
            }
            normalizedCode == "MANAGER_APPROVER_LOCKED" -> {
                if (waitDuration != null) {
                    "Terlalu banyak PIN manager salah. Persetujuan akun ini dikunci sementara. Coba lagi dalam $waitDuration."
                } else {
                    "Terlalu banyak PIN manager salah. Persetujuan akun ini dikunci sementara. Coba lagi nanti."
                }
            }
            normalizedCode == "PIN_LOCKED" || resolvedStatus == 423 -> {
                if (waitDuration != null) {
                    "PIN terkunci sementara. Coba lagi dalam $waitDuration."
                } else {
                    "PIN terkunci sementara. Tunggu sebentar lalu coba lagi."
                }
            }
            normalizedCode == "RATE_LIMITED" || normalizedCode == "TOO_MANY_REQUESTS" || resolvedStatus == 429 -> {
                if (waitDuration != null) {
                    "Terlalu banyak percobaan. Tunggu $waitDuration lalu coba lagi."
                } else {
                    "Terlalu banyak percobaan. Tunggu sebentar lalu coba lagi."
                }
            }
            resolvedStatus == 400 -> {
                if (!backendMessage.isNullOrBlank() &&
                    !backendMessage.startsWith("Format error") &&
                    !backendMessage.startsWith("Server menolak")
                ) {
                    backendMessage
                } else {
                    "Input tidak valid. Periksa kembali data yang diisi."
                }
            }
            resolvedStatus == 401 -> {
                if (!backendMessage.isNullOrBlank() &&
                    !backendMessage.startsWith("Format error") &&
                    !backendMessage.startsWith("Server menolak")
                ) {
                    backendMessage
                } else {
                    "Sesi Anda telah habis. Silakan login kembali."
                }
            }
            resolvedStatus == 403 -> "Anda tidak memiliki akses ke fitur ini."
            resolvedStatus == 404 -> "Data yang diminta tidak ditemukan."
            resolvedStatus == 409 -> "Data berubah atau sedang diproses. Muat ulang lalu coba lagi."
            resolvedStatus != null && resolvedStatus >= 500 ->
                "Server sedang mengalami gangguan. Silakan coba beberapa saat lagi."
            !backendMessage.isNullOrBlank() -> backendMessage
            else -> "Permintaan belum dapat diproses. Silakan coba lagi."
        }
    }

    internal fun formatRetryWaitDuration(seconds: Long): String {
        val safe = seconds.coerceAtLeast(1L)
        if (safe < 60L) {
            return "$safe detik"
        }
        val minutes = safe / 60L
        val remainingSeconds = safe % 60L
        return if (remainingSeconds == 0L) {
            "$minutes menit"
        } else {
            "$minutes menit $remainingSeconds detik"
        }
    }

    private val NETWORK_CODES = setOf(
        "NETWORK_UNAVAILABLE",
        "CONNECTION_FAILED",
        "CONNECTION_INTERRUPTED",
    )
    private val INVALID_RESPONSE_CODES = setOf("INVALID_RESPONSE", "EMPTY_BODY")
}

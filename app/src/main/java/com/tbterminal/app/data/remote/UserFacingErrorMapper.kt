package com.tbterminal.app.data.remote

/** One source of truth for transport errors shown to users. Business error codes stay available to ViewModels. */
internal object UserFacingErrorMapper {
    fun message(
        httpStatus: Int? = null,
        code: String? = null,
        backendMessage: String? = null,
    ): String {
        val normalizedCode = code?.trim()?.uppercase().orEmpty()
        val resolvedStatus = httpStatus ?: normalizedCode
            .removePrefix("HTTP_")
            .toIntOrNull()

        return when {
            normalizedCode == "NETWORK_TIMEOUT" -> "Request terlalu lama. Periksa koneksi lalu coba lagi."
            normalizedCode in NETWORK_CODES -> "Koneksi bermasalah. Periksa jaringan dan status server."
            normalizedCode in INVALID_RESPONSE_CODES -> "Respons server tidak dapat diproses. Silakan coba lagi."
            resolvedStatus == 400 -> "Input tidak valid. Periksa kembali data yang diisi."
            resolvedStatus == 401 -> "Sesi Anda telah habis. Silakan login kembali."
            resolvedStatus == 403 -> "Anda tidak memiliki akses ke fitur ini."
            resolvedStatus == 404 -> "Data yang diminta tidak ditemukan."
            resolvedStatus == 409 -> "Data berubah atau sedang diproses. Muat ulang lalu coba lagi."
            resolvedStatus == 429 -> "Terlalu banyak percobaan. Tunggu sebentar lalu coba lagi."
            resolvedStatus != null && resolvedStatus >= 500 ->
                "Server sedang mengalami gangguan. Silakan coba beberapa saat lagi."
            !backendMessage.isNullOrBlank() -> backendMessage
            else -> "Permintaan belum dapat diproses. Silakan coba lagi."
        }
    }

    private val NETWORK_CODES = setOf(
        "NETWORK_UNAVAILABLE",
        "CONNECTION_FAILED",
        "CONNECTION_INTERRUPTED",
    )
    private val INVALID_RESPONSE_CODES = setOf("INVALID_RESPONSE", "EMPTY_BODY")
}

package com.tbterminal.app.data.sync

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale

enum class SyncErrorCategory {
    NETWORK,
    AUTH,
    VALIDATION,
    CONFLICT,
    UNKNOWN
}

data class SyncErrorClassification(
    val category: SyncErrorCategory,
    val recommendation: String,
    val title: String? = null
)

private data class SyncConflictGuidance(
    val title: String,
    val recommendation: String
)

object SyncErrorClassifier {
    fun classify(
        httpCode: Int? = null,
        message: String? = null,
        throwable: Throwable? = null,
        errorCode: String? = null
    ): SyncErrorClassification {
        val normalizedCode = errorCode?.trim()?.uppercase(Locale.ROOT).orEmpty()
        val codeGuidance = CONFLICT_GUIDANCE_BY_CODE[normalizedCode]

        val category = when {
            codeGuidance != null || normalizedCode in OFFLINE_CONFLICT_CODES -> SyncErrorCategory.CONFLICT
            httpCode == 401 -> SyncErrorCategory.AUTH
            httpCode == 403 || httpCode == 409 -> SyncErrorCategory.CONFLICT
            httpCode == 429 || normalizedCode in RATE_LIMIT_CODES -> SyncErrorCategory.NETWORK
            httpCode != null && httpCode >= 500 -> SyncErrorCategory.NETWORK
            throwable.isNetworkError() || message.hasNetworkKeyword() -> SyncErrorCategory.NETWORK
            message.hasConflictKeyword() -> SyncErrorCategory.CONFLICT
            httpCode != null && httpCode in 400..499 -> SyncErrorCategory.VALIDATION
            else -> SyncErrorCategory.UNKNOWN
        }
        val title = codeGuidance?.title ?: if (httpCode == 403) "Akses sinkronisasi ditolak" else null
        val recommendation = when {
            codeGuidance != null -> codeGuidance.recommendation
            httpCode == 403 -> "Peran akun saat ini tidak diizinkan untuk melakukan tindakan sinkronisasi ini."
            httpCode == 429 || normalizedCode in RATE_LIMIT_CODES ->
                "Server sedang membatasi permintaan sementara. Sinkronisasi akan dicoba ulang otomatis."
            else -> category.recommendation(message)
        }
        return SyncErrorClassification(
            category = category,
            recommendation = recommendation,
            title = title
        )
    }

    fun classifyMessage(message: String?, errorCode: String? = null): SyncErrorClassification {
        return classify(message = message, errorCode = errorCode)
    }

    private fun Throwable?.isNetworkError(): Boolean {
        return this is IOException || this is SocketTimeoutException || this is UnknownHostException
    }

    private fun String?.hasNetworkKeyword(): Boolean {
        val value = normalized()
        if (value.isBlank()) return false
        return NETWORK_KEYWORDS.any { keyword -> value.contains(keyword) }
    }

    private fun String?.hasConflictKeyword(): Boolean {
        val value = normalized()
        if (value.isBlank()) return false
        return CONFLICT_KEYWORDS.any { keyword -> value.contains(keyword) }
    }

    private fun SyncErrorCategory.recommendation(message: String?): String {
        val value = message.normalized()
        return when {
            this == SyncErrorCategory.CONFLICT && (value.contains("stok") || value.contains("stock")) ->
                "Cek stok server atau lakukan koreksi stok sebelum mencoba sync ulang."
            this == SyncErrorCategory.CONFLICT && (value.contains("customer") || value.contains("pelanggan")) ->
                "Pastikan pelanggan masih valid di server, lalu coba sinkronkan ulang."
            this == SyncErrorCategory.CONFLICT && (value.contains("session") || value.contains("sesi")) ->
                "Periksa sesi kas terkait. Jika sesi server sudah ditutup, tinjau transaksi lokal sebelum retry."
            this == SyncErrorCategory.CONFLICT ->
                "Tinjau data lokal dan aturan bisnis server sebelum mencoba sync ulang."
            this == SyncErrorCategory.AUTH ->
                "Login ulang, lalu coba sinkronisasi kembali."
            this == SyncErrorCategory.NETWORK ->
                "Periksa koneksi dan status server, lalu coba lagi."
            this == SyncErrorCategory.VALIDATION ->
                "Periksa kelengkapan data lokal yang diminta server."
            else ->
                "Tinjau detail error sebelum retry."
        }
    }

    private fun String?.normalized(): String {
        return this.orEmpty().lowercase(Locale.ROOT)
    }

    private val NETWORK_KEYWORDS = listOf(
        "timeout",
        "timed out",
        "unable to resolve host",
        "failed to connect",
        "connection refused",
        "connection reset",
        "network",
        "gagal terhubung",
        "koneksi"
    )

    private val CONFLICT_KEYWORDS = listOf(
        "stok",
        "stock",
        "insufficient",
        "tidak cukup",
        "customer invalid",
        "pelanggan tidak valid",
        "customer tidak valid",
        "customer not found",
        "pelanggan tidak ditemukan",
        "session invalid",
        "sesi kas tidak valid",
        "cash session",
        "sesi sudah ditutup",
        "session already closed",
        "already closed",
        "sudah closed",
        "sudah ditutup",
        "sesi kas ditolak",
        "cash session rejected",
        "business rule",
        "rule violation",
        "referensi",
        "reference",
        "produk tidak ditemukan",
        "product not found"
    )

    private val RATE_LIMIT_CODES = setOf(
        "RATE_LIMITED",
        "TOO_MANY_REQUESTS",
    )

    private val OFFLINE_CONFLICT_CODES = setOf(
        "OFFLINE_PRICE_CONFLICT",
        "OFFLINE_ROUNDING_CONFLICT",
        "OFFLINE_TAX_CONFLICT",
        "OFFLINE_UNIT_CONFLICT",
        "OFFLINE_OCCURRED_AT_INVALID",
        "OFFLINE_OPENED_AT_INVALID",
        "OFFLINE_CLOSED_AT_INVALID",
        "FORBIDDEN_CASH_SESSION_ROLE"
    )

    private val CONFLICT_GUIDANCE_BY_CODE = mapOf(
        "OFFLINE_PRICE_CONFLICT" to SyncConflictGuidance(
            title = "Konflik harga produk",
            recommendation = "Harga transaksi offline berbeda dengan daftar harga server. Cek daftar harga terbaru lalu tinjau transaksi lokal ini."
        ),
        "OFFLINE_ROUNDING_CONFLICT" to SyncConflictGuidance(
            title = "Konflik aturan pembulatan",
            recommendation = "Nominal pembulatan tidak sesuai aturan kas server. Segarkan pengaturan toko dan tinjau nominal pembulatan transaksi ini."
        ),
        "OFFLINE_TAX_CONFLICT" to SyncConflictGuidance(
            title = "Konflik aturan pajak",
            recommendation = "Pengaturan pajak transaksi offline berbeda dengan pengaturan pajak server saat ini. Segarkan pengaturan toko lalu tinjau transaksi."
        ),
        "OFFLINE_UNIT_CONFLICT" to SyncConflictGuidance(
            title = "Konflik satuan produk",
            recommendation = "Satuan atau isi per satuan berbeda dengan server. Segarkan data produk lalu tinjau transaksi ini."
        ),
        "OFFLINE_OCCURRED_AT_INVALID" to SyncConflictGuidance(
            title = "Waktu kejadian di luar jendela sesi",
            recommendation = "Waktu transaksi atau pengeluaran berada di luar rentang sesi kas atau mendahului jam server. Pastikan jam perangkat akurat lalu coba sinkronkan lagi bila hanya selisih waktu."
        ),
        "OFFLINE_OPENED_AT_INVALID" to SyncConflictGuidance(
            title = "Waktu buka sesi kas tidak valid",
            recommendation = "Sesi kas offline melewati batas umur sinkronisasi (72 jam) atau jam perangkat terlalu cepat dari server. Tunggu jam server menyusul jika selisih waktu; jika kedaluwarsa, tinjau data lokal."
        ),
        "OFFLINE_CLOSED_AT_INVALID" to SyncConflictGuidance(
            title = "Waktu tutup sesi kas tidak valid",
            recommendation = "Waktu tutup sesi lebih awal dari pembukaan sesi atau mendahului jam server. Pastikan jam perangkat akurat lalu coba sinkronkan ulang."
        ),
        "FORBIDDEN_CASH_SESSION_ROLE" to SyncConflictGuidance(
            title = "Akses buka sesi kas ditolak",
            recommendation = "Hanya peran KASIR yang boleh membuka sesi kas. Masuk dengan akun kasir atau tandai item ini sudah ditinjau."
        )
    )
}

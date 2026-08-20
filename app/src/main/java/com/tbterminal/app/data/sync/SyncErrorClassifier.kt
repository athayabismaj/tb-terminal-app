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
    val recommendation: String
)

object SyncErrorClassifier {
    fun classify(
        httpCode: Int? = null,
        message: String? = null,
        throwable: Throwable? = null
    ): SyncErrorClassification {
        val category = when {
            httpCode == 401 || httpCode == 403 -> SyncErrorCategory.AUTH
            throwable.isNetworkError() || message.hasNetworkKeyword() -> SyncErrorCategory.NETWORK
            message.hasConflictKeyword() -> SyncErrorCategory.CONFLICT
            httpCode != null && httpCode in 400..499 -> SyncErrorCategory.VALIDATION
            else -> SyncErrorCategory.UNKNOWN
        }
        return SyncErrorClassification(
            category = category,
            recommendation = category.recommendation(message)
        )
    }

    fun classifyMessage(message: String?): SyncErrorClassification {
        return classify(message = message)
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
}

package com.tbterminal.app.ui.cashier.sync

import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.sync.CashSessionActiveBlockingMatcher
import com.tbterminal.app.data.sync.SyncErrorCategory
import com.tbterminal.app.data.sync.SyncErrorClassifier
import java.util.Locale

object CashierSyncErrorFormatter {
    fun formatReason(
        syncStatus: SyncStatus,
        lastError: String?,
        lastErrorCode: String?,
    ): String? {
        if (syncStatus == SyncStatus.SYNCED) return null
        val normalizedCode = lastErrorCode?.trim()?.uppercase(Locale.ROOT).orEmpty()
        val rawMessage = lastError?.trim().orEmpty()

        if (normalizedCode == CashSessionActiveBlockingMatcher.ERROR_CODE_ACTIVE_SESSION_BLOCKING_OPEN) {
            return "Menunggu sesi kas sebelumnya selesai ditutup di server."
        }

        KNOWN_CODE_MESSAGES[normalizedCode]?.let { return it }

        if (rawMessage.isBlank()) {
            return when (syncStatus) {
                SyncStatus.FAILED -> "Data belum bisa dikirim ke server. Coba sinkronkan ulang."
                SyncStatus.CONFLICT -> "Data berbeda dengan aturan di server. Perlu dicek admin atau pemilik."
                SyncStatus.PENDING, SyncStatus.SYNCING, SyncStatus.SYNCED -> null
            }
        }

        val classification = SyncErrorClassifier.classifyMessage(
            message = rawMessage,
            errorCode = normalizedCode.ifBlank { null },
        )

        return when (classification.category) {
            SyncErrorCategory.NETWORK ->
                "Koneksi ke server terputus saat mengirim data. Data akan dikirim ulang saat server tersambung."
            SyncErrorCategory.AUTH ->
                "Sesi login sudah berakhir. Silakan login ulang untuk melanjutkan pengiriman data."
            SyncErrorCategory.CONFLICT -> formatConflictMessage(rawMessage, classification.recommendation)
            SyncErrorCategory.VALIDATION -> cleanTechnicalText(rawMessage)
                .ifBlank { "Data tidak sesuai ketentuan server. Periksa kembali lalu coba lagi." }
            SyncErrorCategory.UNKNOWN -> cleanTechnicalText(rawMessage)
                .ifBlank { "Data belum bisa dikirim ke server. Coba sinkronkan ulang." }
        }
    }

    private fun formatConflictMessage(rawMessage: String, defaultRecommendation: String): String {
        val lower = rawMessage.lowercase(Locale.ROOT)
        return when {
            lower.contains("stok") || lower.contains("stock") || lower.contains("insufficient") ->
                "Stok barang di server tidak mencukupi. Minta admin mengecek stok sebelum kirim ulang."
            lower.contains("pelanggan") || lower.contains("customer") ->
                "Data pelanggan belum terdaftar atau berubah di server. Perlu dicek oleh admin."
            lower.contains("sesi kas") || lower.contains("cash session") || lower.contains("already closed") || lower.contains("sudah ditutup") ->
                "Sesi kas di server sudah ditutup atau berbeda. Minta admin meninjau data ini."
            lower.contains("produk") || lower.contains("product") ->
                "Produk tidak ditemukan atau berubah di server. Perlu dicek oleh admin."
            else -> defaultRecommendation
        }
    }

    private fun cleanTechnicalText(raw: String): String {
        return raw
            .replace(Regex("\\s*\\(\\d{3}\\)\\s*$"), "")
            .replace(Regex("HTTP\\s+\\d{3}", RegexOption.IGNORE_CASE), "kendala server")
            .trim()
    }

    private val KNOWN_CODE_MESSAGES = mapOf(
        "OFFLINE_PRICE_CONFLICT" to "Harga produk di server berubah. Minta admin meninjau transaksi ini.",
        "OFFLINE_ROUNDING_CONFLICT" to "Aturan pembulatan kas di server berubah. Perlu dicek admin.",
        "OFFLINE_TAX_CONFLICT" to "Aturan pajak di server berubah. Perlu dicek admin.",
        "OFFLINE_UNIT_CONFLICT" to "Satuan produk berbeda dengan data server. Perlu dicek admin.",
        "OFFLINE_OCCURRED_AT_INVALID" to "Jam perangkat tidak cocok atau waktu transaksi di luar rentang sesi kas.",
        "OFFLINE_OPENED_AT_INVALID" to "Sesi kas offline melewati batas 72 jam atau jam perangkat terlalu cepat dari server.",
        "OFFLINE_CLOSED_AT_INVALID" to "Waktu tutup sesi tidak sesuai dengan waktu buka sesi atau jam server.",
        "FORBIDDEN_CASH_SESSION_ROLE" to "Akun ini tidak memiliki izin untuk membuka sesi kas.",
        "RATE_LIMITED" to "Server sedang sibuk. Coba sinkronkan lagi beberapa saat lagi.",
        "TOO_MANY_REQUESTS" to "Server sedang sibuk. Coba sinkronkan lagi beberapa saat lagi.",
    )
}

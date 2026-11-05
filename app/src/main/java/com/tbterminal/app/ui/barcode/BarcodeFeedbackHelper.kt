package com.tbterminal.app.ui.barcode

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Helper untuk memberikan umpan balik suara (audio beep) dan getaran (haptic feedback)
 * saat barcode berhasil atau gagal dipindai oleh kasir.
 */
object BarcodeFeedbackHelper {

    @Volatile
    var isSoundEnabled: Boolean = true

    @Volatile
    var isVibrateEnabled: Boolean = true

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (_: Throwable) {
            // ToneGenerator fallback jika audio stream belum tersedia
        }
    }

    /**
     * Memutar nada beep sukses (frekuensi tinggi singkat seperti mesin kasir supermarket).
     */
    fun playSuccessBeep() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (_: Throwable) {
        }
    }

    /**
     * Memutar nada error (dua nada rendah sebagai peringatan barang tidak ditemukan).
     */
    fun playErrorBeep() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 250)
        } catch (_: Throwable) {
        }
    }

    /**
     * Memberikan getaran haptic singkat saat barcode terbaca.
     */
    fun vibrate(context: Context, durationMs: Long = 80) {
        if (!isVibrateEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        } catch (_: Throwable) {
        }
    }
}

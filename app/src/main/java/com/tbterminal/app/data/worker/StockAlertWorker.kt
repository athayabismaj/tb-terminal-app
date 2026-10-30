package com.tbterminal.app.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tbterminal.app.MainActivity
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import java.util.concurrent.TimeUnit

/**
 * Background Worker (WorkManager) yang bertugas memeriksa stok produk di database lokal
 * dan memancarkan notifikasi sistem Android saat ada barang yang menipis atau habis.
 */
class StockAlertWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "tbterminal_stock_alerts"
        const val CHANNEL_NAME = "Peringatan Stok Kritis"
        const val NOTIFICATION_ID = 2001
        const val UNIQUE_WORK_NAME = "TB_STOCK_ALERT_WORKER"
    }

    override suspend fun doWork(): Result {
        return try {
            val database = TbTerminalDatabase.getInstance(context)
            val lowStockProducts = database.productDao().getLowStockProducts()

            if (lowStockProducts.isNotEmpty()) {
                createNotificationChannel()
                sendStockNotification(lowStockProducts.size, lowStockProducts.first().name, lowStockProducts.first().stock)
            }

            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi peringatan otomatis untuk produk dengan stok kritis / menipis"
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun sendStockNotification(count: Int, sampleName: String, sampleStock: Double) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stockDisplay = if (sampleStock % 1.0 == 0.0) sampleStock.toInt().toString() else "%.1f".format(sampleStock)
        val contentText = if (count == 1) {
            "Produk \"$sampleName\" tersisa $stockDisplay. Segera lakukan restock!"
        } else {
            "\"$sampleName\" (sisa $stockDisplay) dan ${count - 1} barang lainnya menipis."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Stok Menipis ($count Barang Kritis)")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Pengguna menolak izin notifikasi di Android 13+
        }
    }
}

/**
 * Utilitas untuk mengaktifkan dan mengelola penjadwalan StockAlertWorker.
 */
object StockAlertScheduler {

    /**
     * Menjadwalkan pemeriksaan stok berkala setiap [intervalHours] jam.
     */
    fun schedulePeriodic(context: Context, intervalHours: Long = 4) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<StockAlertWorker>(intervalHours, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            StockAlertWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )
    }

    /**
     * Memeriksa stok kritis secara instan satu kali.
     */
    fun checkImmediately(context: Context) {
        val oneTimeWork = OneTimeWorkRequestBuilder<StockAlertWorker>().build()
        WorkManager.getInstance(context).enqueue(oneTimeWork)
    }
}

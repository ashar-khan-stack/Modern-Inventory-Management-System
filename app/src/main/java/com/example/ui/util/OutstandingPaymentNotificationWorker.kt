package com.example.ui.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class OutstandingPaymentNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            OutstandingPaymentNotificationManager.performSyncInternal(applicationContext)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "OutstandingPaymentNotificationWorker"

        fun scheduleWork(context: Context) {
            try {
                val appContext = context.applicationContext
                val workManager = WorkManager.getInstance(appContext)

                // Enqueue periodic work every 15 minutes
                val periodicWorkRequest = PeriodicWorkRequestBuilder<OutstandingPaymentNotificationWorker>(
                    15, TimeUnit.MINUTES
                ).build()

                workManager.enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicWorkRequest
                )

                // Enqueue immediate one-time check as well
                val oneTimeWorkRequest = OneTimeWorkRequestBuilder<OutstandingPaymentNotificationWorker>().build()
                workManager.enqueue(oneTimeWorkRequest)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

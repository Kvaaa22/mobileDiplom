package com.example.geometka.maps

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object MapDownloadScheduler {

    private const val WORK_NAME = "MapDownloadWorker"

    fun startAutomaticDownloads(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val downloadRequest = PeriodicWorkRequestBuilder<MapDownloadWorker>(
            1, TimeUnit.HOURS,
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            downloadRequest
        )
    }

    fun forceDownloadNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val downloadRequest = OneTimeWorkRequestBuilder<MapDownloadWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "MapDownloadForce",
            ExistingWorkPolicy.REPLACE,
            downloadRequest
        )
    }
}

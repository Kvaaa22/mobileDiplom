package com.example.geometka.maps

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object MapDownloadScheduler {

    private const val WORK_NAME = "MapDownloadWorker"

    fun startAutomaticDownloads(context: Context) {
        MapDownloadDiagnostics.record(
            context = context,
            stage = "Периодическая загрузка карты запланирована",
            detail = "WorkManager будет проверять назначение карты раз в час"
        )

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
        MapDownloadDiagnostics.record(
            context = context,
            stage = "Загрузка карты поставлена в очередь",
            detail = "Ожидание сети и запуска WorkManager"
        )

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

    fun stopDownloads(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork("MapDownloadForce")
    }
}

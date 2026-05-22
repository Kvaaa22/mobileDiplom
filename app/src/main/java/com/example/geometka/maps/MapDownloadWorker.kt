package com.example.geometka.maps

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MapDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        val database = MapPackageDatabase(context)
        val client = MapAssignmentClient(context)

        try {
            val assignedPackage = client.fetchAssignedPackage()
                ?: return@withContext Result.success()

            val existingPackage = database.getPackageByRemoteId(assignedPackage.remoteId)

            val finalFile = MapStorage.mapFile(
                context = context,
                remoteId = assignedPackage.remoteId,
                version = assignedPackage.version
            )

            val alreadyDownloaded =
                existingPackage?.status == MapPackageStatus.DOWNLOADED &&
                        existingPackage.version == assignedPackage.version &&
                        existingPackage.localPath != null &&
                        File(existingPackage.localPath).exists()

            if (alreadyDownloaded) {
                return@withContext Result.success()
            }

            database.upsertAssignedPackage(
                assignedPackage.copy(
                    status = MapPackageStatus.ASSIGNED
                )
            )

            database.updateStatus(
                remoteId = assignedPackage.remoteId,
                status = MapPackageStatus.DOWNLOADING
            )

            val tempFile = MapStorage.tempFile(
                context = context,
                remoteId = assignedPackage.remoteId,
                version = assignedPackage.version
            )

            client.downloadPackageToFile(
                mapPackage = assignedPackage,
                tempFile = tempFile
            )

            verifyDownloadedFile(
                mapPackage = assignedPackage,
                tempFile = tempFile
            )

            if (finalFile.exists()) {
                finalFile.delete()
            }

            val renamed = tempFile.renameTo(finalFile)
            if (!renamed) {
                throw IllegalStateException("Не удалось сохранить файл карты")
            }

            MapStorage.deleteOldVersions(
                context = context,
                currentRemoteId = assignedPackage.remoteId,
                currentVersion = assignedPackage.version
            )

            database.updateStatus(
                remoteId = assignedPackage.remoteId,
                status = MapPackageStatus.DOWNLOADED,
                localPath = finalFile.absolutePath,
                downloadedAt = System.currentTimeMillis(),
                lastError = null
            )

            Result.success()
        } catch (e: Exception) {
            val assignedPackage = runCatching {
                client.fetchAssignedPackage()
            }.getOrNull()

            if (assignedPackage != null) {
                database.updateStatus(
                    remoteId = assignedPackage.remoteId,
                    status = MapPackageStatus.FAILED,
                    lastError = e.message ?: "Неизвестная ошибка"
                )
            }

            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun verifyDownloadedFile(
        mapPackage: MapPackage,
        tempFile: File
    ) {
        if (!tempFile.exists()) {
            throw IllegalStateException("Файл карты не был загружен")
        }

        if (mapPackage.fileSizeBytes != null && tempFile.length() <= 0L) {
            throw IllegalStateException("Загруженный файл карты пустой")
        }

        val expectedChecksum = mapPackage.checksumSha256
        if (!expectedChecksum.isNullOrBlank()) {
            val actualChecksum = MapStorage.sha256(tempFile)

            if (!actualChecksum.equals(expectedChecksum, ignoreCase = true)) {
                tempFile.delete()
                throw IllegalStateException("Контрольная сумма карты не совпадает")
            }
        }
    }
}
package com.example.geometka.maps

import android.content.Context
import java.io.File

object MapDownloadRunner {

    fun download(context: Context) {
        val appContext = context.applicationContext
        val database = MapPackageDatabase(appContext)
        val client = MapAssignmentClient(appContext)
        var assignedPackage: MapPackage? = null

        try {
            MapDownloadDiagnostics.record(
                context = appContext,
                stage = "Direct map download started",
                detail = "Started from app screen"
            )

            assignedPackage = client.fetchAssignedPackage()
                ?: return

            MapDownloadDiagnostics.record(
                context = appContext,
                stage = "Map assignment parsed",
                detail = "Package ${assignedPackage.remoteId}, version ${assignedPackage.version}"
            )

            val existingPackage = database.getPackageByRemoteId(assignedPackage.remoteId)
            val finalFile = MapStorage.mapFile(
                context = appContext,
                remoteId = assignedPackage.remoteId,
                version = assignedPackage.version
            )

            val alreadyDownloaded =
                existingPackage?.status == MapPackageStatus.DOWNLOADED &&
                        existingPackage.version == assignedPackage.version &&
                        existingPackage.localPath != null &&
                        File(existingPackage.localPath).exists()

            if (alreadyDownloaded) {
                MapDownloadDiagnostics.record(
                    context = appContext,
                    stage = "Map already downloaded",
                    detail = existingPackage.localPath.orEmpty(),
                    progressPercent = 100
                )
                return
            }

            database.upsertAssignedPackage(
                assignedPackage.copy(
                    status = MapPackageStatus.ASSIGNED,
                    lastError = "Assignment received"
                )
            )

            database.updateStatus(
                remoteId = assignedPackage.remoteId,
                status = MapPackageStatus.DOWNLOADING,
                lastError = "Downloading started"
            )

            val tempFile = MapStorage.tempFile(
                context = appContext,
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

            MapDownloadDiagnostics.record(
                context = appContext,
                stage = "Map file verified",
                detail = "Temp file ${tempFile.length()} bytes",
                progressPercent = 100
            )

            if (finalFile.exists()) {
                finalFile.delete()
            }

            if (!tempFile.renameTo(finalFile)) {
                throw IllegalStateException("Could not save map file")
            }

            MapStorage.deleteOldVersions(
                context = appContext,
                currentRemoteId = assignedPackage.remoteId,
                currentVersion = assignedPackage.version
            )

            database.updateStatus(
                remoteId = assignedPackage.remoteId,
                status = MapPackageStatus.DOWNLOADED,
                localPath = finalFile.absolutePath,
                downloadedAt = System.currentTimeMillis(),
                lastError = "Downloaded successfully"
            )

            MapDownloadDiagnostics.record(
                context = appContext,
                stage = "Map downloaded successfully",
                detail = "${finalFile.absolutePath} (${finalFile.length()} bytes)",
                progressPercent = 100,
                downloadedBytes = finalFile.length(),
                totalBytes = finalFile.length()
            )
        } catch (e: Exception) {
            val message = e.message ?: e.javaClass.simpleName

            MapDownloadDiagnostics.record(
                context = appContext,
                stage = "Map download failed",
                detail = message
            )

            if (assignedPackage != null) {
                database.updateStatus(
                    remoteId = assignedPackage.remoteId,
                    status = MapPackageStatus.FAILED,
                    lastError = message
                )
            }

            throw e
        }
    }

    private fun verifyDownloadedFile(
        mapPackage: MapPackage,
        tempFile: File
    ) {
        if (!tempFile.exists()) {
            throw IllegalStateException("Downloaded map temp file does not exist")
        }

        if (mapPackage.fileSizeBytes != null && tempFile.length() <= 0L) {
            throw IllegalStateException("Downloaded map file is empty")
        }

        val expectedChecksum = mapPackage.checksumSha256
        if (!expectedChecksum.isNullOrBlank()) {
            val actualChecksum = MapStorage.sha256(tempFile)

            if (!actualChecksum.equals(expectedChecksum, ignoreCase = true)) {
                tempFile.delete()
                throw IllegalStateException("Map checksum mismatch")
            }
        }
    }
}

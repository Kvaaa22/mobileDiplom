package com.example.geometka.maps

data class MapPackage(
    val remoteId: Long,
    val name: String,
    val version: Int,
    val downloadUrl: String,
    val checksumSha256: String?,
    val fileSizeBytes: Long?,
    val localPath: String?,
    val status: MapPackageStatus,
    val downloadedAt: Long?,
    val lastError: String?
)

enum class MapPackageStatus {
    ASSIGNED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED,
    OUTDATED
}
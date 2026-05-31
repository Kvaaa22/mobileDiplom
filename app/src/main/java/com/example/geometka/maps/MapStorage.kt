package com.example.geometka.maps

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object MapStorage {

    fun mapsDir(context: Context): File {
        val externalDir = context.getExternalFilesDir("maps")
        val dir = externalDir ?: File(context.filesDir, "maps")

        if (!dir.exists()) {
            dir.mkdirs()
        }

        return dir
    }

    fun mapFile(
        context: Context,
        remoteId: Long,
        version: Int
    ): File {
        return File(mapsDir(context), "map_${remoteId}_v$version.map")
    }

    fun tempFile(
        context: Context,
        remoteId: Long,
        version: Int
    ): File {
        return File(mapsDir(context), "map_${remoteId}_v$version.map.part")
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")

        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read = input.read(buffer)

            while (read != -1) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }

        return digest.digest()
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    fun deleteOldVersions(
        context: Context,
        currentRemoteId: Long,
        currentVersion: Int
    ) {
        mapsDir(context).listFiles()?.forEach { file ->
            val currentName = "map_${currentRemoteId}_v$currentVersion.mbtiles"

            if (
                file.name.startsWith("map_") &&
                file.name.endsWith(".map") &&
                file.name != currentName
            ) {
                file.delete()
            }
        }
    }
}
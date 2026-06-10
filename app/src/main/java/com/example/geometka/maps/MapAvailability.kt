package com.example.geometka.maps

import android.content.Context
import com.example.geometka.auth.AppSession
import com.example.geometka.data.OfflineMapConfig
import java.io.File

object MapAvailability {

    fun getDownloadedMapPath(context: Context): String? {
        val assignedPath = getAssignedMapPath(context)
        if (assignedPath != null) return assignedPath

        // Заглушка для демо-режима: если есть любой файл .map, используем его
        val token = AppSession.getAccessToken(context)
        if (token == "stub_token") {
            val mapsDir = MapStorage.mapsDir(context)
            
            // 1. Ищем файл с названием из конфига
            val defaultFile = File(mapsDir, OfflineMapConfig.MAP_FILE_NAME)
            if (defaultFile.exists() && defaultFile.length() > 0L) {
                return defaultFile.absolutePath
            }

            // 2. Ищем любой файл .map
            val files = mapsDir.listFiles { _, name -> name.endsWith(".map") }
            if (!files.isNullOrEmpty()) {
                return files.sortedByDescending { it.lastModified() }.first().absolutePath
            }
        }

        return null
    }

    private fun getAssignedMapPath(context: Context): String? {
        if (!AppSession.isMapAssignmentChecked(context)) return null

        val database = MapPackageDatabase(context)
        val mapPackage = database.getDownloadedPackage()
        val path = mapPackage?.localPath

        if (path != null) {
            val file = File(path)
            if (file.exists() && file.length() > 0L) {
                return file.absolutePath
            }
        }

        return null
    }

    fun hasDownloadedMap(context: Context): Boolean {
        return getDownloadedMapPath(context) != null
    }

}

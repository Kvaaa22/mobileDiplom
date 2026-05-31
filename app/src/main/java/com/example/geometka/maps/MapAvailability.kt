package com.example.geometka.maps

import android.content.Context
import java.io.File

object MapAvailability {

    fun getDownloadedMapPath(context: Context): String? {
        val database = MapPackageDatabase(context)
        val mapPackage = database.getDownloadedPackage()
        val path = mapPackage?.localPath

        if (path != null) {
            val file = File(path)

            if (file.exists() && file.length() > 0L) {
                return file.absolutePath
            }
        }

        return findLocalDevelopmentMap(context)
    }

    fun hasDownloadedMap(context: Context): Boolean {
        return getDownloadedMapPath(context) != null
    }

    private fun findLocalDevelopmentMap(context: Context): String? {
        return MapStorage.mapsDir(context)
            .listFiles()
            ?.firstOrNull { file ->
                file.isFile &&
                        file.name.endsWith(".map", ignoreCase = true) &&
                        file.length() > 0L
            }
            ?.absolutePath
    }
}
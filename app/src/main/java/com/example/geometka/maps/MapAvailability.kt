package com.example.geometka.maps

import android.content.Context
import java.io.File

object MapAvailability {

    fun getDownloadedMapPath(context: Context): String? {
        val database = MapPackageDatabase(context)
        val mapPackage = database.getDownloadedPackage()

        val path = mapPackage?.localPath ?: return null
        val file = File(path)

        return if (file.exists()) {
            file.absolutePath
        } else {
            null
        }
    }

    fun hasDownloadedMap(context: Context): Boolean {
        return getDownloadedMapPath(context) != null
    }
}
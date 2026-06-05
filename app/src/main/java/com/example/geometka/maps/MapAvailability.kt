package com.example.geometka.maps

import android.content.Context
import com.example.geometka.auth.AppSession
import java.io.File

object MapAvailability {

    fun getDownloadedMapPath(context: Context): String? {
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

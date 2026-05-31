package com.example.geometka.helpers

import android.content.Context
import com.example.geometka.data.OfflineMapConfig
import java.io.File
import java.io.FileOutputStream

object OfflineMapFileManager {

    fun getMapsDirectory(context: Context): File {
        return File(context.filesDir, "maps").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    fun getLocalMapFile(context: Context): File {
        return File(getMapsDirectory(context), OfflineMapConfig.MAP_FILE_NAME)
    }

    fun ensureMapFile(context: Context): File {
        val targetFile = getLocalMapFile(context)

        if (targetFile.exists() && targetFile.length() > 0L) {
            return targetFile
        }

        context.assets.open(OfflineMapConfig.MAP_ASSET_PATH).use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }

        return targetFile
    }
}
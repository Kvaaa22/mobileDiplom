package com.example.geometka.helpers

import android.content.Context
import com.example.geometka.data.OfflineMapConfig
import com.example.geometka.maps.MapStorage
import java.io.File

object OfflineMapFileManager {

    fun getMapFileOrNull(context: Context): File? {
        val file = File(MapStorage.mapsDir(context), OfflineMapConfig.MAP_FILE_NAME)
        return if (file.exists()) file else null
    }

    fun getExpectedMapPath(context: Context): String {
        return File(MapStorage.mapsDir(context), OfflineMapConfig.MAP_FILE_NAME).absolutePath
    }

    fun ensureMapFile(context: Context): File {
        val file = File(MapStorage.mapsDir(context), OfflineMapConfig.MAP_FILE_NAME)
        if (!file.exists()) {
            // В реальном приложении здесь может быть копирование из assets
            // или другой механизм инициализации.
        }
        return file
    }
}

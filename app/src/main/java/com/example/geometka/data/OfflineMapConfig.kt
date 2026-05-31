package com.example.geometka.data

object OfflineMapConfig {
    const val DEFAULT_MAP_ID: Long = 1L

    const val MAP_FILE_NAME: String = "fire_area.map"

    const val MAP_TITLE: String = "Карта пожара"
    const val MAP_SUBTITLE_ONLINE: String = "Офлайн-карта доступна"
    const val MAP_SUBTITLE_OFFLINE: String = "Карта не загружена · используется схема точек"

    // Эти координаты нужны только как запасной центр,
    // если файл карты не прочитан и точек еще нет.
    const val DEFAULT_CENTER_LAT: Double = 56.0106
    const val DEFAULT_CENTER_LON: Double = 92.8526
    const val DEFAULT_ZOOM: Byte = 13
}
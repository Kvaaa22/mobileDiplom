package com.example.geometka.data

object OfflineMapConfig {
    const val DEFAULT_MAP_ID: Long = 1L

    const val MAP_ASSET_PATH: String = "maps/fire_area.map"
    const val MAP_FILE_NAME: String = "fire_area.map"

    const val MAP_TITLE: String = "Карта пожара"
    const val MAP_SUBTITLE: String = "Квартал 47 · связь нестабильна"

    // Поменяй координаты под свой файл карты.
    // Сейчас стоит Красноярск как демонстрационный центр.
    const val DEFAULT_CENTER_LAT: Double = 56.0106
    const val DEFAULT_CENTER_LON: Double = 92.8526
    const val DEFAULT_ZOOM: Byte = 13
}
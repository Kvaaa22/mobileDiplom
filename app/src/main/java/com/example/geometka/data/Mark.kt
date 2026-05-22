package com.example.geometka.data

import java.text.SimpleDateFormat
import java.util.*

data class Mark(
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,

    val pointType: PointType,
    val intensity: FireIntensity,
    val typeOfFire: FireType,

    val notes: String?, // Дополнительные заметки
    val createdAt: Long = System.currentTimeMillis(),  // Время создания
    val horizontalAccuracyMeters: Float?,
    val syncStatus: SyncStatus = SyncStatus.LOCAL
) {
    // Форматированная дата
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(createdAt))
    }

    // Координаты в читаемом виде
    fun getFormattedCoordinates(): String {
        return "%.6f, %.6f".format(latitude, longitude)
    }
}

// Константы для выбора значений
enum class PointType(val label: String) {
    FRONT("Фронт"),
    FLANK("Фланг"),
    REAR("Тыл")
}

enum class FireIntensity(val label: String) {
    LOW("Слабая"),
    MEDIUM("Средняя"),
    HIGH("Высокая")
}

enum class FireType(val label: String) {
    GROUND("Низовой"),
    CROWN("Верховой"),
    PEAT("Торфяной")
}

enum class SyncStatus {
    LOCAL,
    SYNCED,
    PENDING
}
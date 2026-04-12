package com.example.geometka.data

import java.text.SimpleDateFormat
import java.util.*

data class Mark(
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val objectType: String,           // Тип объекта
    val fireHazardClass: String,       // Класс пожарной опасности
    val waterAvailability: String,     // Доступность воды
    val vehiclePassability: String,    // Проходимость техники
    val notes: String,                 // Дополнительные заметки
    val createdAt: Long = System.currentTimeMillis(),  // Время создания
    val provider: String = "unknown"   // GPS/Network
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
object MarkConstants {
    val OBJECT_TYPES = listOf(
        "🌳 Дерево",
        "🌿 Куст",
        "💧 Водоем",
        "🌾 Опушка",
        "🛣️ Дорога",
        "🏚️ Строение",
        "⚠️ Опасность",
        "📍 Другое"
    )

    val FIRE_HAZARD_CLASSES = listOf(
        "🟢 Низкий",
        "🟡 Средний",
        "🟠 Высокий",
        "🔴 Критический"
    )

    val WATER_AVAILABILITY = listOf(
        "✅ Рядом (до 50м)",
        "🟡 Средне (50-200м)",
        "🔴 Далеко (>200м)",
        "❌ Нет доступа"
    )

    val VEHICLE_PASSABILITY = listOf(
        "✅ Легкая проходимость",
        "🟡 Средняя проходимость",
        "🟠 Тяжелая проходимость",
        "❌ Непроходимо"
    )
}
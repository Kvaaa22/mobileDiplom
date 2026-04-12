package com.example.geometka.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MarkDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "geometka.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_MARKS = "marks"

        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_LATITUDE = "latitude"
        private const val COLUMN_LONGITUDE = "longitude"
        private const val COLUMN_OBJECT_TYPE = "object_type"
        private const val COLUMN_FIRE_HAZARD = "fire_hazard"
        private const val COLUMN_WATER_AVAILABILITY = "water_availability"
        private const val COLUMN_VEHICLE_PASSABILITY = "vehicle_passability"
        private const val COLUMN_NOTES = "notes"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val COLUMN_PROVIDER = "provider"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_MARKS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_LATITUDE REAL NOT NULL,
                $COLUMN_LONGITUDE REAL NOT NULL,
                $COLUMN_OBJECT_TYPE TEXT NOT NULL,
                $COLUMN_FIRE_HAZARD TEXT NOT NULL,
                $COLUMN_WATER_AVAILABILITY TEXT NOT NULL,
                $COLUMN_VEHICLE_PASSABILITY TEXT NOT NULL,
                $COLUMN_NOTES TEXT,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                $COLUMN_PROVIDER TEXT
            )
        """.trimIndent()

        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MARKS")
        onCreate(db)
    }

    // Добавить метку
    fun insertMark(mark: Mark): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, mark.name)
            put(COLUMN_LATITUDE, mark.latitude)
            put(COLUMN_LONGITUDE, mark.longitude)
            put(COLUMN_OBJECT_TYPE, mark.objectType)
            put(COLUMN_FIRE_HAZARD, mark.fireHazardClass)
            put(COLUMN_WATER_AVAILABILITY, mark.waterAvailability)
            put(COLUMN_VEHICLE_PASSABILITY, mark.vehiclePassability)
            put(COLUMN_NOTES, mark.notes)
            put(COLUMN_CREATED_AT, mark.createdAt)
            put(COLUMN_PROVIDER, mark.provider)
        }

        return db.insert(TABLE_MARKS, null, values)
    }

    // Получить все метки
    fun getAllMarks(): List<Mark> {
        val marks = mutableListOf<Mark>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MARKS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_CREATED_AT DESC"  // Сортировка по дате (новые первые)
        )

        with(cursor) {
            while (moveToNext()) {
                val mark = Mark(
                    id = getLong(getColumnIndexOrThrow(COLUMN_ID)),
                    name = getString(getColumnIndexOrThrow(COLUMN_NAME)),
                    latitude = getDouble(getColumnIndexOrThrow(COLUMN_LATITUDE)),
                    longitude = getDouble(getColumnIndexOrThrow(COLUMN_LONGITUDE)),
                    objectType = getString(getColumnIndexOrThrow(COLUMN_OBJECT_TYPE)),
                    fireHazardClass = getString(getColumnIndexOrThrow(COLUMN_FIRE_HAZARD)),
                    waterAvailability = getString(getColumnIndexOrThrow(COLUMN_WATER_AVAILABILITY)),
                    vehiclePassability = getString(getColumnIndexOrThrow(COLUMN_VEHICLE_PASSABILITY)),
                    notes = getString(getColumnIndexOrThrow(COLUMN_NOTES)) ?: "",
                    createdAt = getLong(getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                    provider = getString(getColumnIndexOrThrow(COLUMN_PROVIDER)) ?: "unknown"
                )
                marks.add(mark)
            }
        }
        cursor.close()

        return marks
    }

    // Получить метку по ID
    fun getMarkById(id: Long): Mark? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MARKS,
            null,
            "$COLUMN_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )

        var mark: Mark? = null
        if (cursor.moveToFirst()) {
            mark = Mark(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)),
                longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE)),
                objectType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OBJECT_TYPE)),
                fireHazardClass = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRE_HAZARD)),
                waterAvailability = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WATER_AVAILABILITY)),
                vehiclePassability = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VEHICLE_PASSABILITY)),
                notes = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES)) ?: "",
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                provider = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROVIDER)) ?: "unknown"
            )
        }
        cursor.close()

        return mark
    }

    // Обновить метку
    fun updateMark(mark: Mark): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, mark.name)
            put(COLUMN_LATITUDE, mark.latitude)
            put(COLUMN_LONGITUDE, mark.longitude)
            put(COLUMN_OBJECT_TYPE, mark.objectType)
            put(COLUMN_FIRE_HAZARD, mark.fireHazardClass)
            put(COLUMN_WATER_AVAILABILITY, mark.waterAvailability)
            put(COLUMN_VEHICLE_PASSABILITY, mark.vehiclePassability)
            put(COLUMN_NOTES, mark.notes)
            put(COLUMN_PROVIDER, mark.provider)
        }

        return db.update(TABLE_MARKS, values, "$COLUMN_ID = ?", arrayOf(mark.id.toString()))
    }

    // Удалить метку
    fun deleteMark(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_MARKS, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    // Получить количество меток
    fun getMarksCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_MARKS", null)
        cursor.moveToFirst()
        val count = cursor.getInt(0)
        cursor.close()
        return count
    }
}
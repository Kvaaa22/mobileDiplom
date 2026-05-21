package com.example.geometka.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MarkDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "geometka.db"
        // Увеличили версию с 1 на 2, чтобы сработало onUpgrade
        private const val DATABASE_VERSION = 2

        private const val TABLE_MARKS = "marks"

        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_LATITUDE = "latitude"
        private const val COLUMN_LONGITUDE = "longitude"
        private const val COLUMN_POINT_TYPE = "point_type"
        private const val COLUMN_INTENSITY = "intensity"
        private const val COLUMN_FIRE_TYPE = "type_of_fire"
        private const val COLUMN_NOTES = "notes"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val COLUMN_ACCURACY = "horizontal_accuracy_meters"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_MARKS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_LATITUDE REAL NOT NULL,
                $COLUMN_LONGITUDE REAL NOT NULL,
                $COLUMN_POINT_TYPE TEXT NOT NULL,
                $COLUMN_INTENSITY TEXT NOT NULL,
                $COLUMN_FIRE_TYPE TEXT NOT NULL,
                $COLUMN_NOTES TEXT,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                $COLUMN_ACCURACY REAL
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // При любом обновлении версии сносим старую таблицу и создаем новую с верными колонками
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MARKS")
        onCreate(db)
    }

    fun insertMark(mark: Mark): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, mark.name)
            put(COLUMN_LATITUDE, mark.latitude)
            put(COLUMN_LONGITUDE, mark.longitude)
            put(COLUMN_POINT_TYPE, mark.pointType.name)
            put(COLUMN_INTENSITY, mark.intensity.name)
            put(COLUMN_FIRE_TYPE, mark.typeOfFire.name)
            put(COLUMN_NOTES, mark.notes)
            put(COLUMN_CREATED_AT, mark.createdAt)
            put(COLUMN_ACCURACY, mark.horizontalAccuracyMeters)
        }
        return db.insert(TABLE_MARKS, null, values)
    }

    fun getAllMarks(): List<Mark> {
        val marks = mutableListOf<Mark>()
        val db = readableDatabase
        val cursor = db.query(TABLE_MARKS, null, null, null, null, null, "$COLUMN_CREATED_AT DESC")

        cursor.use {
            while (it.moveToNext()) {
                marks.add(cursorToMark(it))
            }
        }
        return marks
    }

    fun getMarkById(id: Long): Mark? {
        val db = readableDatabase
        val cursor = db.query(TABLE_MARKS, null, "$COLUMN_ID = ?", arrayOf(id.toString()), null, null, null)
        cursor.use {
            return if (it.moveToFirst()) cursorToMark(it) else null
        }
    }

    private fun cursorToMark(cursor: Cursor): Mark {
        return Mark(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
            latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)),
            longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE)),
            pointType = enumValueOrDefault(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POINT_TYPE)), PointType.FRONT),
            intensity = enumValueOrDefault(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INTENSITY)), FireIntensity.LOW),
            typeOfFire = enumValueOrDefault(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRE_TYPE)), FireType.GROUND),
            notes = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES)),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
            horizontalAccuracyMeters = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_ACCURACY))) null else cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_ACCURACY))
        )
    }

    fun updateMark(mark: Mark): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, mark.name)
            put(COLUMN_LATITUDE, mark.latitude)
            put(COLUMN_LONGITUDE, mark.longitude)
            put(COLUMN_POINT_TYPE, mark.pointType.name)
            put(COLUMN_INTENSITY, mark.intensity.name)
            put(COLUMN_FIRE_TYPE, mark.typeOfFire.name)
            put(COLUMN_NOTES, mark.notes)
            put(COLUMN_ACCURACY, mark.horizontalAccuracyMeters)
        }
        return db.update(TABLE_MARKS, values, "$COLUMN_ID = ?", arrayOf(mark.id.toString()))
    }

    fun deleteMark(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_MARKS, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, defaultValue: T): T {
        if (value == null) return defaultValue
        return try { enumValueOf<T>(value) } catch (e: Exception) { defaultValue }
    }
}

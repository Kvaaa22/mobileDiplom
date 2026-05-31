package com.example.geometka.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteOpenHelper

class MarkDatabase(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "geometka.db"
        private const val DATABASE_VERSION = 4

        private const val TABLE_MARKS = "marks"

        private const val COLUMN_ID = "id"
        private const val COLUMN_MAP_ID = "map_id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_LATITUDE = "latitude"
        private const val COLUMN_LONGITUDE = "longitude"
        private const val COLUMN_POINT_TYPE = "point_type"
        private const val COLUMN_INTENSITY = "intensity"
        private const val COLUMN_FIRE_TYPE = "type_of_fire"
        private const val COLUMN_NOTES = "notes"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val COLUMN_ACCURACY = "horizontal_accuracy_meters"
        private const val COLUMN_SYNC_STATUS = "sync_status"
    }

    override fun onCreate(db: android.database.sqlite.SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_MARKS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_MAP_ID INTEGER,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_LATITUDE REAL NOT NULL,
                $COLUMN_LONGITUDE REAL NOT NULL,
                $COLUMN_POINT_TYPE TEXT NOT NULL,
                $COLUMN_INTENSITY TEXT NOT NULL,
                $COLUMN_FIRE_TYPE TEXT NOT NULL,
                $COLUMN_NOTES TEXT,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                $COLUMN_ACCURACY REAL,
                $COLUMN_SYNC_STATUS TEXT NOT NULL
            )
        """.trimIndent()

        db.execSQL(createTable)
    }

    override fun onUpgrade(
        db: android.database.sqlite.SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MARKS")
        onCreate(db)
    }

    fun insertMark(mark: Mark): Long {
        val db = writableDatabase

        val values = ContentValues().apply {
            if (mark.mapId != null) {
                put(COLUMN_MAP_ID, mark.mapId)
            } else {
                putNull(COLUMN_MAP_ID)
            }

            put(COLUMN_NAME, mark.name)
            put(COLUMN_LATITUDE, mark.latitude)
            put(COLUMN_LONGITUDE, mark.longitude)
            put(COLUMN_POINT_TYPE, mark.pointType.name)
            put(COLUMN_INTENSITY, mark.intensity.name)
            put(COLUMN_FIRE_TYPE, mark.typeOfFire.name)
            put(COLUMN_NOTES, mark.notes)
            put(COLUMN_CREATED_AT, mark.createdAt)

            if (mark.horizontalAccuracyMeters != null) {
                put(COLUMN_ACCURACY, mark.horizontalAccuracyMeters)
            } else {
                putNull(COLUMN_ACCURACY)
            }

            put(COLUMN_SYNC_STATUS, mark.syncStatus.name)
        }

        return db.insert(TABLE_MARKS, null, values)
    }

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
            "$COLUMN_CREATED_AT DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                marks.add(cursorToMark(it))
            }
        }

        return marks
    }

    fun getMarksByMapId(mapId: Long): List<Mark> {
        val marks = mutableListOf<Mark>()
        val db = readableDatabase

        val cursor = db.query(
            TABLE_MARKS,
            null,
            "$COLUMN_MAP_ID = ?",
            arrayOf(mapId.toString()),
            null,
            null,
            "$COLUMN_CREATED_AT DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                marks.add(cursorToMark(it))
            }
        }

        return marks
    }

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

        cursor.use {
            return if (it.moveToFirst()) {
                cursorToMark(it)
            } else {
                null
            }
        }
    }

    fun updateMark(mark: Mark): Int {
        val db = writableDatabase

        val values = ContentValues().apply {
            if (mark.mapId != null) {
                put(COLUMN_MAP_ID, mark.mapId)
            } else {
                putNull(COLUMN_MAP_ID)
            }

            put(COLUMN_NAME, mark.name)
            put(COLUMN_LATITUDE, mark.latitude)
            put(COLUMN_LONGITUDE, mark.longitude)
            put(COLUMN_POINT_TYPE, mark.pointType.name)
            put(COLUMN_INTENSITY, mark.intensity.name)
            put(COLUMN_FIRE_TYPE, mark.typeOfFire.name)
            put(COLUMN_NOTES, mark.notes)

            if (mark.horizontalAccuracyMeters != null) {
                put(COLUMN_ACCURACY, mark.horizontalAccuracyMeters)
            } else {
                putNull(COLUMN_ACCURACY)
            }

            put(COLUMN_SYNC_STATUS, mark.syncStatus.name)
        }

        return db.update(
            TABLE_MARKS,
            values,
            "$COLUMN_ID = ?",
            arrayOf(mark.id.toString())
        )
    }

    fun deleteMark(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_MARKS, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    private fun cursorToMark(cursor: Cursor): Mark {
        return Mark(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            mapId = cursor.getNullableLong(COLUMN_MAP_ID),
            name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
            latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)),
            longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE)),
            pointType = enumValueOrDefault(
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POINT_TYPE)),
                PointType.FRONT
            ),
            intensity = enumValueOrDefault(
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INTENSITY)),
                FireIntensity.LOW
            ),
            typeOfFire = enumValueOrDefault(
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRE_TYPE)),
                FireType.GROUND
            ),
            notes = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES)),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
            horizontalAccuracyMeters = cursor.getNullableFloat(COLUMN_ACCURACY),
            syncStatus = enumValueOrDefault(
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SYNC_STATUS)),
                SyncStatus.LOCAL
            )
        )
    }

    private fun Cursor.getNullableLong(columnName: String): Long? {
        val index = getColumnIndex(columnName)
        if (index == -1 || isNull(index)) return null
        return getLong(index)
    }

    private fun Cursor.getNullableFloat(columnName: String): Float? {
        val index = getColumnIndex(columnName)
        if (index == -1 || isNull(index)) return null
        return getFloat(index)
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        value: String?,
        defaultValue: T
    ): T {
        if (value == null) return defaultValue

        return try {
            enumValueOf<T>(value)
        } catch (_: Exception) {
            defaultValue
        }
    }
}
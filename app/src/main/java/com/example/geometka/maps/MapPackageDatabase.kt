package com.example.geometka.maps

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MapPackageDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "map_packages.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_MAP_PACKAGES = "map_packages"

        private const val COLUMN_REMOTE_ID = "remote_id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_VERSION = "version"
        private const val COLUMN_DOWNLOAD_URL = "download_url"
        private const val COLUMN_CHECKSUM_SHA256 = "checksum_sha256"
        private const val COLUMN_FILE_SIZE_BYTES = "file_size_bytes"
        private const val COLUMN_LOCAL_PATH = "local_path"
        private const val COLUMN_STATUS = "status"
        private const val COLUMN_DOWNLOADED_AT = "downloaded_at"
        private const val COLUMN_LAST_ERROR = "last_error"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_MAP_PACKAGES (
                $COLUMN_REMOTE_ID INTEGER PRIMARY KEY,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_VERSION INTEGER NOT NULL,
                $COLUMN_DOWNLOAD_URL TEXT NOT NULL,
                $COLUMN_CHECKSUM_SHA256 TEXT,
                $COLUMN_FILE_SIZE_BYTES INTEGER,
                $COLUMN_LOCAL_PATH TEXT,
                $COLUMN_STATUS TEXT NOT NULL,
                $COLUMN_DOWNLOADED_AT INTEGER,
                $COLUMN_LAST_ERROR TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MAP_PACKAGES")
        onCreate(db)
    }

    fun upsertAssignedPackage(mapPackage: MapPackage) {
        val db = writableDatabase

        val values = ContentValues().apply {
            put(COLUMN_REMOTE_ID, mapPackage.remoteId)
            put(COLUMN_NAME, mapPackage.name)
            put(COLUMN_VERSION, mapPackage.version)
            put(COLUMN_DOWNLOAD_URL, mapPackage.downloadUrl)
            put(COLUMN_CHECKSUM_SHA256, mapPackage.checksumSha256)
            put(COLUMN_FILE_SIZE_BYTES, mapPackage.fileSizeBytes)
            put(COLUMN_LOCAL_PATH, mapPackage.localPath)
            put(COLUMN_STATUS, mapPackage.status.name)
            put(COLUMN_DOWNLOADED_AT, mapPackage.downloadedAt)
            put(COLUMN_LAST_ERROR, mapPackage.lastError)
        }

        db.insertWithOnConflict(
            TABLE_MAP_PACKAGES,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun getPackageByRemoteId(remoteId: Long): MapPackage? {
        val db = readableDatabase

        val cursor = db.query(
            TABLE_MAP_PACKAGES,
            null,
            "$COLUMN_REMOTE_ID = ?",
            arrayOf(remoteId.toString()),
            null,
            null,
            null
        )

        cursor.use {
            return if (it.moveToFirst()) {
                cursorToMapPackage(it)
            } else {
                null
            }
        }
    }

    fun getDownloadedPackage(): MapPackage? {
        val db = readableDatabase

        val cursor = db.query(
            TABLE_MAP_PACKAGES,
            null,
            "$COLUMN_STATUS = ?",
            arrayOf(MapPackageStatus.DOWNLOADED.name),
            null,
            null,
            "$COLUMN_DOWNLOADED_AT DESC",
            "1"
        )

        cursor.use {
            return if (it.moveToFirst()) {
                cursorToMapPackage(it)
            } else {
                null
            }
        }
    }

    fun updateStatus(
        remoteId: Long,
        status: MapPackageStatus,
        localPath: String? = null,
        downloadedAt: Long? = null,
        lastError: String? = null
    ) {
        val db = writableDatabase

        val values = ContentValues().apply {
            put(COLUMN_STATUS, status.name)

            if (localPath != null) {
                put(COLUMN_LOCAL_PATH, localPath)
            }

            if (downloadedAt != null) {
                put(COLUMN_DOWNLOADED_AT, downloadedAt)
            }

            if (lastError != null) {
                put(COLUMN_LAST_ERROR, lastError)
            }
        }

        db.update(
            TABLE_MAP_PACKAGES,
            values,
            "$COLUMN_REMOTE_ID = ?",
            arrayOf(remoteId.toString())
        )
    }

    private fun cursorToMapPackage(cursor: Cursor): MapPackage {
        return MapPackage(
            remoteId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_REMOTE_ID)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
            version = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_VERSION)),
            downloadUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DOWNLOAD_URL)),
            checksumSha256 = cursor.getNullableString(COLUMN_CHECKSUM_SHA256),
            fileSizeBytes = cursor.getNullableLong(COLUMN_FILE_SIZE_BYTES),
            localPath = cursor.getNullableString(COLUMN_LOCAL_PATH),
            status = enumValueOrDefault(
                value = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS)),
                defaultValue = MapPackageStatus.ASSIGNED
            ),
            downloadedAt = cursor.getNullableLong(COLUMN_DOWNLOADED_AT),
            lastError = cursor.getNullableString(COLUMN_LAST_ERROR)
        )
    }

    private fun Cursor.getNullableString(columnName: String): String? {
        val index = getColumnIndexOrThrow(columnName)
        return if (isNull(index)) null else getString(index)
    }

    private fun Cursor.getNullableLong(columnName: String): Long? {
        val index = getColumnIndexOrThrow(columnName)
        return if (isNull(index)) null else getLong(index)
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        value: String?,
        defaultValue: T
    ): T {
        return if (value == null) {
            defaultValue
        } else {
            runCatching { enumValueOf<T>(value) }.getOrDefault(defaultValue)
        }
    }
}
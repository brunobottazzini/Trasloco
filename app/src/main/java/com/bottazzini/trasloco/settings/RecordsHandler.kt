package com.bottazzini.trasloco.settings

import android.content.ContentValues
import android.content.Context
import com.bottazzini.trasloco.db.DatabaseHandler
import com.bottazzini.trasloco.db.columns.RecordsColumns.RecordEntry

enum class Type(val value: String) {
    TIME("time"),
    CONSECUTIVE("consecutive")
}

class RecordsHandler(context: Context) {
    private val dbHandler = DatabaseHandler(context)

    fun insertDefaultSettings() {
        setDefaultSetting(Type.TIME, -1)
        setDefaultSetting(Type.CONSECUTIVE, 0)
    }

    fun update(type: Type, value: Long, currentValue: Long, isNew: Boolean) {
        delete(type)
        insert(type, value, currentValue, isNew)
    }

    fun readNew(type: Type): Boolean? {
        val db = dbHandler.readableDatabase
        val projection = arrayOf(RecordEntry.COLUMN_TYPE, RecordEntry.COLUMN_NEW)
        val selection = "${RecordEntry.COLUMN_TYPE} = ?"
        val selectionArgs = arrayOf(type.value)

        val cursor = db.query(
            RecordEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        if (cursor.count > 0) {
            cursor.moveToFirst()
            val index = cursor.getColumnIndexOrThrow(RecordEntry.COLUMN_NEW)
            val value = cursor.getLong(index)
            cursor.close()

            return value == 1L
        }

        return null
    }

    fun readCurrentValue(type: Type): Long? {
        val db = dbHandler.readableDatabase
        val projection = arrayOf(RecordEntry.COLUMN_TYPE, RecordEntry.COLUMN_CURRENT_VALUE)
        val selection = "${RecordEntry.COLUMN_TYPE} = ?"
        val selectionArgs = arrayOf(type.value)

        val cursor = db.query(
            RecordEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        if (cursor.count > 0) {
            cursor.moveToFirst()
            val index = cursor.getColumnIndexOrThrow(RecordEntry.COLUMN_CURRENT_VALUE)
            val value = cursor.getLong(index)
            cursor.close()

            return value
        }

        return null
    }

    fun readValue(type: Type): Long? {
        val db = dbHandler.readableDatabase
        val projection = arrayOf(RecordEntry.COLUMN_TYPE, RecordEntry.COLUMN_VALUE)
        val selection = "${RecordEntry.COLUMN_TYPE} = ?"
        val selectionArgs = arrayOf(type.value)

        val cursor = db.query(
            RecordEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        if (cursor.count > 0) {
            cursor.moveToFirst()
            val index = cursor.getColumnIndexOrThrow(RecordEntry.COLUMN_VALUE)
            val value = cursor.getLong(index)
            cursor.close()

            return value
        }

        return null
    }

    fun close() {
        dbHandler.readableDatabase.close()
    }

    private fun delete(type: Type) {
        val db = dbHandler.writableDatabase

        val selection = "${RecordEntry.COLUMN_TYPE} = ?"
        val selectionArgs = arrayOf(type.value)

        db.delete(RecordEntry.TABLE_NAME, selection, selectionArgs)
    }

    private fun insert(type: Type, value: Long, currentValue: Long, isNew: Boolean) {
        val db = dbHandler.writableDatabase
        val new = if(isNew) 1 else 0
        val values = ContentValues().apply {
            put(RecordEntry.COLUMN_TYPE, type.value)
            put(RecordEntry.COLUMN_VALUE, value)
            put(RecordEntry.COLUMN_NEW, new)
            put(RecordEntry.COLUMN_CURRENT_VALUE, currentValue)
        }

        db?.insert(RecordEntry.TABLE_NAME, null, values)
    }

    private fun setDefaultSetting(key: Type, value: Long) {
        val readValue = readValue(key)
        if (readValue == null) {
            insert(key, value, 0L, false)
        }
    }
}
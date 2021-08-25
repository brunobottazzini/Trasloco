package com.bottazzini.trasloco.settings

import android.content.ContentValues
import android.content.Context
import com.bottazzini.trasloco.db.DatabaseHandler
import com.bottazzini.trasloco.db.columns.SettingsBaseColumns.SettingEntry

enum class Configuration(val value: String) {
    FAST_DEAL("fastDeal"),
    CARD_BACK("cardBack"),
    BACKGROUND("background")
}

class SettingsHandler(context: Context) {

    private val dbHandler = DatabaseHandler(context)

    fun insertDefaultSettings() {
        setDefaultSetting(Configuration.FAST_DEAL.value, "enabled")
        setDefaultSetting(Configuration.CARD_BACK.value, "bg2")
        setDefaultSetting(Configuration.BACKGROUND.value, "legno")
    }

    fun close() {
        dbHandler.readableDatabase.close()
    }

    fun updateSetting(name: String, value: String) {
        deleteSetting(name)
        insertSetting(name, value)
    }

    fun readValue(name: String): String? {
        val db = dbHandler.readableDatabase
        val projection = arrayOf(SettingEntry.COLUMN_NAME, SettingEntry.COLUMN_VALUE)
        val selection = "${SettingEntry.COLUMN_NAME} = ?"
        val selectionArgs = arrayOf(name)

        val cursor = db.query(
            SettingEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        if (cursor.count > 0) {
            cursor.moveToFirst()
            val index = cursor.getColumnIndexOrThrow(SettingEntry.COLUMN_VALUE)
            val value = cursor.getString(index)
            cursor.close()

            return value
        }

        return null
    }

    private fun deleteSetting(name: String) {
        val db = dbHandler.writableDatabase

        val selection = "${SettingEntry.COLUMN_NAME} = ?"
        val selectionArgs = arrayOf(name)

        db.delete(SettingEntry.TABLE_NAME, selection, selectionArgs)
    }

    private fun insertSetting(name: String, value: String) {
        val db = dbHandler.writableDatabase
        val values = ContentValues().apply {
            put(SettingEntry.COLUMN_NAME, name)
            put(SettingEntry.COLUMN_VALUE, value)
        }

        db?.insert(SettingEntry.TABLE_NAME, null, values)
    }

    private fun setDefaultSetting(key: String, value: String) {
        val readValue = readValue(key)
        if (readValue == null) {
            insertSetting(key, value)
        }
    }
}
package com.bottazzini.trasloco.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.bottazzini.trasloco.db.columns.SettingsBaseColumns.SettingEntry

class DatabaseHandler(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "Trasloco.db"
        private const val SQL_CREATE_ENTRIES =
            "CREATE TABLE IF NOT EXISTS ${SettingEntry.TABLE_NAME} (${SettingEntry.COLUMN_NAME} TEXT," +
                    "${SettingEntry.COLUMN_VALUE} TEXT)"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }
}
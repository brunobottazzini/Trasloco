package com.bottazzini.trasloco.db

import android.content.ContentValues.TAG
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.bottazzini.trasloco.db.columns.RecordsColumns.RecordEntry
import com.bottazzini.trasloco.db.columns.SettingsBaseColumns.SettingEntry

class DatabaseHandler(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_VERSION = 2
        private const val DATABASE_NAME = "Trasloco.db"
        private const val SQL_CREATE_SETTINGS =
            "CREATE TABLE IF NOT EXISTS ${SettingEntry.TABLE_NAME} (${SettingEntry.COLUMN_NAME} TEXT," +
                    "${SettingEntry.COLUMN_VALUE} TEXT)"
        private const val SQL_CREATE_RECORDS =
            "CREATE TABLE IF NOT EXISTS ${RecordEntry.TABLE_NAME} (${RecordEntry.COLUMN_TYPE} TEXT," +
                    "${RecordEntry.COLUMN_VALUE} INTEGER, ${RecordEntry.COLUMN_NEW} INTEGER, ${RecordEntry.COLUMN_CURRENT_VALUE} INTEGER)"

        private const val SQL_DELETE_SETTINGS = "DROP TABLE IF EXISTS ${SettingEntry.TABLE_NAME}"
        private const val SQL_DELETE_RECORDS = "DROP TABLE IF EXISTS ${RecordEntry.TABLE_NAME}"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_CREATE_SETTINGS)
        db?.execSQL(SQL_CREATE_RECORDS)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        Log.w(
            TAG, "onUpgrade: Upgrading database from version $oldVersion to $newVersion. " +
                    "This will destroy all old data."
        )

        db?.execSQL(SQL_DELETE_RECORDS)
        db?.execSQL(SQL_DELETE_SETTINGS)

        onCreate(db)
    }
}
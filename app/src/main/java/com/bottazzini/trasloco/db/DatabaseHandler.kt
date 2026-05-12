package com.bottazzini.trasloco.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.bottazzini.trasloco.db.columns.AchievementsColumns.AchievementEntry
import com.bottazzini.trasloco.db.columns.GameLogColumns.GameLogEntry
import com.bottazzini.trasloco.db.columns.RecordsColumns.RecordEntry
import com.bottazzini.trasloco.db.columns.SettingsBaseColumns.SettingEntry

class DatabaseHandler(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val TAG = "DatabaseHandler"
        private const val DATABASE_VERSION = 4
        private const val DATABASE_NAME = "Trasloco.db"

        private const val SQL_CREATE_SETTINGS =
            "CREATE TABLE IF NOT EXISTS ${SettingEntry.TABLE_NAME} (${SettingEntry.COLUMN_NAME} TEXT," +
                    "${SettingEntry.COLUMN_VALUE} TEXT)"
        private const val SQL_CREATE_RECORDS =
            "CREATE TABLE IF NOT EXISTS ${RecordEntry.TABLE_NAME} (${RecordEntry.COLUMN_TYPE} TEXT," +
                    "${RecordEntry.COLUMN_VALUE} INTEGER, ${RecordEntry.COLUMN_NEW} INTEGER, ${RecordEntry.COLUMN_CURRENT_VALUE} INTEGER)"
        private const val SQL_CREATE_GAME_LOG =
            "CREATE TABLE IF NOT EXISTS ${GameLogEntry.TABLE_NAME} (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "${GameLogEntry.COLUMN_TIMESTAMP} INTEGER NOT NULL," +
                    "${GameLogEntry.COLUMN_DURATION_MS} INTEGER NOT NULL," +
                    "${GameLogEntry.COLUMN_WON} INTEGER NOT NULL," +
                    "${GameLogEntry.COLUMN_HINTS_USED} INTEGER NOT NULL," +
                    "${GameLogEntry.COLUMN_AUTO_MOVES} INTEGER NOT NULL)"
        private const val SQL_CREATE_ACHIEVEMENTS =
            "CREATE TABLE IF NOT EXISTS ${AchievementEntry.TABLE_NAME} (" +
                    "${AchievementEntry.COLUMN_ID} TEXT PRIMARY KEY," +
                    "${AchievementEntry.COLUMN_UNLOCKED_AT} INTEGER NOT NULL)"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_CREATE_SETTINGS)
        db?.execSQL(SQL_CREATE_RECORDS)
        db?.execSQL(SQL_CREATE_GAME_LOG)
        db?.execSQL(SQL_CREATE_ACHIEVEMENTS)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        Log.w(TAG, "onUpgrade: from version $oldVersion to $newVersion")
        if (oldVersion < 4) {
            db?.execSQL(SQL_CREATE_GAME_LOG)
            db?.execSQL(SQL_CREATE_ACHIEVEMENTS)
        }
    }
}

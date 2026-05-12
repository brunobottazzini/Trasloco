package com.bottazzini.trasloco.settings

import android.content.ContentValues
import android.content.Context
import android.provider.BaseColumns
import com.bottazzini.trasloco.db.DatabaseHandler
import com.bottazzini.trasloco.db.columns.GameLogColumns.GameLogEntry

class GameLogRepository(context: Context) {
    private val dbHandler = DatabaseHandler(context)

    fun insert(log: GameLog) {
        val db = dbHandler.writableDatabase
        val values = ContentValues().apply {
            put(GameLogEntry.COLUMN_TIMESTAMP, log.timestamp)
            put(GameLogEntry.COLUMN_DURATION_MS, log.durationMs)
            put(GameLogEntry.COLUMN_WON, if (log.won) 1 else 0)
            put(GameLogEntry.COLUMN_HINTS_USED, log.hintsUsed)
            put(GameLogEntry.COLUMN_AUTO_MOVES, log.autoMoves)
        }
        db.insert(GameLogEntry.TABLE_NAME, null, values)
        trimIfNeeded(db)
    }

    /** Returns up to [n] most-recent rows, newest first. */
    fun getLastN(n: Int): List<GameLog> {
        val db = dbHandler.readableDatabase
        val cursor = db.query(
            GameLogEntry.TABLE_NAME, null, null, null, null, null,
            "${BaseColumns._ID} DESC", n.toString()
        )
        val result = mutableListOf<GameLog>()
        while (cursor.moveToNext()) {
            result.add(cursor.toGameLog())
        }
        cursor.close()
        return result
    }

    fun countAll(): Long {
        val db = dbHandler.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM ${GameLogEntry.TABLE_NAME}", null)
        val count = if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        cursor.close()
        return count
    }

    fun countWins(): Long {
        val db = dbHandler.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM ${GameLogEntry.TABLE_NAME} WHERE ${GameLogEntry.COLUMN_WON}=1", null
        )
        val count = if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        cursor.close()
        return count
    }

    /** Average duration in ms of won games, or null if no wins recorded. */
    fun avgWinDurationMs(): Long? {
        val db = dbHandler.readableDatabase
        val cursor = db.rawQuery(
            "SELECT AVG(${GameLogEntry.COLUMN_DURATION_MS}) FROM ${GameLogEntry.TABLE_NAME} WHERE ${GameLogEntry.COLUMN_WON}=1",
            null
        )
        val avg = if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        cursor.close()
        return avg
    }

    private fun trimIfNeeded(db: android.database.sqlite.SQLiteDatabase) {
        val count = db.rawQuery("SELECT COUNT(*) FROM ${GameLogEntry.TABLE_NAME}", null)
            .use { if (it.moveToFirst()) it.getLong(0) else 0L }
        if (count > 500) {
            db.execSQL(
                "DELETE FROM ${GameLogEntry.TABLE_NAME} WHERE ${BaseColumns._ID} NOT IN " +
                        "(SELECT ${BaseColumns._ID} FROM ${GameLogEntry.TABLE_NAME} ORDER BY ${BaseColumns._ID} DESC LIMIT 500)"
            )
        }
    }

    fun close() { dbHandler.close() }

    private fun android.database.Cursor.toGameLog() = GameLog(
        id = getLong(getColumnIndexOrThrow(BaseColumns._ID)),
        timestamp = getLong(getColumnIndexOrThrow(GameLogEntry.COLUMN_TIMESTAMP)),
        durationMs = getLong(getColumnIndexOrThrow(GameLogEntry.COLUMN_DURATION_MS)),
        won = getLong(getColumnIndexOrThrow(GameLogEntry.COLUMN_WON)) == 1L,
        hintsUsed = getInt(getColumnIndexOrThrow(GameLogEntry.COLUMN_HINTS_USED)),
        autoMoves = getInt(getColumnIndexOrThrow(GameLogEntry.COLUMN_AUTO_MOVES))
    )
}

package com.bottazzini.trasloco.settings

import android.content.ContentValues
import android.content.Context
import com.bottazzini.trasloco.db.DatabaseHandler
import com.bottazzini.trasloco.db.columns.AchievementsColumns.AchievementEntry

class AchievementsRepository(context: Context) {
    private val dbHandler = DatabaseHandler(context)

    fun unlock(id: String, timestamp: Long) {
        if (isUnlocked(id)) return
        val db = dbHandler.writableDatabase
        val values = ContentValues().apply {
            put(AchievementEntry.COLUMN_ID, id)
            put(AchievementEntry.COLUMN_UNLOCKED_AT, timestamp)
        }
        db.insertOrThrow(AchievementEntry.TABLE_NAME, null, values)
    }

    fun isUnlocked(id: String): Boolean {
        val db = dbHandler.readableDatabase
        val cursor = db.query(
            AchievementEntry.TABLE_NAME,
            arrayOf(AchievementEntry.COLUMN_ID),
            "${AchievementEntry.COLUMN_ID} = ?",
            arrayOf(id), null, null, null
        )
        val found = cursor.count > 0
        cursor.close()
        return found
    }

    fun close() { dbHandler.close() }

    /** Returns map of id → unlockedAt (epoch ms) for all unlocked achievements. */
    fun getAllUnlocked(): Map<String, Long> {
        val db = dbHandler.readableDatabase
        val cursor = db.query(
            AchievementEntry.TABLE_NAME, null, null, null, null, null, null
        )
        val result = mutableMapOf<String, Long>()
        while (cursor.moveToNext()) {
            val id = cursor.getString(cursor.getColumnIndexOrThrow(AchievementEntry.COLUMN_ID))
            val at = cursor.getLong(cursor.getColumnIndexOrThrow(AchievementEntry.COLUMN_UNLOCKED_AT))
            result[id] = at
        }
        cursor.close()
        return result
    }
}

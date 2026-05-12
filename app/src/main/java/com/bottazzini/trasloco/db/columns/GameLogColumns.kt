package com.bottazzini.trasloco.db.columns

import android.provider.BaseColumns

object GameLogColumns {
    object GameLogEntry : BaseColumns {
        const val TABLE_NAME = "game_log"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_DURATION_MS = "duration_ms"
        const val COLUMN_WON = "won"
        const val COLUMN_HINTS_USED = "hints_used"
        const val COLUMN_AUTO_MOVES = "auto_moves"
    }
}

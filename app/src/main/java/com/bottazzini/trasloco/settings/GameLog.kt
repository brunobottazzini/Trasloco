package com.bottazzini.trasloco.settings

data class GameLog(
    val id: Long = 0,
    val timestamp: Long,
    val durationMs: Long,
    val won: Boolean,
    val hintsUsed: Int,
    val autoMoves: Int
)

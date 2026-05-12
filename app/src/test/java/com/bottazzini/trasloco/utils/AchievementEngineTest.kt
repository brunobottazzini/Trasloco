package com.bottazzini.trasloco.utils

import com.bottazzini.trasloco.settings.GameLog
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class AchievementEngineTest {

    private fun wonGame(durationMs: Long, hintsUsed: Int = 0, autoMoves: Int = 0, timestamp: Long = System.currentTimeMillis()) =
        GameLog(timestamp = timestamp, durationMs = durationMs, won = true, hintsUsed = hintsUsed, autoMoves = autoMoves)

    private fun lostGame(durationMs: Long = 300_000L) =
        GameLog(timestamp = System.currentTimeMillis(), durationMs = durationMs, won = false, hintsUsed = 0, autoMoves = 0)

    @Test
    fun `first_win unlocks when totalWins is 1`() {
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 1L, totalGames = 1L, currentStreak = 1L,
            lastGame = wonGame(200_000L), lastFourGames = listOf(wonGame(200_000L)),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("first_win" in result)
    }

    @Test
    fun `wins_10 unlocks when totalWins is 10, wins_50 does not`() {
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 10L, totalGames = 10L, currentStreak = 1L,
            lastGame = wonGame(200_000L), lastFourGames = listOf(wonGame(200_000L)),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("wins_10" in result)
        assertFalse("wins_50" in result)
    }

    @Test
    fun `streak milestones up to current streak are all returned`() {
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 12L, totalGames = 12L, currentStreak = 12L,
            lastGame = wonGame(200_000L), lastFourGames = listOf(wonGame(200_000L)),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("streak_3" in result)
        assertTrue("streak_6" in result)
        assertTrue("streak_9" in result)
        assertTrue("streak_12" in result)
        assertFalse("streak_15" in result)
    }

    @Test
    fun `speed_2min unlocks for game under 2 minutes, speed_1min does not`() {
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 5L, totalGames = 5L, currentStreak = 1L,
            lastGame = wonGame(90_000L), lastFourGames = listOf(wonGame(90_000L)),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("speed_3min" in result)
        assertTrue("speed_2min" in result)
        assertFalse("speed_1min" in result)
    }

    @Test
    fun `hint_free unlocks when hintsUsed is 0`() {
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 1L, totalGames = 1L, currentStreak = 1L,
            lastGame = wonGame(200_000L, hintsUsed = 0), lastFourGames = listOf(wonGame(200_000L, hintsUsed = 0)),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("hint_free" in result)
    }

    @Test
    fun `hint_free does not unlock when hints were used`() {
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 1L, totalGames = 1L, currentStreak = 1L,
            lastGame = wonGame(200_000L, hintsUsed = 2), lastFourGames = listOf(wonGame(200_000L, hintsUsed = 2)),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertFalse("hint_free" in result)
    }

    @Test
    fun `no_assist does not unlock when auto_moves were used`() {
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 1L, totalGames = 1L, currentStreak = 1L,
            lastGame = wonGame(200_000L, hintsUsed = 0, autoMoves = 3), lastFourGames = listOf(wonGame(200_000L, hintsUsed = 0, autoMoves = 3)),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertFalse("no_assist" in result)
    }

    @Test
    fun `resilient unlocks when previous 3 games were losses`() {
        val win = wonGame(150_000L)
        val loss = lostGame()
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 1L, totalGames = 4L, currentStreak = 1L,
            lastGame = win, lastFourGames = listOf(win, loss, loss, loss),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("resilient" in result)
    }

    @Test
    fun `resilient does not unlock when previous games were not all losses`() {
        val win = wonGame(150_000L)
        val loss = lostGame()
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 2L, totalGames = 4L, currentStreak = 1L,
            lastGame = win, lastFourGames = listOf(win, loss, win, loss),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertFalse("resilient" in result)
    }

    @Test
    fun `morning unlocks for game won before 7am`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 30)
        }
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 1L, totalGames = 1L, currentStreak = 1L,
            lastGame = wonGame(200_000L, timestamp = cal.timeInMillis),
            lastFourGames = listOf(wonGame(200_000L, timestamp = cal.timeInMillis)),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("morning" in result)
    }

    @Test
    fun `midnight unlocks for game won between midnight and 1am`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 30)
        }
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 1L, totalGames = 1L, currentStreak = 1L,
            lastGame = wonGame(200_000L, timestamp = cal.timeInMillis),
            lastFourGames = listOf(wonGame(200_000L, timestamp = cal.timeInMillis)),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("midnight" in result)
    }

    @Test
    fun `christmas unlocks on APP_OPENED on December 25`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 25)
        }
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.APP_OPENED,
            totalWins = 0L, totalGames = 0L, currentStreak = 0L,
            lastGame = null, lastFourGames = emptyList(),
            isNewTimeRecord = false, now = cal.timeInMillis
        )
        assertTrue("christmas" in result)
    }

    @Test
    fun `christmas does not unlock on non-December 25`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.APP_OPENED,
            totalWins = 0L, totalGames = 0L, currentStreak = 0L,
            lastGame = null, lastFourGames = emptyList(),
            isNewTimeRecord = false, now = cal.timeInMillis
        )
        assertFalse("christmas" in result)
    }

    @Test
    fun `first_loss unlocks on GAME_LOST when there is at least one loss`() {
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_LOST,
            totalWins = 0L, totalGames = 1L, currentStreak = 0L,
            lastGame = lostGame(), lastFourGames = listOf(lostGame()),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("first_loss" in result)
    }

    @Test
    fun `tutorial_done unlocks on TUTORIAL_COMPLETED`() {
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.TUTORIAL_COMPLETED,
            totalWins = 0L, totalGames = 0L, currentStreak = 0L,
            lastGame = null, lastFourGames = emptyList(),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("tutorial_done" in result)
    }

    @Test
    fun `new_record unlocks when isNewTimeRecord is true`() {
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 1L, totalGames = 1L, currentStreak = 1L,
            lastGame = wonGame(200_000L), lastFourGames = listOf(wonGame(200_000L)),
            isNewTimeRecord = true, now = System.currentTimeMillis()
        )
        assertTrue("new_record" in result)
    }

    @Test
    fun `games_50 unlocks on GAME_LOST when totalGames reaches 50`() {
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_LOST,
            totalWins = 30L, totalGames = 50L, currentStreak = 0L,
            lastGame = lostGame(), lastFourGames = listOf(lostGame()),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("games_50" in result)
        assertFalse("games_200" in result)
    }
}

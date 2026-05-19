package com.bottazzini.trasloco.utils

import android.content.Context
import com.bottazzini.trasloco.settings.AchievementsRepository
import com.bottazzini.trasloco.settings.GameLog
import com.bottazzini.trasloco.settings.GameLogRepository
import com.bottazzini.trasloco.settings.RecordsHandler
import com.bottazzini.trasloco.settings.Type
import java.util.Calendar

class AchievementEngine(
    private val recordsHandler: RecordsHandler,
    private val gameLogRepo: GameLogRepository,
    private val achievementsRepo: AchievementsRepository
) {
    companion object {
        fun create(context: Context) = AchievementEngine(
            RecordsHandler(context),
            GameLogRepository(context),
            AchievementsRepository(context)
        )

        /** Pure evaluation logic — testable without Android context. */
        internal fun evaluateConditions(
            trigger: AchievementTrigger,
            totalWins: Long,
            totalGames: Long,
            currentStreak: Long,
            lastGame: GameLog?,
            recentGames: List<GameLog>,
            isNewTimeRecord: Boolean,
            now: Long
        ): List<String> {
            val candidates = mutableListOf<String>()

            when (trigger) {
                AchievementTrigger.GAME_WON -> {
                    // Vittorie totali
                    listOf(1L to "first_win", 10L to "wins_10", 50L to "wins_50",
                           100L to "wins_100", 500L to "wins_500", 1000L to "wins_1000")
                        .forEach { (threshold, id) -> if (totalWins >= threshold) candidates.add(id) }

                    // Streak
                    listOf(3, 6, 9, 12, 15, 18, 21, 24, 27, 30, 50, 100)
                        .forEach { m -> if (currentStreak >= m) candidates.add("streak_$m") }

                    lastGame?.let { game ->
                        // Velocità
                        if (game.durationMs < 3 * 60 * 1000L) candidates.add("speed_3min")
                        if (game.durationMs < 2 * 60 * 1000L) candidates.add("speed_2min")
                        if (game.durationMs < 60 * 1000L)     candidates.add("speed_1min")
                        if (game.durationMs < 45 * 1000L)     candidates.add("speed_45s")

                        // Stile
                        if (game.hintsUsed == 0) candidates.add("hint_free")
                        if (game.hintsUsed == 0 && game.autoMoves == 0) candidates.add("no_assist")

                        // Speciali
                        val cal = Calendar.getInstance().apply { timeInMillis = game.timestamp }
                        val hour = cal.get(Calendar.HOUR_OF_DAY)
                        if (hour < 7)  candidates.add("morning")
                        if (hour == 0) candidates.add("midnight")
                        if (hour in 12..13) candidates.add("lunch_win")
                        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) candidates.add("sunday_player")
                        if (cal.get(Calendar.MONTH) == Calendar.DECEMBER &&
                            cal.get(Calendar.DAY_OF_MONTH) == 31) candidates.add("new_year_eve")

                        if (game.durationMs > 15 * 60 * 1000L) candidates.add("slow_win")
                        if (game.hintsUsed >= 5) candidates.add("hint_hero")
                    }

                    // 3 partite di fila dopo mezzanotte (ore 0-5)
                    if (recentGames.size >= 3 && recentGames.take(3).all {
                            Calendar.getInstance().apply { timeInMillis = it.timestamp }
                                .get(Calendar.HOUR_OF_DAY) in 0..5 })
                        candidates.add("night_owl_3")

                    // 3 vittorie nello stesso giorno
                    val lastThreeWins = recentGames.filter { it.won }.take(3)
                    if (lastThreeWins.size == 3 &&
                        lastThreeWins.map { dayKey(it.timestamp) }.distinct().size == 1)
                        candidates.add("same_day_3")

                    // 5 partite (qualsiasi risultato) nello stesso giorno
                    if (recentGames.size >= 5 &&
                        recentGames.take(5).map { dayKey(it.timestamp) }.distinct().size == 1)
                        candidates.add("same_day_5")

                    // Resilient: recentGames[0]=win, [1..3]=losses
                    if (recentGames.size >= 4 &&
                        recentGames[0].won &&
                        !recentGames[1].won &&
                        !recentGames[2].won &&
                        !recentGames[3].won) candidates.add("resilient")

                    // Stavo solo scaldando
                    if (recentGames.size >= 3 &&
                        recentGames[0].won &&
                        !recentGames[1].won &&
                        !recentGames[2].won) candidates.add("comeback_2")

                    // Hint in tutte le ultime 5 partite
                    if (recentGames.size >= 5 && recentGames.take(5).all { it.hintsUsed > 0 })
                        candidates.add("hint_addict")

                    // 3 vittorie di fila senza hint né auto-mosse
                    if (recentGames.size >= 3 && recentGames.take(3).all {
                            it.won && it.hintsUsed == 0 && it.autoMoves == 0 })
                        candidates.add("perfectionist")

                    // 3 vittorie di fila in meno di 2 minuti
                    if (recentGames.size >= 3 && recentGames.take(3).all {
                            it.won && it.durationMs < 2 * 60 * 1000L })
                        candidates.add("speed_freak")

                    if (isNewTimeRecord) candidates.add("new_record")

                    // Partite giocate (also checked on GAME_LOST)
                    if (totalGames >= 50)  candidates.add("games_50")
                    if (totalGames >= 200) candidates.add("games_200")
                    if (totalGames >= 500) candidates.add("games_500")
                }

                AchievementTrigger.GAME_LOST -> {
                    candidates.add("first_loss") // isUnlocked filter prevents re-unlocking
                    if (totalGames >= 50)  candidates.add("games_50")
                    if (totalGames >= 200) candidates.add("games_200")
                    if (totalGames >= 500) candidates.add("games_500")

                    // Sconfitte consecutive
                    listOf(2, 3, 5, 7, 10).forEach { n ->
                        if (recentGames.size >= n && recentGames.take(n).all { !it.won })
                            candidates.add("loss_$n")
                    }
                    // Perseveranza infinita
                    if (totalGames - totalWins >= 100) candidates.add("big_loser")
                }

                AchievementTrigger.APP_OPENED -> {
                    val cal = Calendar.getInstance().apply { timeInMillis = now }
                    if (cal.get(Calendar.MONTH) == Calendar.DECEMBER &&
                        cal.get(Calendar.DAY_OF_MONTH) == 25) candidates.add("christmas")
                }

                AchievementTrigger.TUTORIAL_COMPLETED -> {
                    candidates.add("tutorial_done")
                }
            }

            return candidates
        }

        private fun dayKey(ts: Long): String {
            val cal = Calendar.getInstance().apply { timeInMillis = ts }
            return "%d-%02d-%02d".format(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }
    }

    /**
     * Evaluates which achievements are newly unlocked for the given trigger.
     * Persists new unlocks to DB and returns them for banner display.
     */
    fun evaluate(trigger: AchievementTrigger): List<AchievementDef> {
        val totalWins = recordsHandler.getTotalWins()
        val totalGames = gameLogRepo.countAll()
        val currentStreak = recordsHandler.readCurrentValue(Type.CONSECUTIVE) ?: 0L
        val recentGames = gameLogRepo.getLastN(10)
        val lastGame = recentGames.firstOrNull()
        val isNewTimeRecord = recordsHandler.readNew(Type.TIME) ?: false
        val now = System.currentTimeMillis()

        val candidateIds = evaluateConditions(
            trigger, totalWins, totalGames, currentStreak,
            lastGame, recentGames, isNewTimeRecord, now
        )

        val newIds = candidateIds.filter { !achievementsRepo.isUnlocked(it) }
        newIds.forEach { achievementsRepo.unlock(it, now) }

        return newIds.mapNotNull { AchievementCatalog.findById(it) }
    }
}

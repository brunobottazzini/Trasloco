# Achievement Expansion (21 New Achievements) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 21 new achievements to Trasloco (loss streaks, style, time-based, holidays) and fix banner timing in MainActivity.

**Architecture:** All logic lives in `AchievementEngine.evaluateConditions` (pure, testable). `AchievementCatalog` is extended with 21 new `AchievementDef` entries. Strings added to 3 language files. `evaluate()` expanded from `getLastN(4)` to `getLastN(10)`. One `postDelayed` fix in `MainActivity` delays the banner until after the splash transition.

**Tech Stack:** Kotlin, JUnit 4, Android Gradle (`./gradlew :app:testDebugUnitTest`)

---

## Files

| File | Change |
|---|---|
| `app/src/main/java/com/bottazzini/trasloco/utils/AchievementEngine.kt` | Rename param, expand to 10 games, add `dayKey` helper, add 21 condition checks |
| `app/src/main/java/com/bottazzini/trasloco/utils/AchievementCatalog.kt` | Add 21 `AchievementDef` entries |
| `app/src/test/java/com/bottazzini/trasloco/utils/AchievementEngineTest.kt` | Rename param, extend `lostGame` helper, add new tests |
| `app/src/main/res/values-it/strings.xml` | Add 42 strings + fix trophies header 34→55 |
| `app/src/main/res/values/strings.xml` | Add 42 strings + fix trophies header 34→55 |
| `app/src/main/res/values-pt/strings.xml` | Add 42 strings + fix trophies header 34→55 |
| `app/src/main/java/com/bottazzini/trasloco/MainActivity.kt` | `postDelayed(500)` fix for banner timing |

---

## Task 1: Rename `lastFourGames` → `recentGames`, expand to 10 games

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/utils/AchievementEngine.kt`
- Modify: `app/src/test/java/com/bottazzini/trasloco/utils/AchievementEngineTest.kt`

- [ ] **Step 1: Update `AchievementEngine.kt`**

In `evaluateConditions` signature (line 30), rename parameter:
```kotlin
recentGames: List<GameLog>,
```

In the `resilient` check (lines 65–70), update all references:
```kotlin
                    // Resilient: recentGames[0]=win, [1..3]=losses
                    if (recentGames.size >= 4 &&
                        recentGames[0].won &&
                        !recentGames[1].won &&
                        !recentGames[2].won &&
                        !recentGames[3].won) candidates.add("resilient")
```

In `evaluate()` (lines 110–117), change `getLastN(4)` to `getLastN(10)` and rename:
```kotlin
        val recentGames = gameLogRepo.getLastN(10)
        val lastGame = recentGames.firstOrNull()
```
and in the `evaluateConditions` call:
```kotlin
            lastGame, recentGames, isNewTimeRecord, now
```

- [ ] **Step 2: Update `AchievementEngineTest.kt`**

Replace every occurrence of `lastFourGames =` with `recentGames =` (use replace_all — ~15 occurrences).

Also extend the `lostGame` helper to accept `hintsUsed` and `timestamp` (needed by later tasks):
```kotlin
    private fun lostGame(durationMs: Long = 300_000L, hintsUsed: Int = 0, timestamp: Long = System.currentTimeMillis()) =
        GameLog(timestamp = timestamp, durationMs = durationMs, won = false, hintsUsed = hintsUsed, autoMoves = 0)
```

- [ ] **Step 3: Run — all existing tests must pass**
```bash
cd /Users/bottazzini/Documents/Progetti/Trasloco && ./gradlew :app:testDebugUnitTest --tests "com.bottazzini.trasloco.utils.AchievementEngineTest" 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/AchievementEngine.kt \
        app/src/test/java/com/bottazzini/trasloco/utils/AchievementEngineTest.kt
git commit -m "refactor: rename lastFourGames→recentGames, expand engine to 10 games"
```

---

## Task 2: TDD — GAME_LOST achievements (loss_2..10, big_loser)

**Files:**
- Modify: `app/src/test/java/com/bottazzini/trasloco/utils/AchievementEngineTest.kt`
- Modify: `app/src/main/java/com/bottazzini/trasloco/utils/AchievementEngine.kt`

- [ ] **Step 1: Write failing tests — append to `AchievementEngineTest.kt`**

```kotlin
    // ---- GAME_LOST streak achievements ----

    @Test
    fun `loss_2 unlocks after 2 consecutive losses`() {
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_LOST,
            totalWins = 0L, totalGames = 2L, currentStreak = 0L,
            lastGame = lostGame(), recentGames = listOf(lostGame(), lostGame()),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("loss_2" in result)
    }

    @Test
    fun `loss_2 does not unlock after only 1 loss`() {
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_LOST,
            totalWins = 0L, totalGames = 1L, currentStreak = 0L,
            lastGame = lostGame(), recentGames = listOf(lostGame()),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertFalse("loss_2" in result)
    }

    @Test
    fun `loss_5 unlocks after 5 consecutive losses, loss_7 does not`() {
        val losses = List(5) { lostGame() }
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_LOST,
            totalWins = 0L, totalGames = 5L, currentStreak = 0L,
            lastGame = lostGame(), recentGames = losses,
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("loss_5" in result)
        assertFalse("loss_7" in result)
    }

    @Test
    fun `loss_10 unlocks all lower streak achievements too`() {
        val losses = List(10) { lostGame() }
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_LOST,
            totalWins = 0L, totalGames = 10L, currentStreak = 0L,
            lastGame = lostGame(), recentGames = losses,
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        listOf("loss_2", "loss_3", "loss_5", "loss_7", "loss_10").forEach {
            assertTrue("$it should be in result", it in result)
        }
    }

    @Test
    fun `loss_5 does not unlock when a win breaks the streak`() {
        val games = listOf(lostGame(), lostGame(), wonGame(100_000L), lostGame(), lostGame())
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_LOST,
            totalWins = 1L, totalGames = 5L, currentStreak = 0L,
            lastGame = lostGame(), recentGames = games,
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertFalse("loss_5" in result)
    }

    @Test
    fun `big_loser unlocks when total losses reach 100`() {
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_LOST,
            totalWins = 50L, totalGames = 151L, currentStreak = 0L,
            lastGame = lostGame(), recentGames = listOf(lostGame()),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("big_loser" in result)
    }

    @Test
    fun `big_loser does not unlock before 100 total losses`() {
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_LOST,
            totalWins = 50L, totalGames = 99L, currentStreak = 0L,
            lastGame = lostGame(), recentGames = listOf(lostGame()),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertFalse("big_loser" in result)
    }
```

- [ ] **Step 2: Run — verify new tests FAIL**
```bash
cd /Users/bottazzini/Documents/Progetti/Trasloco && ./gradlew :app:testDebugUnitTest --tests "com.bottazzini.trasloco.utils.AchievementEngineTest" 2>&1 | tail -20
```

- [ ] **Step 3: Implement — add to `GAME_LOST` branch in `AchievementEngine.kt`**

After `candidates.add("first_loss")` in the `GAME_LOST` branch, add:
```kotlin
                    // Sconfitte consecutive
                    listOf(2, 3, 5, 7, 10).forEach { n ->
                        if (recentGames.size >= n && recentGames.take(n).all { !it.won })
                            candidates.add("loss_$n")
                    }
                    // Perseveranza infinita
                    if (totalGames - totalWins >= 100) candidates.add("big_loser")
```

- [ ] **Step 4: Run — all tests must pass**
```bash
cd /Users/bottazzini/Documents/Progetti/Trasloco && ./gradlew :app:testDebugUnitTest --tests "com.bottazzini.trasloco.utils.AchievementEngineTest" 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**
```bash
git add app/src/test/java/com/bottazzini/trasloco/utils/AchievementEngineTest.kt \
        app/src/main/java/com/bottazzini/trasloco/utils/AchievementEngine.kt
git commit -m "feat: add loss streak and big_loser achievements"
```

---

## Task 3: TDD — GAME_WON style achievements

**Files:**
- Modify: `app/src/test/java/com/bottazzini/trasloco/utils/AchievementEngineTest.kt`
- Modify: `app/src/main/java/com/bottazzini/trasloco/utils/AchievementEngine.kt`

Achievements: `comeback_2`, `slow_win`, `hint_hero`, `hint_addict`, `perfectionist`, `speed_freak`.

- [ ] **Step 1: Write failing tests — append to `AchievementEngineTest.kt`**

```kotlin
    // ---- GAME_WON style achievements ----

    @Test
    fun `comeback_2 unlocks when win follows 2 consecutive losses`() {
        val win = wonGame(120_000L)
        val games = listOf(win, lostGame(), lostGame())
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 1L, totalGames = 3L, currentStreak = 1L,
            lastGame = win, recentGames = games,
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("comeback_2" in result)
    }

    @Test
    fun `comeback_2 does not unlock when only one prior loss`() {
        val win = wonGame(120_000L)
        val games = listOf(win, lostGame())
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 1L, totalGames = 2L, currentStreak = 1L,
            lastGame = win, recentGames = games,
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertFalse("comeback_2" in result)
    }

    @Test
    fun `slow_win unlocks for game longer than 15 minutes`() {
        val game = wonGame(16 * 60 * 1000L)
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 1L, totalGames = 1L, currentStreak = 1L,
            lastGame = game, recentGames = listOf(game),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("slow_win" in result)
    }

    @Test
    fun `slow_win does not unlock for game under 15 minutes`() {
        val game = wonGame(14 * 60 * 1000L)
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 1L, totalGames = 1L, currentStreak = 1L,
            lastGame = game, recentGames = listOf(game),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertFalse("slow_win" in result)
    }

    @Test
    fun `hint_hero unlocks when 5 or more hints used`() {
        val game = wonGame(200_000L, hintsUsed = 5)
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 1L, totalGames = 1L, currentStreak = 1L,
            lastGame = game, recentGames = listOf(game),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("hint_hero" in result)
    }

    @Test
    fun `hint_hero does not unlock with fewer than 5 hints`() {
        val game = wonGame(200_000L, hintsUsed = 4)
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 1L, totalGames = 1L, currentStreak = 1L,
            lastGame = game, recentGames = listOf(game),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertFalse("hint_hero" in result)
    }

    @Test
    fun `hint_addict unlocks when last 5 games all used hints`() {
        val games = List(5) { wonGame(200_000L, hintsUsed = 1) }
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 5L, totalGames = 5L, currentStreak = 1L,
            lastGame = games[0], recentGames = games,
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("hint_addict" in result)
    }

    @Test
    fun `hint_addict does not unlock when one of last 5 games had no hints`() {
        val games = listOf(
            wonGame(200_000L, hintsUsed = 1),
            wonGame(200_000L, hintsUsed = 0),
            wonGame(200_000L, hintsUsed = 1),
            wonGame(200_000L, hintsUsed = 1),
            wonGame(200_000L, hintsUsed = 1)
        )
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 5L, totalGames = 5L, currentStreak = 1L,
            lastGame = games[0], recentGames = games,
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertFalse("hint_addict" in result)
    }

    @Test
    fun `perfectionist unlocks when last 3 wins have zero hints and auto moves`() {
        val games = List(3) { wonGame(200_000L, hintsUsed = 0, autoMoves = 0) }
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 3L, totalGames = 3L, currentStreak = 3L,
            lastGame = games[0], recentGames = games,
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("perfectionist" in result)
    }

    @Test
    fun `perfectionist does not unlock when any game used hints`() {
        val games = listOf(
            wonGame(200_000L, hintsUsed = 0, autoMoves = 0),
            wonGame(200_000L, hintsUsed = 1, autoMoves = 0),
            wonGame(200_000L, hintsUsed = 0, autoMoves = 0)
        )
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 3L, totalGames = 3L, currentStreak = 3L,
            lastGame = games[0], recentGames = games,
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertFalse("perfectionist" in result)
    }

    @Test
    fun `speed_freak unlocks when last 3 games are all wins under 2 minutes`() {
        val games = List(3) { wonGame(90_000L) }
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 3L, totalGames = 3L, currentStreak = 3L,
            lastGame = games[0], recentGames = games,
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("speed_freak" in result)
    }

    @Test
    fun `speed_freak does not unlock when one game is over 2 minutes`() {
        val games = listOf(wonGame(90_000L), wonGame(150_000L), wonGame(90_000L))
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 3L, totalGames = 3L, currentStreak = 3L,
            lastGame = games[0], recentGames = games,
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertFalse("speed_freak" in result)
    }
```

- [ ] **Step 2: Run — verify new tests FAIL**
```bash
cd /Users/bottazzini/Documents/Progetti/Trasloco && ./gradlew :app:testDebugUnitTest --tests "com.bottazzini.trasloco.utils.AchievementEngineTest" 2>&1 | tail -20
```

- [ ] **Step 3: Implement — add to `GAME_WON` branch in `AchievementEngine.kt`**

After the existing `resilient` check, add:
```kotlin
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
```

Inside the existing `lastGame?.let { game -> ... }` block, after the existing `if (hour == 0) candidates.add("midnight")` line, add:
```kotlin
                        if (game.durationMs > 15 * 60 * 1000L) candidates.add("slow_win")
                        if (game.hintsUsed >= 5) candidates.add("hint_hero")
```

- [ ] **Step 4: Run — all tests must pass**
```bash
cd /Users/bottazzini/Documents/Progetti/Trasloco && ./gradlew :app:testDebugUnitTest --tests "com.bottazzini.trasloco.utils.AchievementEngineTest" 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**
```bash
git add app/src/test/java/com/bottazzini/trasloco/utils/AchievementEngineTest.kt \
        app/src/main/java/com/bottazzini/trasloco/utils/AchievementEngine.kt
git commit -m "feat: add comeback_2, slow_win, hint_hero, hint_addict, perfectionist, speed_freak"
```

---

## Task 4: TDD — GAME_WON time/day achievements + `dayKey` helper

**Files:**
- Modify: `app/src/test/java/com/bottazzini/trasloco/utils/AchievementEngineTest.kt`
- Modify: `app/src/main/java/com/bottazzini/trasloco/utils/AchievementEngine.kt`

Achievements: `lunch_win`, `sunday_player`, `night_owl_3`, `same_day_3`, `same_day_5`, `new_year_eve`.

- [ ] **Step 1: Write failing tests — append to `AchievementEngineTest.kt`**

```kotlin
    // ---- GAME_WON time/day achievements ----

    @Test
    fun `lunch_win unlocks for game won between 12 and 13 hours`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 30)
        }
        val game = wonGame(200_000L, timestamp = cal.timeInMillis)
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 1L, totalGames = 1L, currentStreak = 1L,
            lastGame = game, recentGames = listOf(game),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("lunch_win" in result)
    }

    @Test
    fun `lunch_win does not unlock outside 12-13 hours`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 15); set(Calendar.MINUTE, 0)
        }
        val game = wonGame(200_000L, timestamp = cal.timeInMillis)
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 1L, totalGames = 1L, currentStreak = 1L,
            lastGame = game, recentGames = listOf(game),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertFalse("lunch_win" in result)
    }

    @Test
    fun `sunday_player unlocks for game won on Sunday`() {
        val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY) }
        val game = wonGame(200_000L, timestamp = cal.timeInMillis)
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 1L, totalGames = 1L, currentStreak = 1L,
            lastGame = game, recentGames = listOf(game),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("sunday_player" in result)
    }

    @Test
    fun `night_owl_3 unlocks when last 3 games are played between midnight and 5am`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 2); set(Calendar.MINUTE, 0)
        }
        val games = List(3) { wonGame(200_000L, timestamp = cal.timeInMillis) }
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 3L, totalGames = 3L, currentStreak = 3L,
            lastGame = games[0], recentGames = games,
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("night_owl_3" in result)
    }

    @Test
    fun `night_owl_3 does not unlock when a game is played after 6am`() {
        val nightCal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 2) }
        val dayCal   = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 10) }
        val games = listOf(
            wonGame(200_000L, timestamp = nightCal.timeInMillis),
            wonGame(200_000L, timestamp = dayCal.timeInMillis),
            wonGame(200_000L, timestamp = nightCal.timeInMillis)
        )
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 3L, totalGames = 3L, currentStreak = 3L,
            lastGame = games[0], recentGames = games,
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertFalse("night_owl_3" in result)
    }

    @Test
    fun `same_day_3 unlocks when last 3 wins are on the same calendar day`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 1, 10, 0, 0)
        }
        val ts = cal.timeInMillis
        val games = List(3) { wonGame(200_000L, timestamp = ts + it * 3_600_000L) }
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 3L, totalGames = 3L, currentStreak = 3L,
            lastGame = games[0], recentGames = games,
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("same_day_3" in result)
    }

    @Test
    fun `same_day_3 does not unlock when wins span different days`() {
        val day1 = Calendar.getInstance().apply { set(2026, Calendar.JANUARY, 1, 10, 0, 0) }.timeInMillis
        val day2 = Calendar.getInstance().apply { set(2026, Calendar.JANUARY, 2, 10, 0, 0) }.timeInMillis
        val games = listOf(
            wonGame(200_000L, timestamp = day1),
            wonGame(200_000L, timestamp = day2),
            wonGame(200_000L, timestamp = day1)
        )
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 3L, totalGames = 3L, currentStreak = 3L,
            lastGame = games[0], recentGames = games,
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertFalse("same_day_3" in result)
    }

    @Test
    fun `same_day_5 unlocks when last 5 games are on the same calendar day`() {
        val cal = Calendar.getInstance().apply { set(2026, Calendar.MARCH, 15, 9, 0, 0) }
        val ts = cal.timeInMillis
        val games = List(5) { wonGame(200_000L, timestamp = ts + it * 1_800_000L) }
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 5L, totalGames = 5L, currentStreak = 5L,
            lastGame = games[0], recentGames = games,
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("same_day_5" in result)
    }

    @Test
    fun `new_year_eve unlocks for game won on December 31`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.DECEMBER); set(Calendar.DAY_OF_MONTH, 31)
        }
        val game = wonGame(200_000L, timestamp = cal.timeInMillis)
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.GAME_WON,
            totalWins = 1L, totalGames = 1L, currentStreak = 1L,
            lastGame = game, recentGames = listOf(game),
            isNewTimeRecord = false, now = System.currentTimeMillis()
        )
        assertTrue("new_year_eve" in result)
    }
```

- [ ] **Step 2: Run — verify new tests FAIL**
```bash
cd /Users/bottazzini/Documents/Progetti/Trasloco && ./gradlew :app:testDebugUnitTest --tests "com.bottazzini.trasloco.utils.AchievementEngineTest" 2>&1 | tail -20
```

- [ ] **Step 3: Add `dayKey` helper to companion object in `AchievementEngine.kt`**

Inside the `companion object { }`, after the closing brace of `evaluateConditions`, add:
```kotlin
        private fun dayKey(ts: Long): String {
            val cal = Calendar.getInstance().apply { timeInMillis = ts }
            return "%d-%02d-%02d".format(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }
```

- [ ] **Step 4: Implement time/day conditions in `AchievementEngine.kt`**

Inside the existing `lastGame?.let { game -> ... }` block, after `if (hour == 0) candidates.add("midnight")`, add:
```kotlin
                        if (hour in 12..13) candidates.add("lunch_win")
                        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) candidates.add("sunday_player")
                        if (cal.get(Calendar.MONTH) == Calendar.DECEMBER &&
                            cal.get(Calendar.DAY_OF_MONTH) == 31) candidates.add("new_year_eve")
```

After the closing `}` of the `lastGame?.let` block (still inside `GAME_WON`), add:
```kotlin
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
```

- [ ] **Step 5: Run — all tests must pass**
```bash
cd /Users/bottazzini/Documents/Progetti/Trasloco && ./gradlew :app:testDebugUnitTest --tests "com.bottazzini.trasloco.utils.AchievementEngineTest" 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**
```bash
git add app/src/test/java/com/bottazzini/trasloco/utils/AchievementEngineTest.kt \
        app/src/main/java/com/bottazzini/trasloco/utils/AchievementEngine.kt
git commit -m "feat: add time/day achievements (lunch, sunday, night owl, same-day, new year's eve)"
```

---

## Task 5: TDD — APP_OPENED holiday achievements

**Files:**
- Modify: `app/src/test/java/com/bottazzini/trasloco/utils/AchievementEngineTest.kt`
- Modify: `app/src/main/java/com/bottazzini/trasloco/utils/AchievementEngine.kt`

Achievements: `new_year`, `halloween`, `ferragosto`, `new_year_eve` (on APP_OPENED).

- [ ] **Step 1: Write failing tests — append to `AchievementEngineTest.kt`**

```kotlin
    // ---- APP_OPENED holiday achievements ----

    @Test
    fun `new_year unlocks on APP_OPENED on January 1st`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.JANUARY); set(Calendar.DAY_OF_MONTH, 1)
        }
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.APP_OPENED,
            totalWins = 0L, totalGames = 0L, currentStreak = 0L,
            lastGame = null, recentGames = emptyList(),
            isNewTimeRecord = false, now = cal.timeInMillis
        )
        assertTrue("new_year" in result)
    }

    @Test
    fun `new_year does not unlock on January 2nd`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.JANUARY); set(Calendar.DAY_OF_MONTH, 2)
        }
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.APP_OPENED,
            totalWins = 0L, totalGames = 0L, currentStreak = 0L,
            lastGame = null, recentGames = emptyList(),
            isNewTimeRecord = false, now = cal.timeInMillis
        )
        assertFalse("new_year" in result)
    }

    @Test
    fun `halloween unlocks on APP_OPENED on October 31st`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.OCTOBER); set(Calendar.DAY_OF_MONTH, 31)
        }
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.APP_OPENED,
            totalWins = 0L, totalGames = 0L, currentStreak = 0L,
            lastGame = null, recentGames = emptyList(),
            isNewTimeRecord = false, now = cal.timeInMillis
        )
        assertTrue("halloween" in result)
    }

    @Test
    fun `ferragosto unlocks on APP_OPENED on August 15th`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.AUGUST); set(Calendar.DAY_OF_MONTH, 15)
        }
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.APP_OPENED,
            totalWins = 0L, totalGames = 0L, currentStreak = 0L,
            lastGame = null, recentGames = emptyList(),
            isNewTimeRecord = false, now = cal.timeInMillis
        )
        assertTrue("ferragosto" in result)
    }

    @Test
    fun `new_year_eve unlocks on APP_OPENED on December 31st`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.DECEMBER); set(Calendar.DAY_OF_MONTH, 31)
        }
        val result = AchievementEngine.evaluateConditions(
            trigger = AchievementTrigger.APP_OPENED,
            totalWins = 0L, totalGames = 0L, currentStreak = 0L,
            lastGame = null, recentGames = emptyList(),
            isNewTimeRecord = false, now = cal.timeInMillis
        )
        assertTrue("new_year_eve" in result)
    }
```

- [ ] **Step 2: Run — verify new tests FAIL**
```bash
cd /Users/bottazzini/Documents/Progetti/Trasloco && ./gradlew :app:testDebugUnitTest --tests "com.bottazzini.trasloco.utils.AchievementEngineTest" 2>&1 | tail -20
```

- [ ] **Step 3: Implement — replace the `APP_OPENED` branch in `AchievementEngine.kt`**

Replace the existing `APP_OPENED` branch with:
```kotlin
                AchievementTrigger.APP_OPENED -> {
                    val cal = Calendar.getInstance().apply { timeInMillis = now }
                    val month = cal.get(Calendar.MONTH)
                    val day   = cal.get(Calendar.DAY_OF_MONTH)
                    if (month == Calendar.DECEMBER && day == 25) candidates.add("christmas")
                    if (month == Calendar.JANUARY  && day == 1)  candidates.add("new_year")
                    if (month == Calendar.OCTOBER  && day == 31) candidates.add("halloween")
                    if (month == Calendar.AUGUST   && day == 15) candidates.add("ferragosto")
                    if (month == Calendar.DECEMBER && day == 31) candidates.add("new_year_eve")
                }
```

- [ ] **Step 4: Run — all tests must pass**
```bash
cd /Users/bottazzini/Documents/Progetti/Trasloco && ./gradlew :app:testDebugUnitTest --tests "com.bottazzini.trasloco.utils.AchievementEngineTest" 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**
```bash
git add app/src/test/java/com/bottazzini/trasloco/utils/AchievementEngineTest.kt \
        app/src/main/java/com/bottazzini/trasloco/utils/AchievementEngine.kt
git commit -m "feat: add holiday achievements (new_year, halloween, ferragosto, new_year_eve)"
```

---

## Task 6: Add 21 entries to `AchievementCatalog.kt`

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/utils/AchievementCatalog.kt`

- [ ] **Step 1: Add new entries after the existing `// --- Speciali (4) ---` block**

```kotlin
        // --- Sconfitte consecutive (6) ---
        AchievementDef("loss_2",        "😬", R.string.achievement_loss_2_name,        R.string.achievement_loss_2_desc),
        AchievementDef("loss_3",        "🤦", R.string.achievement_loss_3_name,        R.string.achievement_loss_3_desc),
        AchievementDef("loss_5",        "💀", R.string.achievement_loss_5_name,        R.string.achievement_loss_5_desc),
        AchievementDef("loss_7",        "🫣", R.string.achievement_loss_7_name,        R.string.achievement_loss_7_desc),
        AchievementDef("loss_10",       "🃏", R.string.achievement_loss_10_name,       R.string.achievement_loss_10_desc),
        AchievementDef("big_loser",     "🏳️", R.string.achievement_big_loser_name,     R.string.achievement_big_loser_desc),
        // --- Rimonte e stile (8) ---
        AchievementDef("comeback_2",    "😤", R.string.achievement_comeback_2_name,    R.string.achievement_comeback_2_desc),
        AchievementDef("slow_win",      "🐢", R.string.achievement_slow_win_name,      R.string.achievement_slow_win_desc),
        AchievementDef("hint_hero",     "💡", R.string.achievement_hint_hero_name,     R.string.achievement_hint_hero_desc),
        AchievementDef("hint_addict",   "🧪", R.string.achievement_hint_addict_name,   R.string.achievement_hint_addict_desc),
        AchievementDef("perfectionist", "🎭", R.string.achievement_perfectionist_name, R.string.achievement_perfectionist_desc),
        AchievementDef("speed_freak",   "⚡", R.string.achievement_speed_freak_name,   R.string.achievement_speed_freak_desc),
        AchievementDef("lunch_win",     "🍝", R.string.achievement_lunch_win_name,     R.string.achievement_lunch_win_desc),
        AchievementDef("sunday_player", "☕", R.string.achievement_sunday_player_name, R.string.achievement_sunday_player_desc),
        // --- Sessioni (3) ---
        AchievementDef("night_owl_3",   "🦉", R.string.achievement_night_owl_3_name,   R.string.achievement_night_owl_3_desc),
        AchievementDef("same_day_3",    "📅", R.string.achievement_same_day_3_name,    R.string.achievement_same_day_3_desc),
        AchievementDef("same_day_5",    "🔁", R.string.achievement_same_day_5_name,    R.string.achievement_same_day_5_desc),
        // --- Nuove festività (4) ---
        AchievementDef("new_year_eve",  "🥂", R.string.achievement_new_year_eve_name,  R.string.achievement_new_year_eve_desc),
        AchievementDef("new_year",      "🎆", R.string.achievement_new_year_name,      R.string.achievement_new_year_desc),
        AchievementDef("halloween",     "🎃", R.string.achievement_halloween_name,     R.string.achievement_halloween_desc),
        AchievementDef("ferragosto",    "☀️", R.string.achievement_ferragosto_name,    R.string.achievement_ferragosto_desc)
```

- [ ] **Step 2: Run tests — must pass**
```bash
cd /Users/bottazzini/Documents/Progetti/Trasloco && ./gradlew :app:testDebugUnitTest --tests "com.bottazzini.trasloco.utils.AchievementEngineTest" 2>&1 | tail -10
```

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/AchievementCatalog.kt
git commit -m "feat: add 21 new entries to AchievementCatalog"
```

---

## Task 7: Strings IT + update trophies header

**Files:**
- Modify: `app/src/main/res/values-it/strings.xml`

- [ ] **Step 1: Add 42 strings after `achievement_new_record_desc` (line 182)**

```xml
    <!-- Achievement nuovi — sconfitte consecutive -->
    <string name="achievement_loss_2_name">Ci risiamo</string>
    <string name="achievement_loss_2_desc">Perdi 2 partite di fila. Coincidenza?</string>
    <string name="achievement_loss_3_name">Tre è il limite</string>
    <string name="achievement_loss_3_desc">3 sconfitte consecutive. O forse no</string>
    <string name="achievement_loss_5_name">Sei sicuro di saper giocare?</string>
    <string name="achievement_loss_5_desc">5 sconfitte di fila. Magari un tutorial?</string>
    <string name="achievement_loss_7_name">Forse è il telefono</string>
    <string name="achievement_loss_7_desc">7 di fila. Non è il telefono</string>
    <string name="achievement_loss_10_name">Maestro della sconfitta</string>
    <string name="achievement_loss_10_desc">10 sconfitte consecutive. Talento raro</string>
    <string name="achievement_big_loser_name">Perseveranza infinita</string>
    <string name="achievement_big_loser_desc">100 sconfitte totali</string>
    <!-- Achievement nuovi — rimonte e stile -->
    <string name="achievement_comeback_2_name">Stavo solo scaldando</string>
    <string name="achievement_comeback_2_desc">Vinci dopo 2 sconfitte di fila</string>
    <string name="achievement_slow_win_name">Chi va piano...</string>
    <string name="achievement_slow_win_desc">Vinci in più di 15 minuti</string>
    <string name="achievement_hint_hero_name">Con un po\' d\'aiuto</string>
    <string name="achievement_hint_hero_desc">Vinci usando 5 o più suggerimenti</string>
    <string name="achievement_hint_addict_name">Suggerimento-dipendente</string>
    <string name="achievement_hint_addict_desc">Usa suggerimenti nelle ultime 5 partite di fila</string>
    <string name="achievement_perfectionist_name">Perfezionista</string>
    <string name="achievement_perfectionist_desc">Vinci 3 partite di fila senza hint né auto-mosse</string>
    <string name="achievement_speed_freak_name">Veloce e furioso</string>
    <string name="achievement_speed_freak_desc">Vinci 3 partite consecutive in meno di 2 minuti</string>
    <string name="achievement_lunch_win_name">Pausa pranzo produttiva</string>
    <string name="achievement_lunch_win_desc">Vinci tra le 12:00 e le 14:00</string>
    <string name="achievement_sunday_player_name">Domenica rilassata</string>
    <string name="achievement_sunday_player_desc">Vinci di domenica</string>
    <!-- Achievement nuovi — sessioni -->
    <string name="achievement_night_owl_3_name">Nottambulo accanito</string>
    <string name="achievement_night_owl_3_desc">Gioca 3 partite consecutive dopo mezzanotte</string>
    <string name="achievement_same_day_3_name">Non mi fermo</string>
    <string name="achievement_same_day_3_desc">3 vittorie nello stesso giorno</string>
    <string name="achievement_same_day_5_name">Giornata da pro</string>
    <string name="achievement_same_day_5_desc">5 partite giocate nello stesso giorno</string>
    <!-- Achievement nuovi — festività -->
    <string name="achievement_new_year_eve_name">Aspettando la mezzanotte</string>
    <string name="achievement_new_year_eve_desc">Gioca una partita il 31 dicembre</string>
    <string name="achievement_new_year_name">Anno nuovo, carte nuove</string>
    <string name="achievement_new_year_desc">Apri l\'app il 1° gennaio</string>
    <string name="achievement_halloween_name">Dolcetto o scherzetto</string>
    <string name="achievement_halloween_desc">Apri l\'app il 31 ottobre</string>
    <string name="achievement_ferragosto_name">Ferragosto</string>
    <string name="achievement_ferragosto_desc">Apri l\'app il 15 agosto</string>
```

- [ ] **Step 2: Change `stats_trophies_header` on line 197**
```xml
    <string name="stats_trophies_header">Trofei (%1$d / 55)</string>
```

- [ ] **Step 3: Commit**
```bash
git add app/src/main/res/values-it/strings.xml
git commit -m "feat: add IT achievement strings and update trophies counter to 55"
```

---

## Task 8: Strings EN + update trophies header

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add 42 strings after `achievement_new_record_desc` (line 182)**

```xml
    <!-- New achievements — loss streaks -->
    <string name="achievement_loss_2_name">Here We Go Again</string>
    <string name="achievement_loss_2_desc">Lost 2 games in a row. Coincidence?</string>
    <string name="achievement_loss_3_name">Three\'s a Pattern</string>
    <string name="achievement_loss_3_desc">3 consecutive losses. Or maybe more</string>
    <string name="achievement_loss_5_name">Are You Sure You Know How to Play?</string>
    <string name="achievement_loss_5_desc">5 losses in a row. Maybe try the tutorial?</string>
    <string name="achievement_loss_7_name">Maybe It\'s the Phone</string>
    <string name="achievement_loss_7_desc">7 in a row. It\'s not the phone</string>
    <string name="achievement_loss_10_name">Master of Defeat</string>
    <string name="achievement_loss_10_desc">10 consecutive losses. Rare talent</string>
    <string name="achievement_big_loser_name">Infinite Perseverance</string>
    <string name="achievement_big_loser_desc">100 total losses</string>
    <!-- New achievements — comebacks and style -->
    <string name="achievement_comeback_2_name">Just Warming Up</string>
    <string name="achievement_comeback_2_desc">Win after 2 consecutive losses</string>
    <string name="achievement_slow_win_name">Slow and Steady</string>
    <string name="achievement_slow_win_desc">Win a game in more than 15 minutes</string>
    <string name="achievement_hint_hero_name">With a Little Help</string>
    <string name="achievement_hint_hero_desc">Win using 5 or more hints</string>
    <string name="achievement_hint_addict_name">Hint-Dependent</string>
    <string name="achievement_hint_addict_desc">Used hints in the last 5 games in a row</string>
    <string name="achievement_perfectionist_name">Perfectionist</string>
    <string name="achievement_perfectionist_desc">Win 3 games in a row without hints or auto-move</string>
    <string name="achievement_speed_freak_name">Fast and Furious</string>
    <string name="achievement_speed_freak_desc">Win 3 consecutive games in under 2 minutes</string>
    <string name="achievement_lunch_win_name">Productive Lunch Break</string>
    <string name="achievement_lunch_win_desc">Win between 12:00 and 2:00 PM</string>
    <string name="achievement_sunday_player_name">Lazy Sunday</string>
    <string name="achievement_sunday_player_desc">Win a game on Sunday</string>
    <!-- New achievements — sessions -->
    <string name="achievement_night_owl_3_name">Hardcore Night Owl</string>
    <string name="achievement_night_owl_3_desc">Play 3 consecutive games after midnight</string>
    <string name="achievement_same_day_3_name">Can\'t Stop</string>
    <string name="achievement_same_day_3_desc">3 wins in the same day</string>
    <string name="achievement_same_day_5_name">Pro Day</string>
    <string name="achievement_same_day_5_desc">5 games played in the same day</string>
    <!-- New achievements — holidays -->
    <string name="achievement_new_year_eve_name">Waiting for Midnight</string>
    <string name="achievement_new_year_eve_desc">Play a game on December 31st</string>
    <string name="achievement_new_year_name">New Year, New Cards</string>
    <string name="achievement_new_year_desc">Open the app on January 1st</string>
    <string name="achievement_halloween_name">Trick or Treat</string>
    <string name="achievement_halloween_desc">Open the app on October 31st</string>
    <string name="achievement_ferragosto_name">Ferragosto</string>
    <string name="achievement_ferragosto_desc">Open the app on August 15th</string>
```

- [ ] **Step 2: Change `stats_trophies_header` on line 197**
```xml
    <string name="stats_trophies_header">Trophies (%1$d / 55)</string>
```

- [ ] **Step 3: Commit**
```bash
git add app/src/main/res/values/strings.xml
git commit -m "feat: add EN achievement strings and update trophies counter to 55"
```

---

## Task 9: Strings PT + update trophies header

**Files:**
- Modify: `app/src/main/res/values-pt/strings.xml`

- [ ] **Step 1: Add 42 strings after `achievement_new_record_desc` (line 182)**

```xml
    <!-- Conquistas novas — sequência de derrotas -->
    <string name="achievement_loss_2_name">Outra Vez</string>
    <string name="achievement_loss_2_desc">Perdeu 2 partidas seguidas. Coincidência?</string>
    <string name="achievement_loss_3_name">Três é Demais</string>
    <string name="achievement_loss_3_desc">3 derrotas consecutivas. Ou talvez não</string>
    <string name="achievement_loss_5_name">Tem Certeza que Sabe Jogar?</string>
    <string name="achievement_loss_5_desc">5 derrotas seguidas. Que tal o tutorial?</string>
    <string name="achievement_loss_7_name">A Culpa é do Celular</string>
    <string name="achievement_loss_7_desc">7 seguidas. Não é o celular</string>
    <string name="achievement_loss_10_name">Mestre da Derrota</string>
    <string name="achievement_loss_10_desc">10 derrotas consecutivas. Talento raro</string>
    <string name="achievement_big_loser_name">Perseverança Infinita</string>
    <string name="achievement_big_loser_desc">100 derrotas no total</string>
    <!-- Conquistas novas — estilo -->
    <string name="achievement_comeback_2_name">Só Estava Aquecendo</string>
    <string name="achievement_comeback_2_desc">Vença após 2 derrotas consecutivas</string>
    <string name="achievement_slow_win_name">Devagar se Vai Longe</string>
    <string name="achievement_slow_win_desc">Vença em mais de 15 minutos</string>
    <string name="achievement_hint_hero_name">Com Ajudinha</string>
    <string name="achievement_hint_hero_desc">Vença usando 5 ou mais dicas</string>
    <string name="achievement_hint_addict_name">Viciado em Dicas</string>
    <string name="achievement_hint_addict_desc">Usou dicas nas últimas 5 partidas seguidas</string>
    <string name="achievement_perfectionist_name">Perfeccionista</string>
    <string name="achievement_perfectionist_desc">Vença 3 partidas seguidas sem dicas nem movimento automático</string>
    <string name="achievement_speed_freak_name">Rápido e Furioso</string>
    <string name="achievement_speed_freak_desc">Vença 3 partidas consecutivas em menos de 2 minutos</string>
    <string name="achievement_lunch_win_name">Pausa Produtiva</string>
    <string name="achievement_lunch_win_desc">Vença entre 12h e 14h</string>
    <string name="achievement_sunday_player_name">Domingo Relaxante</string>
    <string name="achievement_sunday_player_desc">Vença uma partida no domingo</string>
    <!-- Conquistas novas — sessões -->
    <string name="achievement_night_owl_3_name">Coruja Noturna</string>
    <string name="achievement_night_owl_3_desc">Jogue 3 partidas consecutivas após a meia-noite</string>
    <string name="achievement_same_day_3_name">Não Consigo Parar</string>
    <string name="achievement_same_day_3_desc">3 vitórias no mesmo dia</string>
    <string name="achievement_same_day_5_name">Dia de Profissional</string>
    <string name="achievement_same_day_5_desc">5 partidas jogadas no mesmo dia</string>
    <!-- Conquistas novas — feriados -->
    <string name="achievement_new_year_eve_name">Esperando a Meia-Noite</string>
    <string name="achievement_new_year_eve_desc">Jogue uma partida no dia 31 de dezembro</string>
    <string name="achievement_new_year_name">Ano Novo, Cartas Novas</string>
    <string name="achievement_new_year_desc">Abra o app no dia 1° de janeiro</string>
    <string name="achievement_halloween_name">Doce ou Travessura</string>
    <string name="achievement_halloween_desc">Abra o app no dia 31 de outubro</string>
    <string name="achievement_ferragosto_name">Ferragosto</string>
    <string name="achievement_ferragosto_desc">Abra o app no dia 15 de agosto</string>
```

- [ ] **Step 2: Change `stats_trophies_header` on line 197**
```xml
    <string name="stats_trophies_header">Troféus (%1$d / 55)</string>
```

- [ ] **Step 3: Commit**
```bash
git add app/src/main/res/values-pt/strings.xml
git commit -m "feat: add PT achievement strings and update trophies counter to 55"
```

---

## Task 10: Fix banner timing in `MainActivity.kt`

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/MainActivity.kt`

- [ ] **Step 1: Apply `postDelayed` fix in `onCreate`**

Find this block (lines 67–72):
```kotlin
        val achievementBanner = com.bottazzini.trasloco.utils.AchievementBanner(
            this, findViewById(R.id.mainBannerAchievement)
        )
        val newAchievements = com.bottazzini.trasloco.utils.AchievementEngine.create(applicationContext)
            .evaluate(com.bottazzini.trasloco.utils.AchievementTrigger.APP_OPENED)
        achievementBanner.enqueue(newAchievements)
```

Replace with:
```kotlin
        val bannerRoot = findViewById<View>(R.id.mainBannerAchievement)
        val achievementBanner = com.bottazzini.trasloco.utils.AchievementBanner(this, bannerRoot)
        val newAchievements = com.bottazzini.trasloco.utils.AchievementEngine.create(applicationContext)
            .evaluate(com.bottazzini.trasloco.utils.AchievementTrigger.APP_OPENED)
        bannerRoot.postDelayed({ achievementBanner.enqueue(newAchievements) }, 500L)
```

- [ ] **Step 2: Run tests — must pass**
```bash
cd /Users/bottazzini/Documents/Progetti/Trasloco && ./gradlew :app:testDebugUnitTest 2>&1 | tail -10
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/bottazzini/trasloco/MainActivity.kt
git commit -m "fix: delay APP_OPENED achievement banner until after splash transition"
```

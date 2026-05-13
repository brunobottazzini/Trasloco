# Statistiche & Trofei Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `RecordActivity` with a unified `StatsActivity` showing 6 aggregate stats, a time-series chart of the last 30 games (MPAndroidChart), and a 34-achievement trophy grid with in-game unlock banners.

**Architecture:** Two new SQLite tables (`game_log`, `achievements`) are added in DB v4; `AchievementEngine` evaluates conditions against those tables and `RecordsHandler`; unlock notifications surface as slide-in banners in `GameActivity` (losses/tutorial) and `YouWonActivity` (wins).

**Tech Stack:** Kotlin, SQLite (`DatabaseHandler` v4), MPAndroidChart v3.1.0 (JitPack), RecyclerView, BottomSheetDialog.

---

## File Map

**New files:**
- `app/src/main/java/com/bottazzini/trasloco/db/columns/GameLogColumns.kt`
- `app/src/main/java/com/bottazzini/trasloco/db/columns/AchievementsColumns.kt`
- `app/src/main/java/com/bottazzini/trasloco/settings/GameLog.kt`
- `app/src/main/java/com/bottazzini/trasloco/settings/GameLogRepository.kt`
- `app/src/main/java/com/bottazzini/trasloco/settings/AchievementsRepository.kt`
- `app/src/main/java/com/bottazzini/trasloco/utils/AchievementDef.kt`
- `app/src/main/java/com/bottazzini/trasloco/utils/AchievementTrigger.kt`
- `app/src/main/java/com/bottazzini/trasloco/utils/AchievementCatalog.kt`
- `app/src/main/java/com/bottazzini/trasloco/utils/AchievementEngine.kt`
- `app/src/main/java/com/bottazzini/trasloco/utils/AchievementBanner.kt`
- `app/src/main/java/com/bottazzini/trasloco/StatsActivity.kt`
- `app/src/main/java/com/bottazzini/trasloco/AchievementAdapter.kt`
- `app/src/main/res/layout/activity_stats.xml`
- `app/src/main/res/layout/item_achievement.xml`
- `app/src/main/res/layout/sheet_achievement_detail.xml`
- `app/src/main/res/layout/banner_achievement.xml`
- `app/src/test/java/com/bottazzini/trasloco/utils/AchievementEngineTest.kt`

**Modified files:**
- `app/build.gradle` — MPAndroidChart dep + JitPack
- `app/src/main/java/com/bottazzini/trasloco/db/DatabaseHandler.kt` — v3→v4
- `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt` — counters, game_log, engine, banner
- `app/src/main/java/com/bottazzini/trasloco/YouWonActivity.kt` — engine, banner
- `app/src/main/java/com/bottazzini/trasloco/MainActivity.kt` — navigate to StatsActivity
- `app/src/main/AndroidManifest.xml` — declare StatsActivity
- `app/src/main/res/layout/game.xml` — include banner
- `app/src/main/res/layout-land/game.xml` — include banner
- `app/src/main/res/layout/activity_you_won.xml` — include banner
- `app/src/main/res/values/strings.xml` — achievement strings
- `app/src/main/res/values-it/strings.xml` — achievement strings IT
- `app/src/main/res/values-pt/strings.xml` — achievement strings PT

---

## Task 1: DB Schema — GameLogColumns, AchievementsColumns, DatabaseHandler v4

**Files:**
- Create: `app/src/main/java/com/bottazzini/trasloco/db/columns/GameLogColumns.kt`
- Create: `app/src/main/java/com/bottazzini/trasloco/db/columns/AchievementsColumns.kt`
- Modify: `app/src/main/java/com/bottazzini/trasloco/db/DatabaseHandler.kt`

- [ ] **Step 1: Create GameLogColumns.kt**

```kotlin
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
```

- [ ] **Step 2: Create AchievementsColumns.kt**

```kotlin
package com.bottazzini.trasloco.db.columns

import android.provider.BaseColumns

object AchievementsColumns {
    object AchievementEntry : BaseColumns {
        const val TABLE_NAME = "achievements"
        const val COLUMN_ID = "id"
        const val COLUMN_UNLOCKED_AT = "unlocked_at"
    }
}
```

- [ ] **Step 3: Update DatabaseHandler.kt — bump version to 4, add new tables**

Read the current file first, then replace with:

```kotlin
package com.bottazzini.trasloco.db

import android.content.ContentValues.TAG
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
                    "${GameLogEntry._ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
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
```

- [ ] **Step 4: Build to verify compilation**

```bash
./gradlew compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/db/columns/GameLogColumns.kt \
        app/src/main/java/com/bottazzini/trasloco/db/columns/AchievementsColumns.kt \
        app/src/main/java/com/bottazzini/trasloco/db/DatabaseHandler.kt
git commit -m "feat(db): add game_log and achievements tables — DB v4"
```

---

## Task 2: GameLog data class + GameLogRepository

**Files:**
- Create: `app/src/main/java/com/bottazzini/trasloco/settings/GameLog.kt`
- Create: `app/src/main/java/com/bottazzini/trasloco/settings/GameLogRepository.kt`

- [ ] **Step 1: Create GameLog.kt**

```kotlin
package com.bottazzini.trasloco.settings

data class GameLog(
    val id: Long = 0,
    val timestamp: Long,
    val durationMs: Long,
    val won: Boolean,
    val hintsUsed: Int,
    val autoMoves: Int
)
```

- [ ] **Step 2: Create GameLogRepository.kt**

```kotlin
package com.bottazzini.trasloco.settings

import android.content.ContentValues
import android.content.Context
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
            "${GameLogEntry._ID} DESC", n.toString()
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
                "DELETE FROM ${GameLogEntry.TABLE_NAME} WHERE ${GameLogEntry._ID} NOT IN " +
                        "(SELECT ${GameLogEntry._ID} FROM ${GameLogEntry.TABLE_NAME} ORDER BY ${GameLogEntry._ID} DESC LIMIT 500)"
            )
        }
    }

    private fun android.database.Cursor.toGameLog() = GameLog(
        id = getLong(getColumnIndexOrThrow(GameLogEntry._ID)),
        timestamp = getLong(getColumnIndexOrThrow(GameLogEntry.COLUMN_TIMESTAMP)),
        durationMs = getLong(getColumnIndexOrThrow(GameLogEntry.COLUMN_DURATION_MS)),
        won = getLong(getColumnIndexOrThrow(GameLogEntry.COLUMN_WON)) == 1L,
        hintsUsed = getInt(getColumnIndexOrThrow(GameLogEntry.COLUMN_HINTS_USED)),
        autoMoves = getInt(getColumnIndexOrThrow(GameLogEntry.COLUMN_AUTO_MOVES))
    )
}
```

- [ ] **Step 3: Build to verify**

```bash
./gradlew compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/settings/GameLog.kt \
        app/src/main/java/com/bottazzini/trasloco/settings/GameLogRepository.kt
git commit -m "feat(db): add GameLog data class and GameLogRepository"
```

---

## Task 3: AchievementsRepository

**Files:**
- Create: `app/src/main/java/com/bottazzini/trasloco/settings/AchievementsRepository.kt`

- [ ] **Step 1: Create AchievementsRepository.kt**

```kotlin
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
```

- [ ] **Step 2: Build + commit**

```bash
./gradlew compileDebugKotlin
git add app/src/main/java/com/bottazzini/trasloco/settings/AchievementsRepository.kt
git commit -m "feat(db): add AchievementsRepository"
```

---

## Task 4: AchievementDef, AchievementTrigger, AchievementCatalog

**Files:**
- Create: `app/src/main/java/com/bottazzini/trasloco/utils/AchievementDef.kt`
- Create: `app/src/main/java/com/bottazzini/trasloco/utils/AchievementTrigger.kt`
- Create: `app/src/main/java/com/bottazzini/trasloco/utils/AchievementCatalog.kt`

- [ ] **Step 1: Create AchievementTrigger.kt**

```kotlin
package com.bottazzini.trasloco.utils

enum class AchievementTrigger {
    GAME_WON,
    GAME_LOST,
    TUTORIAL_COMPLETED,
    APP_OPENED
}
```

- [ ] **Step 2: Create AchievementDef.kt**

```kotlin
package com.bottazzini.trasloco.utils

data class AchievementDef(
    val id: String,
    val icon: String,
    val nameRes: Int,
    val descRes: Int
)
```

- [ ] **Step 3: Create AchievementCatalog.kt** (all 34 entries)

```kotlin
package com.bottazzini.trasloco.utils

import com.bottazzini.trasloco.R

object AchievementCatalog {
    val all: List<AchievementDef> = listOf(
        // --- Vittorie totali ---
        AchievementDef("first_win",   "🎉", R.string.achievement_first_win_name,   R.string.achievement_first_win_desc),
        AchievementDef("wins_10",     "🃏", R.string.achievement_wins_10_name,     R.string.achievement_wins_10_desc),
        AchievementDef("wins_50",     "🎰", R.string.achievement_wins_50_name,     R.string.achievement_wins_50_desc),
        AchievementDef("wins_100",    "🏆", R.string.achievement_wins_100_name,    R.string.achievement_wins_100_desc),
        AchievementDef("wins_500",    "👑", R.string.achievement_wins_500_name,    R.string.achievement_wins_500_desc),
        AchievementDef("wins_1000",   "♾️", R.string.achievement_wins_1000_name,   R.string.achievement_wins_1000_desc),
        // --- Streak ---
        AchievementDef("streak_3",   "🔥", R.string.achievement_streak_3_name,   R.string.achievement_streak_3_desc),
        AchievementDef("streak_6",   "🔥", R.string.achievement_streak_6_name,   R.string.achievement_streak_6_desc),
        AchievementDef("streak_9",   "🔥", R.string.achievement_streak_9_name,   R.string.achievement_streak_9_desc),
        AchievementDef("streak_12",  "💥", R.string.achievement_streak_12_name,  R.string.achievement_streak_12_desc),
        AchievementDef("streak_15",  "💥", R.string.achievement_streak_15_name,  R.string.achievement_streak_15_desc),
        AchievementDef("streak_18",  "💥", R.string.achievement_streak_18_name,  R.string.achievement_streak_18_desc),
        AchievementDef("streak_21",  "⚡", R.string.achievement_streak_21_name,  R.string.achievement_streak_21_desc),
        AchievementDef("streak_24",  "⚡", R.string.achievement_streak_24_name,  R.string.achievement_streak_24_desc),
        AchievementDef("streak_27",  "⚡", R.string.achievement_streak_27_name,  R.string.achievement_streak_27_desc),
        AchievementDef("streak_30",  "🌟", R.string.achievement_streak_30_name,  R.string.achievement_streak_30_desc),
        AchievementDef("streak_50",  "🌟", R.string.achievement_streak_50_name,  R.string.achievement_streak_50_desc),
        AchievementDef("streak_100", "💎", R.string.achievement_streak_100_name, R.string.achievement_streak_100_desc),
        // --- Velocità ---
        AchievementDef("speed_3min", "⏱",  R.string.achievement_speed_3min_name, R.string.achievement_speed_3min_desc),
        AchievementDef("speed_2min", "🚀",  R.string.achievement_speed_2min_name, R.string.achievement_speed_2min_desc),
        AchievementDef("speed_1min", "✈️",  R.string.achievement_speed_1min_name, R.string.achievement_speed_1min_desc),
        AchievementDef("speed_45s",  "🌪️", R.string.achievement_speed_45s_name,  R.string.achievement_speed_45s_desc),
        // --- Stile ---
        AchievementDef("tutorial_done", "📚", R.string.achievement_tutorial_done_name, R.string.achievement_tutorial_done_desc),
        AchievementDef("first_loss",    "😅", R.string.achievement_first_loss_name,    R.string.achievement_first_loss_desc),
        AchievementDef("games_50",      "🏋️", R.string.achievement_games_50_name,      R.string.achievement_games_50_desc),
        AchievementDef("games_200",     "🎪", R.string.achievement_games_200_name,     R.string.achievement_games_200_desc),
        AchievementDef("games_500",     "🌍", R.string.achievement_games_500_name,     R.string.achievement_games_500_desc),
        AchievementDef("hint_free",     "🎯", R.string.achievement_hint_free_name,     R.string.achievement_hint_free_desc),
        AchievementDef("no_assist",     "🧘", R.string.achievement_no_assist_name,     R.string.achievement_no_assist_desc),
        AchievementDef("resilient",     "💪", R.string.achievement_resilient_name,     R.string.achievement_resilient_desc),
        // --- Speciali ---
        AchievementDef("morning",     "🌅", R.string.achievement_morning_name,     R.string.achievement_morning_desc),
        AchievementDef("midnight",    "🌙", R.string.achievement_midnight_name,     R.string.achievement_midnight_desc),
        AchievementDef("christmas",   "🎄", R.string.achievement_christmas_name,   R.string.achievement_christmas_desc),
        AchievementDef("new_record",  "⭐", R.string.achievement_new_record_name,  R.string.achievement_new_record_desc)
    )

    fun findById(id: String): AchievementDef? = all.find { it.id == id }
}
```

- [ ] **Step 4: Build** (will fail on missing R.string resources — that is expected, strings added in Task 6)

```bash
./gradlew compileDebugKotlin 2>&1 | grep -E "error:|BUILD"
```

Expected: errors for missing R.string.achievement_* — strings will be added in Task 6. If only string errors, proceed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/AchievementDef.kt \
        app/src/main/java/com/bottazzini/trasloco/utils/AchievementTrigger.kt \
        app/src/main/java/com/bottazzini/trasloco/utils/AchievementCatalog.kt
git commit -m "feat(achievements): add AchievementDef, AchievementTrigger, AchievementCatalog (34 entries)"
```

---

## Task 5: AchievementEngine + unit tests

**Files:**
- Create: `app/src/main/java/com/bottazzini/trasloco/utils/AchievementEngine.kt`
- Create: `app/src/test/java/com/bottazzini/trasloco/utils/AchievementEngineTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
// app/src/test/java/com/bottazzini/trasloco/utils/AchievementEngineTest.kt
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
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew test --tests "com.bottazzini.trasloco.utils.AchievementEngineTest" 2>&1 | tail -20
```

Expected: compilation error `Unresolved reference: AchievementEngine`

- [ ] **Step 3: Create AchievementEngine.kt**

```kotlin
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
            lastFourGames: List<GameLog>,
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
                    }

                    // Resilient: [win, loss, loss, loss]
                    if (lastFourGames.size >= 4 &&
                        lastFourGames[0].won &&
                        !lastFourGames[1].won &&
                        !lastFourGames[2].won &&
                        !lastFourGames[3].won) candidates.add("resilient")

                    if (isNewTimeRecord) candidates.add("new_record")

                    // Partite giocate (conta su GAME_WON e GAME_LOST)
                    if (totalGames >= 50)  candidates.add("games_50")
                    if (totalGames >= 200) candidates.add("games_200")
                    if (totalGames >= 500) candidates.add("games_500")
                }

                AchievementTrigger.GAME_LOST -> {
                    val totalLosses = totalGames - totalWins
                    if (totalLosses >= 1) candidates.add("first_loss")
                    if (totalGames >= 50)  candidates.add("games_50")
                    if (totalGames >= 200) candidates.add("games_200")
                    if (totalGames >= 500) candidates.add("games_500")
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
    }

    /**
     * Evaluates which achievements are newly unlocked for the given trigger.
     * Persists new unlocks to DB and returns them for banner display.
     */
    fun evaluate(trigger: AchievementTrigger): List<AchievementDef> {
        val totalWins = recordsHandler.getTotalWins()
        val totalGames = gameLogRepo.countAll()
        val currentStreak = recordsHandler.readCurrentValue(Type.CONSECUTIVE) ?: 0L
        val lastFourGames = gameLogRepo.getLastN(4)
        val lastGame = lastFourGames.firstOrNull()
        val isNewTimeRecord = recordsHandler.readNew(Type.TIME) ?: false
        val now = System.currentTimeMillis()

        val candidateIds = evaluateConditions(
            trigger, totalWins, totalGames, currentStreak,
            lastGame, lastFourGames, isNewTimeRecord, now
        )

        val newIds = candidateIds.filter { !achievementsRepo.isUnlocked(it) }
        newIds.forEach { achievementsRepo.unlock(it, now) }

        return newIds.mapNotNull { AchievementCatalog.findById(it) }
    }
}
```

- [ ] **Step 4: Run tests — all must pass**

```bash
./gradlew test --tests "com.bottazzini.trasloco.utils.AchievementEngineTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL` — 14 tests passed, 0 failed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/AchievementEngine.kt \
        app/src/test/java/com/bottazzini/trasloco/utils/AchievementEngineTest.kt
git commit -m "feat(achievements): add AchievementEngine with evaluateConditions — 14 tests green"
```

---

## Task 6: MPAndroidChart dependency + i18n strings

**Files:**
- Modify: `app/build.gradle`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-it/strings.xml`
- Modify: `app/src/main/res/values-pt/strings.xml`

- [ ] **Step 1: Add JitPack + MPAndroidChart to app/build.gradle**

In `app/build.gradle`, add JitPack inside the existing `allprojects.repositories` block and add the chart dependency:

```groovy
allprojects {
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }   // ADD THIS LINE
    }
}
```

In the `dependencies` block, add:

```groovy
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
```

- [ ] **Step 2: Sync and verify**

```bash
./gradlew compileDebugKotlin 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Add achievement strings to values/strings.xml**

Add the following block inside the `<resources>` tag of `app/src/main/res/values/strings.xml`:

```xml
<!-- Achievement names and descriptions -->
<string name="achievement_first_win_name">First Win</string>
<string name="achievement_first_win_desc">You won your first game!</string>
<string name="achievement_wins_10_name">Player</string>
<string name="achievement_wins_10_desc">10 total wins</string>
<string name="achievement_wins_50_name">Regular</string>
<string name="achievement_wins_50_desc">50 total wins</string>
<string name="achievement_wins_100_name">Champion</string>
<string name="achievement_wins_100_desc">100 total wins</string>
<string name="achievement_wins_500_name">Legend</string>
<string name="achievement_wins_500_desc">500 total wins</string>
<string name="achievement_wins_1000_name">Eternal</string>
<string name="achievement_wins_1000_desc">1000 total wins</string>
<string name="achievement_streak_3_name">On a Roll</string>
<string name="achievement_streak_3_desc">3 wins in a row</string>
<string name="achievement_streak_6_name">Rhythm</string>
<string name="achievement_streak_6_desc">6 wins in a row</string>
<string name="achievement_streak_9_name">Focused</string>
<string name="achievement_streak_9_desc">9 wins in a row</string>
<string name="achievement_streak_12_name">Unstoppable</string>
<string name="achievement_streak_12_desc">12 wins in a row</string>
<string name="achievement_streak_15_name">Dominant</string>
<string name="achievement_streak_15_desc">15 wins in a row</string>
<string name="achievement_streak_18_name">Master</string>
<string name="achievement_streak_18_desc">18 wins in a row</string>
<string name="achievement_streak_21_name">Phenomenon</string>
<string name="achievement_streak_21_desc">21 wins in a row</string>
<string name="achievement_streak_24_name">Legendary</string>
<string name="achievement_streak_24_desc">24 wins in a row</string>
<string name="achievement_streak_27_name">Absolute</string>
<string name="achievement_streak_27_desc">27 wins in a row</string>
<string name="achievement_streak_30_name">Untouchable</string>
<string name="achievement_streak_30_desc">30 wins in a row</string>
<string name="achievement_streak_50_name">Immortal</string>
<string name="achievement_streak_50_desc">50 wins in a row</string>
<string name="achievement_streak_100_name">God of Trasloco</string>
<string name="achievement_streak_100_desc">100 wins in a row</string>
<string name="achievement_speed_3min_name">Lightning</string>
<string name="achievement_speed_3min_desc">Win a game in under 3 minutes</string>
<string name="achievement_speed_2min_name">Speedster</string>
<string name="achievement_speed_2min_desc">Win a game in under 2 minutes</string>
<string name="achievement_speed_1min_name">Supersonic</string>
<string name="achievement_speed_1min_desc">Win a game in under 1 minute</string>
<string name="achievement_speed_45s_name">Hurricane</string>
<string name="achievement_speed_45s_desc">Win a game in under 45 seconds</string>
<string name="achievement_tutorial_done_name">Self-Taught</string>
<string name="achievement_tutorial_done_desc">Completed the tutorial</string>
<string name="achievement_first_loss_name">Keep Trying</string>
<string name="achievement_first_loss_desc">Lost your first game</string>
<string name="achievement_games_50_name">Training</string>
<string name="achievement_games_50_desc">50 games played (won or lost)</string>
<string name="achievement_games_200_name">Marathon Runner</string>
<string name="achievement_games_200_desc">200 games played</string>
<string name="achievement_games_500_name">Obsession</string>
<string name="achievement_games_500_desc">500 games played</string>
<string name="achievement_hint_free_name">Purist</string>
<string name="achievement_hint_free_desc">Win a game without using hints</string>
<string name="achievement_no_assist_name">Zen</string>
<string name="achievement_no_assist_desc">Win without hints or auto-move</string>
<string name="achievement_resilient_name">Resilient</string>
<string name="achievement_resilient_desc">Win after 3 consecutive losses</string>
<string name="achievement_morning_name">Early Bird</string>
<string name="achievement_morning_desc">Win a game before 7:00 AM</string>
<string name="achievement_midnight_name">Night Owl</string>
<string name="achievement_midnight_desc">Win a game after midnight</string>
<string name="achievement_christmas_name">Festive</string>
<string name="achievement_christmas_desc">Open the app on December 25th</string>
<string name="achievement_new_record_name">Absolute Record</string>
<string name="achievement_new_record_desc">Beat your personal speed record</string>
<!-- Stats screen labels -->
<string name="stats_title">Statistics &amp; Trophies</string>
<string name="stats_best_time">⏱ Best time</string>
<string name="stats_streak_record">🔥 Best streak</string>
<string name="stats_total_wins">🏆 Total wins</string>
<string name="stats_games_played">🎮 Games played</string>
<string name="stats_win_rate">✅ Win rate</string>
<string name="stats_avg_time">⌛ Avg. time (wins)</string>
<string name="stats_no_data">—</string>
<string name="stats_chart_title">Last 30 games</string>
<string name="stats_chart_empty">Play your first game to see the chart</string>
<string name="stats_trophies_header">Trophies (%1$d / 34)</string>
<string name="stats_locked">Not unlocked yet</string>
<string name="stats_unlocked_on">Unlocked on %1$s at %2$s</string>
<string name="achievement_banner_title">Achievement unlocked!</string>
```

- [ ] **Step 4: Add Italian strings to values-it/strings.xml**

Add inside `<resources>`:

```xml
<!-- Achievement names and descriptions -->
<string name="achievement_first_win_name">Prima vittoria</string>
<string name="achievement_first_win_desc">Hai vinto la tua prima partita!</string>
<string name="achievement_wins_10_name">Giocatore</string>
<string name="achievement_wins_10_desc">10 vittorie totali</string>
<string name="achievement_wins_50_name">Assiduo</string>
<string name="achievement_wins_50_desc">50 vittorie totali</string>
<string name="achievement_wins_100_name">Campione</string>
<string name="achievement_wins_100_desc">100 vittorie totali</string>
<string name="achievement_wins_500_name">Leggenda</string>
<string name="achievement_wins_500_desc">500 vittorie totali</string>
<string name="achievement_wins_1000_name">Eterno</string>
<string name="achievement_wins_1000_desc">1000 vittorie totali</string>
<string name="achievement_streak_3_name">In forma</string>
<string name="achievement_streak_3_desc">3 vittorie consecutive</string>
<string name="achievement_streak_6_name">Ritmo</string>
<string name="achievement_streak_6_desc">6 vittorie consecutive</string>
<string name="achievement_streak_9_name">Concentrato</string>
<string name="achievement_streak_9_desc">9 vittorie consecutive</string>
<string name="achievement_streak_12_name">Inarrestabile</string>
<string name="achievement_streak_12_desc">12 vittorie consecutive</string>
<string name="achievement_streak_15_name">Dominatore</string>
<string name="achievement_streak_15_desc">15 vittorie consecutive</string>
<string name="achievement_streak_18_name">Maestro</string>
<string name="achievement_streak_18_desc">18 vittorie consecutive</string>
<string name="achievement_streak_21_name">Fenomeno</string>
<string name="achievement_streak_21_desc">21 vittorie consecutive</string>
<string name="achievement_streak_24_name">Leggendario</string>
<string name="achievement_streak_24_desc">24 vittorie consecutive</string>
<string name="achievement_streak_27_name">Assoluto</string>
<string name="achievement_streak_27_desc">27 vittorie consecutive</string>
<string name="achievement_streak_30_name">Intoccabile</string>
<string name="achievement_streak_30_desc">30 vittorie consecutive</string>
<string name="achievement_streak_50_name">Immortale</string>
<string name="achievement_streak_50_desc">50 vittorie consecutive</string>
<string name="achievement_streak_100_name">Dio del Trasloco</string>
<string name="achievement_streak_100_desc">100 vittorie consecutive</string>
<string name="achievement_speed_3min_name">Fulmine</string>
<string name="achievement_speed_3min_desc">Vinci in meno di 3 minuti</string>
<string name="achievement_speed_2min_name">Velocista</string>
<string name="achievement_speed_2min_desc">Vinci in meno di 2 minuti</string>
<string name="achievement_speed_1min_name">Supersonico</string>
<string name="achievement_speed_1min_desc">Vinci in meno di 1 minuto</string>
<string name="achievement_speed_45s_name">Uragano</string>
<string name="achievement_speed_45s_desc">Vinci in meno di 45 secondi</string>
<string name="achievement_tutorial_done_name">Autodidatta</string>
<string name="achievement_tutorial_done_desc">Hai completato il tutorial</string>
<string name="achievement_first_loss_name">Ci vuole pazienza</string>
<string name="achievement_first_loss_desc">Hai perso la tua prima partita</string>
<string name="achievement_games_50_name">Allenamento</string>
<string name="achievement_games_50_desc">50 partite giocate (vinte o perse)</string>
<string name="achievement_games_200_name">Maratoneta</string>
<string name="achievement_games_200_desc">200 partite giocate</string>
<string name="achievement_games_500_name">Ossessione</string>
<string name="achievement_games_500_desc">500 partite giocate</string>
<string name="achievement_hint_free_name">Purista</string>
<string name="achievement_hint_free_desc">Vinci una partita senza usare i suggerimenti</string>
<string name="achievement_no_assist_name">Zen</string>
<string name="achievement_no_assist_desc">Vinci senza suggerimenti né auto-mossa</string>
<string name="achievement_resilient_name">Resiliente</string>
<string name="achievement_resilient_desc">Vinci dopo 3 sconfitte consecutive</string>
<string name="achievement_morning_name">Mattiniero</string>
<string name="achievement_morning_desc">Vinci una partita prima delle 7:00</string>
<string name="achievement_midnight_name">Nottambulo</string>
<string name="achievement_midnight_desc">Vinci una partita dopo mezzanotte</string>
<string name="achievement_christmas_name">Festivo</string>
<string name="achievement_christmas_desc">Apri l\'app il 25 dicembre</string>
<string name="achievement_new_record_name">Record assoluto</string>
<string name="achievement_new_record_desc">Batti il tuo record personale di velocità</string>
<!-- Stats screen labels -->
<string name="stats_title">Statistiche &amp; Trofei</string>
<string name="stats_best_time">⏱ Miglior tempo</string>
<string name="stats_streak_record">🔥 Streak record</string>
<string name="stats_total_wins">🏆 Vittorie totali</string>
<string name="stats_games_played">🎮 Partite giocate</string>
<string name="stats_win_rate">✅ % vittorie</string>
<string name="stats_avg_time">⌛ Tempo medio (vittorie)</string>
<string name="stats_no_data">—</string>
<string name="stats_chart_title">Ultime 30 partite</string>
<string name="stats_chart_empty">Gioca la tua prima partita per vedere il grafico</string>
<string name="stats_trophies_header">Trofei (%1$d / 34)</string>
<string name="stats_locked">Non ancora sbloccato</string>
<string name="stats_unlocked_on">Sbloccato il %1$s alle %2$s</string>
<string name="achievement_banner_title">Achievement sbloccato!</string>
```

- [ ] **Step 5: Add Portuguese strings to values-pt/strings.xml**

Add inside `<resources>`:

```xml
<!-- Achievement names and descriptions -->
<string name="achievement_first_win_name">Primeira vitória</string>
<string name="achievement_first_win_desc">Você ganhou sua primeira partida!</string>
<string name="achievement_wins_10_name">Jogador</string>
<string name="achievement_wins_10_desc">10 vitórias totais</string>
<string name="achievement_wins_50_name">Assíduo</string>
<string name="achievement_wins_50_desc">50 vitórias totais</string>
<string name="achievement_wins_100_name">Campeão</string>
<string name="achievement_wins_100_desc">100 vitórias totais</string>
<string name="achievement_wins_500_name">Lenda</string>
<string name="achievement_wins_500_desc">500 vitórias totais</string>
<string name="achievement_wins_1000_name">Eterno</string>
<string name="achievement_wins_1000_desc">1000 vitórias totais</string>
<string name="achievement_streak_3_name">Em forma</string>
<string name="achievement_streak_3_desc">3 vitórias consecutivas</string>
<string name="achievement_streak_6_name">Ritmo</string>
<string name="achievement_streak_6_desc">6 vitórias consecutivas</string>
<string name="achievement_streak_9_name">Concentrado</string>
<string name="achievement_streak_9_desc">9 vitórias consecutivas</string>
<string name="achievement_streak_12_name">Imparável</string>
<string name="achievement_streak_12_desc">12 vitórias consecutivas</string>
<string name="achievement_streak_15_name">Dominador</string>
<string name="achievement_streak_15_desc">15 vitórias consecutivas</string>
<string name="achievement_streak_18_name">Mestre</string>
<string name="achievement_streak_18_desc">18 vitórias consecutivas</string>
<string name="achievement_streak_21_name">Fenômeno</string>
<string name="achievement_streak_21_desc">21 vitórias consecutivas</string>
<string name="achievement_streak_24_name">Lendário</string>
<string name="achievement_streak_24_desc">24 vitórias consecutivas</string>
<string name="achievement_streak_27_name">Absoluto</string>
<string name="achievement_streak_27_desc">27 vitórias consecutivas</string>
<string name="achievement_streak_30_name">Intocável</string>
<string name="achievement_streak_30_desc">30 vitórias consecutivas</string>
<string name="achievement_streak_50_name">Imortal</string>
<string name="achievement_streak_50_desc">50 vitórias consecutivas</string>
<string name="achievement_streak_100_name">Deus do Trasloco</string>
<string name="achievement_streak_100_desc">100 vitórias consecutivas</string>
<string name="achievement_speed_3min_name">Relâmpago</string>
<string name="achievement_speed_3min_desc">Vença em menos de 3 minutos</string>
<string name="achievement_speed_2min_name">Veloz</string>
<string name="achievement_speed_2min_desc">Vença em menos de 2 minutos</string>
<string name="achievement_speed_1min_name">Supersônico</string>
<string name="achievement_speed_1min_desc">Vença em menos de 1 minuto</string>
<string name="achievement_speed_45s_name">Furacão</string>
<string name="achievement_speed_45s_desc">Vença em menos de 45 segundos</string>
<string name="achievement_tutorial_done_name">Autodidata</string>
<string name="achievement_tutorial_done_desc">Concluiu o tutorial</string>
<string name="achievement_first_loss_name">Paciência</string>
<string name="achievement_first_loss_desc">Perdeu sua primeira partida</string>
<string name="achievement_games_50_name">Treino</string>
<string name="achievement_games_50_desc">50 partidas jogadas (ganhas ou perdidas)</string>
<string name="achievement_games_200_name">Maratonista</string>
<string name="achievement_games_200_desc">200 partidas jogadas</string>
<string name="achievement_games_500_name">Obsessão</string>
<string name="achievement_games_500_desc">500 partidas jogadas</string>
<string name="achievement_hint_free_name">Purista</string>
<string name="achievement_hint_free_desc">Vença uma partida sem usar dicas</string>
<string name="achievement_no_assist_name">Zen</string>
<string name="achievement_no_assist_desc">Vença sem dicas nem movimento automático</string>
<string name="achievement_resilient_name">Resiliente</string>
<string name="achievement_resilient_desc">Vença após 3 derrotas consecutivas</string>
<string name="achievement_morning_name">Madrugador</string>
<string name="achievement_morning_desc">Vença uma partida antes das 7h</string>
<string name="achievement_midnight_name">Noturno</string>
<string name="achievement_midnight_desc">Vença uma partida após a meia-noite</string>
<string name="achievement_christmas_name">Festivo</string>
<string name="achievement_christmas_desc">Abra o app no dia 25 de dezembro</string>
<string name="achievement_new_record_name">Recorde absoluto</string>
<string name="achievement_new_record_desc">Bata seu recorde pessoal de velocidade</string>
<!-- Stats screen labels -->
<string name="stats_title">Estatísticas &amp; Troféus</string>
<string name="stats_best_time">⏱ Melhor tempo</string>
<string name="stats_streak_record">🔥 Melhor sequência</string>
<string name="stats_total_wins">🏆 Vitórias totais</string>
<string name="stats_games_played">🎮 Partidas jogadas</string>
<string name="stats_win_rate">✅ % vitórias</string>
<string name="stats_avg_time">⌛ Tempo médio (vitórias)</string>
<string name="stats_no_data">—</string>
<string name="stats_chart_title">Últimas 30 partidas</string>
<string name="stats_chart_empty">Jogue sua primeira partida para ver o gráfico</string>
<string name="stats_trophies_header">Troféus (%1$d / 34)</string>
<string name="stats_locked">Ainda não desbloqueado</string>
<string name="stats_unlocked_on">Desbloqueado em %1$s às %2$s</string>
<string name="achievement_banner_title">Conquista desbloqueada!</string>
```

- [ ] **Step 6: Full build to verify all strings resolve**

```bash
./gradlew compileDebugKotlin 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add app/build.gradle \
        app/src/main/res/values/strings.xml \
        app/src/main/res/values-it/strings.xml \
        app/src/main/res/values-pt/strings.xml
git commit -m "feat(achievements): add MPAndroidChart dependency and all 34 achievement i18n strings"
```

---

## Task 7: AchievementBanner — layout + animation class

**Files:**
- Create: `app/src/main/res/layout/banner_achievement.xml`
- Create: `app/src/main/java/com/bottazzini/trasloco/utils/AchievementBanner.kt`

- [ ] **Step 1: Create banner_achievement.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/achievementBannerRoot"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@drawable/casino_tutorial_banner_bg"
    android:orientation="horizontal"
    android:paddingHorizontal="16dp"
    android:paddingVertical="12dp"
    android:gravity="center_vertical"
    android:visibility="gone">

    <TextView
        android:id="@+id/achievementBannerIcon"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="28sp"
        android:layout_marginEnd="12dp" />

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/achievement_banner_title"
            android:textSize="10sp"
            android:fontFamily="serif"
            android:textStyle="italic"
            android:textColor="@color/casino_gold_alpha_50"
            android:letterSpacing="0.1" />

        <TextView
            android:id="@+id/achievementBannerName"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textSize="15sp"
            android:fontFamily="serif"
            android:textStyle="italic|bold"
            android:textColor="@color/casino_gold" />

        <TextView
            android:id="@+id/achievementBannerDesc"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textSize="11sp"
            android:fontFamily="serif"
            android:textStyle="italic"
            android:textColor="@color/casino_gold_alpha_50" />

    </LinearLayout>

</LinearLayout>
```

- [ ] **Step 2: Create AchievementBanner.kt**

```kotlin
package com.bottazzini.trasloco.utils

import android.content.Context
import android.view.View
import android.widget.TextView
import com.bottazzini.trasloco.R
import java.util.LinkedList

class AchievementBanner(
    private val context: Context,
    private val bannerRoot: View
) {
    private val queue: LinkedList<AchievementDef> = LinkedList()
    private var isShowing = false

    private val iconView: TextView = bannerRoot.findViewById(R.id.achievementBannerIcon)
    private val nameView: TextView = bannerRoot.findViewById(R.id.achievementBannerName)
    private val descView: TextView = bannerRoot.findViewById(R.id.achievementBannerDesc)

    fun enqueue(achievements: List<AchievementDef>) {
        if (achievements.isEmpty()) return
        queue.addAll(achievements)
        if (!isShowing) showNext()
    }

    private fun showNext() {
        val def = queue.poll() ?: run { isShowing = false; return }
        isShowing = true

        iconView.text = def.icon
        nameView.text = context.getString(def.nameRes)
        descView.text = context.getString(def.descRes)

        bannerRoot.translationY = -bannerRoot.height.toFloat().coerceAtLeast(200f)
        bannerRoot.visibility = View.VISIBLE

        bannerRoot.animate()
            .translationY(0f)
            .setDuration(300)
            .withEndAction {
                bannerRoot.postDelayed({
                    bannerRoot.animate()
                        .translationY(-bannerRoot.height.toFloat().coerceAtLeast(200f))
                        .setDuration(300)
                        .withEndAction {
                            bannerRoot.visibility = View.GONE
                            showNext()
                        }
                        .start()
                }, 2500)
            }
            .start()
    }
}
```

- [ ] **Step 3: Build + commit**

```bash
./gradlew compileDebugKotlin 2>&1 | tail -5
git add app/src/main/res/layout/banner_achievement.xml \
        app/src/main/java/com/bottazzini/trasloco/utils/AchievementBanner.kt
git commit -m "feat(achievements): add AchievementBanner layout and animation class"
```

---

## Task 8: StatsActivity — layout, activity class, manifest, navigation

**Files:**
- Create: `app/src/main/res/layout/activity_stats.xml`
- Create: `app/src/main/res/layout/item_achievement.xml`
- Create: `app/src/main/res/layout/sheet_achievement_detail.xml`
- Create: `app/src/main/java/com/bottazzini/trasloco/AchievementAdapter.kt`
- Create: `app/src/main/java/com/bottazzini/trasloco/StatsActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/bottazzini/trasloco/MainActivity.kt`

- [ ] **Step 1: Create item_achievement.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="4dp"
    app:cardBackgroundColor="@color/casino_overlay_dark"
    app:cardCornerRadius="8dp">

    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="8dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center"
            android:orientation="vertical">

            <TextView
                android:id="@+id/achievementItemIcon"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="28sp" />

            <TextView
                android:id="@+id/achievementItemName"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="4dp"
                android:fontFamily="serif"
                android:gravity="center"
                android:maxLines="2"
                android:textSize="9sp"
                android:textStyle="italic" />

        </LinearLayout>

        <!-- Lock overlay (shown when not unlocked) -->
        <TextView
            android:id="@+id/achievementItemLock"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="bottom|end"
            android:text="🔒"
            android:textSize="12sp"
            android:visibility="gone" />

    </FrameLayout>

</androidx.cardview.widget.CardView>
```

- [ ] **Step 2: Create sheet_achievement_detail.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="24dp"
    android:gravity="center">

    <TextView
        android:id="@+id/detailIcon"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="48sp" />

    <TextView
        android:id="@+id/detailName"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:fontFamily="serif"
        android:textSize="20sp"
        android:textStyle="italic|bold"
        android:textColor="@color/casino_gold" />

    <TextView
        android:id="@+id/detailDesc"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:fontFamily="serif"
        android:gravity="center"
        android:textSize="14sp"
        android:textStyle="italic"
        android:textColor="@color/casino_gold_alpha_50" />

    <TextView
        android:id="@+id/detailStatus"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:fontFamily="serif"
        android:textSize="12sp"
        android:textStyle="italic"
        android:textColor="@color/casino_gold_alpha_50" />

</LinearLayout>
```

- [ ] **Step 3: Create activity_stats.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/statsScrollView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/verde"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="20dp">

        <!-- Title -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="32dp"
            android:fontFamily="serif"
            android:gravity="center"
            android:text="@string/stats_title"
            android:textColor="@color/casino_gold"
            android:textSize="22sp"
            android:textStyle="italic"
            android:letterSpacing="0.15" />

        <!-- Stats card -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="20dp"
            android:background="@drawable/casino_tile_bg"
            android:orientation="vertical"
            android:padding="16dp">

            <include android:id="@+id/statRowBestTime"    layout="@layout/item_stat_row" />
            <include android:id="@+id/statRowStreak"      layout="@layout/item_stat_row" />
            <include android:id="@+id/statRowTotalWins"   layout="@layout/item_stat_row" />
            <include android:id="@+id/statRowGamesPlayed" layout="@layout/item_stat_row" />
            <include android:id="@+id/statRowWinRate"     layout="@layout/item_stat_row" />
            <include android:id="@+id/statRowAvgTime"     layout="@layout/item_stat_row" />

        </LinearLayout>

        <!-- Chart section -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:fontFamily="serif"
            android:gravity="center"
            android:text="@string/stats_chart_title"
            android:textColor="@color/casino_gold_alpha_50"
            android:textSize="12sp"
            android:textStyle="italic"
            android:letterSpacing="0.2" />

        <com.github.mikephil.charting.charts.LineChart
            android:id="@+id/statsChart"
            android:layout_width="match_parent"
            android:layout_height="200dp"
            android:layout_marginTop="8dp" />

        <TextView
            android:id="@+id/statsChartEmpty"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:fontFamily="serif"
            android:gravity="center"
            android:text="@string/stats_chart_empty"
            android:textColor="@color/casino_gold_alpha_50"
            android:textSize="12sp"
            android:textStyle="italic"
            android:visibility="gone" />

        <!-- Trophies header -->
        <TextView
            android:id="@+id/statsTrophiesHeader"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:fontFamily="serif"
            android:gravity="center"
            android:textColor="@color/casino_gold_alpha_50"
            android:textSize="12sp"
            android:textStyle="italic"
            android:letterSpacing="0.2" />

        <!-- Achievement grid -->
        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/achievementsGrid"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:layout_marginBottom="32dp"
            android:nestedScrollingEnabled="false" />

    </LinearLayout>
</ScrollView>
```

- [ ] **Step 4: Create item_stat_row.xml** (shared row layout for the stats card)

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginVertical="4dp"
    android:orientation="horizontal">

    <TextView
        android:id="@+id/statRowLabel"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:fontFamily="serif"
        android:textColor="@color/casino_gold_alpha_50"
        android:textSize="13sp"
        android:textStyle="italic" />

    <TextView
        android:id="@+id/statRowValue"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:fontFamily="serif"
        android:textColor="@color/casino_gold"
        android:textSize="13sp"
        android:textStyle="italic|bold" />

</LinearLayout>
```

- [ ] **Step 5: Create AchievementAdapter.kt**

```kotlin
package com.bottazzini.trasloco

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bottazzini.trasloco.utils.AchievementDef

class AchievementAdapter(
    private val context: Context,
    private val defs: List<AchievementDef>,
    private val unlockedMap: Map<String, Long>,
    private val onTap: (AchievementDef, isUnlocked: Boolean, unlockedAt: Long?) -> Unit
) : RecyclerView.Adapter<AchievementAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: TextView = view.findViewById(R.id.achievementItemIcon)
        val name: TextView = view.findViewById(R.id.achievementItemName)
        val lock: TextView = view.findViewById(R.id.achievementItemLock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val def = defs[position]
        val unlockedAt = unlockedMap[def.id]
        val isUnlocked = unlockedAt != null

        holder.icon.text = def.icon
        holder.name.text = context.getString(def.nameRes)
        holder.name.alpha = if (isUnlocked) 1f else 0.35f
        holder.icon.alpha = if (isUnlocked) 1f else 0.35f
        holder.lock.visibility = if (isUnlocked) View.GONE else View.VISIBLE

        holder.itemView.setOnClickListener {
            onTap(def, isUnlocked, unlockedAt)
        }
    }

    override fun getItemCount() = defs.size
}
```

- [ ] **Step 6: Create StatsActivity.kt**

```kotlin
package com.bottazzini.trasloco

import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bottazzini.trasloco.settings.AchievementsRepository
import com.bottazzini.trasloco.settings.GameLogRepository
import com.bottazzini.trasloco.settings.RecordsHandler
import com.bottazzini.trasloco.settings.SettingsHandler
import com.bottazzini.trasloco.settings.Type
import com.bottazzini.trasloco.utils.AchievementCatalog
import com.bottazzini.trasloco.utils.ResourceUtils
import com.bottazzini.trasloco.utils.TimeUtils
import com.bottazzini.trasloco.utils.WindowInsetsUtils
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatsActivity : AppCompatActivity() {

    private lateinit var recordsHandler: RecordsHandler
    private lateinit var gameLogRepo: GameLogRepository
    private lateinit var achievementsRepo: AchievementsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_stats)
        WindowInsetsUtils.applySystemBarInsets(window, findViewById(R.id.statsScrollView))
        supportActionBar?.hide()

        recordsHandler = RecordsHandler(applicationContext)
        gameLogRepo = GameLogRepository(applicationContext)
        achievementsRepo = AchievementsRepository(applicationContext)

        val settingsHandler = SettingsHandler(applicationContext)
        val backgroundConf = settingsHandler.readValue(com.bottazzini.trasloco.settings.Configuration.BACKGROUND.value) ?: "verde"
        val drawable = ResourceUtils.getDrawableByName(resources, packageName, backgroundConf)
        findViewById<View>(R.id.statsScrollView).background = ContextCompat.getDrawable(this, drawable)

        loadStats()
        loadChart()
        loadAchievements()
    }

    private fun loadStats() {
        val bestTime = recordsHandler.getBestTime()
        val streakRecord = recordsHandler.readValue(Type.CONSECUTIVE) ?: 0L
        val totalWins = recordsHandler.getTotalWins()
        val totalGames = gameLogRepo.countAll()
        val winRate = if (totalGames > 0) (gameLogRepo.countWins() * 100L / totalGames) else null
        val avgTime = gameLogRepo.avgWinDurationMs()

        setStatRow(R.id.statRowBestTime, getString(R.string.stats_best_time),
            if (bestTime != null) TimeUtils.formatTime(bestTime) else getString(R.string.stats_no_data))
        setStatRow(R.id.statRowStreak, getString(R.string.stats_streak_record), streakRecord.toString())
        setStatRow(R.id.statRowTotalWins, getString(R.string.stats_total_wins), totalWins.toString())
        setStatRow(R.id.statRowGamesPlayed, getString(R.string.stats_games_played), totalGames.toString())
        setStatRow(R.id.statRowWinRate, getString(R.string.stats_win_rate),
            if (winRate != null) "$winRate%" else getString(R.string.stats_no_data))
        setStatRow(R.id.statRowAvgTime, getString(R.string.stats_avg_time),
            if (avgTime != null) TimeUtils.formatTime(avgTime) else getString(R.string.stats_no_data))
    }

    private fun setStatRow(rowId: Int, label: String, value: String) {
        val row = findViewById<View>(rowId)
        row.findViewById<TextView>(R.id.statRowLabel).text = label
        row.findViewById<TextView>(R.id.statRowValue).text = value
    }

    private fun loadChart() {
        val chart = findViewById<LineChart>(R.id.statsChart)
        val emptyLabel = findViewById<TextView>(R.id.statsChartEmpty)
        val games = gameLogRepo.getLastN(30).reversed()

        if (games.isEmpty()) {
            chart.isVisible = false
            emptyLabel.isVisible = true
            return
        }

        chart.isVisible = true
        emptyLabel.isVisible = false

        val goldColor = ContextCompat.getColor(this, R.color.casino_gold)
        val greenColor = ContextCompat.getColor(this, android.R.color.holo_green_dark)
        val redColor = ContextCompat.getColor(this, android.R.color.holo_red_dark)

        val entries = games.mapIndexed { idx, game ->
            Entry(idx.toFloat(), game.durationMs / 60_000f)
        }
        val circleColors = games.map { if (it.won) greenColor else redColor }

        val dataSet = LineDataSet(entries, "").apply {
            color = goldColor
            setCircleColors(circleColors)
            circleRadius = 4f
            lineWidth = 1.5f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        chart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = goldColor
                axisLineColor = goldColor
                gridColor = (goldColor and 0x00FFFFFF) or 0x26000000
                setDrawGridLines(false)
                granularity = 1f
            }
            axisLeft.apply {
                textColor = goldColor
                axisLineColor = goldColor
                gridColor = (goldColor and 0x00FFFFFF) or 0x26000000
            }
            axisRight.isEnabled = false
            invalidate()
        }
    }

    private fun loadAchievements() {
        val unlockedMap = achievementsRepo.getAllUnlocked()
        val unlockedCount = unlockedMap.size

        findViewById<TextView>(R.id.statsTrophiesHeader).text =
            getString(R.string.stats_trophies_header, unlockedCount)

        val recycler = findViewById<RecyclerView>(R.id.achievementsGrid)
        recycler.layoutManager = GridLayoutManager(this, 3)
        recycler.adapter = AchievementAdapter(
            context = this,
            defs = AchievementCatalog.all,
            unlockedMap = unlockedMap,
            onTap = { def, isUnlocked, unlockedAt -> showDetail(def, isUnlocked, unlockedAt) }
        )
    }

    private fun showDetail(def: com.bottazzini.trasloco.utils.AchievementDef, isUnlocked: Boolean, unlockedAt: Long?) {
        val sheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.sheet_achievement_detail, null)
        view.findViewById<TextView>(R.id.detailIcon).text = def.icon
        view.findViewById<TextView>(R.id.detailName).text = getString(def.nameRes)
        view.findViewById<TextView>(R.id.detailDesc).text = getString(def.descRes)
        view.findViewById<TextView>(R.id.detailStatus).text = if (isUnlocked && unlockedAt != null) {
            val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            val d = Date(unlockedAt)
            getString(R.string.stats_unlocked_on, dateFmt.format(d), timeFmt.format(d))
        } else {
            getString(R.string.stats_locked)
        }
        sheet.setContentView(view)
        sheet.show()
    }

    override fun onDestroy() {
        recordsHandler.close()
        super.onDestroy()
    }
}
```

- [ ] **Step 7: Register StatsActivity in AndroidManifest.xml**

Add inside the `<application>` block (after the existing activity declarations):

```xml
<activity android:name=".StatsActivity" />
```

- [ ] **Step 8: Update MainActivity.showRecords() to navigate to StatsActivity**

In `app/src/main/java/com/bottazzini/trasloco/MainActivity.kt`, find:

```kotlin
fun showRecords(view: View) {
    playSound(R.raw.change_activity)
    val intent = Intent(this, RecordActivity::class.java)
    startActivity(intent)
}
```

Replace with:

```kotlin
fun showRecords(view: View) {
    playSound(R.raw.change_activity)
    val intent = Intent(this, StatsActivity::class.java)
    startActivity(intent)
}
```

Also add the import at the top of MainActivity.kt if missing (it may already be there transitively, but add explicitly):

```kotlin
// This import is not needed if StatsActivity is in the same package — it will compile regardless.
```

- [ ] **Step 9: Build + verify**

```bash
./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL` — apk generated.

- [ ] **Step 10: Commit**

```bash
git add app/src/main/res/layout/activity_stats.xml \
        app/src/main/res/layout/item_achievement.xml \
        app/src/main/res/layout/item_stat_row.xml \
        app/src/main/res/layout/sheet_achievement_detail.xml \
        app/src/main/java/com/bottazzini/trasloco/AchievementAdapter.kt \
        app/src/main/java/com/bottazzini/trasloco/StatsActivity.kt \
        app/src/main/AndroidManifest.xml \
        app/src/main/java/com/bottazzini/trasloco/MainActivity.kt
git commit -m "feat(stats): add StatsActivity with stats card, chart, achievement grid and detail sheet"
```

---

## Task 9: GameActivity integration — counters, game_log, engine on loss, banner

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`
- Modify: `app/src/main/res/layout/game.xml`
- Modify: `app/src/main/res/layout-land/game.xml`

- [ ] **Step 1: Add banner include to game.xml**

Read `game.xml`. At the **end of the root ConstraintLayout**, just before the closing `</androidx.constraintlayout.widget.ConstraintLayout>` tag, add:

```xml
<include
    layout="@layout/banner_achievement"
    android:id="@+id/gameBannerAchievement"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    app:layout_constraintEnd_toEndOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintTop_toTopOf="parent" />
```

- [ ] **Step 2: Add banner include to game-land/game.xml**

Same as Step 1 but in `app/src/main/res/layout-land/game.xml`.

- [ ] **Step 3: Add instance fields to GameActivity**

In `GameActivity`, after the existing private var declarations (around line 75), add:

```kotlin
private var hintsUsedThisGame: Int = 0
private var autoMovesThisGame: Int = 0
private lateinit var gameLogRepo: com.bottazzini.trasloco.settings.GameLogRepository
private lateinit var achievementBanner: com.bottazzini.trasloco.utils.AchievementBanner
```

- [ ] **Step 4: Initialize repo and banner in onCreate**

In `GameActivity.onCreate`, after the `setContentView` call and existing initializations, add:

```kotlin
gameLogRepo = com.bottazzini.trasloco.settings.GameLogRepository(applicationContext)
achievementBanner = com.bottazzini.trasloco.utils.AchievementBanner(
    this,
    findViewById(R.id.gameBannerAchievement)
)
```

- [ ] **Step 5: Reset counters on new game start**

Find where a new game is initialized (look for the `zeroFill()` call or `setupNewGame()` equivalent). Add after the reset logic:

```kotlin
hintsUsedThisGame = 0
autoMovesThisGame = 0
```

If resume mode also resets the game state, reset there too. Search for `gameViewModel.hasActiveGame = true` and add the resets nearby.

- [ ] **Step 6: Increment hintsUsedThisGame in onClickHint**

In `onClickHint(view: View)` (line 1273), after `highlightHintMove(hint)`:

```kotlin
hintsUsedThisGame++
```

The full method becomes:

```kotlin
fun onClickHint(view: View) {
    if (!hintEnabled) {
        android.widget.Toast.makeText(this, getString(R.string.hint_no_moves), android.widget.Toast.LENGTH_SHORT).show()
        return
    }
    val hint = hintEngine.findFirstValidMove(cardTableMap, endDeckList)
    if (hint == null) {
        android.widget.Toast.makeText(this, getString(R.string.hint_no_moves), android.widget.Toast.LENGTH_SHORT).show()
        return
    }
    highlightHintMove(hint)
    hintsUsedThisGame++
}
```

- [ ] **Step 7: Increment autoMovesThisGame in triggerAutoMoveCycle**

In `triggerAutoMoveCycle()` (line 1311), after `val moved = tryMove(sourceView, targetView)` and inside the `if (moved)` block, add before the win/lost checks:

```kotlin
autoMovesThisGame++
```

- [ ] **Step 8: Insert game_log row and show banner in showYouLost**

In `showYouLost()` (line 670), after `stopTimer()` and before `gameViewModel.gameLost = true`:

```kotlin
val lostDurationMs = System.currentTimeMillis() - gameStartTimeMillis
gameLogRepo.insert(
    com.bottazzini.trasloco.settings.GameLog(
        timestamp = System.currentTimeMillis(),
        durationMs = lostDurationMs,
        won = false,
        hintsUsed = hintsUsedThisGame,
        autoMoves = autoMovesThisGame
    )
)
val newAchievements = com.bottazzini.trasloco.utils.AchievementEngine.create(this)
    .evaluate(com.bottazzini.trasloco.utils.AchievementTrigger.GAME_LOST)
achievementBanner.enqueue(newAchievements)
```

- [ ] **Step 9: Insert game_log row in showYouWon**

In `showYouWon()` (line 725), after the consecutive wins update block (around line 759) and before the `Intent(this, YouWonActivity::class.java)` line:

```kotlin
gameLogRepo.insert(
    com.bottazzini.trasloco.settings.GameLog(
        timestamp = System.currentTimeMillis(),
        durationMs = millisPassed,
        won = true,
        hintsUsed = hintsUsedThisGame,
        autoMoves = autoMovesThisGame
    )
)
```

Note: `millisPassed` is already computed at line 735 as `System.currentTimeMillis() - gameStartTimeMillis`.

- [ ] **Step 10: Build + verify**

```bash
./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 11: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt \
        app/src/main/res/layout/game.xml \
        app/src/main/res/layout-land/game.xml
git commit -m "feat(achievements): wire game_log insert, hint/auto-move counters, and loss-time banner in GameActivity"
```

---

## Task 10: YouWonActivity integration — engine on win, banner

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/YouWonActivity.kt`
- Modify: `app/src/main/res/layout/activity_you_won.xml`

- [ ] **Step 1: Add banner include to activity_you_won.xml**

Read `activity_you_won.xml`. Add the banner include at the **top of the root layout** (inside the outermost ViewGroup, as the first child), so it overlays the win screen from the top:

```xml
<include
    layout="@layout/banner_achievement"
    android:id="@+id/youWonBannerAchievement"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

If the root is a `ScrollView` or `ConstraintLayout`, wrap appropriately. If `ConstraintLayout`:

```xml
<include
    layout="@layout/banner_achievement"
    android:id="@+id/youWonBannerAchievement"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    app:layout_constraintEnd_toEndOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintTop_toTopOf="parent" />
```

- [ ] **Step 2: Wire AchievementEngine in YouWonActivity.onCreate**

In `YouWonActivity.kt`, add a field:

```kotlin
private lateinit var achievementBanner: com.bottazzini.trasloco.utils.AchievementBanner
```

In `onCreate`, after `setContentView`, initialize the banner:

```kotlin
achievementBanner = com.bottazzini.trasloco.utils.AchievementBanner(
    this,
    findViewById(R.id.youWonBannerAchievement)
)
```

After the block `if (!youWonViewModel.statsRecorded) { ... }` (which calls `incrementTotalWins`), add the achievement evaluation with a short delay so the win screen renders first:

```kotlin
findViewById<View>(R.id.youWonBannerAchievement).postDelayed({
    val newAchievements = com.bottazzini.trasloco.utils.AchievementEngine.create(applicationContext)
        .evaluate(com.bottazzini.trasloco.utils.AchievementTrigger.GAME_WON)
    achievementBanner.enqueue(newAchievements)
}, 600L)
```

- [ ] **Step 3: Build + verify**

```bash
./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/YouWonActivity.kt \
        app/src/main/res/layout/activity_you_won.xml
git commit -m "feat(achievements): evaluate GAME_WON achievements and show banner in YouWonActivity"
```

---

## Task 11: Tutorial + APP_OPENED triggers

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt` (TUTORIAL_COMPLETED trigger)
- Modify: `app/src/main/java/com/bottazzini/trasloco/MainActivity.kt` (APP_OPENED trigger)

- [ ] **Step 1: Fire TUTORIAL_COMPLETED when tutorial exits with a win**

In `GameActivity`, find `showTutorialExitDialog()` (around line 263) and the tutorial completion path. Search for where `isTutorialMode` causes `showYouWon()` to return early. The tutorial ends when the user completes all steps — find the call that marks it complete (look for `tutorialEngine?.isComplete` or the exit dialog).

Add after the tutorial is confirmed complete (inside the confirm-exit dialog positive button, or when the engine reports completion):

```kotlin
val newAchievements = com.bottazzini.trasloco.utils.AchievementEngine.create(this)
    .evaluate(com.bottazzini.trasloco.utils.AchievementTrigger.TUTORIAL_COMPLETED)
achievementBanner.enqueue(newAchievements)
```

- [ ] **Step 2: Fire APP_OPENED in MainActivity.onCreate**

In `MainActivity.kt`, in `onCreate`, after all initializations (after `recordsHandler.insertDefaultSettings()`), add:

```kotlin
com.bottazzini.trasloco.utils.AchievementEngine.create(applicationContext)
    .evaluate(com.bottazzini.trasloco.utils.AchievementTrigger.APP_OPENED)
```

No banner is shown for APP_OPENED (christmas is a silent unlock discoverable in the Trophies screen).

- [ ] **Step 3: Run all tests to verify nothing broke**

```bash
./gradlew test 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL` — all tests pass.

- [ ] **Step 4: Build final APK**

```bash
./gradlew assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt \
        app/src/main/java/com/bottazzini/trasloco/MainActivity.kt
git commit -m "feat(achievements): add TUTORIAL_COMPLETED and APP_OPENED triggers"
```

---

## Self-Review Checklist

After completing all tasks, verify:

- [ ] `./gradlew test` — all 14 AchievementEngineTest pass
- [ ] `./gradlew assembleDebug` — clean build
- [ ] Tapping Records tile on main menu opens StatsActivity (not RecordActivity)
- [ ] Playing and winning a game inserts a row in `game_log` (check via DB browser or log)
- [ ] Winning the first game shows the `first_win` banner in YouWonActivity
- [ ] Losing the first game shows the `first_loss` banner in GameActivity
- [ ] Tapping an achievement in the grid opens the detail sheet
- [ ] Chart renders for games with data; shows empty label when no games played
- [ ] AchievementCatalog.all has exactly 34 entries: `assert(AchievementCatalog.all.size == 34)`
- [ ] All achievement IDs are unique: `assert(AchievementCatalog.all.map { it.id }.distinct().size == 34)`

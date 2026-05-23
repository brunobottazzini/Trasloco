# Deal & Shuffle Intro Animations — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a shuffle intro animation (central ghost deck riffles → splits to 4 talloni) and a deal cascade animation (cards fly one-by-one from talloni to table slots) to New Game; add a faster deal cascade to Retry.

**Architecture:** New `DealAnimator` singleton (mirrors `CardAnimator` pattern) owns all intro animation logic. `GameActivity` adds `isIntroAnimating: Boolean` flag to suppress `setImage()` in `dealCard()` during setup — visuals are deferred to per-card `onLand` callbacks inside `DealEntry`. Skip: tap on `gameRoot` calls `DealAnimator.skip()` which cancels all pending `Runnable`s, removes ghost views, and fires `onComplete` immediately.

**Tech Stack:** Android `ValueAnimator`, `Handler.postDelayed`, existing `CardAnimator.animateCardFlight`, Kotlin `object` singleton, `WeakReference<ViewGroup>`

---

## File Map

| File | Change |
|------|--------|
| `app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt` | **Create** — `DealEntry` data class + full animation engine |
| `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt` | **Modify** — add `isIntroAnimating`, `buildDealEntries()`, wire `DealAnimator` in `startNewGame()` / `retryGame()` / `onDestroy()` |

---

### Task 1: Create `DealAnimator.kt` — skeleton, skip, deal cascade

**Files:**
- Create: `app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt`

- [ ] **Step 1: Create the file with `DealEntry` and the full skeleton**

```kotlin
package com.bottazzini.trasloco.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.drawable.Drawable
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import java.lang.ref.WeakReference

/**
 * One card to deal: source tallone view, target slot view, card drawable, and a callback
 * to invoke when the card visually lands (e.g. setImage + playSoundAtomic).
 * drawable == null → slot is empty, skip this entry.
 */
data class DealEntry(
    val sourceView: ImageView,
    val targetView: ImageView,
    val drawable: Drawable?,
    val onLand: () -> Unit
)

/**
 * Drives all New Game / Retry intro animations.
 *
 * playNewGame  — Phase 1 (riffle ghost) + Phase 2 (split to 4 talloni) + Phase 3 (cascade deal)
 * playRetry    — Phase 3 only (faster stagger)
 * skip         — cancel everything, fire onComplete immediately
 */
object DealAnimator {

    private val pendingRunnables = mutableListOf<Runnable>()
    private val ghostViews       = mutableListOf<View>()
    private var riffleAnim: ValueAnimator? = null
    private var handlerRef: Handler? = null
    private var rootRef: WeakReference<ViewGroup>? = null
    private var skipOnComplete: (() -> Unit)? = null

    // ── Public API ────────────────────────────────────────────────────────

    fun playNewGame(
        root: ViewGroup,
        talloneViews: List<ImageView>,
        dealEntries: List<DealEntry>,
        backDrawable: Drawable?,
        handler: Handler,
        onPhase1Done: () -> Unit,
        onComplete: () -> Unit
    ) {
        reset()
        handlerRef   = handler
        rootRef      = WeakReference(root)
        skipOnComplete = onComplete

        val deckW   = talloneViews.firstOrNull()?.width?.takeIf  { it > 0 } ?: 80
        val deckH   = talloneViews.firstOrNull()?.height?.takeIf { it > 0 } ?: 100
        val startTx = root.width  / 2f - deckW / 2f
        val startTy = root.height / 2f - deckH / 2f
        val dp8     = 8 * root.context.resources.displayMetrics.density

        // ── Phase 1: riffle ghost at screen centre (~400 ms) ──────────────
        val centralGhost = makeGhost(root, backDrawable, deckW, deckH, startTx, startTy)
        ghostViews.add(centralGhost)
        root.addView(centralGhost)

        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 400
            addUpdateListener { va ->
                val f = va.animatedFraction
                centralGhost.scaleX = when {
                    f < 0.33f -> 1f - (f / 0.33f) * 0.15f
                    f < 0.66f -> 0.85f + ((f - 0.33f) / 0.33f) * 0.20f
                    else      -> 1.05f - ((f - 0.66f) / 0.34f) * 0.05f
                }
                centralGhost.translationX =
                    startTx + Math.sin(f.toDouble() * Math.PI * 3).toFloat() * dp8
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // reset translationX to exact center so Phase 2 source position is stable
                    centralGhost.translationX = startTx
                }
            })
        }
        riffleAnim = anim
        anim.start()

        // ── 500 ms after playNewGame start: fire onPhase1Done + start Phase 2 ──
        val r = Runnable {
            onPhase1Done()
            playPhase2(root, centralGhost, talloneViews, backDrawable, handler) {
                val timings = cascadeTimings(staggerMs = 150L, roundGapMs = 250L)
                playDealCascade(root, dealEntries, timings, handler, onComplete)
            }
        }
        pendingRunnables.add(r)
        handler.postDelayed(r, 500L)
    }

    fun playRetry(
        root: ViewGroup,
        dealEntries: List<DealEntry>,
        handler: Handler,
        onComplete: () -> Unit
    ) {
        reset()
        handlerRef     = handler
        rootRef        = WeakReference(root)
        skipOnComplete = onComplete

        val timings = cascadeTimings(staggerMs = 100L, roundGapMs = 160L)
        playDealCascade(root, dealEntries, timings, handler, onComplete)
    }

    /** Cancel pending animations and call onComplete immediately. */
    fun skip() {
        riffleAnim?.cancel()
        riffleAnim = null
        val h = handlerRef
        val r = rootRef?.get()
        pendingRunnables.forEach { h?.removeCallbacks(it) }
        pendingRunnables.clear()
        ghostViews.forEach { r?.removeView(it) }
        ghostViews.clear()
        val cb = skipOnComplete
        skipOnComplete = null
        handlerRef = null
        rootRef    = null
        cb?.invoke()
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private fun reset() {
        riffleAnim?.cancel()
        riffleAnim = null
        val h = handlerRef
        val r = rootRef?.get()
        pendingRunnables.forEach { h?.removeCallbacks(it) }
        pendingRunnables.clear()
        ghostViews.forEach { r?.removeView(it) }
        ghostViews.clear()
        handlerRef     = null
        rootRef        = null
        skipOnComplete = null
    }

    private fun makeGhost(
        root: ViewGroup,
        drawable: Drawable?,
        w: Int, h: Int,
        tx: Float, ty: Float
    ): ImageView = ImageView(root.context).apply {
        setImageDrawable(drawable)
        scaleType  = ImageView.ScaleType.FIT_CENTER
        alpha      = 0.95f
        elevation  = 20f
        translationX = tx
        translationY = ty
        layoutParams = ConstraintLayout.LayoutParams(w, h)
    }

    /**
     * Phase 2: 4 ghost decks fly from centralGhost position to each tallone.
     * Stagger: 0 / 60 / 120 / 180 ms. Fires onAllLanded after the last ghost lands.
     */
    private fun playPhase2(
        root: ViewGroup,
        centralGhost: View,
        talloneViews: List<ImageView>,
        backDrawable: Drawable?,
        handler: Handler,
        onAllLanded: () -> Unit
    ) {
        var landedCount = 0
        val total = talloneViews.size

        talloneViews.forEachIndexed { index, tallone ->
            val r = Runnable {
                val currentRoot = rootRef?.get() ?: return@Runnable
                CardAnimator.animateCardFlight(
                    currentRoot, centralGhost, tallone, backDrawable, 220L
                ) {
                    landedCount++
                    if (landedCount == total) {
                        currentRoot.removeView(centralGhost)
                        ghostViews.remove(centralGhost)
                        onAllLanded()
                    }
                }
            }
            pendingRunnables.add(r)
            handler.postDelayed(r, index * 60L)
        }
    }

    /**
     * Phase 3 (deal cascade): fires each DealEntry at timings[originalIndex].
     * Entries with null drawable are skipped silently.
     * Fires onComplete after the last card's onLand callback.
     */
    private fun playDealCascade(
        root: ViewGroup,
        entries: List<DealEntry>,
        timings: List<Long>,
        handler: Handler,
        onComplete: () -> Unit
    ) {
        val animated = entries.mapIndexedNotNull { i, e ->
            if (e.drawable != null) Pair(i, e) else null
        }
        if (animated.isEmpty()) {
            cleanup()
            onComplete()
            return
        }

        var completedCount = 0
        val total = animated.size

        animated.forEach { (index, entry) ->
            val delay = timings.getOrElse(index) { index * 150L }
            val r = Runnable {
                val currentRoot = rootRef?.get() ?: return@Runnable
                CardAnimator.animateCardFlight(
                    currentRoot, entry.sourceView, entry.targetView, entry.drawable, 200L
                ) {
                    entry.onLand()
                    completedCount++
                    if (completedCount == total) {
                        cleanup()
                        onComplete()
                    }
                }
            }
            pendingRunnables.add(r)
            handler.postDelayed(r, delay)
        }
    }

    private fun cleanup() {
        val r = rootRef?.get()
        ghostViews.forEach { r?.removeView(it) }
        ghostViews.clear()
        pendingRunnables.clear()
        riffleAnim     = null
        handlerRef     = null
        rootRef        = null
        skipOnComplete = null
    }

    /**
     * Compute staggered timing for 3 rounds × 4 rows (12 entries total).
     *
     * Example with staggerMs=150, roundGapMs=250:
     *   Round 1 (slot 1): 0, 150, 300, 450 ms
     *   Round 2 (slot 2): 700, 850, 1000, 1150 ms
     *   Round 3 (slot 3): 1400, 1550, 1700, 1850 ms
     */
    private fun cascadeTimings(
        staggerMs: Long,
        roundGapMs: Long,
        rows: Int = 4,
        cols: Int = 3
    ): List<Long> {
        val result = mutableListOf<Long>()
        var roundStart = 0L
        for (col in 0 until cols) {
            for (row in 0 until rows) {
                result.add(roundStart + row * staggerMs)
            }
            roundStart += (rows - 1) * staggerMs + roundGapMs
        }
        return result
    }
}
```

- [ ] **Step 2: Verify it compiles**

```bash
cd /Users/bottazzini/Documents/Progetti/Trasloco
./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL` with no errors in `DealAnimator.kt`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt
git commit -m "feat: add DealAnimator with shuffle intro and deal cascade"
```

---

### Task 2: Add `isIntroAnimating` flag and `buildDealEntries()` to `GameActivity`

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

- [ ] **Step 1: Add `isIntroAnimating` field near the other boolean fields (~line 71)**

Find the line:
```kotlin
private var isInitializing = true
```

Add directly below it:
```kotlin
private var isIntroAnimating = false
```

- [ ] **Step 2: Modify `dealCard()` to respect `isIntroAnimating` (images AND badge)**

`dealCard()` currently calls `updateSourceDeck(line)` **before** the `isInitializing` check, so with
`isIntroAnimating=true` the badge jumps immediately to 7 (post-deal). We need to gate it.

Find the block in `dealCard()` that currently looks like:
```kotlin
        subDeckMap[line] = subDeck
        updateSourceDeck(line)   // ← new line

        if (deals.isEmpty()) return

        if (isInitializing) {
            // During board setup with no intro animation: apply images directly
            if (!isIntroAnimating) {
                deals.forEach { setImage(it.imageViewId, it.cardName) }
            }
            // With isIntroAnimating=true: state is already updated above; visuals
            // are deferred to DealEntry.onLand callbacks in DealAnimator.
            return
        }
```

Replace it with:
```kotlin
        subDeckMap[line] = subDeck
        // Skip badge/indicator update during intro animation:
        // buildDealEntries() will set the initial count (10) and decrement per-card in onLand.
        if (!isInitializing || !isIntroAnimating) {
            updateSourceDeck(line)
        }

        if (deals.isEmpty()) return

        if (isInitializing) {
            // During board setup with no intro animation: apply images directly
            if (!isIntroAnimating) {
                deals.forEach { setImage(it.imageViewId, it.cardName) }
            }
            // With isIntroAnimating=true: state is already updated above; visuals
            // are deferred to DealEntry.onLand callbacks in DealAnimator.
            return
        }
```

- [ ] **Step 3: Add `updateDeckBadge()` helper and `buildDealEntries()` private helper**

Add both methods in `GameActivity` (e.g. just before `setImage()`):

```kotlin
    /**
     * Updates only the textViewDeck badge for [line] ("1"–"4") to show [count].
     * Used by the intro animation to animate the deck counter while cards fly.
     */
    private fun updateDeckBadge(line: String, count: Int) {
        val badgeId = resources.getIdentifier("textViewDeck$line", "id", packageName)
        val badge = findViewById<TextView>(badgeId) ?: return
        if (count > 0) {
            badge.text  = count.toString()
            badge.visibility = View.VISIBLE
        } else {
            badge.text  = ""
            badge.visibility = View.INVISIBLE
        }
    }

    /**
     * Builds the 12 DealEntry objects for the intro cascade animation.
     * Order: col=1 rows 1-4, col=2 rows 1-4, col=3 rows 1-4 (cascade vertical order).
     * Entries for empty slots (card == "zero" or missing) have drawable=null and are skipped.
     *
     * Badge animation:
     *  - Sets each textViewDeck badge to the pre-deal count (post-deal + dealt, e.g. 10).
     *  - Each onLand decrements its deck's badge so the user sees 10→9→8→7 as cards fly.
     */
    private fun buildDealEntries(): List<DealEntry> {
        val entries = mutableListOf<DealEntry>()

        // Count how many cards were dealt per deck line (to compute pre-deal size).
        val dealtPerLine = mutableMapOf<String, Int>()
        for (col in 1..3) {
            for (row in 1..4) {
                val position = "$row$col"
                val cardName = cardTableMap[position]?.lastOrNull() ?: continue
                if (cardName == "zero") continue
                val line = row.toString()
                dealtPerLine[line] = (dealtPerLine[line] ?: 0) + 1
            }
        }

        // Set initial badges to pre-deal count (e.g. 7 + 3 = 10) and seed running counter.
        val badgeCount = mutableMapOf<String, Int>()
        for (row in 1..4) {
            val line = row.toString()
            val postDealSize = subDeckMap[line]?.size ?: 0
            val dealt = dealtPerLine[line] ?: 0
            val initial = postDealSize + dealt           // = 10 for a full starting deck
            badgeCount[line] = initial
            updateDeckBadge(line, initial)
        }

        for (col in 1..3) {
            for (row in 1..4) {
                val position     = "$row$col"
                val cardName     = cardTableMap[position]?.lastOrNull() ?: continue
                if (cardName == "zero") continue
                val line         = row.toString()
                val imageViewId  = resources.getIdentifier("subDeck$position", "id", packageName)
                val talloneId    = resources.getIdentifier("subDeck$row",      "id", packageName)
                val resourceName = "${cardType}_$cardName"
                val drawableId   = ResourceUtils.getDrawableByName(resources, packageName, resourceName)
                val drawable     = androidx.core.content.ContextCompat.getDrawable(this, drawableId)
                val sourceView   = findViewById<ImageView>(talloneId)
                val targetView   = findViewById<ImageView>(imageViewId)

                // Decrement the running count: this card will remove itself from the deck badge.
                val countAtLand = (badgeCount[line] ?: 1) - 1
                badgeCount[line] = countAtLand

                entries.add(DealEntry(
                    sourceView = sourceView,
                    targetView = targetView,
                    drawable   = drawable,
                    onLand     = {
                        playSoundAtomic(R.raw.flipcard)
                        setImage(imageViewId, cardName)
                        updateDeckBadge(line, countAtLand)   // 10→9→8→7 as cards land
                    }
                ))
            }
        }
        return entries
    }
```

- [ ] **Step 4: Add the import for `DealEntry` at the top of `GameActivity.kt`**

`DealEntry` is in the same package (`com.bottazzini.trasloco.utils`) as `DealAnimator`. Add the import near the other `utils` imports:

```kotlin
import com.bottazzini.trasloco.utils.DealAnimator
import com.bottazzini.trasloco.utils.DealEntry
```

(`CardAnimator` is already imported, so `DealAnimator` will be nearby.)

- [ ] **Step 5: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit -m "feat: add isIntroAnimating flag and buildDealEntries() to GameActivity"
```

---

### Task 3: Wire `DealAnimator.playNewGame()` into `startNewGame()`

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

- [ ] **Step 1: Rewrite the UI-thread block inside `startNewGame()`**

Find the `runOnUiThread` block inside `startNewGame()`:

```kotlin
            runOnUiThread {
                DeckSetup.prepareSubDecks()
                subDeckMap = DeckSetup.getSubDeckMap()
                coppiedSubDeckMap = HashMap(subDeckMap)
                prepareTable()
                if (hasReachedLostConditions()) {
                    startNewGame()
                    return@runOnUiThread
                }
                startTimer()
                isInitializing = false
                findViewById<View>(R.id.loadingOverlay).visibility = View.GONE
            }
```

Replace with:

```kotlin
            runOnUiThread {
                DeckSetup.prepareSubDecks()
                subDeckMap = DeckSetup.getSubDeckMap()
                coppiedSubDeckMap = HashMap(subDeckMap)

                // isIntroAnimating=true → dealCard() updates state but skips setImage;
                // visuals are deferred to DealEntry.onLand callbacks.
                isIntroAnimating = true
                prepareTable()

                if (hasReachedLostConditions()) {
                    isIntroAnimating = false
                    startNewGame()
                    return@runOnUiThread
                }

                val backCardValue = settingsHandler.readValue(Configuration.CARD_BACK.value)!!
                val backDrawableId = ResourceUtils.getDrawableByName(resources, packageName, backCardValue)
                val backDrawable = androidx.core.content.ContextCompat.getDrawable(this, backDrawableId)

                val talloneViews = listOf(1, 2, 3, 4).map { row ->
                    val id = resources.getIdentifier("subDeck$row", "id", packageName)
                    findViewById<ImageView>(id)
                }
                val dealEntries = buildDealEntries()

                // Tap on gameRoot skips the animation
                gameRoot.setOnClickListener { DealAnimator.skip() }

                DealAnimator.playNewGame(
                    root         = gameRoot,
                    talloneViews = talloneViews,
                    dealEntries  = dealEntries,
                    backDrawable = backDrawable,
                    handler      = timerHandler,
                    onPhase1Done = {
                        // Hide loading overlay after riffle so the board
                        // becomes visible as ghost decks fly to the talloni
                        findViewById<View>(R.id.loadingOverlay).visibility = View.GONE
                    },
                    onComplete   = {
                        isIntroAnimating = false
                        isInitializing   = false
                        gameRoot.setOnClickListener(null)
                        startTimer()
                    }
                )
            }
```

- [ ] **Step 2: Remove the `startNewGame()` call to `isInitializing = true` that is already set above the Thread block**

Inside `startNewGame()`, there is already:
```kotlin
isInitializing = true
```
near the top (before `prePrepareTable()`). Confirm it is present and leave it as-is.

Also confirm the existing:
```kotlin
findViewById<View>(R.id.loadingOverlay).visibility = View.VISIBLE
```
is still present just before `Thread {` — it should remain.

- [ ] **Step 3: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit -m "feat: wire DealAnimator.playNewGame() into startNewGame()"
```

---

### Task 4: Wire `DealAnimator.playRetry()` into `retryGame()`

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

- [ ] **Step 1: Rewrite `retryGame()`**

Find the full `retryGame()` method:

```kotlin
    fun retryGame(view: View) {
        if (isTutorialMode) return
        hintsUsedThisGame = 0
        autoMovesThisGame = 0
        isInitializing = true
        // Re-enable persistence: a retried game should also be resumable.
        shouldPersistOnPause = true
        gameViewModel.hasActiveGame = true
        gameViewModel.gameLost = false
        playSound(R.raw.shuffle)
        stopTimer()
        prePrepareTable()
        cardTableMap.clear()
        newPlayList()
        subDeckMap = HashMap(coppiedSubDeckMap)
        prepareTable()
        startTimer()
        isInitializing = false
    }
```

Replace with:

```kotlin
    fun retryGame(view: View) {
        if (isTutorialMode) return
        hintsUsedThisGame = 0
        autoMovesThisGame = 0
        isInitializing = true
        isIntroAnimating = true
        // Re-enable persistence: a retried game should also be resumable.
        shouldPersistOnPause = true
        gameViewModel.hasActiveGame = true
        gameViewModel.gameLost = false
        playSound(R.raw.shuffle)
        stopTimer()
        prePrepareTable()
        cardTableMap.clear()
        newPlayList()
        subDeckMap = HashMap(coppiedSubDeckMap)
        prepareTable()  // state updated; setImage skipped because isIntroAnimating=true

        val dealEntries = buildDealEntries()

        // Tap on gameRoot skips the animation
        gameRoot.setOnClickListener { DealAnimator.skip() }

        DealAnimator.playRetry(
            root        = gameRoot,
            dealEntries = dealEntries,
            handler     = timerHandler,
            onComplete  = {
                isIntroAnimating = false
                isInitializing   = false
                gameRoot.setOnClickListener(null)
                startTimer()
            }
        )
    }
```

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit -m "feat: wire DealAnimator.playRetry() into retryGame()"
```

---

### Task 5: Call `DealAnimator.skip()` in `onDestroy()`

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

- [ ] **Step 1: Add `DealAnimator.skip()` to `onDestroy()`**

Find `onDestroy()`:

```kotlin
    override fun onDestroy() {
        stopTimer()
        autoMoveRunnable?.let { timerHandler.removeCallbacks(it) }
        dealRunnables.forEach { timerHandler.removeCallbacks(it) }
        dealRunnables.clear()
        ...
    }
```

Add `DealAnimator.skip()` immediately after `dealRunnables.clear()`:

```kotlin
    override fun onDestroy() {
        stopTimer()
        autoMoveRunnable?.let { timerHandler.removeCallbacks(it) }
        dealRunnables.forEach { timerHandler.removeCallbacks(it) }
        dealRunnables.clear()
        DealAnimator.skip()   // ← add this line
        ...
    }
```

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit -m "fix: call DealAnimator.skip() in onDestroy to cancel pending intro animations"
```

---

### Task 6: Full build and manual verification

- [ ] **Step 1: Full debug build**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Install on device/emulator and verify New Game**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Checklist:
- [ ] New Game: loading overlay is visible while shuffle runs in background
- [ ] Loading overlay visible → central ghost card appears on top and riffles (~400 ms)
- [ ] Loading overlay hides → board visible with card back talloni, blank table slots
- [ ] 4 ghost decks fly from centre to the 4 talloni positions with stagger
- [ ] Cards deal in cascade: all 4 row-1 slots fill first, then all row-2, then all row-3
- [ ] Each card landing plays flipcard sound
- [ ] Game becomes interactive only after last card lands (timer starts)

- [ ] **Step 3: Verify Retry**

- [ ] Retry: no shuffle visual, deal cascade starts immediately (faster stagger ~100 ms)
- [ ] Tap during Retry cascade → board instantly shows all cards, game becomes interactive

- [ ] **Step 4: Verify Skip (New Game)**

- [ ] Tap during Phase 1 (riffle) → board instantly ready
- [ ] Tap during Phase 2 (split) → board instantly ready
- [ ] Tap during Phase 3 (cascade) → remaining cards appear instantly

- [ ] **Step 5: Verify edge cases**

- [ ] Rotate device during New Game intro animation → activity recreates cleanly, board shows normally (no animation, no ghost residue)
- [ ] Tutorial mode: no intro animation (same as before)
- [ ] Resume game from saved state: no intro animation (same as before)

- [ ] **Step 6: Final commit if any polish fixes were applied**

```bash
git add -A
git commit -m "fix: deal-shuffle animation polish from manual testing"
```

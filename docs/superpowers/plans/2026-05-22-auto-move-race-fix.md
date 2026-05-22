# Auto-Move Race Fix — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix two related auto-move bugs in `GameActivity.kt`: (A) deferred `setImage` callbacks that stomp on logical state after another mutation has progressed, producing inconsistent table/end-deck visuals; and (B) `showYouWon()` / `showYouLost()` firing before the final cascade/move animations finish, killing the animation mid-flight.

**Architecture:** Add a small counter (`pendingEndDeckAnims`) and a deferred-action slot (`pendingEndStateAction`) to `GameActivity`. End-deck animation sites bump the counter on schedule and decrement in their `onComplete`; when the counter returns to 0, the deferred end-state action (if any) fires. Each deferred `setImage` callback gets a one-line consistency guard so stale writes are suppressed when the logical state has progressed past the captured value.

**Tech Stack:** Kotlin, Android `Handler`, existing `CardAnimator.animateCardFlight`. No new libraries.

---

## File Map

| File | Change |
|------|--------|
| `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt` | **Modify only** — add counter fields/helpers, instrument `moveCardToEndDeckAnimated` + `forceCardsEndDeck`, guard 3 deferred `setImage` sites, refactor 3 end-state call sites, reset counter on new-game/retry/restore |

No other files touched. No new files.

---

### Task 1: Add counter fields + helpers + reset wiring

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

- [ ] **Step 1: Add the two fields**

Open `GameActivity.kt`. Find the existing field declarations around lines 80–84 (after `autoMoveRunnable` / `dealRunnables`). Add directly after `private val dealRunnables = mutableListOf<Runnable>()`:

```kotlin
    private var pendingEndDeckAnims: Int = 0
    private var pendingEndStateAction: (() -> Unit)? = null
```

- [ ] **Step 2: Add the three helpers**

Find the existing `clearUndoButton()` method (around line 890) and add these three helpers just before it (anywhere in the class is fine, but grouping them together with the other small helpers keeps the file scannable):

```kotlin
    private fun bumpEndDeckAnim() {
        pendingEndDeckAnims++
    }

    private fun decEndDeckAnim() {
        pendingEndDeckAnims = (pendingEndDeckAnims - 1).coerceAtLeast(0)
        if (pendingEndDeckAnims == 0) {
            val action = pendingEndStateAction
            pendingEndStateAction = null
            action?.invoke()
        }
    }

    private fun runOrDeferEndState(action: () -> Unit) {
        if (pendingEndDeckAnims == 0) action() else pendingEndStateAction = action
    }
```

- [ ] **Step 3: Reset counter in `prePrepareTable()`**

Find `private fun prePrepareTable()` (line 1045). Current body:

```kotlin
    private fun prePrepareTable() {
        clearCardSelection()
        zeroFill()
        endDeckList = hashMapOf("1" to "zero", "2" to "zero", "3" to "zero", "4" to "zero")
        prepareTextAndButtonForNewGame()
    }
```

Replace with:

```kotlin
    private fun prePrepareTable() {
        clearCardSelection()
        zeroFill()
        endDeckList = hashMapOf("1" to "zero", "2" to "zero", "3" to "zero", "4" to "zero")
        pendingEndDeckAnims = 0
        pendingEndStateAction = null
        prepareTextAndButtonForNewGame()
    }
```

- [ ] **Step 4: Reset counter in `restoreGameFromViewModel()`**

Find `private fun restoreGameFromViewModel()` (line 1363). At the start of the function, right after `isInitializing = true`, add the reset:

Current:
```kotlin
    private fun restoreGameFromViewModel() {
        isInitializing = true

        prePrepareTable()
        ...
```

Become:
```kotlin
    private fun restoreGameFromViewModel() {
        isInitializing = true
        pendingEndDeckAnims = 0
        pendingEndStateAction = null

        prePrepareTable()
        ...
```

Note: `prePrepareTable()` (called immediately after) also resets, so this is belt-and-suspenders — but explicit reset at the top is clearer for the restore path.

- [ ] **Step 5: Verify it compiles**

```bash
cd /Users/bottazzini/Documents/misc/Trasloco
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`. (None of the new helpers is called yet — Tasks 2–5 wire them in.)

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit -m "feat(GameActivity): add pendingEndDeckAnims counter + reset wiring"
```

---

### Task 2: Guard `dealCard` deferred `setImage`

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

- [ ] **Step 1: Add the consistency guard**

Find the `dealCard` animation `onComplete` block around line 831–835:

```kotlin
                CardAnimator.animateCardFlight(gameRoot, deckView, targetView, cardDrawable, 200L) {
                    if (!isFinishing) {
                        setImage(entry.imageViewId, entry.cardName)
                    }
                }
```

Replace with:

```kotlin
                CardAnimator.animateCardFlight(gameRoot, deckView, targetView, cardDrawable, 200L) {
                    if (!isFinishing && cardTableMap[entry.position]?.lastOrNull() == entry.cardName) {
                        setImage(entry.imageViewId, entry.cardName)
                    }
                }
```

Why: if an auto-move took the dealt card off this slot between the deal's synchronous state update and this animation's completion, `cardTableMap[entry.position].lastOrNull()` no longer equals `entry.cardName`. Skipping `setImage` leaves the visual as whatever the move already set (typically "zero" or a different card on top).

- [ ] **Step 2: Build**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit -m "fix(GameActivity): guard dealCard onComplete setImage against state drift"
```

---

### Task 3: Guard + instrument `moveCardToEndDeckAnimated`

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

- [ ] **Step 1: Replace the function**

Find `private fun moveCardToEndDeckAnimated(...)` (line 615). Current body:

```kotlin
    private fun moveCardToEndDeckAnimated(
        desiredCardPosition: Int,
        desiredPosition: String,
        selectedCard: String,
        selectedPositionId: Int
    ) {
        val sourceView = findViewById<ImageView>(selectedPositionId)
        val targetView = findViewById<ImageView>(desiredCardPosition)
        val cardResourceName = "${cardType}_${selectedCard}"
        val drawableId = ResourceUtils.getDrawableByName(resources, packageName, cardResourceName)
        val cardDrawable = ContextCompat.getDrawable(this, drawableId)

        val selectedPositionName =
            resources.getResourceEntryName(selectedPositionId).split("subDeck")[1]

        cardTableMap[desiredPosition]?.add(selectedCard)
        // End-deck slots don't show a card count
        cardTableMap[selectedPositionName]!!.remove(selectedCard)
        setNumberOfCards(cardTableMap[selectedPositionName]!!, selectedPositionName)

        if (cardTableMap[selectedPositionName]!!.isEmpty()) {
            setImage(selectedPositionId, "zero")
        } else {
            setImage(selectedPositionId, cardTableMap[selectedPositionName]!!.last())
        }
        playSoundAtomic(R.raw.flipcard)

        CardAnimator.animateCardFlight(gameRoot, sourceView, targetView, cardDrawable, 200L) {
            if (!isFinishing) setImage(desiredCardPosition, selectedCard)
        }
    }
```

Replace with (adds `targetLine` extraction, the guard, and counter bump/decrement):

```kotlin
    private fun moveCardToEndDeckAnimated(
        desiredCardPosition: Int,
        desiredPosition: String,
        selectedCard: String,
        selectedPositionId: Int
    ) {
        val sourceView = findViewById<ImageView>(selectedPositionId)
        val targetView = findViewById<ImageView>(desiredCardPosition)
        val cardResourceName = "${cardType}_${selectedCard}"
        val drawableId = ResourceUtils.getDrawableByName(resources, packageName, cardResourceName)
        val cardDrawable = ContextCompat.getDrawable(this, drawableId)

        val selectedPositionName =
            resources.getResourceEntryName(selectedPositionId).split("subDeck")[1]
        val targetLine = desiredPosition.first().toString()

        cardTableMap[desiredPosition]?.add(selectedCard)
        // End-deck slots don't show a card count
        cardTableMap[selectedPositionName]!!.remove(selectedCard)
        setNumberOfCards(cardTableMap[selectedPositionName]!!, selectedPositionName)

        if (cardTableMap[selectedPositionName]!!.isEmpty()) {
            setImage(selectedPositionId, "zero")
        } else {
            setImage(selectedPositionId, cardTableMap[selectedPositionName]!!.last())
        }
        playSoundAtomic(R.raw.flipcard)

        bumpEndDeckAnim()
        CardAnimator.animateCardFlight(gameRoot, sourceView, targetView, cardDrawable, 200L) {
            if (!isFinishing && endDeckList[targetLine] == selectedCard) {
                setImage(desiredCardPosition, selectedCard)
            }
            decEndDeckAnim()
        }
    }
```

Why: `targetLine` is the digit prefix of `desiredPosition` (e.g., `desiredPosition="34"` → `targetLine="3"`). The guard `endDeckList[targetLine] == selectedCard` ensures the deferred setImage runs only if the end deck's logical top is still the card this animation was for. If `forceCardsEndDeck` has progressed the end deck past, this animation's setImage is skipped (the cascade's own setImage handles the final visual).

`bumpEndDeckAnim()` is called right before scheduling the animation; `decEndDeckAnim()` is the last statement in `onComplete`, ensuring the counter is balanced even if the guard skips the setImage.

- [ ] **Step 2: Build**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit -m "fix(GameActivity): guard + instrument moveCardToEndDeckAnimated"
```

---

### Task 4: Guard + instrument `forceCardsEndDeck`

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

- [ ] **Step 1: Replace the per-card Runnable + animation block**

Find the `forEachIndexed` block at the bottom of `forceCardsEndDeck` (line 565–580):

```kotlin
        val animCards = pileCards.reversed()
        animCards.forEachIndexed { index, card ->
            val r = Runnable {
                if (isFinishing) return@Runnable
                val cardResourceName = if (card == "zero") card else "${cardType}_${card}"
                val drawableId = ResourceUtils.getDrawableByName(resources, packageName, cardResourceName)
                val cardDrawable = ContextCompat.getDrawable(this, drawableId)
                val isLast = (index == animCards.size - 1)
                CardAnimator.animateCardFlight(gameRoot, sourceView, targetView, cardDrawable, 200L) {
                    if (!isFinishing && isLast) {
                        setImage(desiredCardPositionId, endDeckCard)
                    }
                }
            }
            dealRunnables.add(r)
            timerHandler.postDelayed(r, index * 120L)
        }
```

Replace with:

```kotlin
        val animCards = pileCards.reversed()
        animCards.forEachIndexed { index, card ->
            bumpEndDeckAnim()
            val r = Runnable {
                if (isFinishing) {
                    decEndDeckAnim()
                    return@Runnable
                }
                val cardResourceName = if (card == "zero") card else "${cardType}_${card}"
                val drawableId = ResourceUtils.getDrawableByName(resources, packageName, cardResourceName)
                val cardDrawable = ContextCompat.getDrawable(this, drawableId)
                val isLast = (index == animCards.size - 1)
                CardAnimator.animateCardFlight(gameRoot, sourceView, targetView, cardDrawable, 200L) {
                    if (!isFinishing && isLast && endDeckList[line] == endDeckCard) {
                        setImage(desiredCardPositionId, endDeckCard)
                    }
                    decEndDeckAnim()
                }
            }
            dealRunnables.add(r)
            timerHandler.postDelayed(r, index * 120L)
        }
```

Why: `bumpEndDeckAnim()` is called at schedule time (so the counter reflects pending work before any Runnable fires). The Runnable has two completion paths — `isFinishing` early-return and the animation's `onComplete` — and each path calls `decEndDeckAnim()` exactly once. The `onComplete` also adds the guard `endDeckList[line] == endDeckCard` so a later move that has already progressed the end deck past `endDeckCard` won't get clobbered.

- [ ] **Step 2: Build**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit -m "fix(GameActivity): guard + instrument forceCardsEndDeck cascade"
```

---

### Task 5: Refactor end-state call sites to use `runOrDeferEndState`

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

- [ ] **Step 1: Refactor `gameCardClick`**

Find the win/lost check inside `gameCardClick` (line 447–454):

```kotlin
        if (moved) {
            if (hasReachedWonConditions()) {
                showYouWon()
                return
            } else if (hasReachedLostConditions()) {
                showYouLost()
                return
            }
            clearCardSelection()
            triggerAutoMoveCycle()
        } else {
```

Replace with:

```kotlin
        if (moved) {
            if (hasReachedWonConditions()) {
                runOrDeferEndState { showYouWon() }
                return
            } else if (hasReachedLostConditions()) {
                runOrDeferEndState { showYouLost() }
                return
            }
            clearCardSelection()
            triggerAutoMoveCycle()
        } else {
```

- [ ] **Step 2: Refactor `endDragOnTarget`**

Find the win/lost check around line 1275–1282:

```kotlin
            val moved = tryMove(source, target, animateEndDeck = false)
            if (moved) {
                if (hasReachedWonConditions()) {
                    showYouWon()
                } else if (hasReachedLostConditions()) {
                    showYouLost()
                } else {
                    triggerAutoMoveCycle()
                }
            } else {
```

Replace with:

```kotlin
            val moved = tryMove(source, target, animateEndDeck = false)
            if (moved) {
                if (hasReachedWonConditions()) {
                    runOrDeferEndState { showYouWon() }
                } else if (hasReachedLostConditions()) {
                    runOrDeferEndState { showYouLost() }
                } else {
                    triggerAutoMoveCycle()
                }
            } else {
```

- [ ] **Step 3a: Refactor `subDeckClick`**

Find the lost check at the end of `subDeckClick` (around line 419–421):

```kotlin
        if (hasReachedLostConditions()) {
            showYouLost()
        }
```

Replace with:

```kotlin
        if (hasReachedLostConditions()) {
            runOrDeferEndState { showYouLost() }
        }
```

Why: tapping the tallone during an in-flight cascade would otherwise call `showYouLost()` immediately, cutting the cascade. With `runOrDeferEndState`, the lost screen waits until the cascade settles.

- [ ] **Step 3b: Refactor `triggerAutoMoveCycle`**

Find the win/lost check inside `triggerAutoMoveCycle` (line 1633–1642):

```kotlin
        val moved = tryMove(sourceView, targetView)
        if (moved) {
            autoMovesThisGame++
            if (hasReachedWonConditions()) {
                showYouWon()
                return
            } else if (hasReachedLostConditions()) {
                showYouLost()
                return
            }
            // Continue cycle after short delay (350ms for visual feedback)
            autoMoveRunnable = Runnable { triggerAutoMoveCycle() }.also {
                timerHandler.postDelayed(it, 350)
            }
        }
```

Replace with:

```kotlin
        val moved = tryMove(sourceView, targetView)
        if (moved) {
            autoMovesThisGame++
            if (hasReachedWonConditions()) {
                runOrDeferEndState { showYouWon() }
                return
            } else if (hasReachedLostConditions()) {
                runOrDeferEndState { showYouLost() }
                return
            }
            // Continue cycle after short delay (350ms for visual feedback)
            autoMoveRunnable = Runnable { triggerAutoMoveCycle() }.also {
                timerHandler.postDelayed(it, 350)
            }
        }
```

- [ ] **Step 4: Verify no other direct callers of `showYouWon` / `showYouLost`**

```bash
grep -n "showYouWon()\|showYouLost()" app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
```

Expected: exactly two function-declaration lines (`private fun showYouWon() {` and `private fun showYouLost() {`), and seven `runOrDeferEndState { ... }` lambdas (one in `subDeckClick`, two in `gameCardClick`, two in `endDragOnTarget`, two in `triggerAutoMoveCycle`). Total: 2 declarations + 7 lambda usages = 9 matching lines.

If you find any direct unguarded call (e.g. `showYouLost()` not inside a `runOrDeferEndState { ... }` lambda) other than the function declarations, replace it with `runOrDeferEndState { ... }`.

Note: `showYouLostRestoredUI()` at line 1414/1415 is a different function (handles the saved-state-restoration path for an already-lost game); leave it alone — it's not gated by animation timing.

- [ ] **Step 5: Build**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit -m "fix(GameActivity): defer end-state until end-deck anims complete"
```

---

### Task 6: Full build + manual verification

- [ ] **Step 1: Full debug build**

```bash
cd /Users/bottazzini/Documents/misc/Trasloco
./gradlew :app:assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Install on device/emulator**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 3: Verify Bug A (race fix)**

Setup that previously triggered the bug: play a game with auto-move enabled. During an auto-move cascade animation, tap a tallone to deal new cards. Continue until win or until many moves play out.

- [ ] No stuck card on the table that shouldn't be there (the b2-on-table + b3-on-end-deck scenario should not reappear).
- [ ] After every auto-move chain, the visible top of each end deck matches the logical card you'd expect from the sequence of moves.
- [ ] After dealing during a cascade, the just-dealt cards stay on the table only if they weren't taken by a subsequent auto-move. If an auto-move took one, the slot reads "zero" (empty), not a ghost card.

- [ ] **Step 4: Verify Bug B (end-state timing)**

Play a game to completion via auto-move. The winning sequence usually involves placing the last 4 of a suit onto the end deck via a cascade.

- [ ] The final cascade animation completes fully (all 4 cards visibly land on the end deck) BEFORE the YouWon screen appears.
- [ ] The YouWon screen appears within ~1 second after the last animation lands — no awkward stall.
- [ ] Same check for YouLost: trigger a lost game, confirm last animation finishes before the lost screen.

- [ ] **Step 5: Verify Retry / New Game / Rotation don't break the counter**

- [ ] Win a game → YouWon screen → tap "Nuova partita" or back → start a new game → confirm intro plays normally, auto-move works as before.
- [ ] Mid-game, hit Retry → animations replay, no stuck deferred end-state from previous game.
- [ ] Rotate device mid-cascade → activity recreates → game state restores from saved state → counter is fresh (0) → no spurious end-state firing.

- [ ] **Step 6: Final commit if any polish fixes were applied**

```bash
git add -A
git commit -m "fix: auto-move race polish from manual testing"
```

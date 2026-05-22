# Auto-Move Race Fix — Design Spec

**Date:** 2026-05-22
**Status:** Draft for review

## Goal

Two related bugs introduced by deferred `setImage` calls inside animation completion callbacks:

1. **Bug A — Inconsistent card placement.** During an in-flight `dealCard` / `moveCardToEndDeckAnimated` / `forceCardsEndDeck` animation, an auto-move (or any state mutation) may move the card or progress the end deck. The animation's `onComplete` then runs an unconditional `setImage(...)`, restoring the visual to the now-stale value. Observed by the user as "2 di bastoni sul tavolo + 3 di bastoni sul mazzo finale" — visually inconsistent state.

2. **Bug B — Win screen cuts auto-move animations.** When auto-move completes the final winning placement, `hasReachedWonConditions()` returns true (state updated synchronously), and `showYouWon()` immediately starts `YouWonActivity`, killing in-flight cascade/move animations mid-flight.

## Non-goals

- No change to intro/shuffle animations (`DealAnimator`).
- No new tests (consistent with prior plans).
- No fix for the pre-existing "skip during intro leaves table slots at 'zero'" issue — that's a separate spec.
- No refactor of the existing animation architecture beyond what these fixes require.

---

## Section 1 — Bug A: guard deferred `setImage`

Three call-sites in `GameActivity.kt` schedule `setImage(...)` inside an animation's `onComplete`. Each one becomes a stale write if the underlying logical state has progressed between the animation start and end.

### A.1 — `dealCard` (line 833)

**Race**: `dealCard` sets `cardTableMap[pos] = [cardName]` and `tag = cardName` synchronously, then defers `setImage(pos, cardName)` to the animation `onComplete` (~200ms later). If during those 200ms an auto-move moves the card off this slot (cardTableMap[pos] cleared, tag set to "zero"), the `onComplete`'s `setImage` blindly restores the visual to the dealt card. Result: visual shows the card, but cardTableMap is empty and the card is logically elsewhere.

**Fix**: guard the `setImage` with a consistency check.

```kotlin
// app/src/main/java/com/bottazzini/trasloco/GameActivity.kt:833
CardAnimator.animateCardFlight(gameRoot, deckView, targetView, cardDrawable, 200L) {
    if (!isFinishing && cardTableMap[entry.position]?.lastOrNull() == entry.cardName) {
        setImage(entry.imageViewId, entry.cardName)
    }
}
```

If the slot's logical top is no longer `entry.cardName`, the guard fails and `setImage` is skipped. The slot's visual is whatever the move that took the card already set (typically "zero" or another card).

### A.2 — `moveCardToEndDeckAnimated` (line 643)

**Race**: `moveCardToEndDeckAnimated` sets `endDeckList[line] = selectedCard` (via the caller `tryMove`) and defers `setImage(endDeckPos, selectedCard)` to its `onComplete`. If `forceCardsEndDeck` (the immediate cascade) runs and updates `endDeckList[line] = endDeckCard` (a higher card from the cascade), the end deck is now logically at `endDeckCard`, not `selectedCard`. When `moveCardToEndDeckAnimated`'s onComplete fires, it would unconditionally setImage to the lower `selectedCard`, racing with the cascade's setImage.

**Fix**: derive the line key inside the function and guard the setImage.

```kotlin
// app/src/main/java/com/bottazzini/trasloco/GameActivity.kt:615-645
private fun moveCardToEndDeckAnimated(
    desiredCardPosition: Int,
    desiredPosition: String,
    selectedCard: String,
    selectedPositionId: Int
) {
    val targetLine = desiredPosition.first().toString()
    // ... existing setup ...
    CardAnimator.animateCardFlight(gameRoot, sourceView, targetView, cardDrawable, 200L) {
        if (!isFinishing && endDeckList[targetLine] == selectedCard) {
            setImage(desiredCardPosition, selectedCard)
        }
    }
}
```

### A.3 — `forceCardsEndDeck` (line 574)

**Race**: `forceCardsEndDeck` sets `endDeckList[line] = endDeckCard` synchronously, then schedules N animations. Only the `isLast` animation does a `setImage(endDeckPos, endDeckCard)` in onComplete. If a subsequent auto-move (placing a higher card) overwrites `endDeckList[line]` before this onComplete fires, the cascade's setImage restores the lower endDeckCard, leaving visual ≠ logical.

In practice this is rare (auto-move runs at +350ms after move and cascade for typical piles completes within that window), but worth guarding for correctness.

**Fix**: guard with the same pattern.

```kotlin
// app/src/main/java/com/bottazzini/trasloco/GameActivity.kt:572-576
CardAnimator.animateCardFlight(gameRoot, sourceView, targetView, cardDrawable, 200L) {
    if (!isFinishing && isLast && endDeckList[line] == endDeckCard) {
        setImage(desiredCardPositionId, endDeckCard)
    }
}
```

### Invariant after fix

For every deferred `setImage` site, the write happens **only if** the logical state still matches the value being set. If the state has progressed past, the later setImage that does match the new state will run and produce the correct visual. The last consistent write wins.

---

## Section 2 — Bug B: defer end-state until animations complete

When auto-move (or any move path) detects win/lost via `hasReachedWonConditions()` / `hasReachedLostConditions()`, the corresponding `showYouWon()` / `showYouLost()` immediately starts the next activity, killing pending animations.

### Counter-based deferral

Add two fields and three helpers to `GameActivity`:

```kotlin
// Counter of in-flight end-deck animations that should complete before
// the win/lost screen is shown.
private var pendingEndDeckAnims: Int = 0

// If non-null, will be invoked when `pendingEndDeckAnims` returns to 0.
private var pendingEndStateAction: (() -> Unit)? = null

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

### Instrumentation: `moveCardToEndDeckAnimated`

Bump immediately before the animation call; decrement at the end of the onComplete (after the guarded setImage from Section 1.A.2).

```kotlin
bumpEndDeckAnim()
CardAnimator.animateCardFlight(gameRoot, sourceView, targetView, cardDrawable, 200L) {
    if (!isFinishing && endDeckList[targetLine] == selectedCard) {
        setImage(desiredCardPosition, selectedCard)
    }
    decEndDeckAnim()
}
```

### Instrumentation: `forceCardsEndDeck`

Bump at Runnable schedule time (before `timerHandler.postDelayed`). Inside the Runnable, decrement on the `isFinishing` early-return path. The `CardAnimator.animateCardFlight` onComplete decrements after its guarded setImage.

```kotlin
animCards.forEachIndexed { index, card ->
    bumpEndDeckAnim()
    val r = Runnable {
        if (isFinishing) {
            decEndDeckAnim()
            return@Runnable
        }
        // ... existing drawable lookup ...
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

### Call-site refactor

Replace the immediate `showYouWon()` / `showYouLost()` calls with `runOrDeferEndState { ... }` at all three call sites:

1. **`triggerAutoMoveCycle` (lines 1636–1641)** — auto-move chain.
2. **`gameCardClick` (lines 448–453)** — manual click move.
3. **`endDragOnTarget` (lines 1276–1281)** — drag-and-drop move.

Example for the auto-move call site:

```kotlin
if (hasReachedWonConditions()) {
    runOrDeferEndState { showYouWon() }
    return
} else if (hasReachedLostConditions()) {
    runOrDeferEndState { showYouLost() }
    return
}
```

### Behavior

- If win is detected and zero end-deck anims are pending, `showYouWon()` fires immediately (current behavior, no regression).
- If anims are in flight, `pendingEndStateAction` is captured. When the last anim's `onComplete` runs `decEndDeckAnim()` and the counter hits 0, the captured action fires.
- The auto-move chain `return`s after `runOrDeferEndState`, so no further auto-moves are scheduled. The deferred show fires once anims settle.

### Edge cases

- **Animation cancelled via `Animator.cancel()`**: `AnimatorListenerAdapter.onAnimationEnd` still fires on cancel, so `onComplete` runs, decrementing the counter. Safe.
- **Runnable cancelled via `timerHandler.removeCallbacks(r)`**: only happens in `onDestroy` (clears `dealRunnables`). At that point we don't care about end-state — the activity is being torn down. `pendingEndStateAction` is leaked but never observed; harmless on next activity start.
- **Multiple sequential auto-moves with cascades**: each move bumps/decrements its own anims. The counter aggregates correctly across overlapping cascades.
- **New game / Retry / Restore-from-saved**: these reset game state. The counter must be cleared and `pendingEndStateAction = null` to avoid stale deferral from a previous game firing on the new one. Add the reset to `prePrepareTable()` (called by both `startNewGame()` and `retryGame()`) and to `restoreGameFromViewModel()`.

---

## Section 3 — Files touched

| File | Change |
|------|--------|
| `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt` | Add 3 guards + counter fields/helpers + instrument 2 anim sites + refactor 3 end-state call sites + reset counter on new-game/retry/restore |

No other files affected. No new files.

---

## Section 4 — Out of scope

- Skip-during-intro fix for the pre-existing "table slot stays at zero if not all cards dealt" issue.
- Generalized version of the counter to also defer end-state for non-end-deck animations (e.g., manual table moves). Today only end-deck moves trigger win/lost transitions, so end-deck instrumentation is sufficient.
- Adding unit tests for `GameActivity` (no existing infrastructure).

---

## Open questions

None. Verbally approved.

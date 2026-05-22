# Shuffle Variations & Overlay Timing Fix — Design Spec

**Date:** 2026-05-22
**Status:** Draft for review
**Author:** Brainstorming session (Bruno + Claude)

## Goal

Two changes on top of the existing `DealAnimator` (commits `ab28a51`..`ac3f19b`):

1. **Overlay timing fix.** The intro animation currently starts while the `loadingOverlay` is still visible — the riffle ghost is painted on top of the overlay, causing visual overlap. Move the animation so it begins only after the overlay has been dismissed and the empty board (talloni with back card, empty slots) is visible.
2. **10 shuffle variations.** Replace the single hardcoded riffle motion with 10 named coreographies, randomly selected on each New Game, to break the monotony.

Retry is **unchanged** — it already skips the shuffle phases and goes straight to the cascade.

## Non-goals

- No new sounds or haptics tied to variants. The single `flipcard` sound per card landing stays.
- No user-facing setting to pick / disable variants. Always random.
- No new variants for the deal cascade (Phase 3 stays uniform).
- No persistence of "last variant seen" across sessions.

---

## Section 1 — Overlay Timing Fix

### Current flow (problem)
```
[New Game tap]
  ↓
loadingOverlay.visibility = VISIBLE
  ↓
Thread: DeckSetup.shuffleSolvable()   ← background
  ↓
runOnUiThread:
  prepareTable()          (state ready, board hidden by overlay)
  DealAnimator.playNewGame(...)
    Phase 1 starts immediately  ← riffle ghost painted on top of overlay
    [500 ms later] onPhase1Done → overlay.visibility = GONE  ← board appears mid-animation
    Phase 2: ghosts fly to talloni
    Phase 3: cascade
```

The riffle ghost (Phase 1) is added to `gameRoot` and animates while the loading overlay is still on top of the same root, producing the visual overlap.

### Target flow
```
[New Game tap]
  ↓
loadingOverlay.visibility = VISIBLE
  ↓
Thread: DeckSetup.shuffleSolvable()
  ↓
runOnUiThread:
  prepareTable()                 (state ready, board still hidden)
  loadingOverlay.visibility = GONE   ← board visible: talloni with back, empty slots
  DealAnimator.playNewGame(...)
    Phase 1: riffle ghost on visible board   ← clean, no overlap
    Phase 2: 4 ghosts to talloni
    Phase 3: cascade
```

### Implementation impact
- In `GameActivity.startNewGame()`, move the `loadingOverlay.visibility = GONE` line above the `DealAnimator.playNewGame(...)` call.
- Remove the `onPhase1Done` callback parameter from `DealAnimator.playNewGame()` — it no longer has any purpose. Reduces API surface and removes the 500 ms delayed `Runnable` that fired it.
- The `playNewGame` signature shrinks from 8 params to 7.

This is the simpler/safer ordering and removes timing coupling between `DealAnimator` and the loading overlay.

---

## Section 2 — Shuffle Variations Architecture

### Design choice: Enum + private function per style (Approach A)

A new `ShuffleStyle` enum lives inside `DealAnimator.kt`. `playNewGame` picks one at random via `ShuffleStyle.values().random()` and dispatches to a private function that owns both Phase 1 and Phase 2 for that style.

```kotlin
object DealAnimator {

    enum class ShuffleStyle {
        RIFFLE, CUT, SPIN, BOUNCE, WAVE, FLIP, TUMBLE, PULSE, TOSS, FAN
    }

    fun playNewGame(
        root: ViewGroup,
        talloneViews: List<ImageView>,
        dealEntries: List<DealEntry>,
        backDrawable: Drawable?,
        handler: Handler,
        onComplete: () -> Unit,
        style: ShuffleStyle = ShuffleStyle.values().random()   // override for tests
    ) { ... }

    private fun playStyle(style: ShuffleStyle, root, talloneViews, backDrawable, handler, onAfterPhase2: () -> Unit) {
        when (style) {
            RIFFLE  -> playRiffle (root, talloneViews, backDrawable, handler, onAfterPhase2)
            CUT     -> playCut    (root, talloneViews, backDrawable, handler, onAfterPhase2)
            SPIN    -> playSpin   (...)
            ...
        }
    }
    // 10 private playXxx() functions, each composes its own Phase 1 + Phase 2 + calls onAfterPhase2
}
```

After each `playXxx` finishes Phase 2 (all 4 ghosts landed on talloni), the existing `playDealCascade(...)` runs unchanged via the `onAfterPhase2` callback.

### Why this over alternatives
- **Strategy pattern (separate files):** overkill for 10 short coreographies, fragments related code across 10 files.
- **Parametric (one function, params):** flip / cut / fan require structurally different motion (multiple ghosts, rotation axis, etc.) that don't reduce cleanly to scalar params; result would be one mega-function with style-specific `if`s — worst of both worlds.

### Shared utilities (kept in private helpers)
- `makeGhost(...)` — already exists, used by all styles
- `landCallback(...)` — counts landings, cleans up central ghost(s), calls `onAfterPhase2` when total reached
- `flightStagger(views, offsets, duration, onAllLanded)` — Phase 2 dispatch helper used by most styles
  - Centralizes the "fly N ghosts to N targets with timing array, count completions, invoke callback once" pattern.

These helpers stop each style function from re-implementing boilerplate.

---

## Section 3 — The 10 Variants

Each variant describes Phase 1 (central deck motion, ~400-500 ms) and Phase 2 (split to talloni, ~200-300 ms). All variants end the same way: 4 ghosts on the 4 tallone positions, ready for Phase 3 cascade.

### 1. RIFFLE *(the current animation, preserved as one of the 10)*
- **Phase 1 (~400 ms):** single central ghost. Scale wobbles `1.0 → 0.85 → 1.05 → 1.0`. `translationX` follows `sin(f·3π) · 8dp`.
- **Phase 2 (~220 ms):** 4 ghosts spawn at center, fly to talloni with stagger `0 / 60 / 120 / 180 ms`.

### 2. CUT
- **Phase 1 (~500 ms):** central ghost duplicated into two halves (top + bottom). Top half slides left, bottom right (`±20dp`), they cross, swap, return to center. End state: single ghost back at center.
- **Phase 2 (~220 ms):** 4 ghosts fly in pairs — `(row1, row4)` at 0 ms, `(row2, row3)` at 100 ms (outside-in symmetry).

### 3. SPIN
- **Phase 1 (~500 ms):** central ghost rotates `0° → 360°` on Z axis. Scale modulates `1.0 → 1.1 → 1.0` (subtle bounce).
- **Phase 2 (~220 ms):** stagger inverse — row 4 first, then 3, 2, 1.

### 4. BOUNCE
- **Phase 1 (~450 ms):** central ghost scales elastically: `1.0 → 1.2 → 0.9 → 1.1 → 1.0`.
- **Phase 2 (~250 ms):** standard 0/60/120/180 ms stagger, but each ghost overshoots `1.05` scale on land and settles to `1.0`.

### 5. WAVE
- **Phase 1 (~500 ms):** wide horizontal sway `translationX = sin(f·4π) · 16dp` (4 oscillations, larger amplitude than riffle).
- **Phase 2 (~180 ms):** all 4 ghosts fire simultaneously at delay 0.

### 6. FLIP
- **Phase 1 (~500 ms):** central ghost rotates `0° → 360°` on Y axis (looks like card flipping over and back). Drawable swap is avoided — same back drawable throughout.
- **Phase 2 (~220 ms):** standard 0/60/120/180 ms stagger.

### 7. TUMBLE
- **Phase 1 (~500 ms):** rotateZ rocking `0° → +15° → -15° → +15° → 0°` (4 quarter-cycles).
- **Phase 2 (~220 ms):** stagger in pairs — `(row1, row2)` at 0/60 ms, `(row3, row4)` at 160/220 ms.

### 8. PULSE
- **Phase 1 (~400 ms):** 5 rapid scale pulses `1.0 → 1.1 → 1.0` repeated.
- **Phase 2 (~220 ms):** standard 0/60/120/180 ms stagger.

### 9. TOSS
- **Phase 1 (~500 ms):** vertical hop `translationY = -50dp · sin(f·π)` (one full up-and-down). Scale `1.0 → 1.15 → 1.0` to suggest near-camera moment.
- **Phase 2 (~200 ms):** tight stagger 0/40/80/120 ms (fast follow-through).

### 10. FAN
- **Phase 1 (~450 ms):** central ghost duplicated into 3 ghosts. Two fan out left/right at `±20°` rotation, scale to `0.95`. End: all three re-converge and merge back to one.
- **Phase 2 (~220 ms):** standard 0/60/120/180 ms stagger.

### Visual signatures
The 10 variants partition into rough categories so consecutive plays feel different:
- **Translation-driven:** Riffle, Wave, Toss (motion through space)
- **Rotation-driven:** Spin, Flip, Tumble (axis rotation)
- **Scale-driven:** Bounce, Pulse (size pulses)
- **Multi-ghost:** Cut, Fan (deck visibly splits and rejoins)

---

## Section 4 — API surface change

### `DealAnimator.playNewGame()` — before
```kotlin
fun playNewGame(
    root: ViewGroup,
    talloneViews: List<ImageView>,
    dealEntries: List<DealEntry>,
    backDrawable: Drawable?,
    handler: Handler,
    onPhase1Done: () -> Unit,    // ← removed
    onComplete: () -> Unit
)
```

### `DealAnimator.playNewGame()` — after
```kotlin
fun playNewGame(
    root: ViewGroup,
    talloneViews: List<ImageView>,
    dealEntries: List<DealEntry>,
    backDrawable: Drawable?,
    handler: Handler,
    onComplete: () -> Unit,
    style: ShuffleStyle = ShuffleStyle.values().random()
)
```

### `GameActivity.startNewGame()` — caller change
- Remove `onPhase1Done` argument from the call.
- Move `findViewById<View>(R.id.loadingOverlay).visibility = View.GONE` to **before** `DealAnimator.playNewGame(...)`.
- Don't pass `style` — let `DealAnimator` pick random.

### `DealAnimator.playRetry()` — unchanged.

### `DealEntry`, `skip()`, `onDestroy()` hook — unchanged.

---

## Section 5 — Error handling & edge cases

- **Skip mid-Phase 1:** `skip()` already cancels `riffleAnim` (single `ValueAnimator?` field). For multi-ghost variants (Cut, Fan) the field becomes `riffleAnimators: MutableList<ValueAnimator>` and `skip()` cancels all. All ghost views remain tracked in `ghostViews` and are removed in `skip()` / `cleanup()`.
- **Activity destroyed during Phase 1:** `onDestroy()` calls `DealAnimator.skip()` — no change needed, just verify multi-animator cancellation works.
- **`talloneViews.firstOrNull()?.width == 0`:** the existing fallback `?: 80` and `?: 100` for deck w/h stays; some variants (Cut, Fan) depend on those for offset math.
- **Rotation during animation:** activity recreates → `onDestroy` → `skip()` → fresh `startNewGame()` (resume path doesn't re-enter intro animation). Already covered.

## Section 6 — Testing

No automated tests — these are pure visual coreographies on Android views, and the existing project has no UI test infrastructure for `GameActivity`.

**Manual verification checklist (user):**
- Run New Game 15-20 times; confirm visible variety (no two consecutive same variant feels rare in practice, but with `Random` it can happen — acceptable per Section 2 design).
- For each variant at least once: animation completes cleanly, no leftover ghost views on the board, cascade fires after Phase 2 lands.
- Skip during each variant: tap anywhere → board instantly ready, no leftover ghosts.
- Rotation during each variant: activity recreates without crash or ghost residue.
- Overlay timing: New Game shows loading overlay → overlay disappears → board visible empty → animation starts. **No overlap.**

---

## Section 7 — Out of scope (deferred)

- User toggle to disable intro animations entirely (could be a future setting).
- User toggle to pick a favorite variant.
- Reduce-motion accessibility flag detection.
- Per-variant sound effects.

---

## Open questions

None. Approach approved verbally; presenting written spec for final approval.

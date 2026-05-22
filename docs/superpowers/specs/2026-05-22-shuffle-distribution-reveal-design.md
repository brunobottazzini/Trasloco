# Shuffle Distribution + Tallone Reveal — Design Spec

**Date:** 2026-05-22
**Status:** Draft for review

## Goal

Extend the existing 10 `ShuffleStyle` variants so that **distribution** (Phase 2) and **tallone reveal** become coordinated with each style — not just stagger timing. Fix the related bug where the 4 talloni are visibly placed (showing the card back) during the central shuffle, which breaks the illusion that the deck is being split into them.

After this change, the 4 talloni start invisible (`alpha = 0`) at New-Game intro. During Phase 1 the central deck shuffles on an apparently empty board. During Phase 2, the ghost cards fly out on style-specific trajectories (arc, spiral, sinusoid, L-path, fan, …) with style-specific in-flight rotation/scale. At the moment each ghost lands at its tallone position, the corresponding tallone fades/flips/bounces into view via a reveal animation that matches the style's theme.

## Non-goals

- No change to the Retry flow (still cascade-only, no shuffle/distribution).
- No change to Phase 3 (deal cascade).
- No new sounds, no haptics.
- No user-facing setting to pick / disable variants.
- No accessibility "reduced motion" flag detection (deferred).

---

## Section 1 — The Bug

### Current behavior
1. `prepareTable()` is called → the 4 tallone ImageViews receive the card-back drawable and are visible.
2. `loadingOverlay.visibility = GONE` → board is now visible: 4 talloni already placed, showing the back of the deck.
3. `DealAnimator.playNewGame(...)` → Phase 1 starts: central ghost performs the shuffle motion on top of the already-visible talloni.

The user-perceived result: "you can see the 4 piles before the shuffle even happens — what's the point of the shuffle?"

### Target behavior
1. `prepareTable()` still sets the back drawable on talloni (visual prep).
2. Just before Phase 1 starts, `DealAnimator` sets `tallone.alpha = 0` on all 4. Talloni present in layout, invisible to the user.
3. Phase 1: central ghost shuffles on a truly empty board (no talloni visible).
4. Phase 2: ghost cards fly out. At the moment each ghost lands, the corresponding tallone is *revealed* with a style-specific animation (alpha-fade / flip / bounce / …).
5. Phase 3 (cascade) runs as today; talloni are already revealed by this point.

---

## Section 2 — `CardAnimator` Extension

A new public function (does not replace the existing `animateCardFlight`):

```kotlin
fun animateCardFlightCustom(
    root: ViewGroup,
    sourceView: View,
    targetView: View,
    drawable: Drawable?,
    durationMs: Long,
    pathFn: ((Float) -> Pair<Float, Float>)? = null,   // returns (dx, dy) offsets in pixels from the *linear* path
    rotationFn: ((Float) -> Float)? = null,            // returns degrees
    scaleFn: ((Float) -> Float)? = null,               // returns scale multiplier
    onComplete: () -> Unit
)
```

- `pathFn(f)` returns an **offset from the linear-interpolation point** (i.e. the ghost's position at progress `f` is `linear(start, end, f) + pathFn(f)`). Passing `null` keeps the current linear behavior.
- `rotationFn(f)` is applied as `ghost.rotation = rotationFn(f)` (Z-axis rotation in degrees).
- `scaleFn(f)` is applied to both `scaleX` and `scaleY`.
- Same fallback as the existing `animateCardFlight`: if either view has zero dimensions, `onComplete()` fires immediately and no ghost is created.
- Same cleanup: `onAnimationEnd` removes the ghost view from root and invokes `onComplete`.

The existing `animateCardFlight` (used by in-game card moves and Retry cascade) stays untouched — its callers depend on the current linear behavior.

### Why a sibling function and not an extension of the existing one
- Existing callers (dealCard, doMove, completePileMoveAtomic, completeMoveAtomic) all use the linear path. Forcing them to pass three `null` lambdas degrades readability for no benefit.
- The new function is a strict superset of behavior, but each caller picks the shape that matches its need.

---

## Section 3 — The 10 Coreographies

Each variant's Phase 2 = trajectory + ghost-in-flight motion + tallone reveal.

### Notation
- "Trajectory" = path of the *ghost card* from screen center to the tallone.
- "Ghost motion" = rotation/scale applied to the ghost while in flight.
- "Reveal" = animation applied to the *tallone ImageView* when its ghost lands (alpha goes 0 → 1 plus whatever else is described).
- All Phase 2 durations: 250–400 ms unless noted.

### 1. RIFFLE
- **Trajectory:** linear (no `pathFn`).
- **Ghost motion:** none.
- **Stagger:** 0 / 60 / 120 / 180 ms.
- **Reveal:** alpha 0→1 over 180 ms.

### 2. CUT
- **Trajectory:** L-path. First half of duration: ghost moves horizontally to the tallone's X. Second half: vertically down to its Y. Implemented via `pathFn` that contributes the *non-linear* component (anti-linear adjustment).
- **Ghost motion:** none.
- **Stagger:** pairs — `(row1, row4)` at 0 ms, `(row2, row3)` at 120 ms (outside-in, as today).
- **Reveal:** pairs sync — alpha 0→1 plus scale 0.9→1 (180 ms).

### 3. SPIN
- **Trajectory:** spiral. `pathFn` adds a tangential component that grows then shrinks (parametric: `r(f) = sin(πf) · 30dp`, `θ(f) = 2π · f`).
- **Ghost motion:** `rotationFn(f) = f · 360f` (full revolution in flight).
- **Stagger:** reverse — row 4 first, row 1 last (180 / 120 / 60 / 0 ms).
- **Reveal:** tallone rotates `0° → 360°` while alpha 0→1 (300 ms).

### 4. BOUNCE
- **Trajectory:** low arc. `pathFn(f) = (0, -sin(πf) · 40dp)` — peaks at f=0.5 with a 40dp upward bump.
- **Ghost motion:** `scaleFn(f) = 1f + 0.10f * sin(πf)`.
- **Stagger:** 0 / 60 / 120 / 180 ms (standard).
- **Reveal:** scale 0 → 1.2 → 1 over 300 ms, alpha 0→1 in the first 100 ms.

### 5. WAVE
- **Trajectory:** sinusoidal. `pathFn(f) = (sin(2π·f) · 20dp, 0)` — horizontal wave overlay on the linear path.
- **Ghost motion:** none.
- **Stagger:** all 4 simultaneously at 0 ms. Flight 280 ms.
- **Reveal:** slide-in horizontally from `-30dp` to 0, alpha 0→1 (220 ms).

### 6. FLIP
- **Trajectory:** linear.
- **Ghost motion:** `rotationY` 0° → 360° (full Y-axis flip).
- **Stagger:** 0 / 60 / 120 / 180 ms.
- **Reveal:** tallone `rotationY` 90° → 0° while alpha 0→1 (240 ms) — looks like a card flipping into place.

### 7. TUMBLE
- **Trajectory:** linear.
- **Ghost motion:** `rotationFn(f) = sin(πf · 2) · 20f` — Z-axis rocking ±20°.
- **Stagger:** pairs — `(row1, row2)` at 0/60 ms, `(row3, row4)` at 160/220 ms.
- **Reveal:** rocking ±10° once, then settle to 0°. Alpha 0→1 (260 ms).

### 8. PULSE
- **Trajectory:** linear.
- **Ghost motion:** `scaleFn(f) = 1f + 0.05f · |sin(πf · 3)|` — 3 pulses in flight.
- **Stagger:** 0 / 60 / 120 / 180 ms.
- **Reveal:** 2 scale pulses (1 → 1.08 → 1 → 1.08 → 1) plus alpha 0→1 (260 ms).

### 9. TOSS
- **Trajectory:** high arc. `pathFn(f) = (0, -sin(πf) · 80dp)` — peaks at f=0.5 with an 80dp upward toss.
- **Ghost motion:** `scaleFn(f) = 1f + 0.20f · sin(πf)`.
- **Stagger:** 0 / 40 / 80 / 120 ms (tight).
- **Reveal:** drop with squash — `scaleY` 1 → 1.20 → 0.85 → 1 (squash on impact, 280 ms) plus alpha 0→1 in the first 100 ms.

### 10. FAN
- **Trajectory:** diverging fan. `pathFn(f) = (rowOffset · sin(πf) · 20dp, 0)` — early in flight all ghosts shift toward outer corners, then converge to their tallone. `rowOffset` is `-1.5, -0.5, +0.5, +1.5` for the four rows.
- **Ghost motion:** `rotationFn(f) = rowOffset · 10f · sin(πf)` (each row tilts slightly during flight, settles to 0).
- **Stagger:** 0 / 60 / 120 / 180 ms.
- **Reveal:** alpha 0→1 plus rotation `-10°` → 0° (220 ms).

---

## Section 4 — `DealAnimator` Architecture Changes

### New private helper

```kotlin
/**
 * Reveal a tallone view with a style-specific animation. Called when the
 * ghost for that tallone lands. The tallone is assumed to start at alpha=0
 * (set by playStyle at Phase 1 start).
 */
private fun revealTallone(tallone: ImageView, style: ShuffleStyle, onDone: () -> Unit = {})
```

The body is a `when (style) { ... }` dispatch to 10 small private reveal functions: `revealAlpha`, `revealPairSync`, `revealSpin360`, `revealScaleBounce`, `revealSlideX`, `revealFlipY`, `revealRocking`, `revealPulse`, `revealDropSquash`, `revealRotationSettle`. Each is ~10-15 lines of `ValueAnimator` driving the appropriate properties on `tallone`.

### Phase 2 dispatch

Each `playXxx` variant function now ends with its **own** Phase 2 implementation (no more shared `playPhase2`). Replacing the current single `playPhase2` with per-variant `playPhase2Xxx(...)` functions. They share a helper `dispatchGhost(...)` that wraps `CardAnimator.animateCardFlightCustom` plus the on-land callback:

```kotlin
private fun dispatchGhost(
    root: ViewGroup,
    centralGhost: View,
    tallone: ImageView,
    backDrawable: Drawable?,
    durationMs: Long,
    pathFn: ((Float) -> Pair<Float, Float>)?,
    rotationFn: ((Float) -> Float)?,
    scaleFn: ((Float) -> Float)?,
    style: ShuffleStyle,
    onLanded: () -> Unit
) {
    CardAnimator.animateCardFlightCustom(
        root, centralGhost, tallone, backDrawable, durationMs,
        pathFn = pathFn, rotationFn = rotationFn, scaleFn = scaleFn
    ) {
        revealTallone(tallone, style) { onLanded() }
    }
}
```

### Tallone alpha=0 initialization

In every `playXxx` variant, the first line after creating the central ghost is:

```kotlin
talloneViews.forEach { it.alpha = 0f }
```

Moved into a helper `hideTalloni()` called once at the start of each style.

### Skip cleanup

`skip()` and `reset()` must restore `tallone.alpha = 1f` to recover from an interrupted intro animation. New helper:

```kotlin
private fun restoreTalloni() {
    val r = rootRef?.get() ?: return
    // Resolve from view IDs — talloneViews captured by variant closures
    // are gone by the time skip() runs, so DealAnimator does NOT track them.
    // Caller of skip() (GameActivity.onClickListener) is fine — the next
    // frame, prepareTable() in retry/new-game will restore alpha if needed.
    // For onDestroy: not needed — activity is being torn down.
}
```

Wait — `DealAnimator` doesn't hold references to `talloneViews` directly (they're parameters to `playNewGame`). So `skip()` cannot restore them. Solution:

- Store `talloneViews` as a field on `DealAnimator` (set at `playNewGame` start, cleared in `cleanup()`/`reset()`).
- `skip()` and `reset()` iterate the field and set `alpha = 1f` before clearing it.

```kotlin
private var talloneRefs: List<ImageView> = emptyList()
```

Set in `playNewGame` after `reset()`. Used by `skip()`/`reset()` to restore alpha. Cleared in `cleanup()` after success.

---

## Section 5 — Data Flow

```
GameActivity.startNewGame()
  prepareTable()                        // sets back drawable on talloni (alpha=1)
  loadingOverlay = GONE                 // board visible but with talloni alpha=1 still
  DealAnimator.playNewGame(...)
    reset()
    talloneRefs = talloneViews                // store for skip() to restore alpha=1
    talloneRefs.forEach { it.alpha = 0f }     // ← bug fix: hide them during shuffle
    playStyle(random)
      playRiffle/Cut/Spin/.../Fan(...)
        create central ghost
        ValueAnimator for Phase 1 (existing per-variant)
        onAnimationEnd:
          playPhase2_Xxx(centralGhost, talloneViews, ...)
            for each tallone (with style-specific stagger):
              handler.postDelayed({
                dispatchGhost(...) {        // CardAnimator.animateCardFlightCustom + revealTallone
                  // ghost has landed, tallone is now alpha=1 after revealTallone
                  on last landing → playDealCascade
                }
              }, delay)
```

---

## Section 6 — Skip & Lifecycle

- **Tap to skip:** `DealAnimator.skip()` → cancels animators, removes ghosts, **and** restores `tallone.alpha = 1f` for all `talloneRefs`. Then invokes `skipOnComplete`. GameActivity's onComplete then runs `prepareTable`-style state restoration (already does — the cascade `onLand` callbacks call `setImage`).

   **Pre-existing concern (not in scope here):** if skip happens during Phase 2 / Phase 3 before all cards are dealt, the `DealEntry.onLand` callbacks for unfired cards never run, so the corresponding table slots remain at the "zero" placeholder until the user moves. State (`cardTableMap`) is consistent, but visuals can lag until the next interaction. This was already true after Tasks 1-7 and is acknowledged but not fixed here. A future spec can add an "on-skip force-flush" path that iterates remaining `DealEntry.onLand` calls.

- **Activity destroyed:** `onDestroy()` calls `skip()`. Talloni are about to disappear with the activity — no observable visual difference.

---

## Section 7 — Error handling & edge cases

- **Zero-dimensioned views:** `animateCardFlightCustom` falls back to `onComplete()` immediately (same as existing `animateCardFlight`). The reveal still runs because `revealTallone` is called from the on-land callback.
- **Variant interrupted mid-Phase 2:** `skip()` cancels all `riffleAnims` and `pendingRunnables`. Talloni alpha restored to 1. Cascade runs from start (skipOnComplete fires; GameActivity's onComplete updates `isIntroAnimating = false` and calls `startTimer`; any missing card visuals are restored via the optional fix in Section 6).
- **`pathFn`/`rotationFn`/`scaleFn` throwing:** treated as a bug — no try/catch. Standard Android animator behavior surfaces the exception.
- **Multiple consecutive New Games:** each call to `playNewGame` first calls `reset()`, which cancels everything from the previous call.

---

## Section 8 — Testing

No automated tests (consistent with the previous shuffle-variations spec).

**Manual verification checklist (user):**
- For each of the 10 variants (need ~20 New Games to sample each):
  - During Phase 1, NO talloni are visible (no back cards behind the central shuffle).
  - During Phase 2, each tallone appears at its position via the style-specific reveal animation.
  - The reveal feels coordinated with the style theme (e.g. SPIN: ghost spirals + tallone spins; BOUNCE: arc + bounce; TOSS: high arc + drop-squash).
- Tap to skip during Phase 1 → board instantly shows all 4 talloni with back cards visible. No leftover ghost. Cascade then runs normally.
- Tap to skip during Phase 2 (after 1-2 ghosts landed, before others) → all talloni instantly visible, no leftover ghosts, cascade then runs.
- Retry: unchanged (no shuffle, no distribution, just cascade).
- Rotation during intro: activity recreates, no crash, no ghost residue, no stuck alpha=0 talloni.

---

## Section 9 — Out of scope

- Animation timing tweaks based on device performance (we assume modern Android perf).
- Cascade variations (Phase 3 remains uniform).
- Per-variant sound effects.
- User toggle to disable animations.

---

## Open questions

None. Approach approved verbally; written spec pending user review.

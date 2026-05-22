# Shuffle Variations & Overlay Timing Fix — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hide the loading overlay before the intro animation starts (fixing the visual overlap), and replace the single hardcoded riffle with 10 randomly-selected shuffle variants on each New Game.

**Architecture:** Add a `ShuffleStyle` enum inside `DealAnimator`; refactor the inline Phase 1 / Phase 2 code into a `playRiffle()` private function and add 9 sibling functions for the new variants. Generalize `playPhase2` to take a configurable `delays` array. Change `riffleAnim: ValueAnimator?` into `riffleAnims: MutableList<ValueAnimator>` so multi-ghost variants (CUT, FAN) can cancel multiple animators on skip. Remove `onPhase1Done` from `playNewGame` signature and instead hide the loading overlay in `GameActivity.startNewGame()` before calling `playNewGame()`.

**Tech Stack:** Android `ValueAnimator`, `AnimatorListenerAdapter.onAnimationEnd`, Kotlin `enum class`, `Random.random()` on `enum.values()`.

---

## File Map

| File | Change |
|------|--------|
| `app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt` | **Modify** — remove `onPhase1Done`, refactor internals, add `ShuffleStyle` enum + 10 variant functions |
| `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt` | **Modify** — hide loading overlay before calling `playNewGame`, drop `onPhase1Done` argument |

---

### Task 1: Overlay timing fix + drop `onPhase1Done`

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt`
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

- [ ] **Step 1: In `DealAnimator.playNewGame`, drop `onPhase1Done` and trigger Phase 2 via `onAnimationEnd`**

Open `DealAnimator.kt`. Find the current `playNewGame` (around lines 44–101). Replace its body so the 500 ms delayed `Runnable` is removed and Phase 2 is triggered directly from the `onAnimationEnd` listener of the riffle `ValueAnimator`.

Current ending of `playNewGame`:
```kotlin
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
```

Replace with:
```kotlin
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // reset translationX to exact center so Phase 2 source position is stable
                    centralGhost.translationX = startTx
                    centralGhost.scaleX = 1f
                    playPhase2(root, centralGhost, talloneViews, backDrawable, handler) {
                        val timings = cascadeTimings(staggerMs = 150L, roundGapMs = 250L)
                        playDealCascade(root, dealEntries, timings, handler, onComplete)
                    }
                }
            })
        }
        riffleAnim = anim
        anim.start()
    }
```

Also remove the `onPhase1Done: () -> Unit,` parameter from the `playNewGame` parameter list:

Before:
```kotlin
    fun playNewGame(
        root: ViewGroup,
        talloneViews: List<ImageView>,
        dealEntries: List<DealEntry>,
        backDrawable: Drawable?,
        handler: Handler,
        onPhase1Done: () -> Unit,
        onComplete: () -> Unit
    ) {
```

After:
```kotlin
    fun playNewGame(
        root: ViewGroup,
        talloneViews: List<ImageView>,
        dealEntries: List<DealEntry>,
        backDrawable: Drawable?,
        handler: Handler,
        onComplete: () -> Unit
    ) {
```

- [ ] **Step 2: In `GameActivity.startNewGame()`, hide overlay before `playNewGame` and drop `onPhase1Done`**

Open `GameActivity.kt`. Find the `runOnUiThread` block inside `startNewGame()` (around lines 180–225). Replace the `DealAnimator.playNewGame(...)` call so:
1. `loadingOverlay.visibility = GONE` is set immediately **before** `DealAnimator.playNewGame(...)` (right after the `hasReachedLostConditions` early-return and the `backDrawable` / `talloneViews` / `dealEntries` setup).
2. The `onPhase1Done = { ... }` argument is removed entirely.

Current:
```kotlin
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
```

Replace with:
```kotlin
                // Hide loading overlay before the animation so the riffle plays
                // on the visible (empty) board, not on top of the overlay
                findViewById<View>(R.id.loadingOverlay).visibility = View.GONE

                // Tap on gameRoot skips the animation
                gameRoot.setOnClickListener { DealAnimator.skip() }

                DealAnimator.playNewGame(
                    root         = gameRoot,
                    talloneViews = talloneViews,
                    dealEntries  = dealEntries,
                    backDrawable = backDrawable,
                    handler      = timerHandler,
                    onComplete   = {
                        isIntroAnimating = false
                        isInitializing   = false
                        gameRoot.setOnClickListener(null)
                        startTimer()
                    }
                )
```

- [ ] **Step 3: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit -m "fix: hide loading overlay before intro animation to avoid overlap"
```

---

### Task 2: Refactor internals — multi-animator list, parametric `playPhase2`, extract `playRiffle`

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt`

- [ ] **Step 1: Change `riffleAnim` field to a list**

Find:
```kotlin
    private var riffleAnim: ValueAnimator? = null
```

Replace with:
```kotlin
    private val riffleAnims = mutableListOf<ValueAnimator>()
```

- [ ] **Step 2: Update `skip()` to cancel all animators in the list**

Find the body of `skip()`:
```kotlin
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
```

Replace the first two lines so it iterates the list:
```kotlin
    fun skip() {
        riffleAnims.forEach { it.cancel() }
        riffleAnims.clear()
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
```

- [ ] **Step 3: Update `reset()` similarly**

Find:
```kotlin
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
```

Replace with:
```kotlin
    private fun reset() {
        riffleAnims.forEach { it.cancel() }
        riffleAnims.clear()
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
```

- [ ] **Step 4: Update `cleanup()` similarly**

Find:
```kotlin
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
```

Replace with:
```kotlin
    private fun cleanup() {
        val r = rootRef?.get()
        ghostViews.forEach { r?.removeView(it) }
        ghostViews.clear()
        pendingRunnables.clear()
        riffleAnims.clear()
        handlerRef     = null
        rootRef        = null
        skipOnComplete = null
    }
```

- [ ] **Step 5: Update `playNewGame` to use `riffleAnims.add(anim)` instead of `riffleAnim = anim`**

Inside `playNewGame`, find:
```kotlin
        riffleAnim = anim
        anim.start()
```

Replace with:
```kotlin
        riffleAnims.add(anim)
        anim.start()
```

- [ ] **Step 6: Generalize `playPhase2` to take a `delays` array**

Find the current `playPhase2`:
```kotlin
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
```

Replace with:
```kotlin
    private fun playPhase2(
        root: ViewGroup,
        centralGhost: View,
        talloneViews: List<ImageView>,
        backDrawable: Drawable?,
        handler: Handler,
        delays: LongArray = longArrayOf(0L, 60L, 120L, 180L),
        flightDurationMs: Long = 220L,
        onAllLanded: () -> Unit
    ) {
        var landedCount = 0
        val total = talloneViews.size

        talloneViews.forEachIndexed { index, tallone ->
            val r = Runnable {
                val currentRoot = rootRef?.get() ?: return@Runnable
                CardAnimator.animateCardFlight(
                    currentRoot, centralGhost, tallone, backDrawable, flightDurationMs
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
            handler.postDelayed(r, delays.getOrElse(index) { index * 60L })
        }
    }
```

- [ ] **Step 7: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt
git commit -m "refactor: support multi-animator and parametric Phase 2 in DealAnimator"
```

---

### Task 3: Add `ShuffleStyle` enum + extract `playRiffle` + dispatcher

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt`

- [ ] **Step 1: Add the `ShuffleStyle` enum at the top of the `object DealAnimator { ... }` body**

Inside `object DealAnimator {`, immediately after the field declarations (i.e. after `private var skipOnComplete: (() -> Unit)? = null`), add:

```kotlin
    enum class ShuffleStyle {
        RIFFLE, CUT, SPIN, BOUNCE, WAVE, FLIP, TUMBLE, PULSE, TOSS, FAN
    }
```

- [ ] **Step 2: Refactor `playNewGame` to dispatch to a style function**

Replace the entire `playNewGame` body so it:
1. Picks a random style if not supplied.
2. Calls a `playStyle(...)` private dispatcher that runs Phase 1 + Phase 2 for the chosen style and then invokes the cascade.

Current `playNewGame`:
```kotlin
    fun playNewGame(
        root: ViewGroup,
        talloneViews: List<ImageView>,
        dealEntries: List<DealEntry>,
        backDrawable: Drawable?,
        handler: Handler,
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
                    centralGhost.translationX = startTx
                    centralGhost.scaleX = 1f
                    playPhase2(root, centralGhost, talloneViews, backDrawable, handler) {
                        val timings = cascadeTimings(staggerMs = 150L, roundGapMs = 250L)
                        playDealCascade(root, dealEntries, timings, handler, onComplete)
                    }
                }
            })
        }
        riffleAnims.add(anim)
        anim.start()
    }
```

Replace with:
```kotlin
    fun playNewGame(
        root: ViewGroup,
        talloneViews: List<ImageView>,
        dealEntries: List<DealEntry>,
        backDrawable: Drawable?,
        handler: Handler,
        onComplete: () -> Unit,
        style: ShuffleStyle = ShuffleStyle.values().random()
    ) {
        reset()
        handlerRef     = handler
        rootRef        = WeakReference(root)
        skipOnComplete = onComplete

        playStyle(style, root, talloneViews, backDrawable, handler) {
            val timings = cascadeTimings(staggerMs = 150L, roundGapMs = 250L)
            playDealCascade(root, dealEntries, timings, handler, onComplete)
        }
    }

    private fun playStyle(
        style: ShuffleStyle,
        root: ViewGroup,
        talloneViews: List<ImageView>,
        backDrawable: Drawable?,
        handler: Handler,
        onAfterPhase2: () -> Unit
    ) {
        when (style) {
            ShuffleStyle.RIFFLE  -> playRiffle (root, talloneViews, backDrawable, handler, onAfterPhase2)
            ShuffleStyle.CUT     -> playRiffle (root, talloneViews, backDrawable, handler, onAfterPhase2) // placeholder until Task 7
            ShuffleStyle.SPIN    -> playRiffle (root, talloneViews, backDrawable, handler, onAfterPhase2) // placeholder until Task 4
            ShuffleStyle.BOUNCE  -> playRiffle (root, talloneViews, backDrawable, handler, onAfterPhase2) // placeholder until Task 5
            ShuffleStyle.WAVE    -> playRiffle (root, talloneViews, backDrawable, handler, onAfterPhase2) // placeholder until Task 6
            ShuffleStyle.FLIP    -> playRiffle (root, talloneViews, backDrawable, handler, onAfterPhase2) // placeholder until Task 4
            ShuffleStyle.TUMBLE  -> playRiffle (root, talloneViews, backDrawable, handler, onAfterPhase2) // placeholder until Task 4
            ShuffleStyle.PULSE   -> playRiffle (root, talloneViews, backDrawable, handler, onAfterPhase2) // placeholder until Task 5
            ShuffleStyle.TOSS    -> playRiffle (root, talloneViews, backDrawable, handler, onAfterPhase2) // placeholder until Task 6
            ShuffleStyle.FAN     -> playRiffle (root, talloneViews, backDrawable, handler, onAfterPhase2) // placeholder until Task 7
        }
    }
```

- [ ] **Step 3: Add `playRiffle` private function**

Add this new private function just before `playPhase2` (so it's grouped near the style implementations):

```kotlin
    /** RIFFLE: scale wobble 1→0.85→1.05→1.0 + sin sway. */
    private fun playRiffle(
        root: ViewGroup,
        talloneViews: List<ImageView>,
        backDrawable: Drawable?,
        handler: Handler,
        onAfterPhase2: () -> Unit
    ) {
        val deckW   = talloneViews.firstOrNull()?.width?.takeIf  { it > 0 } ?: 80
        val deckH   = talloneViews.firstOrNull()?.height?.takeIf { it > 0 } ?: 100
        val startTx = root.width  / 2f - deckW / 2f
        val startTy = root.height / 2f - deckH / 2f
        val dp8     = 8 * root.context.resources.displayMetrics.density

        val ghost = makeGhost(root, backDrawable, deckW, deckH, startTx, startTy)
        ghostViews.add(ghost)
        root.addView(ghost)

        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 400
            addUpdateListener { va ->
                val f = va.animatedFraction
                ghost.scaleX = when {
                    f < 0.33f -> 1f - (f / 0.33f) * 0.15f
                    f < 0.66f -> 0.85f + ((f - 0.33f) / 0.33f) * 0.20f
                    else      -> 1.05f - ((f - 0.66f) / 0.34f) * 0.05f
                }
                ghost.translationX =
                    startTx + Math.sin(f.toDouble() * Math.PI * 3).toFloat() * dp8
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    ghost.translationX = startTx
                    ghost.scaleX = 1f
                    playPhase2(root, ghost, talloneViews, backDrawable, handler,
                        onAllLanded = onAfterPhase2)
                }
            })
        }
        riffleAnims.add(anim)
        anim.start()
    }
```

- [ ] **Step 4: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`. All 10 enum values currently route to `playRiffle` (placeholders for upcoming tasks) so behavior is unchanged from the user's perspective until Tasks 4-7 land.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt
git commit -m "feat: add ShuffleStyle enum and per-style dispatcher in DealAnimator"
```

---

### Task 4: Implement rotation variants — SPIN, FLIP, TUMBLE

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt`

- [ ] **Step 1: Add `makeCenterGhost` helper (shared by all single-ghost variants)**

These variants and the ones coming in Tasks 5–6 all need the same boilerplate: compute deckW/deckH/startTx/startTy and add a centered ghost to the root. Add this private helper in `DealAnimator.kt` immediately after `makeGhost`:

```kotlin
    /**
     * Helper: creates a single back-card ghost centered in the root, adds it to
     * the view hierarchy and to `ghostViews`, and returns it along with its
     * start translationX / translationY (useful for variants that animate
     * around the center).
     */
    private fun makeCenterGhost(
        root: ViewGroup,
        talloneViews: List<ImageView>,
        backDrawable: Drawable?
    ): Triple<ImageView, Float, Float> {
        val deckW   = talloneViews.firstOrNull()?.width?.takeIf  { it > 0 } ?: 80
        val deckH   = talloneViews.firstOrNull()?.height?.takeIf { it > 0 } ?: 100
        val startTx = root.width  / 2f - deckW / 2f
        val startTy = root.height / 2f - deckH / 2f
        val ghost = makeGhost(root, backDrawable, deckW, deckH, startTx, startTy)
        ghostViews.add(ghost)
        root.addView(ghost)
        return Triple(ghost, startTx, startTy)
    }
```

- [ ] **Step 2: Add `playSpin`, `playFlip`, `playTumble` private functions**

Add these three functions in `DealAnimator.kt`, immediately after `playRiffle`:

```kotlin
    /** SPIN: rotateZ 0→360° + subtle scale bounce. Phase 2 reverse stagger. */
    private fun playSpin(
        root: ViewGroup,
        talloneViews: List<ImageView>,
        backDrawable: Drawable?,
        handler: Handler,
        onAfterPhase2: () -> Unit
    ) {
        val (ghost, startTx, _) = makeCenterGhost(root, talloneViews, backDrawable)
        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            addUpdateListener { va ->
                val f = va.animatedFraction
                ghost.rotation = f * 360f
                val s = 1f + 0.10f * Math.sin(f.toDouble() * Math.PI).toFloat()
                ghost.scaleX = s
                ghost.scaleY = s
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    ghost.rotation = 0f
                    ghost.scaleX = 1f
                    ghost.scaleY = 1f
                    playPhase2(root, ghost, talloneViews, backDrawable, handler,
                        delays = longArrayOf(0L, 60L, 120L, 180L).reversedArray(),
                        onAllLanded = onAfterPhase2)
                }
            })
        }
        riffleAnims.add(anim)
        anim.start()
    }

    /** FLIP: rotateY 0→360° (card flipping on its vertical axis). Phase 2 normal stagger. */
    private fun playFlip(
        root: ViewGroup,
        talloneViews: List<ImageView>,
        backDrawable: Drawable?,
        handler: Handler,
        onAfterPhase2: () -> Unit
    ) {
        val (ghost, _, _) = makeCenterGhost(root, talloneViews, backDrawable)
        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            addUpdateListener { va ->
                ghost.rotationY = va.animatedFraction * 360f
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    ghost.rotationY = 0f
                    playPhase2(root, ghost, talloneViews, backDrawable, handler,
                        onAllLanded = onAfterPhase2)
                }
            })
        }
        riffleAnims.add(anim)
        anim.start()
    }

    /** TUMBLE: rotateZ rocking ±15°. Phase 2 in pairs (1+2 fast, 3+4 slow). */
    private fun playTumble(
        root: ViewGroup,
        talloneViews: List<ImageView>,
        backDrawable: Drawable?,
        handler: Handler,
        onAfterPhase2: () -> Unit
    ) {
        val (ghost, _, _) = makeCenterGhost(root, talloneViews, backDrawable)
        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            addUpdateListener { va ->
                val f = va.animatedFraction
                ghost.rotation = 15f * Math.sin(f.toDouble() * Math.PI * 4).toFloat()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    ghost.rotation = 0f
                    playPhase2(root, ghost, talloneViews, backDrawable, handler,
                        delays = longArrayOf(0L, 60L, 160L, 220L),
                        onAllLanded = onAfterPhase2)
                }
            })
        }
        riffleAnims.add(anim)
        anim.start()
    }
```

- [ ] **Step 3: Update the dispatcher in `playStyle`**

Find the `when` block inside `playStyle`. Update these three branches from the placeholder routing to the new functions:

```kotlin
            ShuffleStyle.SPIN    -> playSpin   (root, talloneViews, backDrawable, handler, onAfterPhase2)
            ShuffleStyle.FLIP    -> playFlip   (root, talloneViews, backDrawable, handler, onAfterPhase2)
            ShuffleStyle.TUMBLE  -> playTumble (root, talloneViews, backDrawable, handler, onAfterPhase2)
```

- [ ] **Step 4: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt
git commit -m "feat: add SPIN, FLIP, TUMBLE shuffle variants"
```

---

### Task 5: Implement scale variants — BOUNCE, PULSE

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt`

- [ ] **Step 1: Add `playBounce` and `playPulse` private functions**

Add these two functions immediately after `playTumble`:

```kotlin
    /** BOUNCE: elastic scale pop 1→1.2→0.9→1.1→1. Phase 2 normal stagger. */
    private fun playBounce(
        root: ViewGroup,
        talloneViews: List<ImageView>,
        backDrawable: Drawable?,
        handler: Handler,
        onAfterPhase2: () -> Unit
    ) {
        val (ghost, _, _) = makeCenterGhost(root, talloneViews, backDrawable)
        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 450
            addUpdateListener { va ->
                val f = va.animatedFraction
                val s = when {
                    f < 0.25f -> 1f + (f / 0.25f) * 0.20f                   // 1.0 → 1.2
                    f < 0.50f -> 1.20f - ((f - 0.25f) / 0.25f) * 0.30f      // 1.2 → 0.9
                    f < 0.75f -> 0.90f + ((f - 0.50f) / 0.25f) * 0.20f      // 0.9 → 1.1
                    else      -> 1.10f - ((f - 0.75f) / 0.25f) * 0.10f      // 1.1 → 1.0
                }
                ghost.scaleX = s
                ghost.scaleY = s
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    ghost.scaleX = 1f
                    ghost.scaleY = 1f
                    playPhase2(root, ghost, talloneViews, backDrawable, handler,
                        onAllLanded = onAfterPhase2)
                }
            })
        }
        riffleAnims.add(anim)
        anim.start()
    }

    /** PULSE: 5 rapid scale pulses 1→1.1→1. Phase 2 normal stagger. */
    private fun playPulse(
        root: ViewGroup,
        talloneViews: List<ImageView>,
        backDrawable: Drawable?,
        handler: Handler,
        onAfterPhase2: () -> Unit
    ) {
        val (ghost, _, _) = makeCenterGhost(root, talloneViews, backDrawable)
        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 400
            addUpdateListener { va ->
                val f = va.animatedFraction
                // sin² wave gives a clean 0→1→0→1... bell-shape pulse
                val s = 1f + 0.10f * Math.abs(Math.sin(f.toDouble() * Math.PI * 5)).toFloat()
                ghost.scaleX = s
                ghost.scaleY = s
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    ghost.scaleX = 1f
                    ghost.scaleY = 1f
                    playPhase2(root, ghost, talloneViews, backDrawable, handler,
                        onAllLanded = onAfterPhase2)
                }
            })
        }
        riffleAnims.add(anim)
        anim.start()
    }
```

- [ ] **Step 2: Update the dispatcher in `playStyle`**

In the `when` block, replace these two placeholder branches:

```kotlin
            ShuffleStyle.BOUNCE  -> playBounce (root, talloneViews, backDrawable, handler, onAfterPhase2)
            ShuffleStyle.PULSE   -> playPulse  (root, talloneViews, backDrawable, handler, onAfterPhase2)
```

- [ ] **Step 3: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt
git commit -m "feat: add BOUNCE, PULSE shuffle variants"
```

---

### Task 6: Implement translation variants — WAVE, TOSS

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt`

- [ ] **Step 1: Add `playWave` and `playToss` private functions**

Add these immediately after `playPulse`:

```kotlin
    /** WAVE: wide horizontal sway ±16dp, 4 oscillations. Phase 2 simultaneous (all delay 0). */
    private fun playWave(
        root: ViewGroup,
        talloneViews: List<ImageView>,
        backDrawable: Drawable?,
        handler: Handler,
        onAfterPhase2: () -> Unit
    ) {
        val (ghost, startTx, _) = makeCenterGhost(root, talloneViews, backDrawable)
        val dp16 = 16 * root.context.resources.displayMetrics.density
        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            addUpdateListener { va ->
                val f = va.animatedFraction
                ghost.translationX =
                    startTx + Math.sin(f.toDouble() * Math.PI * 4).toFloat() * dp16
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    ghost.translationX = startTx
                    playPhase2(root, ghost, talloneViews, backDrawable, handler,
                        delays = longArrayOf(0L, 0L, 0L, 0L),
                        flightDurationMs = 180L,
                        onAllLanded = onAfterPhase2)
                }
            })
        }
        riffleAnims.add(anim)
        anim.start()
    }

    /** TOSS: vertical hop -50dp (sin half-cycle) + scale 1→1.15→1. Phase 2 tight stagger. */
    private fun playToss(
        root: ViewGroup,
        talloneViews: List<ImageView>,
        backDrawable: Drawable?,
        handler: Handler,
        onAfterPhase2: () -> Unit
    ) {
        val (ghost, _, startTy) = makeCenterGhost(root, talloneViews, backDrawable)
        val dp50 = 50 * root.context.resources.displayMetrics.density
        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            addUpdateListener { va ->
                val f = va.animatedFraction
                ghost.translationY = startTy - Math.sin(f.toDouble() * Math.PI).toFloat() * dp50
                val s = 1f + 0.15f * Math.sin(f.toDouble() * Math.PI).toFloat()
                ghost.scaleX = s
                ghost.scaleY = s
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    ghost.translationY = startTy
                    ghost.scaleX = 1f
                    ghost.scaleY = 1f
                    playPhase2(root, ghost, talloneViews, backDrawable, handler,
                        delays = longArrayOf(0L, 40L, 80L, 120L),
                        flightDurationMs = 200L,
                        onAllLanded = onAfterPhase2)
                }
            })
        }
        riffleAnims.add(anim)
        anim.start()
    }
```

- [ ] **Step 2: Update the dispatcher in `playStyle`**

Replace these two placeholder branches:

```kotlin
            ShuffleStyle.WAVE    -> playWave   (root, talloneViews, backDrawable, handler, onAfterPhase2)
            ShuffleStyle.TOSS    -> playToss   (root, talloneViews, backDrawable, handler, onAfterPhase2)
```

- [ ] **Step 3: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt
git commit -m "feat: add WAVE, TOSS shuffle variants"
```

---

### Task 7: Implement multi-ghost variants — CUT, FAN

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt`

- [ ] **Step 1: Add `playCut` private function**

Add immediately after `playToss`:

```kotlin
    /**
     * CUT: two full-size ghosts at center crosshatch. Ghost A drifts +20dp x / +5dp y,
     * Ghost B drifts -20dp x / -5dp y, then both return to center; the second ghost is
     * removed at end so Phase 2 launches from a single source ghost.
     * Phase 2 in pairs: row1+row4 at 0 ms, row2+row3 at 100 ms (outside-in).
     */
    private fun playCut(
        root: ViewGroup,
        talloneViews: List<ImageView>,
        backDrawable: Drawable?,
        handler: Handler,
        onAfterPhase2: () -> Unit
    ) {
        val deckW   = talloneViews.firstOrNull()?.width?.takeIf  { it > 0 } ?: 80
        val deckH   = talloneViews.firstOrNull()?.height?.takeIf { it > 0 } ?: 100
        val startTx = root.width  / 2f - deckW / 2f
        val startTy = root.height / 2f - deckH / 2f
        val dp20    = 20 * root.context.resources.displayMetrics.density
        val dp5     = 5  * root.context.resources.displayMetrics.density

        val ghostA = makeGhost(root, backDrawable, deckW, deckH, startTx, startTy)
        val ghostB = makeGhost(root, backDrawable, deckW, deckH, startTx, startTy)
        ghostViews.add(ghostA)
        ghostViews.add(ghostB)
        root.addView(ghostA)
        root.addView(ghostB)

        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            addUpdateListener { va ->
                val f = va.animatedFraction
                // Triangle wave: 0 → 1 → 0 over the duration
                val tri = if (f < 0.5f) f * 2f else (1f - f) * 2f
                ghostA.translationX = startTx + dp20 * tri
                ghostA.translationY = startTy + dp5  * tri
                ghostB.translationX = startTx - dp20 * tri
                ghostB.translationY = startTy - dp5  * tri
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    ghostA.translationX = startTx
                    ghostA.translationY = startTy
                    // Drop ghostB so Phase 2 fires from a single source
                    val r = rootRef?.get()
                    r?.removeView(ghostB)
                    ghostViews.remove(ghostB)
                    playPhase2(root, ghostA, talloneViews, backDrawable, handler,
                        delays = longArrayOf(0L, 100L, 100L, 0L),
                        onAllLanded = onAfterPhase2)
                }
            })
        }
        riffleAnims.add(anim)
        anim.start()
    }
```

Note on the delays array `[0, 100, 100, 0]`: it maps to rows `[1, 2, 3, 4]`, giving the outside-in pairs `(row1, row4)` at 0 ms and `(row2, row3)` at 100 ms.

- [ ] **Step 2: Add `playFan` private function**

Add immediately after `playCut`:

```kotlin
    /**
     * FAN: three ghosts at center. Left and right fan out ±20° rotation + outward
     * translation, then converge back; the two outer ghosts are removed at end.
     * Phase 2 normal stagger.
     */
    private fun playFan(
        root: ViewGroup,
        talloneViews: List<ImageView>,
        backDrawable: Drawable?,
        handler: Handler,
        onAfterPhase2: () -> Unit
    ) {
        val deckW   = talloneViews.firstOrNull()?.width?.takeIf  { it > 0 } ?: 80
        val deckH   = talloneViews.firstOrNull()?.height?.takeIf { it > 0 } ?: 100
        val startTx = root.width  / 2f - deckW / 2f
        val startTy = root.height / 2f - deckH / 2f
        val dp25    = 25 * root.context.resources.displayMetrics.density

        val centerGhost = makeGhost(root, backDrawable, deckW, deckH, startTx, startTy)
        val leftGhost   = makeGhost(root, backDrawable, deckW, deckH, startTx, startTy)
        val rightGhost  = makeGhost(root, backDrawable, deckW, deckH, startTx, startTy)
        ghostViews.add(centerGhost)
        ghostViews.add(leftGhost)
        ghostViews.add(rightGhost)
        root.addView(centerGhost)
        root.addView(leftGhost)
        root.addView(rightGhost)

        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 450
            addUpdateListener { va ->
                val f = va.animatedFraction
                val tri = if (f < 0.5f) f * 2f else (1f - f) * 2f
                leftGhost.translationX  = startTx - dp25 * tri
                leftGhost.rotation      = -20f * tri
                rightGhost.translationX = startTx + dp25 * tri
                rightGhost.rotation     = 20f  * tri
                // Center stays put, slightly scales to suggest a "deck of 3" pop
                val s = 1f + 0.05f * tri
                centerGhost.scaleX = s
                centerGhost.scaleY = s
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    centerGhost.scaleX = 1f
                    centerGhost.scaleY = 1f
                    val r = rootRef?.get()
                    r?.removeView(leftGhost)
                    r?.removeView(rightGhost)
                    ghostViews.remove(leftGhost)
                    ghostViews.remove(rightGhost)
                    playPhase2(root, centerGhost, talloneViews, backDrawable, handler,
                        onAllLanded = onAfterPhase2)
                }
            })
        }
        riffleAnims.add(anim)
        anim.start()
    }
```

- [ ] **Step 3: Update the dispatcher in `playStyle`**

Replace these two placeholder branches:

```kotlin
            ShuffleStyle.CUT     -> playCut    (root, talloneViews, backDrawable, handler, onAfterPhase2)
            ShuffleStyle.FAN     -> playFan    (root, talloneViews, backDrawable, handler, onAfterPhase2)
```

After this edit, **no `playStyle` branch should still say "placeholder"**. Confirm by reading the `when` block — every enum value points to its own function.

- [ ] **Step 4: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt
git commit -m "feat: add CUT, FAN multi-ghost shuffle variants"
```

---

### Task 8: Full build + manual verification

- [ ] **Step 1: Full debug build**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Install on a device/emulator**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 3: Verify the overlay timing fix**

- [ ] Tap New Game.
- [ ] Loading overlay appears alone (no card visuals on top).
- [ ] Loading overlay disappears.
- [ ] Empty board visible: 4 talloni show card back, table slots are blank.
- [ ] Shuffle animation begins on the visible board. No overlap with the loading overlay.

- [ ] **Step 4: Verify variant variety**

Run New Game 15-20 times in a row. Visually confirm that:
- [ ] You see at least 5 distinct shuffle styles within 15 plays.
- [ ] Each play, the animation completes cleanly (no leftover ghost views, no flicker).
- [ ] After the shuffle, the cascade deal fires normally (12 cards onto the table).

- [ ] **Step 5: Verify Skip works for each variant**

For at least 3 different variants caught while playing:
- [ ] Tap the screen during the shuffle phase → all animation stops instantly, board becomes interactive immediately, no ghost views remain.

- [ ] **Step 6: Verify Retry is unchanged**

- [ ] Start a game, then tap Retry. Confirm:
  - [ ] No shuffle phase plays (no central ghost, no riffle).
  - [ ] The deal cascade plays at the same speed as before (faster stagger).

- [ ] **Step 7: Verify rotation cleanup**

- [ ] Start a New Game and rotate the device during the shuffle phase. Confirm:
  - [ ] Activity recreates without crash.
  - [ ] No ghost views are stuck on the screen after recreation.
  - [ ] A fresh New Game animation plays after recreation (or the saved game is restored cleanly — depends on how the activity treats rotation).

- [ ] **Step 8: Final commit if any polish fixes were applied**

```bash
git add -A
git commit -m "fix: shuffle-variants polish from manual testing"
```

# Shuffle Distribution + Tallone Reveal — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hide the 4 talloni during Phase 1 (shuffle) so the central deck appears to be the actual source of the cards, then distribute the 4 ghosts via style-specific trajectories (arc, spiral, sinusoid, L-path, fan, …) with style-specific in-flight rotation/scale, and reveal each tallone at the moment its ghost lands via a style-specific animation (alpha-fade, flip, bounce, drop-squash, …).

**Architecture:** Extend `CardAnimator` with a new `animateCardFlightCustom(...)` that accepts optional `pathFn`/`rotationFn`/`scaleFn` lambdas (offset from the linear path, plus rotation/scale during flight). Replace the single shared `playPhase2` in `DealAnimator` with a generic `playPhase2Custom(...)` that takes per-variant lambdas + a `ShuffleStyle`. Add `revealTallone(view, style, onDone)` and 10 small reveal helper functions. Track talloneViews in a new `talloneRefs` field on `DealAnimator` so `skip()` can restore `alpha = 1f` for interrupted intro animations.

**Tech Stack:** Android `ValueAnimator`, `View.alpha/rotation/rotationY/scaleX/scaleY/translationX/translationY`, Kotlin lambdas.

---

## File Map

| File | Change |
|------|--------|
| `app/src/main/java/com/bottazzini/trasloco/utils/CardAnimator.kt` | **Modify** — add `animateCardFlightCustom(...)` sibling method |
| `app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt` | **Modify** — add `talloneRefs` field, hide/restore helpers, `revealTallone` + 10 reveal helpers, `playPhase2Custom`, per-variant Phase 2 updates; remove old `playPhase2` |

---

### Task 1: Add `talloneRefs` field + hide/restore talloni + wire into `playStyle`/`skip`/`reset`

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt`

- [ ] **Step 1: Add the `talloneRefs` field**

Open `DealAnimator.kt`. After the existing field declarations (around line 40, right after `private var skipOnComplete: (() -> Unit)? = null`), add:

```kotlin
    private var talloneRefs: List<ImageView> = emptyList()
```

- [ ] **Step 2: Add `hideTalloni()` and `restoreTalloni()` private helpers**

Add these two helpers right before `private fun reset()`:

```kotlin
    /** Hide all 4 talloni at Phase 1 start so the shuffle appears to be the deck's source. */
    private fun hideTalloni() {
        talloneRefs.forEach { it.alpha = 0f }
    }

    /** Restore tallone visibility (called by skip()/reset() in case the intro was interrupted). */
    private fun restoreTalloni() {
        talloneRefs.forEach { it.alpha = 1f }
    }
```

- [ ] **Step 3: Modify `playNewGame` to store `talloneRefs` and hide them**

Find the body of `playNewGame`:

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
        talloneRefs    = talloneViews
        hideTalloni()

        playStyle(style, root, talloneViews, backDrawable, handler) {
            val timings = cascadeTimings(staggerMs = 150L, roundGapMs = 250L)
            playDealCascade(root, dealEntries, timings, handler, onComplete)
        }
    }
```

- [ ] **Step 4: Modify `skip()` to restore talloni**

Find `fun skip()`:

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

Replace with (add `restoreTalloni()` + clear `talloneRefs`):

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
        restoreTalloni()
        talloneRefs = emptyList()
        val cb = skipOnComplete
        skipOnComplete = null
        handlerRef = null
        rootRef    = null
        cb?.invoke()
    }
```

- [ ] **Step 5: Modify `reset()` to restore talloni**

Find `private fun reset()`:

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
        restoreTalloni()
        talloneRefs    = emptyList()
        handlerRef     = null
        rootRef        = null
        skipOnComplete = null
    }
```

- [ ] **Step 6: Modify `cleanup()` to clear `talloneRefs` (NOT restore alpha — cleanup runs on success after talloni are already revealed)**

Find `private fun cleanup()`:

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

Replace with:

```kotlin
    private fun cleanup() {
        val r = rootRef?.get()
        ghostViews.forEach { r?.removeView(it) }
        ghostViews.clear()
        pendingRunnables.clear()
        riffleAnims.clear()
        talloneRefs    = emptyList()
        handlerRef     = null
        rootRef        = null
        skipOnComplete = null
    }
```

- [ ] **Step 7: Verify it compiles**

```bash
cd /Users/bottazzini/Documents/misc/Trasloco
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`. At this stage, talloni are hidden at intro start but NEVER revealed (because Phase 2 currently just calls `playPhase2` which doesn't touch `alpha`). So if you run the app, you'll see invisible talloni — that's expected; the next tasks add the reveal. **Do not commit at this point** if you need to test interactively; commit is at Step 8 below.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt
git commit -m "feat(DealAnimator): hide talloni during shuffle, track refs for skip-restore"
```

---

### Task 2: Add `CardAnimator.animateCardFlightCustom`

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/utils/CardAnimator.kt`

- [ ] **Step 1: Add the new method inside the `CardAnimator` object**

Open `CardAnimator.kt`. After the existing `animateCardFlight(...)` function (after its closing brace), add this new sibling method (still inside `object CardAnimator { ... }`):

```kotlin
    /**
     * Variant of [animateCardFlight] that allows custom in-flight motion:
     * - [pathFn]:    returns an (Δx, Δy) **offset** added to the linear-interpolation position
     *                at progress f. Pass null for straight-line flight.
     * - [rotationFn]: returns Z-axis rotation degrees at progress f. Pass null for no rotation.
     * - [scaleFn]:   returns a uniform scale multiplier at progress f. Pass null for scale=1.
     *
     * Same zero-dimension fallback as [animateCardFlight]: if either view has zero
     * dimensions, [onComplete] is invoked immediately with no ghost created.
     */
    fun animateCardFlightCustom(
        root: ViewGroup,
        sourceView: View,
        targetView: View,
        drawable: Drawable?,
        durationMs: Long,
        pathFn: ((Float) -> Pair<Float, Float>)? = null,
        rotationFn: ((Float) -> Float)? = null,
        scaleFn: ((Float) -> Float)? = null,
        onComplete: () -> Unit
    ) {
        if (sourceView.width == 0 || sourceView.height == 0 ||
            targetView.width == 0 || targetView.height == 0) {
            onComplete()
            return
        }

        val srcLoc  = IntArray(2)
        val dstLoc  = IntArray(2)
        val rootLoc = IntArray(2)
        sourceView.getLocationOnScreen(srcLoc)
        targetView.getLocationOnScreen(dstLoc)
        root.getLocationOnScreen(rootLoc)

        val startX = (srcLoc[0]  - rootLoc[0]).toFloat()
        val startY = (srcLoc[1]  - rootLoc[1]).toFloat()
        val endX   = (dstLoc[0]  - rootLoc[0]).toFloat()
        val endY   = (dstLoc[1]  - rootLoc[1]).toFloat()

        val ghost = ImageView(root.context).apply {
            setImageDrawable(drawable)
            scaleType = ImageView.ScaleType.FIT_CENTER
            alpha = 0.92f
            elevation = 20f
            translationX = startX
            translationY = startY
            layoutParams = ConstraintLayout.LayoutParams(sourceView.width, sourceView.height)
        }
        root.addView(ghost)

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            addUpdateListener { anim ->
                val f = anim.animatedFraction
                val linX = startX + (endX - startX) * f
                val linY = startY + (endY - startY) * f
                val (dx, dy) = pathFn?.invoke(f) ?: (0f to 0f)
                ghost.translationX = linX + dx
                ghost.translationY = linY + dy
                rotationFn?.invoke(f)?.let { ghost.rotation = it }
                scaleFn?.invoke(f)?.let {
                    ghost.scaleX = it
                    ghost.scaleY = it
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    root.removeView(ghost)
                    onComplete()
                }
                override fun onAnimationCancel(animation: Animator) {
                    root.removeView(ghost)
                }
            })
            start()
        }
    }
```

Note: `Pair<Float, Float>` destructuring works on Kotlin Pairs out of the box (`val (dx, dy) = pairValue`). The `0f to 0f` expression is the standard Kotlin way to build a Pair literal.

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/CardAnimator.kt
git commit -m "feat(CardAnimator): add animateCardFlightCustom with path/rotation/scale hooks"
```

---

### Task 3: Add `revealTallone` dispatcher + 10 reveal helpers

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt`

- [ ] **Step 1: Add `revealTallone` and the 10 reveal helpers**

Add these as private functions in `DealAnimator.kt`, immediately after `restoreTalloni()` (Task 1 placed it near `reset()`):

```kotlin
    /** Dispatch the style-specific reveal animation for a tallone landing. */
    private fun revealTallone(tallone: ImageView, style: ShuffleStyle, onDone: () -> Unit = {}) {
        when (style) {
            ShuffleStyle.RIFFLE -> revealAlphaIn        (tallone, onDone)
            ShuffleStyle.CUT    -> revealPairSync       (tallone, onDone)
            ShuffleStyle.SPIN   -> revealSpin360        (tallone, onDone)
            ShuffleStyle.BOUNCE -> revealScaleBounce    (tallone, onDone)
            ShuffleStyle.WAVE   -> revealSlideInX       (tallone, onDone)
            ShuffleStyle.FLIP   -> revealFlipY          (tallone, onDone)
            ShuffleStyle.TUMBLE -> revealRocking        (tallone, onDone)
            ShuffleStyle.PULSE  -> revealPulse          (tallone, onDone)
            ShuffleStyle.TOSS   -> revealDropSquash     (tallone, onDone)
            ShuffleStyle.FAN    -> revealRotationSettle (tallone, onDone)
        }
    }

    /** Plain alpha 0→1 fade-in. */
    private fun revealAlphaIn(tallone: ImageView, onDone: () -> Unit) {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 180
            addUpdateListener { tallone.alpha = it.animatedFraction }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { tallone.alpha = 1f; onDone() }
            })
            start()
        }
    }

    /** Pair sync: alpha + scale 0.9→1. */
    private fun revealPairSync(tallone: ImageView, onDone: () -> Unit) {
        tallone.scaleX = 0.9f
        tallone.scaleY = 0.9f
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 180
            addUpdateListener {
                val f = it.animatedFraction
                tallone.alpha = f
                val s = 0.9f + 0.1f * f
                tallone.scaleX = s
                tallone.scaleY = s
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    tallone.alpha = 1f; tallone.scaleX = 1f; tallone.scaleY = 1f; onDone()
                }
            })
            start()
        }
    }

    /** Spin 360° while fading in. */
    private fun revealSpin360(tallone: ImageView, onDone: () -> Unit) {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            addUpdateListener {
                val f = it.animatedFraction
                tallone.alpha = f
                tallone.rotation = f * 360f
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    tallone.alpha = 1f; tallone.rotation = 0f; onDone()
                }
            })
            start()
        }
    }

    /** Scale bounce 0→1.2→1, alpha 0→1 in first 100ms. */
    private fun revealScaleBounce(tallone: ImageView, onDone: () -> Unit) {
        tallone.scaleX = 0f
        tallone.scaleY = 0f
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            addUpdateListener {
                val f = it.animatedFraction
                tallone.alpha = minOf(1f, f * 3f)              // alpha hits 1 at f≈0.33
                val s = if (f < 0.5f) f * 2.4f                  // 0 → 1.2
                        else 1.2f - (f - 0.5f) * 0.4f           // 1.2 → 1.0
                tallone.scaleX = s
                tallone.scaleY = s
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    tallone.alpha = 1f; tallone.scaleX = 1f; tallone.scaleY = 1f; onDone()
                }
            })
            start()
        }
    }

    /** Slide in from -30dp on X, alpha 0→1. */
    private fun revealSlideInX(tallone: ImageView, onDone: () -> Unit) {
        val dp30 = 30f * tallone.context.resources.displayMetrics.density
        tallone.translationX = -dp30
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 220
            addUpdateListener {
                val f = it.animatedFraction
                tallone.alpha = f
                tallone.translationX = -dp30 * (1f - f)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    tallone.alpha = 1f; tallone.translationX = 0f; onDone()
                }
            })
            start()
        }
    }

    /** RotateY 90°→0° (card flipping into place), alpha 0→1. */
    private fun revealFlipY(tallone: ImageView, onDone: () -> Unit) {
        tallone.rotationY = 90f
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 240
            addUpdateListener {
                val f = it.animatedFraction
                tallone.alpha = f
                tallone.rotationY = 90f * (1f - f)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    tallone.alpha = 1f; tallone.rotationY = 0f; onDone()
                }
            })
            start()
        }
    }

    /** Rock ±10° once, settle, alpha 0→1. */
    private fun revealRocking(tallone: ImageView, onDone: () -> Unit) {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 260
            addUpdateListener {
                val f = it.animatedFraction
                tallone.alpha = f
                tallone.rotation = 10f * Math.sin(f.toDouble() * Math.PI * 2).toFloat()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    tallone.alpha = 1f; tallone.rotation = 0f; onDone()
                }
            })
            start()
        }
    }

    /** Pulse scale 1→1.08→1→1.08→1, alpha 0→1 in first 100ms. */
    private fun revealPulse(tallone: ImageView, onDone: () -> Unit) {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 260
            addUpdateListener {
                val f = it.animatedFraction
                tallone.alpha = minOf(1f, f * 3f)
                val s = 1f + 0.08f * Math.abs(Math.sin(f.toDouble() * Math.PI * 2)).toFloat()
                tallone.scaleX = s
                tallone.scaleY = s
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    tallone.alpha = 1f; tallone.scaleX = 1f; tallone.scaleY = 1f; onDone()
                }
            })
            start()
        }
    }

    /** Drop-squash: scaleY 1→1.2→0.85→1 (squash on impact), alpha 0→1 quickly. */
    private fun revealDropSquash(tallone: ImageView, onDone: () -> Unit) {
        tallone.scaleY = 1.2f
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 280
            addUpdateListener {
                val f = it.animatedFraction
                tallone.alpha = minOf(1f, f * 3f)
                val sy = when {
                    f < 0.4f -> 1.2f - (f / 0.4f) * 0.35f         // 1.2 → 0.85
                    else     -> 0.85f + ((f - 0.4f) / 0.6f) * 0.15f // 0.85 → 1.0
                }
                tallone.scaleY = sy
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    tallone.alpha = 1f; tallone.scaleY = 1f; onDone()
                }
            })
            start()
        }
    }

    /** Rotation -10° → 0° + alpha 0→1. */
    private fun revealRotationSettle(tallone: ImageView, onDone: () -> Unit) {
        tallone.rotation = -10f
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 220
            addUpdateListener {
                val f = it.animatedFraction
                tallone.alpha = f
                tallone.rotation = -10f * (1f - f)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    tallone.alpha = 1f; tallone.rotation = 0f; onDone()
                }
            })
            start()
        }
    }
```

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`. (The reveal functions are not yet called from anywhere — Tasks 4-7 wire them in.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt
git commit -m "feat(DealAnimator): add revealTallone dispatcher and 10 reveal helpers"
```

---

### Task 4: Add generic `playPhase2Custom` per-variant Phase 2 helper

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt`

- [ ] **Step 1: Add `playPhase2Custom` private function**

Add this immediately after the existing `playPhase2` function (we will remove the old `playPhase2` later in Task 8):

```kotlin
    /**
     * Generic Phase 2 dispatcher used by per-variant `playXxx` functions.
     *
     * For each tallone (with optional per-row delays), creates a ghost via
     * [CardAnimator.animateCardFlightCustom] with style-specific path / rotation /
     * scale, and when the ghost lands triggers [revealTallone] for that tallone.
     * Fires [onAllLanded] after the **last ghost lands** (the reveal runs in
     * parallel — we don't wait for it to allow Phase 3 cascade to chain).
     *
     * Lambdas are index-aware (the index is the row, 0..3) so variants like FAN
     * can vary per-ghost behavior.
     */
    private fun playPhase2Custom(
        root: ViewGroup,
        centralGhost: View,
        talloneViews: List<ImageView>,
        backDrawable: Drawable?,
        handler: Handler,
        delays: LongArray = longArrayOf(0L, 60L, 120L, 180L),
        flightDurationMs: Long = 280L,
        style: ShuffleStyle,
        pathFn: ((Int, Float) -> Pair<Float, Float>)? = null,
        rotationFn: ((Int, Float) -> Float)? = null,
        scaleFn: ((Int, Float) -> Float)? = null,
        onAllLanded: () -> Unit
    ) {
        var landedCount = 0
        val total = talloneViews.size

        talloneViews.forEachIndexed { index, tallone ->
            val r = Runnable {
                val currentRoot = rootRef?.get() ?: return@Runnable
                val perRowPath = pathFn?.let { fn -> { f: Float -> fn(index, f) } }
                val perRowRot  = rotationFn?.let { fn -> { f: Float -> fn(index, f) } }
                val perRowScl  = scaleFn?.let { fn -> { f: Float -> fn(index, f) } }
                CardAnimator.animateCardFlightCustom(
                    currentRoot, centralGhost, tallone, backDrawable, flightDurationMs,
                    pathFn = perRowPath,
                    rotationFn = perRowRot,
                    scaleFn = perRowScl
                ) {
                    // Reveal runs in parallel — we fire onLanded right away so the cascade can chain.
                    revealTallone(tallone, style)
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

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`. The helper is unused so far — Tasks 5-7 wire each variant to it.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt
git commit -m "feat(DealAnimator): add generic playPhase2Custom with per-row lambdas"
```

---

### Task 5: Update RIFFLE, SPIN, FLIP, TUMBLE to use `playPhase2Custom`

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt`

For each of these 4 variants, replace the `playPhase2(...)` call inside `onAnimationEnd` with a `playPhase2Custom(...)` call carrying style-specific lambdas + the appropriate `style:` argument.

- [ ] **Step 1: Update `playRiffle`'s onAnimationEnd**

Find inside `playRiffle`:

```kotlin
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (rootRef?.get() == null) return
                    ghost.translationX = startTx
                    ghost.scaleX = 1f
                    playPhase2(root, ghost, talloneViews, backDrawable, handler,
                        onAllLanded = onAfterPhase2)
                }
            })
```

Replace with:

```kotlin
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (rootRef?.get() == null) return
                    ghost.translationX = startTx
                    ghost.scaleX = 1f
                    playPhase2Custom(root, ghost, talloneViews, backDrawable, handler,
                        style = ShuffleStyle.RIFFLE,
                        onAllLanded = onAfterPhase2)
                }
            })
```

- [ ] **Step 2: Update `playSpin`'s onAnimationEnd**

Find inside `playSpin`:

```kotlin
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (rootRef?.get() == null) return
                    ghost.rotation = 0f
                    ghost.scaleX = 1f
                    ghost.scaleY = 1f
                    playPhase2(root, ghost, talloneViews, backDrawable, handler,
                        delays = longArrayOf(180L, 120L, 60L, 0L),
                        onAllLanded = onAfterPhase2)
                }
            })
```

Replace with (adds spiral path + in-flight Z rotation):

```kotlin
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (rootRef?.get() == null) return
                    ghost.rotation = 0f
                    ghost.scaleX = 1f
                    ghost.scaleY = 1f
                    val dp30 = 30f * root.context.resources.displayMetrics.density
                    playPhase2Custom(root, ghost, talloneViews, backDrawable, handler,
                        delays = longArrayOf(180L, 120L, 60L, 0L),
                        flightDurationMs = 320L,
                        style = ShuffleStyle.SPIN,
                        pathFn = { _, f ->
                            // Tangential offset: peaks at f=0.5, returns to 0
                            val r = Math.sin(f.toDouble() * Math.PI).toFloat() * dp30
                            val theta = (2.0 * Math.PI * f).toFloat()
                            (r * Math.cos(theta.toDouble()).toFloat()) to (r * Math.sin(theta.toDouble()).toFloat())
                        },
                        rotationFn = { _, f -> f * 360f },
                        onAllLanded = onAfterPhase2)
                }
            })
```

- [ ] **Step 3: Update `playFlip`'s onAnimationEnd**

Find inside `playFlip`:

```kotlin
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (rootRef?.get() == null) return
                    ghost.rotationY = 0f
                    playPhase2(root, ghost, talloneViews, backDrawable, handler,
                        onAllLanded = onAfterPhase2)
                }
            })
```

Replace with:

```kotlin
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (rootRef?.get() == null) return
                    ghost.rotationY = 0f
                    playPhase2Custom(root, ghost, talloneViews, backDrawable, handler,
                        style = ShuffleStyle.FLIP,
                        rotationFn = { _, f -> f * 360f }, // Z rotation in flight (rotationY isn't supported by our API; use rotation)
                        onAllLanded = onAfterPhase2)
                }
            })
```

Note: `animateCardFlightCustom`'s `rotationFn` controls Z rotation. The spec calls for Y-axis flip in flight; since the API is Z-only, use a 360° Z spin as a proxy for "flip" during the ghost flight. The talloni reveal still does the full Y-flip (handled by `revealFlipY`).

- [ ] **Step 4: Update `playTumble`'s onAnimationEnd**

Find inside `playTumble`:

```kotlin
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (rootRef?.get() == null) return
                    ghost.rotation = 0f
                    playPhase2(root, ghost, talloneViews, backDrawable, handler,
                        delays = longArrayOf(0L, 60L, 160L, 220L),
                        onAllLanded = onAfterPhase2)
                }
            })
```

Replace with:

```kotlin
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (rootRef?.get() == null) return
                    ghost.rotation = 0f
                    playPhase2Custom(root, ghost, talloneViews, backDrawable, handler,
                        delays = longArrayOf(0L, 60L, 160L, 220L),
                        style = ShuffleStyle.TUMBLE,
                        rotationFn = { _, f -> Math.sin(f.toDouble() * Math.PI * 2).toFloat() * 20f },
                        onAllLanded = onAfterPhase2)
                }
            })
```

- [ ] **Step 5: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`. At this point, 4 of 10 variants will produce visible talloni again (via `revealTallone` inside `playPhase2Custom`). The other 6 still call the OLD `playPhase2` and will leave talloni invisible — fix in Tasks 6-7.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt
git commit -m "feat(DealAnimator): RIFFLE/SPIN/FLIP/TUMBLE use playPhase2Custom + reveal"
```

---

### Task 6: Update BOUNCE, PULSE, WAVE, TOSS to use `playPhase2Custom`

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt`

- [ ] **Step 1: Update `playBounce`'s onAnimationEnd**

Find inside `playBounce`:

```kotlin
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (rootRef?.get() == null) return
                    ghost.scaleX = 1f
                    ghost.scaleY = 1f
                    playPhase2(root, ghost, talloneViews, backDrawable, handler,
                        onAllLanded = onAfterPhase2)
                }
            })
```

Replace with (adds low-arc trajectory + scale pulse during flight):

```kotlin
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (rootRef?.get() == null) return
                    ghost.scaleX = 1f
                    ghost.scaleY = 1f
                    val dp40 = 40f * root.context.resources.displayMetrics.density
                    playPhase2Custom(root, ghost, talloneViews, backDrawable, handler,
                        style = ShuffleStyle.BOUNCE,
                        pathFn = { _, f -> 0f to -Math.sin(f.toDouble() * Math.PI).toFloat() * dp40 },
                        scaleFn = { _, f -> 1f + 0.10f * Math.sin(f.toDouble() * Math.PI).toFloat() },
                        onAllLanded = onAfterPhase2)
                }
            })
```

- [ ] **Step 2: Update `playPulse`'s onAnimationEnd**

Find inside `playPulse`:

```kotlin
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (rootRef?.get() == null) return
                    ghost.scaleX = 1f
                    ghost.scaleY = 1f
                    playPhase2(root, ghost, talloneViews, backDrawable, handler,
                        onAllLanded = onAfterPhase2)
                }
            })
```

Replace with:

```kotlin
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (rootRef?.get() == null) return
                    ghost.scaleX = 1f
                    ghost.scaleY = 1f
                    playPhase2Custom(root, ghost, talloneViews, backDrawable, handler,
                        style = ShuffleStyle.PULSE,
                        scaleFn = { _, f ->
                            1f + 0.05f * Math.abs(Math.sin(f.toDouble() * Math.PI * 3)).toFloat()
                        },
                        onAllLanded = onAfterPhase2)
                }
            })
```

- [ ] **Step 3: Update `playWave`'s onAnimationEnd**

Find inside `playWave`:

```kotlin
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (rootRef?.get() == null) return
                    ghost.translationX = startTx
                    playPhase2(root, ghost, talloneViews, backDrawable, handler,
                        delays = longArrayOf(0L, 0L, 0L, 0L),
                        flightDurationMs = 180L,
                        onAllLanded = onAfterPhase2)
                }
            })
```

Replace with (adds horizontal sinusoidal wave path):

```kotlin
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (rootRef?.get() == null) return
                    ghost.translationX = startTx
                    val dp20 = 20f * root.context.resources.displayMetrics.density
                    playPhase2Custom(root, ghost, talloneViews, backDrawable, handler,
                        delays = longArrayOf(0L, 0L, 0L, 0L),
                        flightDurationMs = 280L,
                        style = ShuffleStyle.WAVE,
                        pathFn = { _, f ->
                            Math.sin(f.toDouble() * Math.PI * 2).toFloat() * dp20 to 0f
                        },
                        onAllLanded = onAfterPhase2)
                }
            })
```

- [ ] **Step 4: Update `playToss`'s onAnimationEnd**

Find inside `playToss`:

```kotlin
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (rootRef?.get() == null) return
                    ghost.translationY = startTy
                    ghost.scaleX = 1f
                    ghost.scaleY = 1f
                    playPhase2(root, ghost, talloneViews, backDrawable, handler,
                        delays = longArrayOf(0L, 40L, 80L, 120L),
                        flightDurationMs = 200L,
                        onAllLanded = onAfterPhase2)
                }
            })
```

Replace with (high-arc trajectory + scale during flight):

```kotlin
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (rootRef?.get() == null) return
                    ghost.translationY = startTy
                    ghost.scaleX = 1f
                    ghost.scaleY = 1f
                    val dp80 = 80f * root.context.resources.displayMetrics.density
                    playPhase2Custom(root, ghost, talloneViews, backDrawable, handler,
                        delays = longArrayOf(0L, 40L, 80L, 120L),
                        flightDurationMs = 280L,
                        style = ShuffleStyle.TOSS,
                        pathFn = { _, f -> 0f to -Math.sin(f.toDouble() * Math.PI).toFloat() * dp80 },
                        scaleFn = { _, f -> 1f + 0.20f * Math.sin(f.toDouble() * Math.PI).toFloat() },
                        onAllLanded = onAfterPhase2)
                }
            })
```

- [ ] **Step 5: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`. 8 of 10 variants now produce visible talloni. CUT and FAN still call the old `playPhase2`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt
git commit -m "feat(DealAnimator): BOUNCE/PULSE/WAVE/TOSS use playPhase2Custom + reveal"
```

---

### Task 7: Update CUT and FAN to use `playPhase2Custom`

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt`

- [ ] **Step 1: Update `playCut`'s onAnimationEnd**

Find inside `playCut`:

```kotlin
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (rootRef?.get() == null) return
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
```

Replace with (adds L-path: horizontal first, then vertical):

```kotlin
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (rootRef?.get() == null) return
                    ghostA.translationX = startTx
                    ghostA.translationY = startTy
                    // Drop ghostB so Phase 2 fires from a single source
                    val r = rootRef?.get()
                    r?.removeView(ghostB)
                    ghostViews.remove(ghostB)
                    playPhase2Custom(root, ghostA, talloneViews, backDrawable, handler,
                        delays = longArrayOf(0L, 100L, 100L, 0L),
                        flightDurationMs = 300L,
                        style = ShuffleStyle.CUT,
                        // L-path: in first half, suppress Y movement (offset +linY's complement);
                        // in second half, suppress X movement.
                        // The animator already lerps linear startX→endX, startY→endY.
                        // We use pathFn to bias toward an L-shape:
                        pathFn = { _, f ->
                            // f in [0, 0.5]: stay at startY (subtract the linear Y progression)
                            // f in [0.5, 1]: stay at endX (subtract the X progression that hasn't completed)
                            // Easier: at progress f, the linear contribution to (dy_extra) cancels Y for f<0.5,
                            // and cancels X for f>0.5. Since we don't know start/end here, just emit zero offsets;
                            // L-path can be done by easing pathFn=0 (linear) with custom interpolation — but the
                            // interpolator is fixed to linear. So we use two-segment offsets approximating L:
                            // for f<0.5: add a +Y offset that grows = 0.5·(endY-startY) magnitude ... we don't
                            // have direct access to start/end here. Pragmatic compromise: emit zero pathFn and
                            // rely on the linear lerp + ghost rotation-free flight. This makes CUT visually a
                            // straight diagonal flight; the "L" is implemented via the *reveal* (pair sync) and
                            // the paired delays. Documented limitation.
                            0f to 0f
                        },
                        onAllLanded = onAfterPhase2)
                }
            })
```

Note about the L-path: `pathFn` only receives the current progress `f`, not the start/end coordinates. A true L-path would require knowing `endX - startX` and `endY - startY` to bias the path. Implementing this cleanly would require either (a) extending `animateCardFlightCustom` to pass start/end to `pathFn`, or (b) passing per-row offsets via a separate channel. For this plan we keep CUT visually as a paired diagonal (which already matches the "pairs" theme via the delay array `[0, 100, 100, 0]` and the `revealPairSync` animation), and we leave the L-path bias as an "L-shape via reveal/timing only" approximation. The `pathFn` is kept (returning zero offsets) so the call shape is uniform across variants. If you decide later that a true L-path is required, extend `animateCardFlightCustom`'s `pathFn` signature to receive `(f, startX, startY, endX, endY)`.

- [ ] **Step 2: Update `playFan`'s onAnimationEnd**

Find inside `playFan`:

```kotlin
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (rootRef?.get() == null) return
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
```

Replace with (per-row X offset and rotation: fan spread):

```kotlin
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (rootRef?.get() == null) return
                    centerGhost.scaleX = 1f
                    centerGhost.scaleY = 1f
                    val r = rootRef?.get()
                    r?.removeView(leftGhost)
                    r?.removeView(rightGhost)
                    ghostViews.remove(leftGhost)
                    ghostViews.remove(rightGhost)
                    val dp20 = 20f * root.context.resources.displayMetrics.density
                    // Row offset: -1.5, -0.5, +0.5, +1.5 for rows 0..3.
                    // Mid-flight peaks at sin(πf)=1 → ghosts spread outward, then converge to their tallone.
                    playPhase2Custom(root, centerGhost, talloneViews, backDrawable, handler,
                        flightDurationMs = 300L,
                        style = ShuffleStyle.FAN,
                        pathFn = { index, f ->
                            val rowOffset = -1.5f + index.toFloat()       // -1.5, -0.5, +0.5, +1.5
                            val bow = Math.sin(f.toDouble() * Math.PI).toFloat()
                            (rowOffset * bow * dp20) to 0f
                        },
                        rotationFn = { index, f ->
                            val rowOffset = -1.5f + index.toFloat()
                            rowOffset * 10f * Math.sin(f.toDouble() * Math.PI).toFloat()
                        },
                        onAllLanded = onAfterPhase2)
                }
            })
```

- [ ] **Step 3: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`. All 10 variants now use `playPhase2Custom`. The old `playPhase2` is no longer called by anyone but still defined.

- [ ] **Step 4: Confirm no remaining callers of the old `playPhase2`**

```bash
grep -n "playPhase2(" app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt
```

Expected output: ONE line — the `private fun playPhase2(` declaration. No call sites.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt
git commit -m "feat(DealAnimator): CUT/FAN use playPhase2Custom; all 10 variants migrated"
```

---

### Task 8: Remove the dead `playPhase2` + full build verification

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt`

- [ ] **Step 1: Delete the old `playPhase2`**

Find the entire `playPhase2` function (its docblock + signature + body) — it should look like this:

```kotlin
    /**
     * Phase 2: 4 ghost decks fly from centralGhost position to each tallone.
     * Stagger and flight duration are caller-controlled via [delays] and
     * [flightDurationMs] (defaults: 0/60/120/180 ms stagger, 220 ms flight).
     * Fires onAllLanded after the last ghost lands.
     */
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

Delete it entirely.

- [ ] **Step 2: Verify the build still passes**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Full debug build**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt
git commit -m "refactor(DealAnimator): remove dead playPhase2 (replaced by playPhase2Custom)"
```

- [ ] **Step 5: Manual verification (user)**

Install on device and run New Game 20+ times. For each of the 10 variants (will require multiple plays to sample them all):

- [ ] During Phase 1 (central shuffle), the 4 talloni are NOT visible. The board shows only the central shuffling deck.
- [ ] During Phase 2 (distribution), the ghost cards fly out on style-specific paths (arcs for BOUNCE/TOSS, spiral for SPIN, sinusoid for WAVE, fan for FAN, etc.).
- [ ] Each tallone reveals at landing with its style-specific animation (alpha-fade for RIFFLE; spin for SPIN; bounce for BOUNCE; flip for FLIP; etc.).
- [ ] Phase 3 cascade fires after the last ghost lands. The cascade doesn't visibly conflict with the in-progress reveals (reveals run ~250ms in parallel with cascade start).

Skip tests:
- [ ] Tap during Phase 1 → all 4 talloni instantly visible at alpha=1, no leftover ghosts.
- [ ] Tap during Phase 2 (after some ghosts landed, some still in flight) → all 4 talloni visible, no leftover ghosts.
- [ ] Tap during Phase 3 cascade → instantly all cards visible (unchanged behavior).

Retry test:
- [ ] Tap Retry → no shuffle phase plays; cascade fires immediately. Talloni stay visible throughout. (Retry doesn't use the new code paths.)

Rotation test:
- [ ] Rotate the device during the shuffle phase. Activity recreates without crash. No talloni stuck at alpha=0. No ghost residue.

- [ ] **Step 6: Final commit if any polish fixes were applied**

```bash
git add -A
git commit -m "fix: distribution/reveal polish from manual testing"
```

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

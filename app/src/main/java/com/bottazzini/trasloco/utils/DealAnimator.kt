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
    private val riffleAnims = mutableListOf<ValueAnimator>()
    private var handlerRef: Handler? = null
    private var rootRef: WeakReference<ViewGroup>? = null
    private var skipOnComplete: (() -> Unit)? = null
    private var talloneRefs: List<ImageView> = emptyList()

    enum class ShuffleStyle {
        RIFFLE, CUT, SPIN, BOUNCE, WAVE, FLIP, TUMBLE, PULSE, TOSS, FAN
    }

    // ── Public API ────────────────────────────────────────────────────────

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
            ShuffleStyle.CUT     -> playCut    (root, talloneViews, backDrawable, handler, onAfterPhase2)
            ShuffleStyle.SPIN    -> playSpin   (root, talloneViews, backDrawable, handler, onAfterPhase2)
            ShuffleStyle.BOUNCE  -> playBounce (root, talloneViews, backDrawable, handler, onAfterPhase2)
            ShuffleStyle.WAVE    -> playWave   (root, talloneViews, backDrawable, handler, onAfterPhase2)
            ShuffleStyle.FLIP    -> playFlip   (root, talloneViews, backDrawable, handler, onAfterPhase2)
            ShuffleStyle.TUMBLE  -> playTumble (root, talloneViews, backDrawable, handler, onAfterPhase2)
            ShuffleStyle.PULSE   -> playPulse  (root, talloneViews, backDrawable, handler, onAfterPhase2)
            ShuffleStyle.TOSS    -> playToss   (root, talloneViews, backDrawable, handler, onAfterPhase2)
            ShuffleStyle.FAN     -> playFan    (root, talloneViews, backDrawable, handler, onAfterPhase2)
        }
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

    // ── Private helpers ───────────────────────────────────────────────────

    /** Hide all 4 talloni at Phase 1 start so the shuffle appears to be the deck's source. */
    private fun hideTalloni() {
        talloneRefs.forEach { it.alpha = 0f }
    }

    /** Restore tallone visibility (called by skip()/reset() in case the intro was interrupted). */
    private fun restoreTalloni() {
        talloneRefs.forEach { it.alpha = 1f }
    }

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
                    if (rootRef?.get() == null) return
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

    /** SPIN: rotateZ 0→360° + subtle scale bounce. Phase 2 reverse stagger. */
    private fun playSpin(
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
                ghost.rotation = f * 360f
                val s = 1f + 0.10f * Math.sin(f.toDouble() * Math.PI).toFloat()
                ghost.scaleX = s
                ghost.scaleY = s
            }
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
                    if (rootRef?.get() == null) return
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
                    if (rootRef?.get() == null) return
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
                    if (rootRef?.get() == null) return
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
                    if (rootRef?.get() == null) return
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
                    if (rootRef?.get() == null) return
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
        }
        riffleAnims.add(anim)
        anim.start()
    }

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
        }
        riffleAnims.add(anim)
        anim.start()
    }

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
        }
        riffleAnims.add(anim)
        anim.start()
    }

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
        riffleAnims.clear()
        talloneRefs    = emptyList()
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

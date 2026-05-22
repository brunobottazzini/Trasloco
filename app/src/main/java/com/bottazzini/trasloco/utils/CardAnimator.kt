package com.bottazzini.trasloco.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * Animates a card image flying from one slot to another using a ghost ImageView.
 *
 * The caller is responsible for updating game state before calling this.
 * The [onComplete] callback should perform the visual-only update on the target slot.
 */
object CardAnimator {

    /**
     * Fly a ghost card from [sourceView]'s screen position to [targetView]'s screen position
     * inside [root]. Calls [onComplete] when the animation ends.
     *
     * Falls back to calling [onComplete] immediately if either view has zero dimensions
     * (e.g. not yet laid out).
     */
    fun animateCardFlight(
        root: ViewGroup,
        sourceView: View,
        targetView: View,
        drawable: Drawable?,
        durationMs: Long = 350L,
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
                ghost.translationX = startX + (endX - startX) * f
                ghost.translationY = startY + (endY - startY) * f
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
}

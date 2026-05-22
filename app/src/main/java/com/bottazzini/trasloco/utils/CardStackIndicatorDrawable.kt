package com.bottazzini.trasloco.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * Draws two thin horizontal white lines near the bottom of a card view,
 * indicating that cards are stacked below.
 *
 * Apply via ViewOverlay so it doesn't interfere with existing foreground/background.
 */
class CardStackIndicatorDrawable(context: Context) : Drawable() {

    private val density = context.resources.displayMetrics.density

    // line 1: 2dp thick, 7dp from bottom edge (~30% white)
    private val paint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(76, 255, 255, 255)
    }
    // line 2: 1.5dp thick, 3dp from bottom edge (~18% white)
    private val paint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(46, 255, 255, 255)
    }

    private val margin      = 4f   * density   // left/right inset
    private val l1Height    = 2f   * density
    private val l1FromBot   = 7f   * density
    private val l2Height    = 1.5f * density
    private val l2FromBot   = 3f   * density

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        val bottom = b.bottom.toFloat()
        val left   = b.left  + margin
        val right  = b.right - margin
        // upper line (more visible)
        canvas.drawRect(left, bottom - l1FromBot - l1Height, right, bottom - l1FromBot, paint1)
        // lower line (fainter)
        canvas.drawRect(left, bottom - l2FromBot - l2Height, right, bottom - l2FromBot, paint2)
    }

    override fun setAlpha(alpha: Int) { /* not used */ }
    override fun setColorFilter(cf: ColorFilter?) { /* not used */ }
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

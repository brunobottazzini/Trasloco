package com.bottazzini.trasloco.utils

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.bottazzini.trasloco.R

object ThemeUtils {
    fun accentColor(bg: String, context: Context): Int = when (bg) {
        "bordeaux", "verde" -> ContextCompat.getColor(context, R.color.casino_gold)
        else -> Color.WHITE
    }

    fun accentColorDim(bg: String, context: Context): Int {
        val base = accentColor(bg, context)
        return Color.argb(128, Color.red(base), Color.green(base), Color.blue(base))
    }
}

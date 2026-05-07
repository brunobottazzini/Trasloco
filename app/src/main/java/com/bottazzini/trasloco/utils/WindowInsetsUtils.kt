package com.bottazzini.trasloco.utils

import android.graphics.Rect
import android.view.View
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

object WindowInsetsUtils {

    fun applySystemBarInsets(window: Window, rootView: View) {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val initialPadding = Rect(
            rootView.paddingLeft,
            rootView.paddingTop,
            rootView.paddingRight,
            rootView.paddingBottom
        )

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(
                initialPadding.left + bars.left,
                initialPadding.top + bars.top,
                initialPadding.right + bars.right,
                initialPadding.bottom + bars.bottom
            )
            WindowInsetsCompat.CONSUMED
        }
    }
}

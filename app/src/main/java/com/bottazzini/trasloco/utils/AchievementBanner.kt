package com.bottazzini.trasloco.utils

import android.content.Context
import android.view.View
import android.widget.TextView
import com.bottazzini.trasloco.R
import java.util.LinkedList

class AchievementBanner(
    private val context: Context,
    private val bannerRoot: View
) {
    private val queue: LinkedList<AchievementDef> = LinkedList()
    private var isShowing = false

    private val iconView: TextView = bannerRoot.findViewById(R.id.achievementBannerIcon)
    private val nameView: TextView = bannerRoot.findViewById(R.id.achievementBannerName)
    private val descView: TextView = bannerRoot.findViewById(R.id.achievementBannerDesc)

    fun enqueue(achievements: List<AchievementDef>) {
        if (achievements.isEmpty()) return
        queue.addAll(achievements)
        if (!isShowing) showNext()
    }

    private fun showNext() {
        val def = queue.poll() ?: run { isShowing = false; return }
        isShowing = true

        iconView.text = def.icon
        nameView.text = context.getString(def.nameRes)
        descView.text = context.getString(def.descRes)

        bannerRoot.translationY = -bannerRoot.height.toFloat().coerceAtLeast(200f)
        bannerRoot.visibility = View.VISIBLE

        bannerRoot.animate()
            .translationY(0f)
            .setDuration(300)
            .withEndAction {
                bannerRoot.postDelayed({
                    bannerRoot.animate()
                        .translationY(-bannerRoot.height.toFloat().coerceAtLeast(200f))
                        .setDuration(300)
                        .withEndAction {
                            bannerRoot.visibility = View.GONE
                            showNext()
                        }
                        .start()
                }, 2500)
            }
            .start()
    }
}

package com.bottazzini.trasloco

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var skipEnabled = false
    private var hasNavigated = false

    private val skipEnableDelayMs = 500L
    private val totalDurationMs = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.hide()
        setContentView(R.layout.activity_splash)

        startAnimation()

        handler.postDelayed({ skipEnabled = true }, skipEnableDelayMs)
        handler.postDelayed({ navigateToMain() }, totalDurationMs)
    }

    private fun startAnimation() {
        val card1 = findViewById<ImageView>(R.id.splashCard1)
        val card2 = findViewById<ImageView>(R.id.splashCard2)
        val card3 = findViewById<ImageView>(R.id.splashCard3)
        val card4 = findViewById<ImageView>(R.id.splashCard4)

        // Phase 1 (0.0s - 0.6s): stacked cards fade in with slight rotation
        val stackCards = listOf(card1, card2, card3, card4)
        stackCards.forEachIndexed { idx, card ->
            card.alpha = 0f
            card.translationY = -(idx * 2f)
            card.rotation = (idx - 1.5f) * 2f
            card.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(idx * 50L)
                .start()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN && skipEnabled) {
            navigateToMain()
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun navigateToMain() {
        if (hasNavigated) return
        hasNavigated = true
        handler.removeCallbacksAndMessages(null)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}

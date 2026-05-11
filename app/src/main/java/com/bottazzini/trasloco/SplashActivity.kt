package com.bottazzini.trasloco

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.media.MediaPlayer
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
    private var mediaPlayer: MediaPlayer? = null

    private val density by lazy { resources.displayMetrics.density }
    private fun dp(value: Float): Float = value * density

    private val skipEnableDelayMs = 500L
    private val totalDurationMs = 3200L

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
        // Brand intro Phase A: Bottazzini Softworks (0ms - 600ms)
        val brandStudio = findViewById<ImageView>(R.id.splashBrandStudio)
        brandStudio.alpha = 0f
        brandStudio.animate().alpha(1f).setDuration(150).withEndAction {
            brandStudio.postDelayed({
                brandStudio.animate().alpha(0f).setDuration(150).start()
            }, 800L)
        }.start()

        // Brand intro Phase B: Game logo (600ms - 1200ms)
//        handler.postDelayed({
//            val brandGame = findViewById<ImageView>(R.id.splashBrandGame)
//            brandGame.alpha = 0f
//            brandGame.animate().alpha(1f).setDuration(150).withEndAction {
//                brandGame.postDelayed({
//                    brandGame.animate().alpha(0f).setDuration(150).start()
//                }, 300L)
//            }.start()
//        }, 600L)

        // Card animation starts at 1200ms — schedule the existing shuffle sound + Phase 1
        handler.postDelayed({
            try {
                mediaPlayer = MediaPlayer.create(this, R.raw.shuffle)
                mediaPlayer?.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val card1 = findViewById<ImageView>(R.id.splashCard1)
            val card2 = findViewById<ImageView>(R.id.splashCard2)
            val card3 = findViewById<ImageView>(R.id.splashCard3)
            val card4 = findViewById<ImageView>(R.id.splashCard4)

            // Phase 1 (1200ms - 1800ms): stacked cards fade in with slight rotation
            val stackCards = listOf(card1, card2, card3, card4)
            stackCards.forEachIndexed { idx, card ->
                card.alpha = 0f
                card.translationY = -(idx * dp(2f))
                card.rotation = (idx - 1.5f) * 2f
                card.animate()
                    .alpha(1f)
                    .setDuration(400)
                    .setStartDelay(idx * 50L)
                    .start()
            }

            // Phase 2 (1800ms - 2600ms): shuffle in air
            handler.postDelayed({
                shuffleCard(card1, dp(-180f), dp(-120f), -40f)
                shuffleCard(card2, dp(90f), dp(100f), 25f)
                shuffleCard(card3, dp(-60f), dp(60f), -15f)
                shuffleCard(card4, dp(200f), dp(-80f), 35f)
            }, 600L)

            // Phase 3 (2600ms - 3200ms): fan deal-out + title reveal
            handler.postDelayed({
                dealCard(card1, dp(-240f), dp(100f), -30f)
                dealCard(card2, dp(-80f), dp(100f), -10f)
                dealCard(card3, dp(80f), dp(100f), 10f)
                dealCard(card4, dp(240f), dp(100f), 30f)

                val title = findViewById<TextView>(R.id.splashTitle)
                title.alpha = 0f
                title.translationY = -dp(40f)
                title.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(600)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }, 1400L)
        }, 1200L)
    }

    private fun shuffleCard(card: ImageView, dx: Float, dy: Float, rot: Float) {
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(card, "translationX", 0f, dx, 0f),
                ObjectAnimator.ofFloat(card, "translationY", card.translationY, dy, 0f),
                ObjectAnimator.ofFloat(card, "rotation", card.rotation, rot, 0f)
            )
            duration = 800
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun dealCard(card: ImageView, finalX: Float, finalY: Float, finalRot: Float) {
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(card, "translationX", card.translationX, finalX),
                ObjectAnimator.ofFloat(card, "translationY", card.translationY, finalY),
                ObjectAnimator.ofFloat(card, "rotation", card.rotation, finalRot)
            )
            duration = 600
            interpolator = AccelerateDecelerateInterpolator()
            start()
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
        mediaPlayer?.release()
        mediaPlayer = null
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}

package com.bottazzini.trasloco

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.Window
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

        handler.postDelayed({ skipEnabled = true }, skipEnableDelayMs)
        handler.postDelayed({ navigateToMain() }, totalDurationMs)
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

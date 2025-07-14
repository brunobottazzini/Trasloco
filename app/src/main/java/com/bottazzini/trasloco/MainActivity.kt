package com.bottazzini.trasloco

import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.bottazzini.trasloco.settings.Configuration
import com.bottazzini.trasloco.settings.RecordsHandler
import com.bottazzini.trasloco.settings.SettingsHandler
import com.bottazzini.trasloco.utils.DeckSetup
import com.bottazzini.trasloco.utils.ResourceUtils

class MainActivity : AppCompatActivity() {

    private lateinit var settingsHandler: SettingsHandler
    private lateinit var recordsHandler: RecordsHandler
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener {
            Thread.sleep(700)
            it.remove()
        }
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        settingsHandler = SettingsHandler(applicationContext)
        settingsHandler.insertDefaultSettings()
        recordsHandler = RecordsHandler(applicationContext)
        recordsHandler.insertDefaultSettings()
    }

    fun startGame(view: View) {
        val intent = Intent(this, GameActivity::class.java)
        startActivity(intent)
    }

    fun showRecords(view: View) {
        playSound(R.raw.change_activity)
        val intent = Intent(this, RecordActivity::class.java)
        startActivity(intent)
    }

    fun showRules(view: View) {
        playSound(R.raw.change_activity)
        val intent = Intent(this, RulesActivity::class.java)
        startActivity(intent)
    }

    fun openSettings(view: View) {
        playSound(R.raw.change_activity)
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        playSound(R.raw.change_activity)
        super.onResume()
    }

    override fun onDestroy() {
        settingsHandler.close()
        super.onDestroy()
    }

    private fun playSound(soundId: Int) {
        try {
            if (mediaPlayer?.isPlaying == true) {
                return
            }
            mediaPlayer?.release()
            mediaPlayer = null

            mediaPlayer = MediaPlayer.create(this, soundId)
            mediaPlayer?.setOnCompletionListener {
                mediaPlayer?.release()
                mediaPlayer = null
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
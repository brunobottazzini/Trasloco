package com.bottazzini.trasloco

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.Toast
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
import com.bottazzini.trasloco.utils.WindowInsetsUtils

class MainActivity : AppCompatActivity() {

    private lateinit var settingsHandler: SettingsHandler
    private lateinit var recordsHandler: RecordsHandler
    private var mediaPlayer: MediaPlayer? = null

    private var tapCount = 0
    private var lastTapTime: Long = 0
    private var tripleTapTimeout: Long = 1000 //ms

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener {
            Thread.sleep(700)
            it.remove()
        }
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)
        WindowInsetsUtils.applySystemBarInsets(window, findViewById(R.id.mainScrollView))
        supportActionBar?.hide()
        settingsHandler = SettingsHandler(applicationContext)
        settingsHandler.insertDefaultSettings()
        recordsHandler = RecordsHandler(applicationContext)
        recordsHandler.insertDefaultSettings()

        val mainImage: ImageView = findViewById(R.id.c4)

        mainImage.setOnClickListener {
            handleTripleTap()
        }
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

    private fun handleTripleTap() {
        val currentTime = SystemClock.uptimeMillis()

        if (tapCount > 0 && (currentTime - lastTapTime > tripleTapTimeout)) {
            // Timeout, reset count
            tapCount = 0
        }

        tapCount++
        lastTapTime = currentTime

        if (tapCount == 3) {
            // Triple tap detected
            showAppVersionToast()
            tapCount = 0 // Reset for next triple tap
        }
    }

    private fun showAppVersionToast() {
        try {
            val packageInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName: String
            if (packageInfo.versionName != null) {
                versionName = packageInfo.versionName.toString()
            } else {
                versionName = "N/A"
            }
            val versionCode: Int = packageInfo.versionCode // Or use Long for modern AGP: Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? packageInfo.longVersionCode : packageInfo.versionCode

            val versionText = "Version: $versionName (Code: $versionCode)"
            Toast.makeText(this, versionText, Toast.LENGTH_LONG).show()
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            Toast.makeText(this, "Could not get app version", Toast.LENGTH_SHORT).show()
        }
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
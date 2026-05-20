package com.bottazzini.trasloco

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bottazzini.trasloco.settings.Configuration
import com.bottazzini.trasloco.settings.RecordsHandler
import com.bottazzini.trasloco.settings.Type
import com.bottazzini.trasloco.settings.SettingsHandler
import com.bottazzini.trasloco.utils.ResourceUtils
import com.bottazzini.trasloco.utils.ThemeUtils
import com.bottazzini.trasloco.utils.WindowInsetsUtils

class MainActivity : AppCompatActivity() {

    private lateinit var settingsHandler: SettingsHandler
    private lateinit var recordsHandler: RecordsHandler
    private lateinit var gameStateRepo: com.bottazzini.trasloco.settings.GameStateRepository
    private var mediaPlayer: MediaPlayer? = null

    private var tapCount = 0
    private var lastTapTime: Long = 0
    private var tripleTapTimeout: Long = 1000 //ms

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Write defaults before any screen reads them (idempotent — safe to call early)
        SettingsHandler(applicationContext).insertDefaultSettings()

        // One-time deck picker gate — runs before layout inflation
        val prefs = getSharedPreferences("trasloco_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("tutorial_seen", false) && !prefs.getBoolean("deck_chosen", false)) {
            prefs.edit().putBoolean("deck_chosen", true).apply()
        }
        if (!prefs.getBoolean("deck_chosen", false)) {
            startActivity(Intent(this, DeckPickerActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)
        WindowInsetsUtils.applySystemBarInsets(window, findViewById(R.id.mainScrollView))
        supportActionBar?.hide()

        settingsHandler = SettingsHandler(applicationContext)
        settingsHandler.insertDefaultSettings()
        settingsHandler.migrateRemovedBackgrounds()

        gameStateRepo = com.bottazzini.trasloco.settings.GameStateRepository(applicationContext)
        recordsHandler = RecordsHandler(applicationContext)
        recordsHandler.insertDefaultSettings()

        val bannerRoot = findViewById<View>(R.id.mainBannerAchievement)
        val achievementBanner = com.bottazzini.trasloco.utils.AchievementBanner(this, bannerRoot)
        val newAchievements = com.bottazzini.trasloco.utils.AchievementEngine.create(applicationContext)
            .evaluate(com.bottazzini.trasloco.utils.AchievementTrigger.APP_OPENED)
        bannerRoot.postDelayed({ achievementBanner.enqueue(newAchievements) }, 500L)

        findViewById<View>(R.id.textViewTitle).setOnClickListener {
            handleTripleTap()
        }
    }

    fun startGame(view: View) {
        if (gameStateRepo.hasSavedGame()) {
            showAbandonGameDialog()
        } else if (!isTutorialSeen()) {
            showTutorialPromptDialog()
        } else {
            launchGameActivity(tutorial = false)
        }
    }

    private fun showAbandonGameDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.abandon_game_title)
            .setMessage(R.string.abandon_game_message)
            .setCancelable(true)
            .setNegativeButton(R.string.abandon_game_confirm) { _, _ ->
                val consecutive = recordsHandler.readValue(Type.CONSECUTIVE)
                if (consecutive != null) {
                    recordsHandler.update(Type.CONSECUTIVE, consecutive, 0L, false)
                }
                if (!isTutorialSeen()) {
                    showTutorialPromptDialog()
                } else {
                    launchGameActivity(tutorial = false)
                }
            }
            .setPositiveButton(R.string.abandon_game_resume) { _, _ ->
                resumeGame(null)
            }
            .show()
    }

    fun showTutorial(view: View) {
        playSound(R.raw.change_activity)
        markTutorialSeen()
        launchGameActivity(tutorial = true)
    }

    private fun isTutorialSeen(): Boolean {
        val prefs = getSharedPreferences("trasloco_prefs", MODE_PRIVATE)
        return prefs.getBoolean("tutorial_seen", false)
    }

    private fun markTutorialSeen() {
        val prefs = getSharedPreferences("trasloco_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("tutorial_seen", true).apply()
    }

    private fun showTutorialPromptDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.tutorial_prompt_title)
            .setMessage(R.string.tutorial_prompt_message)
            .setCancelable(false)
            .setPositiveButton(R.string.tutorial_prompt_yes) { _, _ ->
                markTutorialSeen()
                launchGameActivity(tutorial = true)
            }
            .setNegativeButton(R.string.tutorial_prompt_no) { _, _ ->
                markTutorialSeen()
                launchGameActivity(tutorial = false)
            }
            .show()
    }

    private fun launchGameActivity(tutorial: Boolean) {
        val intent = Intent(this, GameActivity::class.java)
        if (tutorial) {
            intent.putExtra(GameActivity.EXTRA_TUTORIAL_MODE, true)
        }
        startActivity(intent)
    }

    fun showRecords(view: View) {
        playSound(R.raw.change_activity)
        val intent = Intent(this, StatsActivity::class.java)
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
        updateRiprendiTile()
        val bg = settingsHandler.readValue(Configuration.BACKGROUND.value) ?: "bordeaux"
        val bgDrawable = ResourceUtils.getDrawableByName(resources, packageName, bg)
        findViewById<View>(R.id.mainScrollView).background = ContextCompat.getDrawable(this, bgDrawable)
        applyMenuTextColor(bg)
    }

    private fun applyMenuTextColor(bg: String) {
        val color = ThemeUtils.accentColor(bg, this)
        val dimColor = ThemeUtils.accentColorDim(bg, this)
        findViewById<TextView>(R.id.textViewTitle).setTextColor(color)
        findViewById<TextView>(R.id.textViewSubtitle).setTextColor(dimColor)
        findViewById<TextView>(R.id.buttonTutorial).setTextColor(color)
        listOf(R.id.tileNuovaPartita, R.id.tileRiprendi, R.id.tileRecords, R.id.tileSettings)
            .forEach { id ->
                val tile = findViewById<LinearLayout>(id)
                for (i in 0 until tile.childCount) {
                    (tile.getChildAt(i) as? TextView)?.setTextColor(color)
                }
            }
    }

    override fun onDestroy() {
        if (::gameStateRepo.isInitialized) {
            gameStateRepo.close()
        }
        if (::settingsHandler.isInitialized) {
            settingsHandler.close()
        }
        super.onDestroy()
    }

    private fun updateRiprendiTile() {
        val tile = findViewById<android.view.View>(R.id.tileRiprendi)
        if (gameStateRepo.hasSavedGame()) {
            tile.alpha = 1.0f
            tile.isClickable = true
        } else {
            tile.alpha = 0.4f
            tile.isClickable = false
        }
    }

    fun resumeGame(view: View?) {
        playSound(R.raw.change_activity)
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("resume", true)
        startActivity(intent)
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
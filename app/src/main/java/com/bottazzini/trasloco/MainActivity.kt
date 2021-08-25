package com.bottazzini.trasloco

import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.bottazzini.trasloco.settings.Configuration
import com.bottazzini.trasloco.settings.SettingsHandler

class MainActivity : AppCompatActivity() {

    private lateinit var settingsHandler: SettingsHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        settingsHandler = SettingsHandler(applicationContext)
        settingsHandler.insertDefaultSettings()
        changeBackground()

    }

    override fun onResume() {
        super.onResume()
        changeBackground()
    }

    private fun changeBackground() {
        val backgroundConf = settingsHandler.readValue(Configuration.BACKGROUND.value)
        val drawable = resources.getIdentifier("drawable/$backgroundConf", "id", this.packageName)
        val layout = findViewById<ConstraintLayout>(R.id.mainConstraintLayout)
        layout.background = ContextCompat.getDrawable(this, drawable)
    }

    fun startGame(view: View) {
        val intent = Intent(this, GameActivity::class.java)
        startActivity(intent)
    }

    fun openSettings(view: View) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroy() {
        settingsHandler.close()
        super.onDestroy()
    }
}
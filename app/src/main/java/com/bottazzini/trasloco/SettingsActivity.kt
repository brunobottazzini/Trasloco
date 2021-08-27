package com.bottazzini.trasloco

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.bottazzini.trasloco.settings.Configuration
import com.bottazzini.trasloco.settings.SettingsHandler
import com.bottazzini.trasloco.utils.ResourceUtils

class SettingsActivity : AppCompatActivity() {

    private lateinit var settingsHandler: SettingsHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.settings)
        supportActionBar?.hide()

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        settingsHandler = SettingsHandler(applicationContext)
        readConfigurations()
    }

    fun changeFastDeal(view: View) {
        val switch = findViewById<Switch>(R.id.switchFastDeal)
        val value = if (switch.isChecked) "enabled" else "disabled"
        settingsHandler.updateSetting(Configuration.FAST_DEAL.value, value)
    }

    fun setBackCard(view: View) {
        val radioButton = findViewById<RadioButton>(view.id)
        val imageName = radioButton.tag as String
        settingsHandler.updateSetting(Configuration.CARD_BACK.value, imageName)
    }

    fun setBackGround(view: View) {
        val radioButton = findViewById<RadioButton>(view.id)
        val imageName = radioButton.tag as String
        settingsHandler.updateSetting(Configuration.BACKGROUND.value, imageName)
        changeBackGround()
    }

    override fun onDestroy() {
        settingsHandler.close()
        super.onDestroy()
    }

    private fun changeBackGround() {
        val backgroundConf = settingsHandler.readValue(Configuration.BACKGROUND.value)!!
        val drawable = ResourceUtils.getDrawableByName(resources, this.packageName, backgroundConf)
        val layout = findViewById<ConstraintLayout>(R.id.settingsConstraintLayout)
        layout.background = ContextCompat.getDrawable(this, drawable)
    }

    private fun readConfigurations() {
        val readValue = settingsHandler.readValue(Configuration.FAST_DEAL.value)!!
        val switch = findViewById<Switch>(R.id.switchFastDeal)
        switch.isChecked = readValue == "enabled"

        val cardBackValue = settingsHandler.readValue(Configuration.CARD_BACK.value)!!
        findRadioButtonFromTag(cardBackValue).isChecked = true

        val backgroundConf = settingsHandler.readValue(Configuration.BACKGROUND.value)!!
        findRadioButtonFromTag(backgroundConf).isChecked = true

        changeBackGround()
    }

    private fun findRadioButtonFromTag(tag: String): RadioButton {
        val layout = findViewById<ConstraintLayout>(R.id.settingsConstraintLayout)
        return layout.findViewWithTag(tag)
    }

}
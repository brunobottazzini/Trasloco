package com.bottazzini.trasloco

import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bottazzini.trasloco.settings.Configuration
import com.bottazzini.trasloco.settings.SettingsHandler
import com.bottazzini.trasloco.utils.WindowInsetsUtils

class SettingsActivity : AppCompatActivity() {

    private lateinit var settingsHandler: SettingsHandler

    private val cardTypeTileIds = listOf(R.id.cardTypePiacentine, R.id.cardTypeNapoletane, R.id.cardTypeFrancesi)
    private val cardBackTileIds = listOf(R.id.cardBackBg1, R.id.cardBackBg2, R.id.cardBackBg3)
    private lateinit var backgroundTileIds: List<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.settings)
        WindowInsetsUtils.applySystemBarInsets(window, findViewById(R.id.settingsScrollView))
        supportActionBar?.hide()

        settingsHandler = SettingsHandler(applicationContext)

        // Discover all FrameLayouts in backgroundRow (they have tags)
        val bgRow = findViewById<ViewGroup>(R.id.backgroundRow)
        val ids = mutableListOf<Int>()
        for (i in 0 until bgRow.childCount) {
            val child = bgRow.getChildAt(i)
            if (child.id != View.NO_ID) ids.add(child.id)
        }
        backgroundTileIds = ids

        readConfigurations()
    }

    fun selectCardType(view: View) {
        val tag = view.tag?.toString() ?: return
        settingsHandler.updateSetting(Configuration.CARD_TYPE.value, tag)
        updateSelection(cardTypeTileIds, tag)
        updateHeroPreview()
    }

    fun selectCardBack(view: View) {
        val tag = view.tag?.toString() ?: return
        settingsHandler.updateSetting(Configuration.CARD_BACK.value, tag)
        updateSelection(cardBackTileIds, tag)
        updateHeroPreview()
    }

    fun selectBackground(view: View) {
        val tag = view.tag?.toString() ?: return
        settingsHandler.updateSetting(Configuration.BACKGROUND.value, tag)
        updateSelection(backgroundTileIds, tag)
        applyScreenBackground(tag)
        updateHeroPreview()
    }

    fun changeFastDeal(view: View) {
        val switch = findViewById<Switch>(R.id.switchFastDeal)
        val value = if (switch.isChecked) "enabled" else "disabled"
        settingsHandler.updateSetting(Configuration.FAST_DEAL.value, value)
    }

    fun changeHintEnabled(view: View) {
        val switch = view as Switch
        val value = if (switch.isChecked) "enabled" else "disabled"
        settingsHandler.updateSetting(Configuration.HINT_ENABLED.value, value)
    }

    fun changeAutoMove(view: View) {
        val switch = view as Switch
        val value = if (switch.isChecked) "enabled" else "disabled"
        settingsHandler.updateSetting(Configuration.AUTO_MOVE.value, value)
    }

    override fun onDestroy() {
        settingsHandler.close()
        super.onDestroy()
    }

    private fun updateSelection(tileIds: List<Int>, selectedTag: String) {
        tileIds.forEach { id ->
            val view = findViewById<View>(id)
            view.isSelected = (view.tag?.toString() == selectedTag)
        }
    }

    private fun applyScreenBackground(backgroundTag: String) {
        val drawableId = resources.getIdentifier(backgroundTag, "drawable", packageName)
        val root = findViewById<View>(R.id.settingsScrollView)
        if (drawableId != 0) {
            root.background = ContextCompat.getDrawable(this, drawableId)
        }
    }

    private fun updateHeroPreview() {
        val backgroundTag = settingsHandler.readValue(Configuration.BACKGROUND.value) ?: "verde"
        val cardTypeTag = settingsHandler.readValue(Configuration.CARD_TYPE.value) ?: "piacentine"
        val cardBackTag = settingsHandler.readValue(Configuration.CARD_BACK.value) ?: "bg2"

        // Update hero background
        val heroBg = findViewById<ImageView>(R.id.heroBackgroundImage)
        val bgDrawableId = resources.getIdentifier(backgroundTag, "drawable", packageName)
        if (bgDrawableId != 0) {
            heroBg.setImageDrawable(ContextCompat.getDrawable(this, bgDrawableId))
        }

        // Update 3 cards (use cards b1, c1, d1 of card type) + 1 card back
        val cardIds = listOf(R.id.heroCard1, R.id.heroCard2, R.id.heroCard3)
        val sampleCards = listOf("${cardTypeTag}_b1", "${cardTypeTag}_c1", "${cardTypeTag}_d1")
        cardIds.forEachIndexed { idx, viewId ->
            val img = findViewById<ImageView>(viewId)
            val drawableId = resources.getIdentifier(sampleCards[idx], "drawable", packageName)
            if (drawableId != 0) {
                img.setImageDrawable(ContextCompat.getDrawable(this, drawableId))
            } else {
                img.setImageDrawable(null)
            }
        }

        // Update card back
        val backImg = findViewById<ImageView>(R.id.heroCardBack)
        val backDrawableId = resources.getIdentifier(cardBackTag, "drawable", packageName)
        if (backDrawableId != 0) {
            backImg.setImageDrawable(ContextCompat.getDrawable(this, backDrawableId))
        }
    }

    private fun readConfigurations() {
        val fastDeal = settingsHandler.readValue(Configuration.FAST_DEAL.value) ?: "disabled"
        findViewById<Switch>(R.id.switchFastDeal).isChecked = (fastDeal == "enabled")

        val cardType = settingsHandler.readValue(Configuration.CARD_TYPE.value) ?: "piacentine"
        updateSelection(cardTypeTileIds, cardType)

        val cardBack = settingsHandler.readValue(Configuration.CARD_BACK.value) ?: "bg2"
        updateSelection(cardBackTileIds, cardBack)

        val background = settingsHandler.readValue(Configuration.BACKGROUND.value) ?: "verde"
        updateSelection(backgroundTileIds, background)

        val hint = settingsHandler.readValue(Configuration.HINT_ENABLED.value) ?: "enabled"
        findViewById<Switch>(R.id.switchHint).isChecked = (hint == "enabled")

        val autoMove = settingsHandler.readValue(Configuration.AUTO_MOVE.value) ?: "disabled"
        findViewById<Switch>(R.id.switchAutoMove).isChecked = (autoMove == "enabled")

        applyScreenBackground(background)
        updateHeroPreview()
    }
}

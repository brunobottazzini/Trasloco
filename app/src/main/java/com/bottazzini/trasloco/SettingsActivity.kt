package com.bottazzini.trasloco

import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bottazzini.trasloco.settings.Configuration
import com.bottazzini.trasloco.settings.SettingsHandler
import com.bottazzini.trasloco.utils.CardDeckRegistry
import com.bottazzini.trasloco.utils.DeckGridAdapter
import com.bottazzini.trasloco.utils.DeckRegion
import com.bottazzini.trasloco.utils.ThemeUtils
import com.bottazzini.trasloco.utils.WindowInsetsUtils
import com.google.android.material.tabs.TabLayout

class SettingsActivity : AppCompatActivity() {

    private lateinit var settingsHandler: SettingsHandler

    private val cardBackTileIds = listOf(R.id.cardBackBg1, R.id.cardBackBg2, R.id.cardBackBg3)
    private lateinit var backgroundTileIds: List<Int>

    private lateinit var deckTabLayout: TabLayout
    private lateinit var deckRecycler: RecyclerView
    private lateinit var deckSelectedName: TextView
    private val deckAdapterByRegion = mutableMapOf<DeckRegion, DeckGridAdapter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.settings)
        WindowInsetsUtils.applySystemBarInsets(window, findViewById(R.id.settingsScrollView))
        supportActionBar?.hide()

        settingsHandler = SettingsHandler(applicationContext)

        val bgRow = findViewById<ViewGroup>(R.id.backgroundRow)
        val ids = mutableListOf<Int>()
        for (i in 0 until bgRow.childCount) {
            val child = bgRow.getChildAt(i)
            if (child.id != View.NO_ID) ids.add(child.id)
        }
        backgroundTileIds = ids

        deckTabLayout = findViewById(R.id.settingsDeckTabLayout)
        deckRecycler = findViewById(R.id.settingsDeckGrid)
        deckSelectedName = findViewById(R.id.settingsDeckSelectedName)

        deckRecycler.layoutManager = GridLayoutManager(this, 3)

        setupDeckAdapters()
        setupDeckTabs()

        readConfigurations()
    }

    private fun setupDeckAdapters() {
        DeckRegion.values().forEach { region ->
            deckAdapterByRegion[region] = DeckGridAdapter(CardDeckRegistry.byRegion(region)) { deck ->
                if (!deck.available) return@DeckGridAdapter
                settingsHandler.updateSetting(Configuration.CARD_TYPE.value, deck.id)
                deckSelectedName.text = getString(deck.labelRes)
                deckAdapterByRegion.values.forEach { it.setSelectedId(deck.id) }
                updateHeroPreview()
            }
        }
    }

    private fun setupDeckTabs() {
        deckTabLayout.addTab(deckTabLayout.newTab().setText(R.string.region_nord))
        deckTabLayout.addTab(deckTabLayout.newTab().setText(R.string.region_sud_isole))
        deckTabLayout.addTab(deckTabLayout.newTab().setText(R.string.region_internazionali))

        deckTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                showDeckRegion(DeckRegion.values()[tab.position])
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun showDeckRegion(region: DeckRegion) {
        deckRecycler.adapter = deckAdapterByRegion[region]
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

    fun changeSoundEnabled(view: View) {
        val switch = view as Switch
        val value = if (switch.isChecked) "enabled" else "disabled"
        settingsHandler.updateSetting(Configuration.SOUND_ENABLED.value, value)
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
        applyAccentColor(backgroundTag)
    }

    private fun applyAccentColor(bg: String) {
        val color = ThemeUtils.accentColor(bg, this)
        val dimColor = ThemeUtils.accentColorDim(bg, this)
        listOf(R.id.textViewTitle, R.id.labelCardDeck, R.id.labelCardBack, R.id.labelBackground)
            .forEach { findViewById<TextView>(it).setTextColor(color) }
        listOf(R.id.heroPreviewLabel, R.id.textViewCredits)
            .forEach { findViewById<TextView>(it).setTextColor(dimColor) }
    }

    private fun updateHeroPreview() {
        val backgroundTag = settingsHandler.readValue(Configuration.BACKGROUND.value) ?: "bordeaux"
        val cardTypeTag = settingsHandler.readValue(Configuration.CARD_TYPE.value) ?: "piacentine"
        val cardBackTag = settingsHandler.readValue(Configuration.CARD_BACK.value) ?: "bg2"

        val heroBg = findViewById<ImageView>(R.id.heroBackgroundImage)
        val bgDrawableId = resources.getIdentifier(backgroundTag, "drawable", packageName)
        if (bgDrawableId != 0) {
            heroBg.setImageDrawable(ContextCompat.getDrawable(this, bgDrawableId))
        }

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

        val backImg = findViewById<ImageView>(R.id.heroCardBack)
        val backDrawableId = resources.getIdentifier(cardBackTag, "drawable", packageName)
        if (backDrawableId != 0) {
            backImg.setImageDrawable(ContextCompat.getDrawable(this, backDrawableId))
        }
        val deck = CardDeckRegistry.byId(cardTypeTag)
        backImg.post {
            val px = (backImg.width * deck.insetX / 2f).toInt()
            val py = (backImg.height * deck.insetY / 2f).toInt()
            backImg.setPadding(px, py, px, py)
        }
    }

    private fun readConfigurations() {
        val fastDeal = settingsHandler.readValue(Configuration.FAST_DEAL.value) ?: "disabled"
        findViewById<Switch>(R.id.switchFastDeal).isChecked = (fastDeal == "enabled")

        val cardType = settingsHandler.readValue(Configuration.CARD_TYPE.value) ?: "piacentine"
        val currentDeck = CardDeckRegistry.byId(cardType)
        deckSelectedName.text = getString(currentDeck.labelRes)
        deckAdapterByRegion.values.forEach { it.setSelectedId(cardType) }
        val tabIndex = DeckRegion.values().indexOf(currentDeck.region)
        deckTabLayout.getTabAt(tabIndex)?.select()
        showDeckRegion(currentDeck.region)

        val cardBack = settingsHandler.readValue(Configuration.CARD_BACK.value) ?: "bg2"
        updateSelection(cardBackTileIds, cardBack)

        val background = settingsHandler.readValue(Configuration.BACKGROUND.value) ?: "bordeaux"
        updateSelection(backgroundTileIds, background)

        val hint = settingsHandler.readValue(Configuration.HINT_ENABLED.value) ?: "enabled"
        findViewById<Switch>(R.id.switchHint).isChecked = (hint == "enabled")

        val autoMove = settingsHandler.readValue(Configuration.AUTO_MOVE.value) ?: "disabled"
        findViewById<Switch>(R.id.switchAutoMove).isChecked = (autoMove == "enabled")

        val sound = settingsHandler.readValue(Configuration.SOUND_ENABLED.value) ?: "enabled"
        findViewById<Switch>(R.id.switchSound).isChecked = (sound == "enabled")

        applyScreenBackground(background)
        updateHeroPreview()
    }
}

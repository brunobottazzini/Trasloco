package com.bottazzini.trasloco

import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.bottazzini.trasloco.settings.Configuration
import com.bottazzini.trasloco.settings.SettingsHandler
import com.bottazzini.trasloco.utils.CardDeckRegistry
import com.bottazzini.trasloco.utils.DeckCarouselAdapter
import com.bottazzini.trasloco.utils.WindowInsetsUtils
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class DeckPickerActivity : AppCompatActivity() {

    private lateinit var settingsHandler: SettingsHandler
    private lateinit var viewPager: ViewPager2
    private lateinit var confirmButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.hide()
        setContentView(R.layout.activity_deck_picker)
        WindowInsetsUtils.applySystemBarInsets(window, findViewById(R.id.deckPickerScrollView))

        settingsHandler = SettingsHandler(applicationContext)

        viewPager = findViewById(R.id.viewPagerDecks)
        confirmButton = findViewById(R.id.buttonDeckConfirm)

        val adapter = DeckCarouselAdapter(CardDeckRegistry.ALL)
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 1

        val dots = findViewById<TabLayout>(R.id.deckDotsIndicator)
        TabLayoutMediator(dots, viewPager) { _, _ -> }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateConfirmButton(position)
            }
        })

        val initialPage = savedInstanceState?.getInt("selected_page", 0) ?: 0
        viewPager.setCurrentItem(initialPage, false)
        updateConfirmButton(initialPage)

        confirmButton.setOnClickListener { confirmSelection() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("selected_page", viewPager.currentItem)
    }

    private fun updateConfirmButton(position: Int) {
        val deck = CardDeckRegistry.ALL.getOrNull(position)
        val enabled = deck?.available == true
        confirmButton.isEnabled = enabled
        confirmButton.alpha = if (enabled) 1f else 0.4f
    }

    private fun confirmSelection() {
        val deck = CardDeckRegistry.ALL.getOrNull(viewPager.currentItem) ?: return
        if (!deck.available) return
        settingsHandler.updateSetting(Configuration.CARD_TYPE.value, deck.id)
        getSharedPreferences("trasloco_prefs", MODE_PRIVATE)
            .edit().putBoolean("deck_chosen", true).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

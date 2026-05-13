package com.bottazzini.trasloco

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bottazzini.trasloco.settings.Configuration
import com.bottazzini.trasloco.settings.SettingsHandler
import com.bottazzini.trasloco.utils.WindowInsetsUtils

class DeckPickerActivity : AppCompatActivity() {

    private lateinit var settingsHandler: SettingsHandler
    private var selectedTag: String? = null

    private lateinit var tiles: List<LinearLayout>
    private lateinit var confirmButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.hide()
        setContentView(R.layout.activity_deck_picker)
        WindowInsetsUtils.applySystemBarInsets(window, findViewById(R.id.deckPickerScrollView))

        settingsHandler = SettingsHandler(applicationContext)

        tiles = listOf(
            findViewById(R.id.tilePiacentine),
            findViewById(R.id.tileNapoletane),
            findViewById(R.id.tileFrancesi)
        )
        confirmButton = findViewById(R.id.buttonDeckConfirm)

        tiles.forEach { tile ->
            tile.setOnClickListener { onTileSelected(tile) }
        }

        confirmButton.setOnClickListener { confirmSelection() }
    }

    private fun onTileSelected(selected: LinearLayout) {
        tiles.forEach { tile ->
            tile.background = ContextCompat.getDrawable(this, R.drawable.casino_tile_bg)
        }
        selected.background = ContextCompat.getDrawable(this, R.drawable.casino_tile_bg_primary)
        selectedTag = selected.tag as String
        confirmButton.isEnabled = true
        confirmButton.alpha = 1f
    }

    private fun confirmSelection() {
        val tag = selectedTag ?: return
        settingsHandler.updateSetting(Configuration.CARD_TYPE.value, tag)
        getSharedPreferences("trasloco_prefs", MODE_PRIVATE)
            .edit().putBoolean("deck_chosen", true).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

package com.bottazzini.trasloco

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bottazzini.trasloco.settings.Configuration
import com.bottazzini.trasloco.settings.SettingsHandler
import com.bottazzini.trasloco.utils.PartyGifs.Companion.partyGifUrls
import com.bottazzini.trasloco.utils.ResourceUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.util.Random

class YouWonActivity : AppCompatActivity() {

    private lateinit var imageViewPartyGif: ImageView
    private lateinit var buttonNewGame: Button
    private lateinit var buttonMenu: Button
    private lateinit var buttonExit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        setContentView(R.layout.activity_you_won)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        imageViewPartyGif = findViewById(R.id.imageViewPartyGif)
        buttonNewGame = findViewById(R.id.buttonNewGameYouWon)
        buttonMenu = findViewById(R.id.buttonMenuYouWon)
        buttonExit = findViewById(R.id.buttonExitYouWon)


        loadRandomPartyGifFromUrl()

        buttonNewGame.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java) // Or directly GameActivity
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        buttonMenu.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java) // Or directly GameActivity
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        buttonExit.setOnClickListener {
            ActivityCompat.finishAffinity(this)
        }

        val settingsHandler = SettingsHandler(applicationContext)
        val backgroundConf = settingsHandler.readValue(Configuration.BACKGROUND.value)
        val drawable = ResourceUtils.getDrawableByName(resources, this.packageName, backgroundConf!!)
        val layout = findViewById<ConstraintLayout>(R.id.gameConstraintLayout)
        layout.background = ContextCompat.getDrawable(this, drawable)
    }

    private fun loadRandomPartyGifFromUrl() {
        if (partyGifUrls.isNotEmpty()) {
            val randomGifUrl = partyGifUrls[Random().nextInt(partyGifUrls.size)]
            Glide.with(this)
                .asGif()
                .load(randomGifUrl)
                .placeholder(R.drawable.loading) // Optional: placeholder while loading
                .error(R.drawable.you_won_no_internet) // Optional: image to show if loading fails
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC) // Optional: Caching strategy
                .into(imageViewPartyGif)
        }
    }

    override fun onBackPressed() {
        buttonNewGame.performClick() // Optionally force new game on back press
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false) // Crucial for edge-to-edge

        val controller = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars()) // Hides status AND navigation bars
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
package com.bottazzini.trasloco

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bottazzini.trasloco.settings.Configuration
import com.bottazzini.trasloco.settings.RecordsHandler
import com.bottazzini.trasloco.settings.SettingsHandler
import com.bottazzini.trasloco.settings.Type
import com.bottazzini.trasloco.utils.PartyGifs.Companion.partyGifUrls
import com.bottazzini.trasloco.utils.ResourceUtils
import com.bottazzini.trasloco.utils.TimeUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.util.Random

class YouWonActivity : AppCompatActivity() {

    private lateinit var recordsHandler: RecordsHandler
    private lateinit var imageViewPartyGif: ImageView
    private lateinit var buttonNewGame: Button
    private lateinit var buttonMenu: Button
    private lateinit var buttonExit: Button
    private lateinit var textViewGameTimeTaken: TextView
    private lateinit var victoryInARow: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        setContentView(R.layout.activity_you_won)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        imageViewPartyGif = findViewById(R.id.imageViewPartyGif)
        buttonNewGame = findViewById(R.id.buttonNewGameYouWon)
        buttonMenu = findViewById(R.id.buttonMenuYouWon)
        buttonExit = findViewById(R.id.buttonExitYouWon)

        recordsHandler = RecordsHandler(applicationContext)
        val millisPassed = recordsHandler.readValue(Type.TIME)
        val currentMillis = recordsHandler.readCurrentValue(Type.TIME)
        val isNewTime = recordsHandler.readNew(Type.TIME)
        textViewGameTimeTaken = findViewById(R.id.textViewTimeTaken)
        victoryInARow = findViewById(R.id.textConcurrentWin)

        if (millisPassed != null && currentMillis != null) {
            var text: CharSequence?
            text = getString(R.string.time_taken, TimeUtils.formatTime(currentMillis), TimeUtils.formatTime(millisPassed))
            if (isNewTime != null && isNewTime == true) {
                text = text + "\n" + getString(R.string.new_record)
            }
            textViewGameTimeTaken.text = text
            recordsHandler.update(Type.TIME, millisPassed, 0L, false)
        } else {
            textViewGameTimeTaken.text = ""
        }

        val victoryInARow = recordsHandler.readValue(Type.CONSECUTIVE)
        val currentConsecutive = recordsHandler.readCurrentValue(Type.CONSECUTIVE)
        if (victoryInARow != null) {
            var text: CharSequence?
            val isNewinARow = recordsHandler.readNew(Type.CONSECUTIVE)
             text = getString(R.string.victory_in_a_row, currentConsecutive.toString(), victoryInARow.toString())
            if (isNewinARow != null && isNewinARow == true) {
                text = text + "\n" + getString(R.string.new_record)
            }

            this.victoryInARow.text = text
        }

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
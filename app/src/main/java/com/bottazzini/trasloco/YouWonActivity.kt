package com.bottazzini.trasloco

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
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
    private lateinit var buttonNewGame: Button
    private var mediaPlayer: MediaPlayer? = null
    private val youWonViewModel: YouWonViewModel by lazy {
        ViewModelProvider(this).get(YouWonViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        setContentView(R.layout.activity_you_won)

        recordsHandler = RecordsHandler(applicationContext)
        buttonNewGame = findViewById(R.id.buttonNewGame)

        if (!youWonViewModel.statsRecorded) {
            recordsHandler.incrementTotalWins()
            youWonViewModel.statsRecorded = true
        }

        val currentTimeMillis = recordsHandler.readCurrentValue(Type.TIME) ?: 0L
        val bestTimeMillis = recordsHandler.getBestTime()
        val isNewRecord = recordsHandler.readNew(Type.TIME) ?: false
        val currentStreak = recordsHandler.readCurrentValue(Type.CONSECUTIVE) ?: 0L
        val totalWins = recordsHandler.getTotalWins()

        findViewById<TextView>(R.id.statTimeValue).text = TimeUtils.formatTime(currentTimeMillis)
        findViewById<TextView>(R.id.statBestValue).text =
            if (bestTimeMillis != null) TimeUtils.formatTime(bestTimeMillis) else "--:--"
        findViewById<TextView>(R.id.statStreakValue).text =
            getString(R.string.streak_format, currentStreak.toString())
        findViewById<TextView>(R.id.statTotalValue).text = totalWins.toString()

        findViewById<TextView>(R.id.newRecordBadge).visibility =
            if (isNewRecord) View.VISIBLE else View.GONE

        val settingsHandler = SettingsHandler(applicationContext)
        val backgroundConf = settingsHandler.readValue(Configuration.BACKGROUND.value) ?: "tappeto"
        val drawable = ResourceUtils.getDrawableByName(resources, this.packageName, backgroundConf)
        val rootView: View = findViewById(R.id.youWonScrollView)
        rootView.background = ContextCompat.getDrawable(this, drawable)

        loadRandomPartyGif()

        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.youwin)
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        buttonNewGame.setOnClickListener {
            startActivity(
                Intent(this, GameActivity::class.java)
                    .apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK }
            )
            finish()
        }

        findViewById<Button>(R.id.buttonMenu).setOnClickListener {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK }
            )
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                buttonNewGame.performClick()
            }
        })
    }

    private fun loadRandomPartyGif() {
        if (partyGifUrls.isEmpty()) return
        val gifUrl = youWonViewModel.gifUrl ?: run {
            val picked = partyGifUrls[Random().nextInt(partyGifUrls.size)]
            youWonViewModel.gifUrl = picked
            picked
        }
        Glide.with(this)
            .asGif()
            .load(gifUrl)
            .placeholder(R.drawable.loading)
            .error(R.drawable.you_won_no_internet)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(findViewById(R.id.partyGif))
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onPause() {
        super.onPause()
        releaseMediaPlayer()
    }

    override fun onDestroy() {
        releaseMediaPlayer()
        try {
            recordsHandler.close()
        } finally {
            super.onDestroy()
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

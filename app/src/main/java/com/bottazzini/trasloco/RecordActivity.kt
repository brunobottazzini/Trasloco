package com.bottazzini.trasloco // Assicurati che il package sia corretto

// import com.bottazzini.trasloco.R // Se non usi ViewBinding
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.bottazzini.trasloco.settings.Configuration
import com.bottazzini.trasloco.settings.RecordsHandler
import com.bottazzini.trasloco.settings.SettingsHandler
import com.bottazzini.trasloco.settings.Type
import com.bottazzini.trasloco.utils.ResourceUtils
import com.bottazzini.trasloco.utils.TimeUtils

class RecordActivity : AppCompatActivity() {

    private lateinit var recordsHandler: RecordsHandler
    private lateinit var settingsHandler: SettingsHandler

    private lateinit var textViewBestTimeValue: TextView
    private lateinit var textViewConsecutiveWinsValue: TextView
    private lateinit var textViewNoRecords: TextView
    private lateinit var textViewBestTimeLabel: TextView
    private lateinit var textViewConsecutiveWinsLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_record)
        supportActionBar?.hide()

        textViewBestTimeValue = findViewById(R.id.textViewBestTimeValue)
        textViewConsecutiveWinsValue = findViewById(R.id.textViewConsecutiveWinsValue)
        textViewNoRecords = findViewById(R.id.textViewNoRecords)
        textViewBestTimeLabel = findViewById(R.id.textViewBestTimeLabel)
        textViewConsecutiveWinsLabel = findViewById(R.id.textViewConsecutiveWinsLabel)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        recordsHandler = RecordsHandler(applicationContext)
        settingsHandler = SettingsHandler(applicationContext)
        changeBackGround(settingsHandler)

        loadRecords()
    }

    private fun loadRecords() {
        val bestTimeMillis = recordsHandler.readValue(Type.TIME)
        val consecutiveWins = recordsHandler.readValue(Type.CONSECUTIVE)

        var hasRecords = false

        if (bestTimeMillis != null && bestTimeMillis != -1L) {
            textViewBestTimeValue.text = TimeUtils.formatTime(bestTimeMillis)
            textViewBestTimeLabel.visibility = View.VISIBLE
            textViewBestTimeValue.visibility = View.VISIBLE
            hasRecords = true
        } else {
            textViewBestTimeLabel.visibility = View.GONE
            textViewBestTimeValue.visibility = View.GONE
        }

        if (consecutiveWins != null && consecutiveWins > 0) { // consecutiveWins > 0
            textViewConsecutiveWinsValue.text = consecutiveWins.toString()
            textViewConsecutiveWinsLabel.visibility = View.VISIBLE
            textViewConsecutiveWinsValue.visibility = View.VISIBLE
            hasRecords = true
        } else {
            textViewConsecutiveWinsLabel.visibility = View.GONE
            textViewConsecutiveWinsValue.visibility = View.GONE
        }

        if (hasRecords) {
            textViewNoRecords.visibility = View.GONE
        } else {
            textViewNoRecords.visibility = View.VISIBLE

            textViewBestTimeLabel.visibility = View.GONE
            textViewBestTimeValue.visibility = View.GONE
            textViewConsecutiveWinsLabel.visibility = View.GONE
            textViewConsecutiveWinsValue.visibility = View.GONE
        }
    }

    private fun changeBackGround(settingsHandler: SettingsHandler) {
        val backgroundConf = settingsHandler.readValue(Configuration.BACKGROUND.value)!!
        val drawable = ResourceUtils.getDrawableByName(resources, this.packageName, backgroundConf)
        val layout = findViewById<ConstraintLayout>(R.id.settingsConstraintLayout)
        layout.background = ContextCompat.getDrawable(this, drawable)
    }

    override fun onDestroy() {
        recordsHandler.close()
        super.onDestroy()
    }
}
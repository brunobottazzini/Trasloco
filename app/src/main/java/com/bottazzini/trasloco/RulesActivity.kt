package com.bottazzini.trasloco

import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.bottazzini.trasloco.settings.Configuration
import com.bottazzini.trasloco.settings.SettingsHandler
import com.bottazzini.trasloco.utils.ResourceUtils

class RulesActivity : AppCompatActivity() {

    private lateinit var linearLayoutRulesContainer: LinearLayout
    private lateinit var settingsHandler: SettingsHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_rules)
        supportActionBar?.hide()

        linearLayoutRulesContainer = findViewById(R.id.linearLayoutRulesContainer)
        val buttonGotIt: Button = findViewById(R.id.buttonGotIt)

        buttonGotIt.setOnClickListener {
            finish()
        }

        supportActionBar?.title = getString(R.string.title_activity_rules)

        settingsHandler = SettingsHandler(applicationContext)
        changeBackGround()

        loadAndDisplayRules()
    }

    private fun loadAndDisplayRules() {
        val rulesArray = resources.getStringArray(R.array.game_rules_array)

        for (ruleText in rulesArray) {
            val textViewRule = TextView(this)
            textViewRule.text = ruleText
            textViewRule.textSize = 18f
            textViewRule.setTextColor(resources.getColor(R.color.white, theme))

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val eightDpInPixels = (8 * resources.displayMetrics.density + 0.5f).toInt()
            layoutParams.bottomMargin = eightDpInPixels // 8dp
            textViewRule.layoutParams = layoutParams

            linearLayoutRulesContainer.addView(textViewRule)
        }
    }

    private fun changeBackGround() {
        val backgroundConf = settingsHandler.readValue(Configuration.BACKGROUND.value)!!
        val drawable = ResourceUtils.getDrawableByName(resources, this.packageName, backgroundConf)
        val layout = findViewById<ConstraintLayout>(R.id.settingsConstraintLayout)
        layout.background = ContextCompat.getDrawable(this, drawable)
    }
}
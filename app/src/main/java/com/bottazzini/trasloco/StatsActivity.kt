package com.bottazzini.trasloco

import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bottazzini.trasloco.settings.AchievementsRepository
import com.bottazzini.trasloco.settings.Configuration
import com.bottazzini.trasloco.settings.GameLogRepository
import com.bottazzini.trasloco.settings.RecordsHandler
import com.bottazzini.trasloco.settings.SettingsHandler
import com.bottazzini.trasloco.settings.Type
import com.bottazzini.trasloco.utils.AchievementCatalog
import com.bottazzini.trasloco.utils.ResourceUtils
import com.bottazzini.trasloco.utils.TimeUtils
import com.bottazzini.trasloco.utils.WindowInsetsUtils
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatsActivity : AppCompatActivity() {

    private lateinit var recordsHandler: RecordsHandler
    private lateinit var gameLogRepo: GameLogRepository
    private lateinit var achievementsRepo: AchievementsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_stats)
        WindowInsetsUtils.applySystemBarInsets(window, findViewById(R.id.statsScrollView))
        supportActionBar?.hide()

        recordsHandler = RecordsHandler(applicationContext)
        gameLogRepo = GameLogRepository(applicationContext)
        achievementsRepo = AchievementsRepository(applicationContext)

        val settingsHandler = SettingsHandler(applicationContext)
        val backgroundConf = settingsHandler.readValue(Configuration.BACKGROUND.value) ?: "verde"
        val drawable = ResourceUtils.getDrawableByName(resources, packageName, backgroundConf)
        findViewById<View>(R.id.statsScrollView).background = ContextCompat.getDrawable(this, drawable)

        loadStats()
        loadChart()
        loadPieChart()
        loadAchievements()
    }

    private fun loadStats() {
        val bestTime = recordsHandler.getBestTime()
        val streakRecord = recordsHandler.readValue(Type.CONSECUTIVE) ?: 0L
        val totalWins = recordsHandler.getTotalWins()
        val totalGames = gameLogRepo.countAll()
        val winRate = if (totalGames > 0) (gameLogRepo.countWins() * 100L / totalGames) else null
        val avgTime = gameLogRepo.avgWinDurationMs()

        setStatRow(R.id.statRowBestTime, getString(R.string.stats_best_time),
            if (bestTime != null) TimeUtils.formatTime(bestTime) else getString(R.string.stats_no_data))
        setStatRow(R.id.statRowStreak, getString(R.string.stats_streak_record), streakRecord.toString())
        setStatRow(R.id.statRowTotalWins, getString(R.string.stats_total_wins), totalWins.toString())
        setStatRow(R.id.statRowGamesPlayed, getString(R.string.stats_games_played), totalGames.toString())
        setStatRow(R.id.statRowWinRate, getString(R.string.stats_win_rate),
            if (winRate != null) "$winRate%" else getString(R.string.stats_no_data))
        setStatRow(R.id.statRowAvgTime, getString(R.string.stats_avg_time),
            if (avgTime != null) TimeUtils.formatTime(avgTime) else getString(R.string.stats_no_data))
    }

    private fun setStatRow(rowId: Int, label: String, value: String) {
        val row = findViewById<View>(rowId)
        row.findViewById<TextView>(R.id.statRowLabel).text = label
        row.findViewById<TextView>(R.id.statRowValue).text = value
    }

    private fun loadChart() {
        val chart = findViewById<LineChart>(R.id.statsChart)
        val emptyLabel = findViewById<TextView>(R.id.statsChartEmpty)
        val games = gameLogRepo.getLastN(30).reversed()

        if (games.isEmpty()) {
            chart.isVisible = false
            emptyLabel.isVisible = true
            return
        }

        chart.isVisible = true
        emptyLabel.isVisible = false

        val goldColor = ContextCompat.getColor(this, R.color.casino_gold)
        val greenColor = ContextCompat.getColor(this, android.R.color.holo_green_dark)
        val redColor = ContextCompat.getColor(this, android.R.color.holo_red_dark)

        val entries = games.mapIndexed { idx, game ->
            Entry(idx.toFloat(), game.durationMs / 60_000f)
        }
        val circleColors = games.map { if (it.won) greenColor else redColor }

        val dataSet = LineDataSet(entries, "").apply {
            color = goldColor
            setCircleColors(circleColors)
            circleRadius = 4f
            lineWidth = 1.5f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        chart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.apply {
                isEnabled = true
                textColor = goldColor
                form = Legend.LegendForm.CIRCLE
                setCustom(listOf(
                    LegendEntry().also { it.label = getString(R.string.stats_legend_win); it.formColor = greenColor },
                    LegendEntry().also { it.label = getString(R.string.stats_legend_loss); it.formColor = redColor }
                ))
            }
            setTouchEnabled(true)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = goldColor
                axisLineColor = goldColor
                setDrawGridLines(false)
                granularity = 1f
            }
            axisLeft.apply {
                textColor = goldColor
                axisLineColor = goldColor
                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?) = "${value.toInt()}'"
                }
            }
            axisRight.isEnabled = false
            invalidate()
        }
    }

    private fun loadPieChart() {
        val chart = findViewById<PieChart>(R.id.statsWLChart)
        val wins = gameLogRepo.countWins().toFloat()
        val total = gameLogRepo.countAll()
        val losses = (total - wins.toInt()).toFloat()

        val goldColor = ContextCompat.getColor(this, R.color.casino_gold)
        val bordeauxColor = ContextCompat.getColor(this, R.color.casino_bordeaux)

        if (total == 0L) {
            chart.isVisible = false
            return
        }

        val entries = listOf(
            PieEntry(wins, getString(R.string.stats_legend_win)),
            PieEntry(losses, getString(R.string.stats_legend_loss))
        )
        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(goldColor, bordeauxColor)
            sliceSpace = 3f
            setDrawValues(false)
        }

        chart.apply {
            data = PieData(dataSet)
            holeRadius = 55f
            transparentCircleRadius = 60f
            setHoleColor(android.graphics.Color.TRANSPARENT)
            setTransparentCircleColor(android.graphics.Color.TRANSPARENT)
            val winPct = (wins / total * 100).toInt()
            centerText = "$winPct%"
            setCenterTextColor(goldColor)
            setCenterTextTypeface(android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD))
            setCenterTextSize(20f)
            description.isEnabled = false
            legend.apply {
                isEnabled = true
                textColor = goldColor
                form = Legend.LegendForm.CIRCLE
            }
            setDrawEntryLabels(false)
            isRotationEnabled = false
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            invalidate()
        }
    }

    private fun loadAchievements() {
        val unlockedMap = achievementsRepo.getAllUnlocked()
        val unlockedCount = unlockedMap.size

        findViewById<TextView>(R.id.statsTrophiesHeader).text =
            getString(R.string.stats_trophies_header, unlockedCount)

        val recycler = findViewById<RecyclerView>(R.id.achievementsGrid)
        recycler.layoutManager = GridLayoutManager(this, 3)
        recycler.adapter = AchievementAdapter(
            context = this,
            defs = AchievementCatalog.all,
            unlockedMap = unlockedMap,
            onTap = { def, isUnlocked, unlockedAt -> showDetail(def, isUnlocked, unlockedAt) }
        )
    }

    private fun showDetail(def: com.bottazzini.trasloco.utils.AchievementDef, isUnlocked: Boolean, unlockedAt: Long?) {
        val sheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.sheet_achievement_detail, null)
        view.findViewById<TextView>(R.id.detailIcon).text = def.icon
        view.findViewById<TextView>(R.id.detailName).text = getString(def.nameRes)
        view.findViewById<TextView>(R.id.detailDesc).text = getString(def.descRes)
        view.findViewById<TextView>(R.id.detailStatus).text = if (isUnlocked && unlockedAt != null) {
            val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            val d = Date(unlockedAt)
            getString(R.string.stats_unlocked_on, dateFmt.format(d), timeFmt.format(d))
        } else {
            getString(R.string.stats_locked)
        }
        sheet.setContentView(view)
        sheet.show()
    }

    override fun onDestroy() {
        recordsHandler.close()
        gameLogRepo.close()
        achievementsRepo.close()
        super.onDestroy()
    }
}

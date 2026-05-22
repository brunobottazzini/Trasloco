package com.bottazzini.trasloco

import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import com.bottazzini.trasloco.settings.Configuration
import com.bottazzini.trasloco.settings.RecordsHandler
import com.bottazzini.trasloco.settings.SettingsHandler
import com.bottazzini.trasloco.settings.Type
import com.bottazzini.trasloco.utils.CardAnimator
import com.bottazzini.trasloco.utils.DeckSetup
import com.bottazzini.trasloco.utils.ResourceUtils
import com.bottazzini.trasloco.utils.ThemeUtils
import com.bottazzini.trasloco.utils.TimeUtils
import android.widget.ImageButton
import java.util.LinkedList


class GameActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TUTORIAL_MODE = "tutorial_mode"
    }

    private lateinit var settingsHandler: SettingsHandler
    private lateinit var recordsHandler: RecordsHandler
    private lateinit var gameStateRepo: com.bottazzini.trasloco.settings.GameStateRepository
    private var resumeMode: Boolean = false
    private var shouldPersistOnPause: Boolean = true
    private lateinit var textViewGameTimer: TextView
    private var timerPausedTimeMillis: Long = 0L
    private var isTimerPaused: Boolean = false
    private var gameStartTimeMillis: Long = 0L
    private var consecutiveWins: Long = 0L
    private var coppiedSubDeckMap = HashMap<String, List<String>>()
    private var subDeckMap = HashMap<String, List<String>>()
    private var cardTableMap = HashMap<String, ArrayList<String>>()
    private var endDeckList = HashMap<String, String>()
    private var playList = HashMap<String, LinkedList<Int>>()
    private var selectedCard: String? = null
    private var selectedPositionId: Int? = null
    private var isTutorialMode: Boolean = false
    private var tutorialEngine: com.bottazzini.trasloco.utils.TutorialEngine? = null
    private var enabledFastEndDeckClick = true
    private var cardType: String = "piacentine"
    private val timerHandler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable
    private var mediaPlayerAtomic: MediaPlayer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isInitializing = true
    private val gameViewModel: GameViewModel by lazy {
        ViewModelProvider(this).get(GameViewModel::class.java)
    }
    private lateinit var hintEngine: HintEngine
    private var hintEnabled: Boolean = true
    private var autoMoveEnabled: Boolean = false
    private var soundEnabled: Boolean = true
    private var autoMoveRunnable: Runnable? = null
    private val gameRoot: ConstraintLayout by lazy {
        findViewById<ConstraintLayout>(R.id.gameConstraintLayout)
    }
    private val touchSlop: Int by lazy { ViewConfiguration.get(this).scaledTouchSlop }
    private var dragTouchStartX: Float = 0f
    private var dragTouchStartY: Float = 0f
    private var dragTouchView: View? = null
    private var dragGhost: ImageView? = null
    private var dragSourceView: View? = null
    private var dragHoverTarget: View? = null
    private var dragRoot: ConstraintLayout? = null
    private val dragSlotIds = listOf(
        R.id.subDeck11, R.id.subDeck12, R.id.subDeck13, R.id.subDeck14,
        R.id.subDeck21, R.id.subDeck22, R.id.subDeck23, R.id.subDeck24,
        R.id.subDeck31, R.id.subDeck32, R.id.subDeck33, R.id.subDeck34,
        R.id.subDeck41, R.id.subDeck42, R.id.subDeck43, R.id.subDeck44
    )
    private var hintsUsedThisGame: Int = 0
    private var autoMovesThisGame: Int = 0
    private lateinit var gameLogRepo: com.bottazzini.trasloco.settings.GameLogRepository
    private lateinit var achievementBanner: com.bottazzini.trasloco.utils.AchievementBanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        setContentView(R.layout.game)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBar)) { view, insets ->
            val top = maxOf(
                insets.getInsets(WindowInsetsCompat.Type.statusBars()).top,
                insets.getInsets(WindowInsetsCompat.Type.displayCutout()).top
            )
            view.updatePadding(top = top)
            insets
        }
        textViewGameTimer = findViewById(R.id.textViewGameTimer)
        supportActionBar?.hide()
        settingsHandler = SettingsHandler(applicationContext)
        recordsHandler = RecordsHandler(applicationContext)
        gameStateRepo = com.bottazzini.trasloco.settings.GameStateRepository(applicationContext)
        gameLogRepo = com.bottazzini.trasloco.settings.GameLogRepository(applicationContext)
        achievementBanner = com.bottazzini.trasloco.utils.AchievementBanner(
            this,
            findViewById(R.id.gameBannerAchievement)
        )
        resumeMode = intent.getBooleanExtra("resume", false)
        isTutorialMode = intent.getBooleanExtra(EXTRA_TUTORIAL_MODE, false)
        if (recordsHandler.readValue(Type.CONSECUTIVE) != null) {
            consecutiveWins = recordsHandler.readValue(Type.CONSECUTIVE)!!
        }

        processSettings()
        setupDragAndDrop()
        if (isTutorialMode) {
            startTutorial()
            return
        }
        if (resumeMode) {
            val loaded = gameStateRepo.load()
            if (loaded != null) {
                restoreFromSavedState(loaded)
                return
            }
            resumeMode = false
        }
        if (gameViewModel.hasActiveGame) {
            restoreGameFromViewModel()
        } else {
            startNewGame()
        }
    }

    fun startNewGame(view: View) {
        startNewGame()
    }

    fun startNewGame() {
        abandonCurrentGame()
        isInitializing = true
        // Starting a fresh game invalidates any previously saved snapshot.
        shouldPersistOnPause = true
        if (::gameStateRepo.isInitialized) {
            gameStateRepo.clear()
        }
        gameViewModel.hasActiveGame = true
        gameViewModel.gameLost = false
        hintsUsedThisGame = 0
        autoMovesThisGame = 0
        playSound(R.raw.shuffle)
        stopTimer()
        prePrepareTable()

        findViewById<View>(R.id.loadingOverlay).visibility = View.VISIBLE

        Thread {
            DeckSetup.shuffleSolvable()
            runOnUiThread {
                DeckSetup.prepareSubDecks()
                subDeckMap = DeckSetup.getSubDeckMap()
                coppiedSubDeckMap = HashMap(subDeckMap)
                prepareTable()
                if (hasReachedLostConditions()) {
                    startNewGame()
                    return@runOnUiThread
                }
                startTimer()
                isInitializing = false
                findViewById<View>(R.id.loadingOverlay).visibility = View.GONE
            }
        }.start()
    }

    private fun startTutorial() {
        // Tutorial state is not persisted across activity recreation; rotation restarts from step 1.
        isInitializing = true
        shouldPersistOnPause = false
        gameStateRepo.clear()
        clearCardSelection()

        // Hide game chrome that doesn't belong in tutorial mode.
        findViewById<View>(R.id.topBar).visibility = View.GONE

        // Deterministic deck → table.
        com.bottazzini.trasloco.utils.DeckSetup.setTutorialDeck()
        com.bottazzini.trasloco.utils.DeckSetup.prepareSubDecks()
        subDeckMap = com.bottazzini.trasloco.utils.DeckSetup.getSubDeckMap()
        coppiedSubDeckMap = HashMap(subDeckMap)
        prepareTable()

        // Init engine + banner.
        tutorialEngine = com.bottazzini.trasloco.utils.TutorialEngine(
            com.bottazzini.trasloco.utils.TutorialSteps.build()
        )
        val banner = findViewById<View>(R.id.tutorialBanner)
        banner.visibility = View.VISIBLE
        findViewById<View>(R.id.tutorialNextButton).setOnClickListener { onTutorialNext() }
        findViewById<View>(R.id.tutorialExitButton).setOnClickListener { showTutorialExitDialog() }

        renderTutorialStep()
        isInitializing = false
    }

    private fun renderTutorialStep() {
        val engine = tutorialEngine ?: return
        if (engine.isComplete()) {
            finish()
            return
        }

        val step = engine.currentStep()
        val moveCompleted = step.requiredMove != null && engine.isCurrentStepComplete()

        // Highlight management: clear all slots, then apply pulse on targets for the current step.
        // After the move is complete, drop the highlights (the new state speaks for itself).
        for (id in dragSlotIds) {
            findViewById<View>(id).setBackgroundResource(R.drawable.casino_slot_frame)
        }
        if (!moveCompleted) {
            for (target in step.highlightTargets) {
                findTutorialViewForTarget(target)?.setBackgroundResource(R.drawable.hint_pulse)
            }
        }

        // Banner text: confirmation if move just done, instruction otherwise.
        val textResId = if (moveCompleted && step.confirmationResId != null) {
            step.confirmationResId
        } else {
            step.instructionResId
        }
        findViewById<TextView>(R.id.tutorialBannerText).text = getString(textResId)

        // Next button visibility:
        //  - intro/outro (no requiredMove): always visible
        //  - move-step before the move: hidden
        //  - move-step after the move: visible
        val nextBtn = findViewById<Button>(R.id.tutorialNextButton)
        nextBtn.visibility = if (step.requiredMove == null || moveCompleted) View.VISIBLE else View.GONE
        nextBtn.text = if (step.label == "outro") getString(R.string.tutorial_finish) else getString(R.string.tutorial_next)
    }

    private fun findTutorialViewForTarget(target: String): View? {
        // TutorialSteps.FOUNDATION_1 == "endDeck1" maps to the first foundation slot (line 1).
        if (target == "endDeck1") {
            return findViewById(R.id.subDeck14)
        }
        for ((position, cards) in cardTableMap) {
            if (cards.isNotEmpty() && cards.last() == target) {
                val id = resources.getIdentifier("subDeck$position", "id", this.packageName)
                if (id != 0) return findViewById(id)
            }
        }
        return null
    }

    private fun onTutorialNext() {
        val engine = tutorialEngine ?: return
        engine.advanceToNext()
        if (engine.isComplete()) {
            val nextBtn = findViewById<Button>(R.id.tutorialNextButton)
            nextBtn.isEnabled = false
            nextBtn.isClickable = false
            val newAchievements = com.bottazzini.trasloco.utils.AchievementEngine.create(this)
                .evaluate(com.bottazzini.trasloco.utils.AchievementTrigger.TUTORIAL_COMPLETED)
            if (newAchievements.isNotEmpty()) {
                achievementBanner.enqueue(newAchievements)
                window.decorView.postDelayed({ finish() }, 3200L)
            } else {
                finish()
            }
        } else {
            renderTutorialStep()
        }
    }

    private fun showTutorialExitDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.tutorial_exit_title)
            .setPositiveButton(R.string.tutorial_exit_confirm) { _, _ -> finish() }
            .setNegativeButton(R.string.tutorial_exit_cancel) { d, _ -> d.dismiss() }
            .show()
    }

    fun retryGame(view: View) {
        if (isTutorialMode) return
        hintsUsedThisGame = 0
        autoMovesThisGame = 0
        isInitializing = true
        // Re-enable persistence: a retried game should also be resumable.
        shouldPersistOnPause = true
        gameViewModel.hasActiveGame = true
        gameViewModel.gameLost = false
        playSound(R.raw.shuffle)
        stopTimer()
        prePrepareTable()
        cardTableMap.clear()
        newPlayList()
        subDeckMap = HashMap(coppiedSubDeckMap)
        prepareTable()
        startTimer()
        isInitializing = false
    }

    fun goToMenuFromLost(view: View) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK }
        )
        finish()
    }

    fun undoClick(view: View) {
        if (playList.isNotEmpty()) {
            val undoSelectedCard = playList.keys.iterator().next()
            val cardPosition = playList[undoSelectedCard]!![0]
            val desiredPosition = playList[undoSelectedCard]!![1]
            val undoPosition = resources.getResourceEntryName(desiredPosition).split("subDeck")[1]

            moveCard(desiredPosition, undoPosition, undoSelectedCard, cardPosition)

            clearUndoButton()
        }
    }

    fun subDeckClick(view: View) {
        if (isTutorialMode) return
        val cardPosition = view.id
        val cardName = getCardName(cardPosition)
        if (cardName == "zero") {
            return
        }

        val imageView = findViewById<ImageView>(cardPosition)
        val line = (imageView.tag) as String
        val subDeck = getSubDeckListConcurrentSafely(line)
        if (subDeck.isNotEmpty()) {
            dealCard(line)
            if (getSubDeckListConcurrentSafely(line).isEmpty()) {
                setImage(cardPosition, "zero")
                imageView.foreground = null
                imageView.isClickable = false
            }
        }

        clearCardSelection()

        if (hasReachedLostConditions()) {
            showYouLost()
        }
    }

    fun gameCardClick(view: View) {
        val cardPosition = view.id
        val cardName = getCardName(cardPosition)
        val desiredPosition = resources.getResourceEntryName(cardPosition).split("subDeck")[1]
        val textView = findViewById<TextView>(R.id.selectedCardTextView)
        textView.text = ""
        if (cardName == "zero" && !isEndDeckClick(desiredPosition)) {
            return
        }

        if (selectedCard == null) {
            if (isEndDeckClick(desiredPosition)) {
                return
            }
            clearCardSelection()
            selectedCard = cardName
            selectedPositionId = view.id
            setSelected(selectedPositionId)
            return
        }

        val sourceView = findViewById<ImageView>(selectedPositionId!!)
        val moved = tryMove(sourceView, view)
        if (moved) {
            if (hasReachedWonConditions()) {
                showYouWon()
                return
            } else if (hasReachedLostConditions()) {
                showYouLost()
                return
            }
            clearCardSelection()
            triggerAutoMoveCycle()
        } else {
            if (selectedPositionId == cardPosition) {
                return
            }
            if (isEndDeckClick(desiredPosition)) {
                return
            }
            clearCardSelection()
            selectedPositionId = cardPosition
            selectedCard = cardName
            setSelected(cardPosition)
            textView.text = resources.getString(R.string.invaild_move)
        }
    }

    private fun tryMove(sourceView: View, targetView: View): Boolean {
        val sourceCard = sourceView.tag as String
        val sourcePositionId = sourceView.id
        val targetCard = targetView.tag as String
        val targetPositionId = targetView.id
        val targetPosition =
            resources.getResourceEntryName(targetPositionId).split("subDeck")[1]

        if (isTutorialMode) {
            val engine = tutorialEngine ?: return false
            val tutorialTarget = if (isEndDeckClick(targetPosition)) "endDeck1" else targetCard
            if (!engine.isMoveAllowed(sourceCard, tutorialTarget)) {
                return false
            }
        }

        if (!canBeInserted(targetCard, sourceCard, isEndDeckClick(targetPosition))) {
            return false
        }

        moveCard(targetPositionId, targetPosition, sourceCard, sourcePositionId)

        if (!isEndDeckClick(targetPosition)) {
            newPlayList()
            playList[sourceCard] = linkedListOf(targetPositionId, sourcePositionId)
        } else {
            val line = targetPosition.first()
            endDeckList[line.toString()] = sourceCard
            clearUndoButton()

            if (enabledFastEndDeckClick) {
                val sourcePositionName =
                    resources.getResourceEntryName(sourcePositionId).split("subDeck")[1]

                if (cardTableMap[sourcePositionName]!!.isNotEmpty()) {
                    forceCardsEndDeck(
                        sourcePositionId,
                        sourcePositionName,
                        targetPositionId,
                        line.toString()
                    )
                }
            }
        }

        val resetButton = findViewById<View>(R.id.resetButton)
        resetButton.isEnabled = playList.isNotEmpty()

        if (isTutorialMode) {
            val tutorialTarget = if (isEndDeckClick(targetPosition)) "endDeck1" else targetCard
            tutorialEngine?.onMoveExecuted(sourceCard, tutorialTarget)
            renderTutorialStep()
        }

        return true
    }

    private fun clearCardSelection() {
        val textView = findViewById<TextView>(R.id.selectedCardTextView)
        textView.text = ""
        setUnselected(selectedPositionId)
        selectedPositionId = null
        selectedCard = null
    }

    private fun forceCardsEndDeck(
        selectedPositionId: Int,
        selectedPositionName: String,
        desiredCardPositionId: Int,
        line: String
    ) {
        val lastCard = cardTableMap[selectedPositionName]!!.first()

        // Capture source view + card drawable BEFORE clearing the slot
        val sourceView = findViewById<ImageView>(selectedPositionId)
        val cardResourceName = if (lastCard == "zero") lastCard else "${cardType}_${lastCard}"
        val drawableId = ResourceUtils.getDrawableByName(resources, packageName, cardResourceName)
        val cardDrawable = ContextCompat.getDrawable(this, drawableId)
        val targetView = findViewById<ImageView>(desiredCardPositionId)

        // Update game state synchronously so win-condition checks are correct immediately
        cardTableMap[selectedPositionName]!!.clear()
        setNumberOfCards(cardTableMap[selectedPositionName]!!, selectedPositionName)
        endDeckList[line] = lastCard

        // Clear source slot visually right away
        setImage(selectedPositionId, "zero")

        // Animate the card ghost flying to the end-deck slot; show the card there on completion
        CardAnimator.animateCardFlight(gameRoot, sourceView, targetView, cardDrawable, 350L) {
            if (!isFinishing) {
                setImage(desiredCardPositionId, lastCard)
            }
        }
    }


    private fun moveCard(
        desiredCardPosition: Int,
        desiredPosition: String,
        selectedCard: String,
        selectedPositionId: Int
    ) {
        setImage(desiredCardPosition, selectedCard)
        val selectedPositionName =
            resources.getResourceEntryName(selectedPositionId).split("subDeck")[1]

        cardTableMap[desiredPosition]?.add(selectedCard)
        if (!isEndDeckClick(desiredPosition)) {
            setNumberOfCards(cardTableMap[desiredPosition]!!, desiredPosition)
        }

        cardTableMap[selectedPositionName]!!.remove(selectedCard)
        setNumberOfCards(cardTableMap[selectedPositionName]!!, selectedPositionName)

        if (cardTableMap[selectedPositionName]!!.isEmpty()) {
            setImage(selectedPositionId, "zero")
        } else {
            setImage(selectedPositionId, cardTableMap[selectedPositionName]!!.last())
        }
        playSoundAtomic(R.raw.flipcard)
    }

    private fun setNumberOfCards(cardsList: List<String>, position: String) {
        val textView = findViewById<TextView>(getTextViewByName(position))
        if (cardsList.size > 1) {
            textView.text = cardsList.size.toString()
            textView.visibility = View.VISIBLE
        } else {
            textView.text = ""
            textView.visibility = View.INVISIBLE
        }
    }

    private fun isEndDeckClick(positionName: String) = positionName.last() == '4'

    private fun canBeInserted(
        sourceCard: String,
        movingCard: String,
        endClickDeck: Boolean
    ): Boolean = com.bottazzini.trasloco.utils.CardMoveValidator.canBeInserted(
        sourceCard, movingCard, endClickDeck
    )

    private fun getTextViewByName(textName: String) =
        resources.getIdentifier("textView$textName", "id", this.packageName)

    private fun prepareTable() {
        arrayOf("1", "2", "3", "4").forEach { line ->
            dealCard(line)
        }
    }

    private fun getSubDeckListConcurrentSafely(line: String): MutableList<String> {
        val list = ArrayList<String>()
        list.addAll(subDeckMap[line]!!)
        return list
    }

    private fun hasReachedWonConditions() =
        endDeckList.values.count { card -> card.substring(1, card.length) == "10" } == 4

    private fun hasReachedLostConditions(): Boolean {
        if (canDealCard()) {
            return false
        }

        return !canMoveCard()
    }

    private fun canMoveCard(): Boolean {
        for (tableMap in cardTableMap.entries) {
            if (tableMap.value.isEmpty()) {
                continue
            }

            val currentCard = tableMap.value.last()
            if (currentCard == "zero") {
                continue
            }

            for (endPos in listOf("14", "24", "34", "44")) {
                val line = endPos.substring(0, 1)
                val endCard = endDeckList[line]
                if (endCard != "zero") {
                    if (canBeInserted(
                            endCard!!,
                            currentCard,
                            isEndDeckClick(endPos)
                        )
                    ) {
                        return true
                    }
                } else {
                    val currentCardNumber = currentCard.substring(1, currentCard.length)
                    if (currentCardNumber == "1" && endCard == "zero") {
                        return true
                    }
                }
            }

            for (destinationTableMap in cardTableMap.entries) {
                val desiredPosition = destinationTableMap.key

                if (destinationTableMap.value.isEmpty()) {
                    continue
                }

                val destinationCard = destinationTableMap.value.last()
                if (destinationCard == "zero") {
                    continue
                }

                when {
                    canBeInserted(
                        currentCard,
                        destinationCard,
                        isEndDeckClick(desiredPosition)
                    ) -> {
                        return true
                    }
                    canBeInserted(
                        destinationCard,
                        currentCard,
                        isEndDeckClick(desiredPosition)
                    ) -> {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun canDealCard(): Boolean {
        for (line in listOf("1", "2", "3", "4")) {
            val subDeck = subDeckMap[line]
            if (subDeck?.isNotEmpty() == true) {
                for (pos in 1..3) {
                    val position = "$line${pos}"
                    val imageViewId =
                        resources.getIdentifier("subDeck$position", "id", this.packageName)
                    if (getCardName(imageViewId) == "zero") {
                        return true
                    }
                }
            }
        }

        return false
    }

    private fun dealCard(line: String) {
        val subDeck = getSubDeckListConcurrentSafely(line)
        val iterator = subDeck.iterator()
        while (iterator.hasNext()) {
            val cardName = iterator.next()
            for (pos in 1..3) {
                val position = "$line${pos}"
                val imageViewId =
                    resources.getIdentifier("subDeck$position", "id", this.packageName)

                if (getCardName(imageViewId) == "zero") {
                    if (!isInitializing) {
                        playSoundAtomic(R.raw.flipcard)
                    }
                    setImage(imageViewId, cardName)
                    cardTableMap[position] = arrayListOf(cardName)
                    iterator.remove()

                    clearUndoButton()
                    break
                }

            }
        }

        subDeckMap[line] = subDeck
    }

    private fun setImage(position: Int, imageName: String) {
        val imageView = findViewById<ImageView>(position)
        // Drawable prefisso per le carte di gioco (b/c/d/s + numero); "zero" e back card
        // restano universali condivisi tra i tipi.
        val resourceName = if (imageName == "zero") imageName else "${cardType}_${imageName}"
        val id = ResourceUtils.getDrawableByName(resources, this.packageName, resourceName)
        imageView.setImageResource(id)
        imageView.tag = imageName
    }

    private fun getCardName(cardPosition: Int): String {
        val imageView = findViewById<ImageView>(cardPosition)
        return (imageView.tag) as String
    }

    private fun clearUndoButton() {
        val resetButton = findViewById<View>(R.id.resetButton)
        resetButton.isEnabled = false
        newPlayList()
    }

    private fun prepareTextAndButtonForNewGame() {
        val resetButton = findViewById<View>(R.id.resetButton)
        resetButton.isInvisible = false
        findViewById<TextView>(R.id.selectedCardTextView).isInvisible = false
        findViewById<View>(R.id.lostOverlay).isGone = true
    }

    private fun showYouLost() {
        if (isTutorialMode) return
        shouldPersistOnPause = false
        gameStateRepo.clear()
        clearCardSelection()
        val resetButton = findViewById<View>(R.id.resetButton)
        resetButton.isInvisible = true
        findViewById<TextView>(R.id.selectedCardTextView).isInvisible = true
        findViewById<View>(R.id.lostOverlay).isGone = false
        stopTimer()
        val lostDurationMs = System.currentTimeMillis() - gameStartTimeMillis
        gameLogRepo.insert(
            com.bottazzini.trasloco.settings.GameLog(
                timestamp = System.currentTimeMillis(),
                durationMs = lostDurationMs,
                won = false,
                hintsUsed = hintsUsedThisGame,
                autoMoves = autoMovesThisGame
            )
        )
        val newAchievements = com.bottazzini.trasloco.utils.AchievementEngine.create(this)
            .evaluate(com.bottazzini.trasloco.utils.AchievementTrigger.GAME_LOST)
        achievementBanner.enqueue(newAchievements)
        val consecutive = recordsHandler.readValue(Type.CONSECUTIVE)
        if (consecutive != null) {
            recordsHandler.update(Type.CONSECUTIVE, consecutive, 0L, false)
        }

        gameViewModel.gameLost = true
        playSound(R.raw.youlost)
    }

    private fun playSound(soundId: Int) {
        if (!soundEnabled) return
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
                mediaPlayer?.reset()
            }
            mediaPlayer?.release()
            mediaPlayer = null

            mediaPlayer = MediaPlayer.create(this, soundId)
            mediaPlayer?.setOnCompletionListener {
                mediaPlayer?.release()
                mediaPlayer = null
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playSoundAtomic(soundId: Int) {
        if (!soundEnabled) return
        try {
            mediaPlayerAtomic =
                MediaPlayer.create(this, soundId)
            mediaPlayerAtomic?.setOnCompletionListener {
                if (mediaPlayerAtomic?.isPlaying == false) {
                    mediaPlayerAtomic?.release()
                    mediaPlayerAtomic = null
                }
            }
            mediaPlayerAtomic?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showYouWon() {
        if (isTutorialMode) return
        shouldPersistOnPause = false
        gameStateRepo.clear()
        clearCardSelection()
        val resetButton = findViewById<View>(R.id.resetButton)
        resetButton.isInvisible = true
        findViewById<TextView>(R.id.selectedCardTextView).isInvisible = true
        stopTimer()
        val previousTime = recordsHandler.readValue(Type.TIME)
        val millisPassed = System.currentTimeMillis() - gameStartTimeMillis
        if (previousTime != null) {
            if (previousTime > millisPassed || previousTime == -1L) {
                recordsHandler.update(Type.TIME, millisPassed, millisPassed, true)
            } else {
                val currentRecord = recordsHandler.readValue(Type.TIME)
                if (currentRecord != null) {
                    recordsHandler.update(Type.TIME, currentRecord, millisPassed, false)
                }
            }
        }

        val consecutive = recordsHandler.readValue(Type.CONSECUTIVE)
        val currentValue = if (recordsHandler.readCurrentValue(Type.CONSECUTIVE) != null) {
            recordsHandler.readCurrentValue(Type.CONSECUTIVE)!! + 1L
        } else {
            0L
        }
        if (consecutive != null) {
            if (currentValue > consecutive) {
                recordsHandler.update(Type.CONSECUTIVE, currentValue, currentValue, true)
            } else {
                recordsHandler.update(Type.CONSECUTIVE, consecutive, currentValue, false)
            }
        }
        gameLogRepo.insert(
            com.bottazzini.trasloco.settings.GameLog(
                timestamp = System.currentTimeMillis(),
                durationMs = millisPassed,
                won = true,
                hintsUsed = hintsUsedThisGame,
                autoMoves = autoMovesThisGame
            )
        )
        // --- Navigate to YouWonActivity ---
        val intent = Intent(this, YouWonActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun zeroFill() {
        for (line in listOf("1", "2", "3", "4")) {
            val hideCardDeck =
                resources.getIdentifier("subDeck$line", "id", this.packageName)
            val backCardValue = settingsHandler.readValue(Configuration.CARD_BACK.value)!!
            setBackDeckCard(backCardValue, line)
            findViewById<ImageView>(hideCardDeck).tag = line
            for (pos in 1..4) {
                val position = "$line${pos}"
                val imageViewId =
                    resources.getIdentifier("subDeck$position", "id", this.packageName)
                setImage(imageViewId, "zero")

                if (!isEndDeckClick(position)) {
                    val textView = findViewById<TextView>(getTextViewByName(position))
                    textView.text = ""
                    textView.visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun prePrepareTable() {
        clearCardSelection()
        zeroFill()
        endDeckList = hashMapOf("1" to "zero", "2" to "zero", "3" to "zero", "4" to "zero")
        prepareTextAndButtonForNewGame()
    }

    private fun processSettings() {
        val fastDealValue = settingsHandler.readValue(Configuration.FAST_DEAL.value)
        if (fastDealValue != "enabled") enabledFastEndDeckClick = false

        cardType = settingsHandler.readValue(Configuration.CARD_TYPE.value) ?: "piacentine"

        val backCardValue = settingsHandler.readValue(Configuration.CARD_BACK.value)
        setBackCards(backCardValue!!)

        val backgroundConf = settingsHandler.readValue(Configuration.BACKGROUND.value) ?: "bordeaux"
        val drawable = ResourceUtils.getDrawableByName(resources, this.packageName, backgroundConf)
        val layout = findViewById<ConstraintLayout>(R.id.gameConstraintLayout)
        layout.background = ContextCompat.getDrawable(this, drawable)
        applyAccentColor(backgroundConf)

        hintEnabled = settingsHandler.readValue(Configuration.HINT_ENABLED.value) == "enabled"
        if (!::hintEngine.isInitialized) {
            hintEngine = HintEngine(this)
        }

        autoMoveEnabled = settingsHandler.readValue(Configuration.AUTO_MOVE.value) == "enabled"
        soundEnabled = settingsHandler.readValue(Configuration.SOUND_ENABLED.value) != "disabled"
    }

    private fun applyAccentColor(bg: String) {
        val color = ThemeUtils.accentColor(bg, this)
        val dimColor = ThemeUtils.accentColorDim(bg, this)
        listOf(
            R.id.iconBack, R.id.textViewGameTimer, R.id.iconPause, R.id.resetButton,
            R.id.lostTextView, R.id.newGameButton, R.id.retryButton, R.id.menuLostButton,
            R.id.pauseTextTitle, R.id.tutorialBannerText, R.id.tutorialNextButton
        ).forEach { findViewById<TextView>(it)?.setTextColor(color) }
        findViewById<TextView>(R.id.pauseTextSubtitle)?.setTextColor(dimColor)
        findViewById<ImageButton>(R.id.tutorialExitButton)?.setColorFilter(color)
    }

    private fun setBackCards(imageName: String) {
        for (line in listOf("1", "2", "3", "4")) {
            setBackDeckCard(imageName, line)
        }
    }

    private fun setBackDeckCard(imageName: String, position: String) {
        val imageViewId =
            resources.getIdentifier("subDeck$position", "id", this.packageName)
        val imageView = findViewById<ImageView>(imageViewId)
        imageView.setImageResource(
            ResourceUtils.getDrawableByName(
                resources,
                this.packageName,
                imageName
            )
        )
        imageView.foreground =
            ContextCompat.getDrawable(this, R.drawable.subdeck_background_selector)
        imageView.isClickable = true
        applyDeckInsets(imageView)
    }

    private fun applyDeckInsets(imageView: ImageView) {
        val deck = com.bottazzini.trasloco.utils.CardDeckRegistry.byId(cardType)
        if (deck.insetX == 0f && deck.insetY == 0f) {
            imageView.setPadding(0, 0, 0, 0)
            return
        }
        imageView.post {
            val px = (imageView.width * deck.insetX / 2f).toInt()
            val py = (imageView.height * deck.insetY / 2f).toInt()
            imageView.setPadding(px, py, px, py)
        }
    }

    private fun linkedListOf(val1: Int, val2: Int): LinkedList<Int> {
        val linkedList = LinkedList<Int>()
        linkedList.add(val1)
        linkedList.add(val2)
        return linkedList
    }

    private fun setSelected(cardPositionId: Int?) {
        if (cardPositionId == null) {
            return
        }

        val currentImageView = findViewById<ImageView>(cardPositionId)
        currentImageView.foreground = ContextCompat.getDrawable(this, R.drawable.selected_border)
        currentImageView.scaleType = ImageView.ScaleType.CENTER_CROP
    }

    private fun setUnselected(cardPositionId: Int?) {
        if (cardPositionId == null) {
            return
        }

        val currentImageView = findViewById<ImageView>(cardPositionId)
        currentImageView.foreground = null
    }

    private fun newPlayList() {
        playList = HashMap()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragAndDrop() {
        dragSlotIds.forEach { id ->
            val view = findViewById<ImageView>(id)
            view.setOnTouchListener { v, event -> onCardTouch(v, event) }
        }
    }

    private fun onCardTouch(v: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragTouchStartX = event.rawX
                dragTouchStartY = event.rawY
                dragTouchView = v
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragSourceView != null) {
                    updateManualDrag(event.rawX, event.rawY)
                    return true
                }
                if (dragTouchView == v) {
                    val dx = event.rawX - dragTouchStartX
                    val dy = event.rawY - dragTouchStartY
                    if (dx * dx + dy * dy > touchSlop * touchSlop) {
                        if (startManualDrag(v)) {
                            updateManualDrag(event.rawX, event.rawY)
                            return true
                        }
                        dragTouchView = null
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (dragSourceView != null) {
                    finishManualDrag(event.rawX, event.rawY)
                    return true
                }
                dragTouchView = null
            }
            MotionEvent.ACTION_CANCEL -> {
                if (dragSourceView != null) {
                    cancelManualDrag()
                    return true
                }
                dragTouchView = null
            }
        }
        return false
    }

    private fun startManualDrag(v: View): Boolean {
        val cardName = (v.tag as? String) ?: return false
        if (cardName == "zero") return false
        val positionName = resources.getResourceEntryName(v.id).split("subDeck")[1]
        if (isEndDeckClick(positionName)) return false

        clearCardSelection()

        val root = findViewById<ConstraintLayout>(R.id.gameConstraintLayout) ?: return false
        val ghost = ImageView(this).apply {
            setImageDrawable((v as ImageView).drawable)
            scaleType = (v as ImageView).scaleType
            alpha = 0.85f
            elevation = 16f
            layoutParams = ConstraintLayout.LayoutParams(v.width, v.height)
        }
        root.addView(ghost)

        v.alpha = 0.3f

        dragGhost = ghost
        dragSourceView = v
        dragRoot = root
        dragHoverTarget = null
        return true
    }

    private fun updateManualDrag(rawX: Float, rawY: Float) {
        val ghost = dragGhost ?: return
        val root = dragRoot ?: return

        val rootLoc = IntArray(2)
        root.getLocationOnScreen(rootLoc)
        ghost.translationX = rawX - rootLoc[0] - ghost.width / 2f
        ghost.translationY = rawY - rootLoc[1] - ghost.height / 2f

        val target = findSlotUnder(rawX.toInt(), rawY.toInt())
        if (target !== dragHoverTarget) {
            dragHoverTarget?.foreground = null
            if (target != null && target !== dragSourceView) {
                val targetTag = target.tag as? String ?: ""
                val sourceTag = dragSourceView?.tag as? String ?: ""
                val targetPos = resources.getResourceEntryName(target.id).removePrefix("subDeck")
                val isValidDrop = targetTag.isNotEmpty() && sourceTag.isNotEmpty() &&
                    canBeInserted(targetTag, sourceTag, isEndDeckClick(targetPos))
                target.foreground = ContextCompat.getDrawable(
                    this,
                    if (isValidDrop) R.drawable.casino_drop_zone_valid else R.drawable.selected_border
                )
            }
            dragHoverTarget = target
        }
    }

    private fun finishManualDrag(rawX: Float, rawY: Float) {
        val source = dragSourceView
        val target = findSlotUnder(rawX.toInt(), rawY.toInt())

        dragHoverTarget?.foreground = null
        target?.foreground = null
        dragGhost?.let { dragRoot?.removeView(it) }
        source?.alpha = 1f

        dragGhost = null
        dragSourceView = null
        dragHoverTarget = null
        dragRoot = null
        dragTouchView = null

        if (source != null && target != null && source !== target) {
            val moved = tryMove(source, target)
            if (moved) {
                if (hasReachedWonConditions()) {
                    showYouWon()
                } else if (hasReachedLostConditions()) {
                    showYouLost()
                } else {
                    triggerAutoMoveCycle()
                }
            } else {
                findViewById<TextView>(R.id.selectedCardTextView).text =
                    resources.getString(R.string.invaild_move)
            }
        }
    }

    private fun cancelManualDrag() {
        dragHoverTarget?.foreground = null
        dragGhost?.let { dragRoot?.removeView(it) }
        dragSourceView?.alpha = 1f

        dragGhost = null
        dragSourceView = null
        dragHoverTarget = null
        dragRoot = null
        dragTouchView = null
    }

    private fun findSlotUnder(rawX: Int, rawY: Int): View? {
        val loc = IntArray(2)
        for (id in dragSlotIds) {
            val view = findViewById<View>(id) ?: continue
            view.getLocationOnScreen(loc)
            val left = loc[0]
            val top = loc[1]
            val right = left + view.width
            val bottom = top + view.height
            if (rawX in left..right && rawY in top..bottom) {
                return view
            }
        }
        return null
    }

    private fun snapshotToViewModel() {
        gameViewModel.gameStartTimeMillis = gameStartTimeMillis
        gameViewModel.timerPausedTimeMillis = timerPausedTimeMillis
        gameViewModel.isTimerPaused = isTimerPaused
        gameViewModel.coppiedSubDeckMap = HashMap(coppiedSubDeckMap)
        gameViewModel.subDeckMap = HashMap(subDeckMap)
        gameViewModel.cardTableMap = HashMap(cardTableMap)
        gameViewModel.endDeckList = HashMap(endDeckList)
        gameViewModel.playList = HashMap(playList)
        gameViewModel.selectedCard = selectedCard
        gameViewModel.selectedPositionId = selectedPositionId
    }

    private fun restoreFromSavedState(state: com.bottazzini.trasloco.settings.GameStateSnapshot) {
        // Restore activity fields
        gameStartTimeMillis = state.gameStartTimeMillis
        timerPausedTimeMillis = state.timerPausedTimeMillis
        isTimerPaused = state.isTimerPaused
        coppiedSubDeckMap = HashMap(state.coppiedSubDeckMap)
        subDeckMap = HashMap(state.subDeckMap)
        cardTableMap = HashMap(state.cardTableMap)
        endDeckList = HashMap(state.endDeckList)
        playList = HashMap(state.playList)
        cardType = state.cardType

        // Push to ViewModel so config-change survives
        gameViewModel.gameStartTimeMillis = state.gameStartTimeMillis
        gameViewModel.timerPausedTimeMillis = state.timerPausedTimeMillis
        gameViewModel.isTimerPaused = state.isTimerPaused
        gameViewModel.coppiedSubDeckMap = HashMap(state.coppiedSubDeckMap)
        gameViewModel.subDeckMap = HashMap(state.subDeckMap)
        gameViewModel.cardTableMap = HashMap(state.cardTableMap)
        gameViewModel.endDeckList = HashMap(state.endDeckList)
        gameViewModel.playList = HashMap(state.playList)
        gameViewModel.hasActiveGame = state.hasActiveGame
        gameViewModel.gameLost = state.gameLost
        gameViewModel.selectedCard = null
        gameViewModel.selectedPositionId = null

        // Reuse existing render-from-ViewModel pipeline so the visible board
        // (deck backs, table cards, end-deck cards, counters, reset button)
        // is reconstructed from the loaded maps.
        restoreGameFromViewModel()
    }

    private fun restoreGameFromViewModel() {
        isInitializing = true

        prePrepareTable()

        gameStartTimeMillis = gameViewModel.gameStartTimeMillis
        timerPausedTimeMillis = gameViewModel.timerPausedTimeMillis
        isTimerPaused = gameViewModel.isTimerPaused
        coppiedSubDeckMap = HashMap(gameViewModel.coppiedSubDeckMap)
        subDeckMap = HashMap(gameViewModel.subDeckMap)
        cardTableMap = HashMap(gameViewModel.cardTableMap)
        endDeckList = HashMap(gameViewModel.endDeckList)
        playList = HashMap(gameViewModel.playList)
        selectedCard = gameViewModel.selectedCard
        selectedPositionId = gameViewModel.selectedPositionId

        val backCard = settingsHandler.readValue(Configuration.CARD_BACK.value)!!
        for (line in listOf("1", "2", "3", "4")) {
            val deckImageId = resources.getIdentifier("subDeck$line", "id", this.packageName)
            if (subDeckMap[line]?.isNotEmpty() == true) {
                setBackDeckCard(backCard, line)
            } else {
                setImage(deckImageId, "zero")
                val deckView = findViewById<ImageView>(deckImageId)
                deckView.foreground = null
                deckView.isClickable = false
            }
        }

        for ((position, cards) in cardTableMap) {
            val imageViewId = resources.getIdentifier("subDeck$position", "id", this.packageName)
            if (cards.isNotEmpty()) {
                setImage(imageViewId, cards.last())
            }
            if (!isEndDeckClick(position)) {
                setNumberOfCards(cards, position)
            }
        }

        for ((line, card) in endDeckList) {
            val endDeckId = resources.getIdentifier("subDeck${line}4", "id", this.packageName)
            setImage(endDeckId, card)
        }

        if (selectedPositionId != null) {
            setSelected(selectedPositionId)
        }

        val resetButton = findViewById<View>(R.id.resetButton)
        resetButton.isEnabled = playList.isNotEmpty()

        if (gameViewModel.gameLost) {
            showYouLostRestoredUI()
            isTimerPaused = false
        }

        isInitializing = false
    }

    private fun showYouLostRestoredUI() {
        val resetButton = findViewById<View>(R.id.resetButton)
        resetButton.isInvisible = true
        findViewById<TextView>(R.id.selectedCardTextView).isInvisible = true
        findViewById<View>(R.id.lostOverlay).isGone = false
    }

    override fun onDestroy() {
        stopTimer()
        autoMoveRunnable?.let { timerHandler.removeCallbacks(it) }
        if (::gameStateRepo.isInitialized) {
            gameStateRepo.close()
        }
        if (::gameLogRepo.isInitialized) {
            gameLogRepo.close()
        }
        settingsHandler.close()
        super.onDestroy()
    }

    override fun onStop() {
        super.onStop()
        if (::timerRunnable.isInitialized && !isFinishing) {
            pauseTimer()
        }
        if (!isFinishing && gameViewModel.hasActiveGame) {
            snapshotToViewModel()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isTimerPaused) {
            startTimer()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::timerRunnable.isInitialized) {
            pauseTimer()
        }
        if (shouldPersistOnPause && gameViewModel.hasActiveGame && !gameViewModel.gameLost) {
            val snapshot = com.bottazzini.trasloco.settings.GameStateSnapshot(
                version = com.bottazzini.trasloco.settings.GameStateRepository.CURRENT_VERSION,
                hasActiveGame = gameViewModel.hasActiveGame,
                gameLost = gameViewModel.gameLost,
                gameStartTimeMillis = gameStartTimeMillis,
                timerPausedTimeMillis = timerPausedTimeMillis,
                isTimerPaused = isTimerPaused,
                cardType = cardType,
                subDeckMap = subDeckMap,
                cardTableMap = cardTableMap,
                coppiedSubDeckMap = coppiedSubDeckMap,
                endDeckList = endDeckList,
                playList = playList
            )
            gameStateRepo.save(snapshot)
        }
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

    private fun startTimer() {
        if (isTimerPaused) { // Se il timer era in pausa, calcola il nuovo tempo di inizio
            val timeElapsedBeforePause = timerPausedTimeMillis - gameStartTimeMillis
            gameStartTimeMillis = System.currentTimeMillis() - timeElapsedBeforePause
        } else { // Altrimenti, è un nuovo inizio o una ripresa da uno stato non in pausa
            gameStartTimeMillis = System.currentTimeMillis()
        }
        isTimerPaused = false
        timerPausedTimeMillis = 0L // Resetta il tempo di pausa

        if (!::timerRunnable.isInitialized) {
            timerRunnable = object : Runnable {
                override fun run() {
                    if (!isTimerPaused) { // Controlla di nuovo prima di aggiornare e ripianificare
                        val millisPassed = System.currentTimeMillis() - gameStartTimeMillis
                        textViewGameTimer.text = TimeUtils.formatTime(millisPassed)
                        timerHandler.postDelayed(this, 1000) // Aggiorna ogni secondo
                    }
                }
            }
        }
        timerHandler.post(timerRunnable) // Avvia o riavvia il runnable
    }

    private fun pauseTimer() {
        if (::timerRunnable.isInitialized && !isTimerPaused) {
            timerHandler.removeCallbacks(timerRunnable)
            timerPausedTimeMillis = System.currentTimeMillis() // Salva l'ora corrente come ora di pausa
            isTimerPaused = true
        }
    }

    private fun stopTimer() {
        if (::timerRunnable.isInitialized) { // Controlla se timerRunnable è stata inizializzata
            timerHandler.removeCallbacks(timerRunnable)
        }
    }

    private fun abandonCurrentGame() {
        if (!isTutorialMode && gameViewModel.hasActiveGame && !gameViewModel.gameLost) {
            val consecutive = recordsHandler.readValue(Type.CONSECUTIVE)
            if (consecutive != null) {
                recordsHandler.update(Type.CONSECUTIVE, consecutive, 0L, false)
            }
        }
    }

    fun onClickBack(view: View) {
        abandonCurrentGame()
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isTutorialMode) {
            showTutorialExitDialog()
            return
        }
        abandonCurrentGame()
        super.onBackPressed()
    }

    fun onClickPause(view: View) {
        if (isTimerPaused) {
            startTimer()
            findViewById<View>(R.id.pauseOverlay).visibility = View.GONE
        } else {
            pauseTimer()
            findViewById<View>(R.id.pauseOverlay).visibility = View.VISIBLE
        }
    }

    fun onClickResumeFromPause(view: View) {
        if (isTimerPaused) {
            startTimer()
            findViewById<View>(R.id.pauseOverlay).visibility = View.GONE
        }
    }

    fun onClickHint(view: View) {
        if (!hintEnabled) {
            android.widget.Toast.makeText(this, getString(R.string.hint_no_moves), android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val hint = hintEngine.findFirstValidMove(cardTableMap, endDeckList)
        if (hint == null) {
            android.widget.Toast.makeText(this, getString(R.string.hint_no_moves), android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        highlightHintMove(hint)
        hintsUsedThisGame++
    }

    private fun highlightHintMove(hint: HintMove) {
        val sourceView = findViewById<View>(hint.sourceSlotId) ?: return
        val targetView = findViewById<View>(hint.targetSlotId) ?: return
        sourceView.setBackgroundResource(R.drawable.hint_pulse)
        targetView.setBackgroundResource(R.drawable.hint_pulse)
        val animSource = android.animation.ObjectAnimator.ofPropertyValuesHolder(
            sourceView,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.1f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.1f, 1f)
        ).apply { duration = 500; repeatCount = 2 }
        val animTarget = android.animation.ObjectAnimator.ofPropertyValuesHolder(
            targetView,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.1f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.1f, 1f)
        ).apply { duration = 500; repeatCount = 2 }
        animSource.start()
        animTarget.start()
        timerHandler.postDelayed({
            if (!isFinishing) {
                sourceView.setBackgroundResource(R.drawable.casino_slot_frame)
                targetView.setBackgroundResource(R.drawable.casino_slot_frame)
            }
        }, 1500)
    }

    private fun triggerAutoMoveCycle() {
        if (isTutorialMode) return
        if (!autoMoveEnabled) return
        if (dragSourceView != null) {
            // Manual drag in progress: don't steal the dragged card. Retry shortly so the
            // auto-move chain stays alive even if the drag ends with an invalid drop.
            autoMoveRunnable = Runnable { triggerAutoMoveCycle() }.also {
                timerHandler.postDelayed(it, 350)
            }
            return
        }
        val move = findUniquelyPlaceableCard() ?: return
        val (tablePos, endDeckKey) = move
        val endDeckPos = "${endDeckKey}4"
        val sourceId = resources.getIdentifier("subDeck$tablePos", "id", packageName)
        val targetId = resources.getIdentifier("subDeck$endDeckPos", "id", packageName)
        val sourceView = findViewById<android.widget.ImageView>(sourceId) ?: return
        val targetView = findViewById<android.widget.ImageView>(targetId) ?: return

        val moved = tryMove(sourceView, targetView)
        if (moved) {
            autoMovesThisGame++
            if (hasReachedWonConditions()) {
                showYouWon()
                return
            } else if (hasReachedLostConditions()) {
                showYouLost()
                return
            }
            // Continue cycle after short delay (350ms for visual feedback)
            autoMoveRunnable = Runnable { triggerAutoMoveCycle() }.also {
                timerHandler.postDelayed(it, 350)
            }
        }
    }

    private fun findUniquelyPlaceableCard(): Pair<String, String>? {
        for (endDeckKey in listOf("1", "2", "3", "4")) {
            val currentTop = endDeckList[endDeckKey] ?: continue
            if (currentTop == "zero") {
                // Look for an ace on top of any table slot; use this empty endDeck as target
                val aceCard = findAceOnTop() ?: continue
                val tablePos = findCardOnTop(aceCard) ?: continue
                return Pair(tablePos, endDeckKey)
            } else {
                val seme = currentTop.substring(0, 1)
                val number = currentTop.substring(1).toInt()
                if (number >= 10) continue  // Full, skip
                val expectedCard = "$seme${number + 1}"
                val tablePos = findCardOnTop(expectedCard) ?: continue
                return Pair(tablePos, endDeckKey)
            }
        }
        return null
    }

    private fun findAceOnTop(): String? {
        // Returns the ace card name if any ace is on top of a table slot, null otherwise
        for (row in 1..4) {
            for (col in 1..3) {
                val pos = "$row$col"
                val cards = cardTableMap[pos]
                if (!cards.isNullOrEmpty() && cards.last() != "zero") {
                    val card = cards.last()
                    if (card.length > 1 && card.substring(1).toInt() == 1) {
                        return card
                    }
                }
            }
        }
        return null
    }

    private fun findCardOnTop(card: String): String? {
        // Returns the table position key (e.g., "11") where card is on top, or null
        for (row in 1..4) {
            for (col in 1..3) {
                val pos = "$row$col"
                val cards = cardTableMap[pos]
                if (!cards.isNullOrEmpty() && cards.last() == card) {
                    return pos
                }
            }
        }
        return null
    }
}
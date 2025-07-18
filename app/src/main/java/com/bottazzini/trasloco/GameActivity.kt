package com.bottazzini.trasloco

import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isInvisible
import com.bottazzini.trasloco.settings.Configuration
import com.bottazzini.trasloco.settings.RecordsHandler
import com.bottazzini.trasloco.settings.SettingsHandler
import com.bottazzini.trasloco.settings.Type
import com.bottazzini.trasloco.utils.DeckSetup
import com.bottazzini.trasloco.utils.ResourceUtils
import com.bottazzini.trasloco.utils.TimeUtils
import java.util.LinkedList


class GameActivity : AppCompatActivity() {

    private lateinit var settingsHandler: SettingsHandler
    private lateinit var recordsHandler: RecordsHandler
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
    private var enabledFastEndDeckClick = true
    private val timerHandler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable
    private var mediaPlayerAtomic: MediaPlayer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isInitializing = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        setContentView(R.layout.game)
        textViewGameTimer = findViewById(R.id.textViewGameTimer)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        supportActionBar?.hide()
        settingsHandler = SettingsHandler(applicationContext)
        recordsHandler = RecordsHandler(applicationContext)
        if (recordsHandler.readValue(Type.CONSECUTIVE) != null) {
            consecutiveWins = recordsHandler.readValue(Type.CONSECUTIVE)!!
        }

        processSettings()
        startNewGame()
    }

    fun startNewGame(view: View) {
        startNewGame()
    }

    fun startNewGame() {
        isInitializing = true
        playSound(R.raw.shuffle)
        stopTimer()
        prePrepareTable()
        DeckSetup.shuffleDeck()
        DeckSetup.prepareSubDecks()
        subDeckMap = DeckSetup.getSubDeckMap()
        coppiedSubDeckMap = HashMap(subDeckMap)
        prepareTable()
        if (hasReachedLostConditions()) {
            startNewGame()
        }
        startTimer()
        isInitializing = false
    }

    fun retryGame(view: View) {
        isInitializing = true
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
        } else {
            if (canBeInserted(cardName, selectedCard!!, isEndDeckClick(desiredPosition))) {
                moveCard(cardPosition, desiredPosition, selectedCard!!, selectedPositionId!!)
                if (!isEndDeckClick(desiredPosition)) {
                    newPlayList()
                    playList[selectedCard!!] = linkedListOf(cardPosition, selectedPositionId!!)
                } else {
                    val line = desiredPosition.first()
                    endDeckList[line.toString()] = selectedCard!!
                    clearUndoButton()

                    if (enabledFastEndDeckClick) {
                        val selectedPositionName =
                            resources.getResourceEntryName(selectedPositionId!!).split("subDeck")[1]

                        if (cardTableMap[selectedPositionName]!!.isNotEmpty()) {
                            forceCardsEndDeck(
                                selectedPositionId!!,
                                selectedPositionName,
                                cardPosition,
                                line.toString()
                            )
                        }
                    }
                }

                if (hasReachedWonConditions()) {
                    showYouWon()
                    return
                } else if (hasReachedLostConditions()) {
                    showYouLost()
                    return
                }
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
                return
            }

            clearCardSelection()
        }

        val resetButton = findViewById<Button>(R.id.resetButton)
        resetButton.isEnabled = playList.isNotEmpty()
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
        cardTableMap[selectedPositionName]!!.clear()
        setNumberOfCards(cardTableMap[selectedPositionName]!!, selectedPositionName)
        setImage(selectedPositionId, "zero")
        setImage(desiredCardPositionId, lastCard)
        endDeckList[line] = lastCard
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
        } else {
            textView.text = ""
        }
    }

    private fun isEndDeckClick(positionName: String) = positionName.last() == '4'

    private fun canBeInserted(
        sourceCard: String,
        movingCard: String,
        endClickDeck: Boolean
    ): Boolean {
        val sourceSeme = sourceCard.substring(0, 1)
        val movingSeme = movingCard.substring(0, 1)
        val movingNumber = movingCard.substring(1, movingCard.length).toInt()
        if (endClickDeck) {
            return canBeInsertedEndDeck(
                sourceCard = sourceCard,
                sourceSeme = sourceSeme,
                movingSeme = movingSeme,
                movingNumber = movingNumber
            )
        } else {
            if (sourceSeme == movingSeme) {
                val sourceNumber = sourceCard.substring(1, sourceCard.length).toInt()
                if (sourceNumber - 1 == movingNumber) {
                    return true
                }
            }
        }

        return false
    }

    private fun canBeInsertedEndDeck(
        sourceCard: String,
        sourceSeme: String,
        movingSeme: String,
        movingNumber: Int
    ): Boolean {
        if (sourceCard == "zero") {
            return movingNumber == 1
        }

        val sourceNumber = sourceCard.substring(1, sourceCard.length).toInt()
        if (sourceSeme == movingSeme) {
            if (sourceNumber == movingNumber - 1) {
                return true
            }
        }

        return false
    }

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
        val id = ResourceUtils.getDrawableByName(resources, this.packageName, imageName)
        imageView.setImageResource(id)
        imageView.tag = imageName
    }

    private fun getCardName(cardPosition: Int): String {
        val imageView = findViewById<ImageView>(cardPosition)
        return (imageView.tag) as String
    }

    private fun clearUndoButton() {
        findViewById<Button>(R.id.resetButton).isEnabled = false
        newPlayList()
    }

    private fun prepareTextAndButtonForNewGame() {
        findViewById<Button>(R.id.resetButton).isInvisible = false
        findViewById<TextView>(R.id.selectedCardTextView).isInvisible = false
        findViewById<TextView>(R.id.lostTextView).isInvisible = true
        findViewById<Button>(R.id.retryButton).isInvisible = true
        findViewById<Button>(R.id.newGameButton).isInvisible = true
    }

    private fun showYouLost() {
        clearCardSelection()
        findViewById<Button>(R.id.resetButton).isInvisible = true
        findViewById<TextView>(R.id.selectedCardTextView).isInvisible = true
        findViewById<TextView>(R.id.lostTextView).text = resources.getString(R.string.hai_perso)
        findViewById<TextView>(R.id.lostTextView).isInvisible = false
        findViewById<Button>(R.id.retryButton).isInvisible = false
        findViewById<Button>(R.id.newGameButton).isInvisible = false
        stopTimer()
        val consecutive = recordsHandler.readValue(Type.CONSECUTIVE)
        if (consecutive != null) {
            recordsHandler.update(Type.CONSECUTIVE, consecutive, 0L, false)
        }

        playSound(R.raw.youlost)
    }

    private fun playSound(soundId: Int) {
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
        clearCardSelection()
        findViewById<Button>(R.id.resetButton).isInvisible = true
        findViewById<TextView>(R.id.selectedCardTextView).isInvisible = true
        findViewById<Button>(R.id.retryButton).isInvisible = true
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

        val backCardValue = settingsHandler.readValue(Configuration.CARD_BACK.value)
        setBackCards(backCardValue!!)

        val backgroundConf = settingsHandler.readValue(Configuration.BACKGROUND.value)
        val drawable = ResourceUtils.getDrawableByName(resources, this.packageName, backgroundConf!!)
        val layout = findViewById<ConstraintLayout>(R.id.gameConstraintLayout)
        layout.background = ContextCompat.getDrawable(this, drawable)

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

    override fun onDestroy() {
        settingsHandler.close()
        super.onDestroy()
    }

    override fun onStop() {
        super.onStop()
        if (::timerRunnable.isInitialized && !isFinishing) {
            pauseTimer()
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
        if (::timerRunnable.isInitialized && !isFinishing) {
            pauseTimer()
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
}
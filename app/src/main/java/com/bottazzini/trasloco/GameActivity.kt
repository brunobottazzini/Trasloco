package com.bottazzini.trasloco

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bottazzini.trasloco.utils.CardNameTranslator
import com.bottazzini.trasloco.utils.DeckSetup

class GameActivity : AppCompatActivity() {

    private var subDeckMap = HashMap<String, List<String>>()
    private var cardTableMap = HashMap<String, ArrayList<String>>()
    private val playList = HashMap<String, List<Int>>()
    private var selectedCard: String? = null
    private var selectedPositionId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.game)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        supportActionBar?.hide()

        DeckSetup.shuffleDeck()
        DeckSetup.prepareSubDecks()
        subDeckMap = DeckSetup.getSubDeckMap()

        prepareTable()
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
    }

    fun gameCardClick(view: View) {
        val cardPosition = view.id
        val cardName = getCardName(cardPosition)
        val desiredPosition = resources.getResourceEntryName(cardPosition).split("subDeck")[1]
        if (cardName == "zero" && !isEndDeckClick(desiredPosition)) {
            return
        }

        val textView = findViewById<TextView>(R.id.selectedCardTextView)
        if (selectedCard == null) {
            if (isEndDeckClick(desiredPosition)) {
                return
            }
            selectedCard = cardName
            selectedPositionId = view.id
            textView.text = CardNameTranslator.translate(cardName)
        } else {
            if (canBeInserted(cardName, selectedCard!!, isEndDeckClick(desiredPosition))) {
                moveCard(cardPosition, desiredPosition, selectedCard!!, selectedPositionId!!)
                if (!isEndDeckClick(desiredPosition)) {
                    playList[selectedCard!!] = listOf(cardPosition, selectedPositionId!!)
                } else {
                    clearUndoButton()
                }
            }

            textView.text = resources.getString(R.string.nessuna_carta_selezionata)
            selectedPositionId = null
            selectedCard = null
        }

        val resetButton = findViewById<Button>(R.id.resetButton)
        resetButton.isEnabled = playList.isNotEmpty()
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

    private fun canBeInserted(sourceCard: String, movingCard: String, endClickDeck: Boolean): Boolean {
        val sourceSeme = sourceCard.substring(0, 1)
        val movingSeme = movingCard.substring(0, 1)
        val movingNumber = movingCard.substring(1, movingCard.length).toInt()
        if (endClickDeck ) {
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

    private fun getDrawableByName(imageName: String) =
            resources.getIdentifier("drawable/$imageName", "id", this.packageName)

    private fun getTextViewByName(textName: String) =
        resources.getIdentifier("textView$textName", "id", this.packageName)

    private fun prepareTable() {
        arrayOf("1", "2", "3", "4").forEach { line ->
            dealCard(line)
        }
    }

    private fun getSubDeckListConcurrentSafely(line: String) : MutableList<String> {
        val list = ArrayList<String>()
        list.addAll(subDeckMap[line]!!)
        return list
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
        val id = getDrawableByName(imageName)
        imageView.setImageResource(id)
        imageView.tag = imageName
    }

    private fun getCardName(cardPosition: Int): String {
        val imageView = findViewById<ImageView>(cardPosition)
        return (imageView.tag) as String
    }

    private fun clearUndoButton() {
        findViewById<Button>(R.id.resetButton).isEnabled = false
        playList.clear()
    }
}
package com.bottazzini.trasloco

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import kotlin.coroutines.EmptyCoroutineContext

class GameActivity : AppCompatActivity() {
    companion object {
        private val SEMI = listOf<String>("b", "c", "d", "s")
    }

    private var randomDeck: List<String> = getRandomDeck()
    private var subDeckMap: HashMap<String, List<String>> = HashMap()

    private fun getOrderedDeck(): List<String> {
        val orderedDeck = ArrayList<String>()
        for (s in SEMI) {
            for (i in 1..10) {
                orderedDeck.add(s + i)
            }
        }

        return orderedDeck
    }

    private fun getRandomDeck(): List<String> {
        val deck = (getOrderedDeck() as ArrayList)
        deck.shuffle()
        return deck
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.game)

        supportActionBar?.hide()

        prepareGame()
    }

    fun prepareGame() {
        subDeckMap["1"] = randomDeck.subList(0, 9)
        subDeckMap["2"] = randomDeck.subList(10, 19)
        subDeckMap["3"] = randomDeck.subList(20, 29)
        subDeckMap["4"] = randomDeck.subList(30, 39)

        findViewById<ImageView>(R.id.subDeck11).setImageResource(getImageId(subDeckMap["1"]!![0]))
        findViewById<ImageView>(R.id.subDeck12).setImageResource(getImageId(subDeckMap["1"]!![1]))
        findViewById<ImageView>(R.id.subDeck13).setImageResource(getImageId(subDeckMap["1"]!![2]))

        findViewById<ImageView>(R.id.subDeck21).setImageResource(getImageId(subDeckMap["2"]!![0]))
        findViewById<ImageView>(R.id.subDeck22).setImageResource(getImageId(subDeckMap["2"]!![1]))
        findViewById<ImageView>(R.id.subDeck23).setImageResource(getImageId(subDeckMap["2"]!![2]))

        findViewById<ImageView>(R.id.subDeck31).setImageResource(getImageId(subDeckMap["3"]!![0]))
        findViewById<ImageView>(R.id.subDeck32).setImageResource(getImageId(subDeckMap["3"]!![1]))
        findViewById<ImageView>(R.id.subDeck33).setImageResource(getImageId(subDeckMap["3"]!![2]))

        findViewById<ImageView>(R.id.subDeck41).setImageResource(getImageId(subDeckMap["4"]!![0]))
        findViewById<ImageView>(R.id.subDeck42).setImageResource(getImageId(subDeckMap["4"]!![1]))
        findViewById<ImageView>(R.id.subDeck43).setImageResource(getImageId(subDeckMap["4"]!![2]))
    }

    fun subDeckClick(view: View) {
        val id = getImageId(randomDeck[0])

        val imageView = findViewById<ImageView>(R.id.subDeck11)
        imageView.setImageResource(id)
    }

    private fun getImageId(position: String) : Int {
        return resources.getIdentifier("drawable/$position", "id", this.packageName)
    }
}
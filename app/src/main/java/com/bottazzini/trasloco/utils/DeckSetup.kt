package com.bottazzini.trasloco.utils

class DeckSetup {
    companion object {
        private var randomDeck = ArrayList<String>()
        private var subDeckMap = HashMap<String, List<String>>()

        private val SEMI = listOf<String>("b", "c", "d", "s")

        private fun getOrderedDeck(): List<String> {
            val orderedDeck = ArrayList<String>()
            SEMI.forEach { s ->
                for (i in 1..10) {
                    orderedDeck.add(s + i)
                }
            }
            return orderedDeck
        }

        fun getSubDeckMap() = subDeckMap

        fun prepareSubDecks() {
            subDeckMap["1"] = randomDeck.subList(0, 10)
            subDeckMap["2"] = randomDeck.subList(10, 20)
            subDeckMap["3"] = randomDeck.subList(20, 30)
            subDeckMap["4"] = randomDeck.subList(30, 40)
        }

        fun shuffleDeck() {
            val deck = (getOrderedDeck() as ArrayList)
            deck.shuffle()
            randomDeck = deck
        }
    }
}
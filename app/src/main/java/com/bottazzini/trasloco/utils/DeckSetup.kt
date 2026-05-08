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

        fun getRandomDeck() = randomDeck

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

        /**
         * Shuffles until a solvable deal is found. Falls back to last shuffle
         * if no solvable deal is reached within maxAttempts × timeBudgetPerAttemptMs.
         * Returns true if a verified-solvable deal was set, false on fallback.
         */
        fun shuffleSolvable(
            maxAttempts: Int = 10,
            timeBudgetPerAttemptMs: Long = 500
        ): Boolean {
            val solver = TraslocoSolver()
            var lastDeck: ArrayList<String>? = null
            repeat(maxAttempts) {
                val deck = (getOrderedDeck() as ArrayList)
                deck.shuffle()
                lastDeck = deck
                if (solver.isSolvable(deck, timeBudgetPerAttemptMs)) {
                    randomDeck = deck
                    return true
                }
            }
            randomDeck = lastDeck ?: (getOrderedDeck() as ArrayList).also { it.shuffle() }
            return false
        }
    }
}
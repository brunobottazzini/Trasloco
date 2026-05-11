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
         * Sets a deterministic deck for the tutorial. After prepareSubDecks() +
         * dealing 3 cards per line, the top of column 1 slots will be c5, c6, d1
         * and the top of line-2 slot 0 will be d2 (the 4 key cards for the
         * scripted tutorial moves).
         */
        fun setTutorialDeck() {
            randomDeck = arrayListOf(
                // line 1 sub-deck (10 cards): first 3 go to slots 11, 12, 13
                "c5", "c6", "d1",
                "b2", "b3", "b4", "b5", "b6", "b7", "b8",
                // line 2 sub-deck: first 3 go to slots 21, 22, 23 (we only care about 21=d2)
                "d2", "b9", "b10", "c1", "c2", "c3", "c4", "c7", "c8", "c9",
                // line 3 sub-deck
                "c10", "d3", "d4", "d5", "d6", "d7", "d8", "d9", "d10", "s1",
                // line 4 sub-deck
                "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "b1"
            )
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
package com.bottazzini.trasloco.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class DeckSetupTutorialTest {

    @Test
    fun setTutorialDeck_putsKeyCardsOnTopOfDistinctColumns() {
        DeckSetup.setTutorialDeck()
        DeckSetup.prepareSubDecks()
        val map = DeckSetup.getSubDeckMap()

        assertEquals("c5", map["1"]!![0])
        assertEquals("c6", map["1"]!![1])
        assertEquals("d1", map["1"]!![2])
        assertEquals("d2", map["2"]!![0])
    }

    @Test
    fun setTutorialDeck_containsAll40UniqueCards() {
        DeckSetup.setTutorialDeck()
        val deck = DeckSetup.getRandomDeck()
        assertEquals(40, deck.size)
        assertEquals(40, deck.toSet().size)
    }
}

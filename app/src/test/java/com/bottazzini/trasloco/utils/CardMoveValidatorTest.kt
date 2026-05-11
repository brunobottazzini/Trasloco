package com.bottazzini.trasloco.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardMoveValidatorTest {

    // Regression: a stale drag source whose card was moved to the foundation by
    // auto-move carries tag "zero". canBeInserted used to call parseInt("ero") → crash.
    @Test
    fun canBeInserted_returnsFalseWhenMovingCardIsZero() {
        assertFalse(CardMoveValidator.canBeInserted(sourceCard = "c6", movingCard = "zero", endClickDeck = false))
        assertFalse(CardMoveValidator.canBeInserted(sourceCard = "zero", movingCard = "zero", endClickDeck = true))
    }

    @Test
    fun canBeInserted_columnMove_acceptsDescendingSameSuit() {
        // 5 of coppe can be placed on 6 of coppe (descending, same suit)
        assertTrue(CardMoveValidator.canBeInserted(sourceCard = "c6", movingCard = "c5", endClickDeck = false))
    }

    @Test
    fun canBeInserted_columnMove_rejectsDifferentSuit() {
        assertFalse(CardMoveValidator.canBeInserted(sourceCard = "c6", movingCard = "d5", endClickDeck = false))
    }

    @Test
    fun canBeInserted_columnMove_rejectsNonAdjacentRank() {
        assertFalse(CardMoveValidator.canBeInserted(sourceCard = "c6", movingCard = "c4", endClickDeck = false))
    }

    @Test
    fun canBeInserted_endDeck_acceptsAceOnEmpty() {
        assertTrue(CardMoveValidator.canBeInserted(sourceCard = "zero", movingCard = "d1", endClickDeck = true))
    }

    @Test
    fun canBeInserted_endDeck_rejectsNonAceOnEmpty() {
        assertFalse(CardMoveValidator.canBeInserted(sourceCard = "zero", movingCard = "d2", endClickDeck = true))
    }

    @Test
    fun canBeInserted_endDeck_acceptsNextRankSameSuit() {
        // 2 of denari can go on Ace of denari
        assertTrue(CardMoveValidator.canBeInserted(sourceCard = "d1", movingCard = "d2", endClickDeck = true))
    }

    @Test
    fun canBeInserted_endDeck_rejectsWrongSuit() {
        assertFalse(CardMoveValidator.canBeInserted(sourceCard = "d1", movingCard = "c2", endClickDeck = true))
    }

    @Test
    fun canBeInserted_endDeck_rejectsRankSkip() {
        assertFalse(CardMoveValidator.canBeInserted(sourceCard = "d1", movingCard = "d3", endClickDeck = true))
    }
}

package com.bottazzini.trasloco.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardDeckRegistryTest {

    @Test
    fun `byRegion NORD contains piacentine and 11 decks`() {
        val nord = CardDeckRegistry.byRegion(DeckRegion.NORD)
        assertEquals(11, nord.size)
        assertTrue(nord.any { it.id == "piacentine" })
        assertTrue(nord.all { it.region == DeckRegion.NORD })
    }

    @Test
    fun `byRegion SUD_ISOLE contains napoletane sarde siciliane`() {
        val sud = CardDeckRegistry.byRegion(DeckRegion.SUD_ISOLE)
        assertEquals(3, sud.size)
        assertTrue(sud.any { it.id == "napoletane" })
        assertTrue(sud.any { it.id == "sarde" })
        assertTrue(sud.any { it.id == "siciliane" })
    }

    @Test
    fun `byRegion INTERNAZIONALI contains only francesi`() {
        val intl = CardDeckRegistry.byRegion(DeckRegion.INTERNAZIONALI)
        assertEquals(1, intl.size)
        assertEquals("francesi", intl[0].id)
    }

    @Test
    fun `all 15 decks are covered across regions`() {
        val total = DeckRegion.values().sumOf { CardDeckRegistry.byRegion(it).size }
        assertEquals(15, total)
    }

    @Test
    fun `piacentine is first in NORD`() {
        assertEquals("piacentine", CardDeckRegistry.byRegion(DeckRegion.NORD)[0].id)
    }
}

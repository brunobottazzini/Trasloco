package com.bottazzini.trasloco.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.random.Random

class TraslocoSolverTest {

    private val solver = TraslocoSolver()

    private val SUITS = listOf("b", "c", "d", "s")

    private fun orderedDeck(): List<String> = SUITS.flatMap { s -> (1..10).map { "$s$it" } }

    private fun shuffledDeck(seed: Long): List<String> =
        orderedDeck().shuffled(Random(seed))

    @Test
    fun isSolvable_throwsOnDeckTooSmall() {
        try {
            solver.isSolvable(List(39) { "b1" })
            fail("Expected IllegalArgumentException for 39-card deck")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun isSolvable_throwsOnDeckTooLarge() {
        try {
            solver.isSolvable(List(41) { "b1" })
            fail("Expected IllegalArgumentException for 41-card deck")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun isSolvable_returnsTrueOnTriviallyOrderedDeck() {
        // Each sub-deck holds one full suit in 1..10 ascending order.
        // Strategy: move Asso to foundation, deal next, build up. Always solvable.
        val deck = orderedDeck()
        assertTrue(
            "An ordered 1..10 per suit deck must be solvable",
            solver.isSolvable(deck, timeBudgetMs = 2000)
        )
    }

    @Test
    fun isSolvable_isDeterministicOnSameDeck() {
        val deck = shuffledDeck(seed = 12345L)
        val first = solver.isSolvable(deck, timeBudgetMs = 1500)
        val second = solver.isSolvable(deck, timeBudgetMs = 1500)
        assertEquals("Same deck must yield same answer", first, second)
    }

    @Test
    fun isSolvable_returnsFalseWhenBudgetIsNegligible() {
        // With a 1 ms budget, DFS has no time to explore enough states
        // and must conservatively return false on at least some random decks.
        var anyFalse = false
        repeat(5) { i ->
            if (!solver.isSolvable(shuffledDeck(seed = (i + 1).toLong()), timeBudgetMs = 1)) {
                anyFalse = true
            }
        }
        assertTrue("Expected at least one false result with 1ms budget", anyFalse)
    }

    @Test
    fun isSolvable_respectsTimeBudgetRoughly() {
        // Solver should complete close to the budget on hard decks (no infinite loops).
        val deck = shuffledDeck(seed = 42L)
        val start = System.currentTimeMillis()
        solver.isSolvable(deck, timeBudgetMs = 300)
        val elapsed = System.currentTimeMillis() - start
        // Allow 2× slack to accommodate JVM/CI variance.
        assertTrue(
            "Solver elapsed ${elapsed}ms exceeded 600ms (budget=300ms)",
            elapsed <= 600
        )
    }

    @Test
    fun isSolvable_distinguishesSolvableFromUnsolvableAcrossSeeds() {
        // Sanity: across many seeds the solver should not be constant.
        // We don't know the true rate, but with a generous budget on 12 random
        // decks we expect the solver to find at least one solvable AND at least
        // one unsolvable (or timeout-equivalent). This catches a "always true"
        // or "always false" implementation.
        var trueCount = 0
        var falseCount = 0
        repeat(12) { i ->
            val deck = shuffledDeck(seed = (1000L + i))
            if (solver.isSolvable(deck, timeBudgetMs = 400)) trueCount++ else falseCount++
        }
        assertTrue(
            "Expected at least one solvable across 12 seeds (got $trueCount true, $falseCount false)",
            trueCount >= 1
        )
        assertTrue(
            "Expected at least one non-solvable across 12 seeds (got $trueCount true, $falseCount false)",
            falseCount >= 1
        )
    }

}

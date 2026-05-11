package com.bottazzini.trasloco.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialEngineTest {

    private fun stepNoMove(label: String) = TutorialStep(
        label = label,
        instructionResId = 0,
        confirmationResId = null,
        highlightTargets = emptyList(),
        requiredMove = null
    )

    private fun stepMove(label: String, source: String, target: String) = TutorialStep(
        label = label,
        instructionResId = 0,
        confirmationResId = 0,
        highlightTargets = listOf(source, target),
        requiredMove = TutorialMove(source, target)
    )

    @Test
    fun currentStep_returnsFirstStep() {
        val engine = TutorialEngine(listOf(stepNoMove("intro"), stepMove("m1", "c5", "c6")))
        assertEquals("intro", engine.currentStep().label)
    }

    @Test
    fun isMoveAllowed_falseOnIntroStep() {
        val engine = TutorialEngine(listOf(stepNoMove("intro")))
        assertFalse(engine.isMoveAllowed("c5", "c6"))
    }

    @Test
    fun isMoveAllowed_trueWhenMatchesRequired() {
        val engine = TutorialEngine(listOf(stepMove("m1", "c5", "c6")))
        assertTrue(engine.isMoveAllowed("c5", "c6"))
    }

    @Test
    fun isMoveAllowed_falseWhenNotMatchesRequired() {
        val engine = TutorialEngine(listOf(stepMove("m1", "c5", "c6")))
        assertFalse(engine.isMoveAllowed("c5", "c7"))
        assertFalse(engine.isMoveAllowed("c4", "c6"))
    }

    @Test
    fun onMoveExecuted_marksCurrentStepCompleteOnMatch() {
        val engine = TutorialEngine(
            listOf(stepMove("m1", "c5", "c6"), stepMove("m2", "d1", "endDeck1"))
        )
        engine.onMoveExecuted("c5", "c6")
        assertEquals("m1", engine.currentStep().label)
        assertTrue(engine.isCurrentStepComplete())
        engine.advanceToNext()
        assertEquals("m2", engine.currentStep().label)
    }

    @Test
    fun onMoveExecuted_noopWhenNoMatch() {
        val engine = TutorialEngine(
            listOf(stepMove("m1", "c5", "c6"), stepMove("m2", "d1", "endDeck1"))
        )
        engine.onMoveExecuted("d1", "endDeck1")
        assertEquals("m1", engine.currentStep().label)
    }

    @Test
    fun advanceToNext_advancesOnIntroOutro() {
        val engine = TutorialEngine(listOf(stepNoMove("intro"), stepNoMove("outro")))
        engine.advanceToNext()
        assertEquals("outro", engine.currentStep().label)
    }

    @Test
    fun isComplete_trueAfterLastStepAdvanced() {
        val engine = TutorialEngine(listOf(stepNoMove("intro"), stepNoMove("outro")))
        assertFalse(engine.isComplete())
        engine.advanceToNext()
        assertFalse(engine.isComplete())
        engine.advanceToNext()
        assertTrue(engine.isComplete())
    }

    @Test
    fun isCurrentStepComplete_falseBeforeMove_trueAfterMove() {
        val engine = TutorialEngine(listOf(stepMove("m1", "c5", "c6")))
        assertFalse(engine.isCurrentStepComplete())
        engine.onMoveExecuted("c5", "c6")
        assertTrue(engine.isCurrentStepComplete())
    }

    @Test
    fun isStepComplete_trueOnNoMoveStep() {
        val engine = TutorialEngine(listOf(stepNoMove("intro")))
        // intro/outro have no required move, considered complete (so "Avanti" can show)
        assertTrue(engine.isCurrentStepComplete())
    }

    @Test
    fun isMoveAllowed_falseWhenComplete() {
        val engine = TutorialEngine(listOf(stepMove("m1", "c5", "c6")))
        engine.onMoveExecuted("c5", "c6")
        engine.advanceToNext()
        assertTrue(engine.isComplete())
        assertFalse(engine.isMoveAllowed("c5", "c6"))
    }

    @Test
    fun advanceToNext_doesNotSkipUncompletedMoveStep() {
        val engine = TutorialEngine(
            listOf(stepMove("m1", "c5", "c6"), stepMove("m2", "d1", "endDeck1"))
        )
        engine.advanceToNext()
        assertEquals("m1", engine.currentStep().label)
    }
}

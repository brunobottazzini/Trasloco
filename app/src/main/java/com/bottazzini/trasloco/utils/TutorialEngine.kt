package com.bottazzini.trasloco.utils

/**
 * A single guided step in the tutorial.
 *
 * @param label internal id, useful for tests and logs.
 * @param instructionResId string resource shown in the banner while this step is active.
 * @param confirmationResId string resource shown after the required move is executed
 *   (null for intro/outro steps that have no required move).
 * @param highlightTargets card tags (e.g. "c5") or slot names (e.g. "endDeck1") that the
 *   UI should pulse-highlight while this step is active.
 * @param requiredMove the only move the player can execute during this step; null means
 *   the step is informational and advances via the "Avanti" button.
 */
data class TutorialStep(
    val label: String,
    val instructionResId: Int,
    val confirmationResId: Int?,
    val highlightTargets: List<String>,
    val requiredMove: TutorialMove?
)

data class TutorialMove(
    val source: String,
    val target: String
)

class TutorialEngine(private val steps: List<TutorialStep>) {

    private var index = 0
    private var lastMoveExecuted = false

    fun currentStep(): TutorialStep = steps[index.coerceAtMost(steps.lastIndex)]

    fun isComplete(): Boolean = index >= steps.size

    /** True when the current step is informational (no move) or its required move has been executed. */
    fun isCurrentStepComplete(): Boolean {
        if (isComplete()) return true
        val step = currentStep()
        return step.requiredMove == null || lastMoveExecuted
    }

    fun isMoveAllowed(source: String, target: String): Boolean {
        if (isComplete()) return false
        val required = currentStep().requiredMove ?: return false
        return required.source == source && required.target == target
    }

    fun onMoveExecuted(source: String, target: String) {
        if (!isMoveAllowed(source, target)) return
        lastMoveExecuted = true
    }

    fun advanceToNext() {
        if (isComplete()) return
        val step = currentStep()
        if (step.requiredMove != null && !lastMoveExecuted) return
        index++
        lastMoveExecuted = false
    }
}

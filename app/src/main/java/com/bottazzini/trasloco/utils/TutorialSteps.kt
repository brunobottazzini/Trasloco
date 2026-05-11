package com.bottazzini.trasloco.utils

import com.bottazzini.trasloco.R

object TutorialSteps {

    /**
     * Slot identifiers used as targets in `TutorialMove` for foundation drops.
     * The actual Android view id is resolved at runtime in GameActivity
     * (e.g. "endDeck1" → R.id.subDeck14).
     */
    const val FOUNDATION_1 = "endDeck1"

    fun build(): List<TutorialStep> = listOf(
        TutorialStep(
            label = "intro",
            instructionResId = R.string.tutorial_step_intro,
            confirmationResId = null,
            highlightTargets = emptyList(),
            requiredMove = null
        ),
        TutorialStep(
            label = "move_c5_to_c6",
            instructionResId = R.string.tutorial_step1_instruction,
            confirmationResId = R.string.tutorial_step1_confirm,
            highlightTargets = listOf("c5", "c6"),
            requiredMove = TutorialMove(source = "c5", target = "c6")
        ),
        TutorialStep(
            label = "ace_to_foundation",
            instructionResId = R.string.tutorial_step2_instruction,
            confirmationResId = R.string.tutorial_step2_confirm,
            highlightTargets = listOf("d1", FOUNDATION_1),
            requiredMove = TutorialMove(source = "d1", target = FOUNDATION_1)
        ),
        TutorialStep(
            label = "two_to_foundation",
            instructionResId = R.string.tutorial_step3_instruction,
            confirmationResId = R.string.tutorial_step3_confirm,
            highlightTargets = listOf("d2", FOUNDATION_1),
            requiredMove = TutorialMove(source = "d2", target = FOUNDATION_1)
        ),
        TutorialStep(
            label = "outro",
            instructionResId = R.string.tutorial_step_outro,
            confirmationResId = null,
            highlightTargets = emptyList(),
            requiredMove = null
        )
    )
}

package com.bottazzini.trasloco.utils

/**
 * Pure move-validation logic for Trasloco. Cards are encoded as "<suit><rank>"
 * (e.g. "c5" = 5 of Coppe); the placeholder "zero" represents an empty slot.
 */
object CardMoveValidator {

    fun canBeInserted(
        sourceCard: String,
        movingCard: String,
        endClickDeck: Boolean
    ): Boolean {
        // Defense against the auto-move-vs-drag race: a drag source whose card was
        // stolen by auto-move carries tag "zero". Reject silently instead of crashing
        // on parseInt("ero").
        if (movingCard == "zero") return false

        val sourceSeme = sourceCard.substring(0, 1)
        val movingSeme = movingCard.substring(0, 1)
        val movingNumber = movingCard.substring(1, movingCard.length).toInt()

        if (endClickDeck) {
            return canBeInsertedEndDeck(sourceCard, sourceSeme, movingSeme, movingNumber)
        }

        if (sourceSeme == movingSeme) {
            val sourceNumber = sourceCard.substring(1, sourceCard.length).toInt()
            if (sourceNumber - 1 == movingNumber) {
                return true
            }
        }
        return false
    }

    private fun canBeInsertedEndDeck(
        sourceCard: String,
        sourceSeme: String,
        movingSeme: String,
        movingNumber: Int
    ): Boolean {
        if (sourceCard == "zero") {
            return movingNumber == 1
        }

        val sourceNumber = sourceCard.substring(1, sourceCard.length).toInt()
        if (sourceSeme == movingSeme && sourceNumber == movingNumber - 1) {
            return true
        }
        return false
    }
}

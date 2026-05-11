package com.bottazzini.trasloco

import android.content.Context

data class HintMove(
    val sourceCardId: String,
    val sourceSlotId: Int,
    val targetSlotId: Int,
    val targetType: HintTargetType
)

enum class HintTargetType { END_DECK, TABLE_STACK }

class HintEngine(private val context: Context) {

    fun findFirstValidMove(
        cardTableMap: Map<String, List<String>>,
        endDeckList: Map<String, String>
    ): HintMove? {
        // Step 1: try each top card in playable table slots → each endDeck
        for (row in 1..4) {
            for (col in 1..3) {
                val position = "$row$col"
                val cards = cardTableMap[position]
                if (cards.isNullOrEmpty()) continue
                val topCard = cards.last()
                if (topCard == "zero") continue

                for (endDeckKey in listOf("1", "2", "3", "4")) {
                    val endDeckTopCard = endDeckList[endDeckKey] ?: "zero"
                    val endDeckPosition = "${endDeckKey}4"
                    if (canBeInserted(endDeckTopCard, topCard, isEndDeckPosition = true)) {
                        return HintMove(
                            sourceCardId = topCard,
                            sourceSlotId = resolveSlotId("subDeck$position"),
                            targetSlotId = resolveSlotId("subDeck$endDeckPosition"),
                            targetType = HintTargetType.END_DECK
                        )
                    }
                }
            }
        }

        // Step 2: try each top card → another table slot
        for (row in 1..4) {
            for (col in 1..3) {
                val sourcePosition = "$row$col"
                val sourceCards = cardTableMap[sourcePosition]
                if (sourceCards.isNullOrEmpty()) continue
                val topCard = sourceCards.last()
                if (topCard == "zero") continue

                for (row2 in 1..4) {
                    for (col2 in 1..3) {
                        if (row == row2 && col == col2) continue
                        val targetPosition = "$row2$col2"
                        val targetCards = cardTableMap[targetPosition]
                        if (targetCards.isNullOrEmpty()) continue
                        val targetTop = targetCards.last()
                        if (targetTop == "zero") continue

                        if (canBeInserted(targetTop, topCard, isEndDeckPosition = false)) {
                            return HintMove(
                                sourceCardId = topCard,
                                sourceSlotId = resolveSlotId("subDeck$sourcePosition"),
                                targetSlotId = resolveSlotId("subDeck$targetPosition"),
                                targetType = HintTargetType.TABLE_STACK
                            )
                        }
                    }
                }
            }
        }

        return null
    }

    private fun canBeInserted(targetCard: String, movingCard: String, isEndDeckPosition: Boolean): Boolean {
        val movingSeme = movingCard.substring(0, 1)
        val movingNumber = movingCard.substring(1).toInt()

        return if (isEndDeckPosition) {
            if (targetCard == "zero") {
                movingNumber == 1
            } else {
                val targetSeme = targetCard.substring(0, 1)
                val targetNumber = targetCard.substring(1).toInt()
                targetSeme == movingSeme && targetNumber + 1 == movingNumber
            }
        } else {
            if (targetCard == "zero") return false
            val targetSeme = targetCard.substring(0, 1)
            val targetNumber = targetCard.substring(1).toInt()
            targetSeme == movingSeme && targetNumber - 1 == movingNumber
        }
    }

    private fun resolveSlotId(slotName: String): Int =
        context.resources.getIdentifier(slotName, "id", context.packageName)
}

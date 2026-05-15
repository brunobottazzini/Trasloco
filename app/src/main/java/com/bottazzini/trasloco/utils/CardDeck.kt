package com.bottazzini.trasloco.utils

import com.bottazzini.trasloco.R

data class CardDeck(val id: String, val labelRes: Int, val available: Boolean = true)

object CardDeckRegistry {
    val ALL = listOf(
        CardDeck("piacentine",  R.string.card_type_piacentine),
        CardDeck("napoletane",  R.string.card_type_napoletane),
        CardDeck("francesi",    R.string.card_type_francesi),
        CardDeck("bergamasche", R.string.card_type_bergamasche, available = false),
        CardDeck("siciliane",   R.string.card_type_siciliane,   available = false),
        CardDeck("trevisane",   R.string.card_type_trevisane,   available = false),
        CardDeck("bresciane",   R.string.card_type_bresciane,   available = false),
        CardDeck("sarde",       R.string.card_type_sarde,       available = false),
    )

    fun indexOf(id: String): Int = ALL.indexOfFirst { it.id == id }.coerceAtLeast(0)
}

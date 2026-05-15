package com.bottazzini.trasloco.utils

import com.bottazzini.trasloco.R

data class CardDeck(
    val id: String,
    val labelRes: Int,
    val available: Boolean = true,
    val insetX: Float = 0f,
    val insetY: Float = 0f,
)

object CardDeckRegistry {
    val ALL = listOf(
        CardDeck("piacentine",  R.string.card_type_piacentine,  insetY = 0.06f),
        CardDeck("napoletane",  R.string.card_type_napoletane,  insetY = 0.14f),
        CardDeck("francesi",    R.string.card_type_francesi,    insetY = 0.14f),
        CardDeck("bergamasche", R.string.card_type_bergamasche),
        CardDeck("bolognesi",   R.string.card_type_bolognesi,   insetX = 0.04f),
        CardDeck("bresciane",   R.string.card_type_bresciane,   insetX = 0.11f),
        CardDeck("genovesi",    R.string.card_type_genovesi,    insetY = 0.14f),
        CardDeck("milanesi",    R.string.card_type_milanesi),
        CardDeck("piemontesi",  R.string.card_type_piemontesi,  insetY = 0.10f),
        CardDeck("romagnole",   R.string.card_type_romagnole,   insetY = 0.06f),
        CardDeck("sarde",       R.string.card_type_sarde,       insetY = 0.14f),
        CardDeck("siciliane",   R.string.card_type_siciliane,   insetY = 0.14f),
        CardDeck("trentine",    R.string.card_type_trentine,    insetY = 0.06f),
        CardDeck("trevisane",   R.string.card_type_trevisane),
        CardDeck("triestine",   R.string.card_type_triestine),
    )

    fun indexOf(id: String): Int = ALL.indexOfFirst { it.id == id }.coerceAtLeast(0)
    fun byId(id: String): CardDeck = ALL.firstOrNull { it.id == id } ?: ALL[0]
}

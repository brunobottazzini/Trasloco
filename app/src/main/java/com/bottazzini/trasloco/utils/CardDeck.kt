package com.bottazzini.trasloco.utils

import com.bottazzini.trasloco.R

data class CardDeck(
    val id: String,
    val labelRes: Int,
    val available: Boolean = true,
    val insetX: Float = 0f,
    val insetY: Float = 0f,
    val region: DeckRegion = DeckRegion.NORD,
)

object CardDeckRegistry {
    val ALL = listOf(
        CardDeck("piacentine",  R.string.card_type_piacentine,  region = DeckRegion.NORD),
        CardDeck("bergamasche", R.string.card_type_bergamasche, region = DeckRegion.NORD),
        CardDeck("bolognesi",   R.string.card_type_bolognesi,   insetX = 0.04f, region = DeckRegion.NORD),
        CardDeck("bresciane",   R.string.card_type_bresciane,   insetX = 0.11f, region = DeckRegion.NORD),
        CardDeck("genovesi",    R.string.card_type_genovesi,    insetY = 0.14f, region = DeckRegion.NORD),
        CardDeck("milanesi",    R.string.card_type_milanesi,    region = DeckRegion.NORD),
        CardDeck("piemontesi",  R.string.card_type_piemontesi,  insetY = 0.10f, region = DeckRegion.NORD),
        CardDeck("romagnole",   R.string.card_type_romagnole,   insetY = 0.06f, region = DeckRegion.NORD),
        CardDeck("trentine",    R.string.card_type_trentine,    insetY = 0.06f, region = DeckRegion.NORD),
        CardDeck("trevisane",   R.string.card_type_trevisane,   region = DeckRegion.NORD),
        CardDeck("triestine",   R.string.card_type_triestine,   region = DeckRegion.NORD),
        CardDeck("napoletane",  R.string.card_type_napoletane,  insetY = 0.14f, region = DeckRegion.SUD_ISOLE),
        CardDeck("sarde",       R.string.card_type_sarde,       insetY = 0.14f, region = DeckRegion.SUD_ISOLE),
        CardDeck("siciliane",   R.string.card_type_siciliane,   insetY = 0.14f, region = DeckRegion.SUD_ISOLE),
        CardDeck("francesi",    R.string.card_type_francesi,    region = DeckRegion.INTERNAZIONALI),
    )

    fun indexOf(id: String): Int = ALL.indexOfFirst { it.id == id }.coerceAtLeast(0)
    fun byId(id: String): CardDeck = ALL.firstOrNull { it.id == id } ?: ALL[0]
    fun byRegion(region: DeckRegion): List<CardDeck> = ALL.filter { it.region == region }
}

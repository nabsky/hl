package com.zorindisplays.display.model

enum class Suit(val code: String, val symbol: String) {
    CLUBS("c", "♣"),
    DIAMONDS("d", "♦"),
    HEARTS("h", "♥"),
    SPADES("s", "♠")
}

enum class Rank(val value: Int, val fileCode: String, val label: String) {
    TWO(2, "2", "2"),
    THREE(3, "3", "3"),
    FOUR(4, "4", "4"),
    FIVE(5, "5", "5"),
    SIX(6, "6", "6"),
    SEVEN(7, "7", "7"),
    EIGHT(8, "8", "8"),
    NINE(9, "9", "9"),
    TEN(10, "10", "10"),
    JACK(11, "j", "J"),
    QUEEN(12, "q", "Q"),
    KING(13, "k", "K"),
    ACE(14, "a", "A")
}

data class Card(
    val suit: Suit,
    val rank: Rank
) {
    fun assetUrl(): String = "file:///android_asset/cards/card_${rank.fileCode}${suit.code}.svg"
    val display: String get() = "${rank.label}${suit.symbol}"
}

fun cardBackAssetUrl(): String = "file:///android_asset/cards/card_back.svg"

enum class Guess { HIGHER, LOWER }

enum class CompareResult { HIGHER, LOWER, TIE }

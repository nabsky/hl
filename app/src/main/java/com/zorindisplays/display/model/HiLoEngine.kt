package com.zorindisplays.display.model

import kotlin.random.Random

object HiLoEngine {

    fun drawFiveCards(rng: Random = Random.Default): List<Card> {
        val deck = buildDeck().shuffled(rng)
        return deck.take(5)
    }

    fun compare(prev: Card, next: Card): CompareResult {
        return when {
            next.rank.value > prev.rank.value -> CompareResult.HIGHER
            next.rank.value < prev.rank.value -> CompareResult.LOWER
            else -> CompareResult.TIE
        }
    }

    fun isCorrect(guess: Guess, prev: Card, next: Card): Boolean {
        return when (compare(prev, next)) {
            CompareResult.TIE -> false // tie is neither win nor lose
            CompareResult.HIGHER -> guess == Guess.HIGHER
            CompareResult.LOWER -> guess == Guess.LOWER
        }
    }

    private fun buildDeck(): List<Card> {
        val suits = Suit.entries
        val ranks = Rank.entries
        val out = ArrayList<Card>(52)
        for (s in suits) for (r in ranks) out.add(Card(s, r))
        return out
    }
}

package com.zorindisplays.hilo.emulator

import com.zorindisplays.hilo.model.Card
import com.zorindisplays.hilo.model.Guess
import com.zorindisplays.hilo.model.Rank
import com.zorindisplays.hilo.model.Suit

class Emulator {

    fun chooseOptimalGuess(
        currentCard: Card,
        revealedCards: List<Card>
    ): Guess {
        val usedCards = revealedCards.toSet()

        var higherCount = 0
        var lowerCount = 0

        for (rank in Rank.values()) {
            for (suit in Suit.values()) {
                val candidate = Card(rank = rank, suit = suit)
                if (candidate in usedCards) continue

                when {
                    rank.ordinal > currentCard.rank.ordinal -> higherCount++
                    rank.ordinal < currentCard.rank.ordinal -> lowerCount++
                }
            }
        }

        return if (higherCount >= lowerCount) {
            Guess.HIGHER
        } else {
            Guess.LOWER
        }
    }
}

package com.zorindisplays.display.model

sealed class UiState {

    data object Idle : UiState()

    data class AmountEntry(
        val raw: String
    ) : UiState()

    data class Ready(
        val amount: Long,
        val cards: List<Card>
    ) : UiState()

    data class Playing(
        val amount: Long,
        val cards: List<Card>,
        val revealedCount: Int, // 1..5
        val awaitingGuess: Boolean,
        val showConfetti: Boolean = false,
        val confettiTick: Int = 0,
        val playCoinSound: Boolean = false // флаг для сигнала о воспроизведении звука
    ) : UiState()

    data class Lost(
        val lastAmount: Long,
        val cards: List<Card>,
        val revealedCount: Int
    ) : UiState()

    data class Won(
        val amount: Long,
        val cards: List<Card>,
        val playWinnerSound: Boolean = false // флаг для сигнала о воспроизведении winner.wav
    ) : UiState()
}

package com.zorindisplays.display.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import com.zorindisplays.display.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "HiLoGame"

class GameViewModel : ViewModel() {

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state

    // блокируем ввод на время анимаций (пока без корутин-анимаций, но флаг уже есть)
    private var inputLocked: Boolean = false

    private var confettiJob: Job? = null

    fun onDigit(d: Int) {
        if (inputLocked) return
        require(d in 0..9)

        _state.update { st ->
            when (st) {
                UiState.Idle -> UiState.AmountEntry(raw = d.toString())
                is UiState.AmountEntry -> {
                    val newRaw = (st.raw + d.toString()).trimStart('0')
                    UiState.AmountEntry(raw = if (newRaw.isEmpty()) "0" else newRaw)
                }
                is UiState.Ready,
                is UiState.Playing,
                is UiState.Lost,
                is UiState.Won -> UiState.AmountEntry(raw = d.toString()) // начать новый раунд вводом
            }
        }
    }

    fun onBackspace() {
        if (inputLocked) return

        _state.update { st ->
            when (st) {
                is UiState.AmountEntry -> {
                    val newRaw = st.raw.dropLast(1)
                    if (newRaw.isBlank()) UiState.Idle else UiState.AmountEntry(newRaw)
                }
                else -> st
            }
        }
    }

    fun onEnter() {
        if (inputLocked) return

        _state.update { st ->
            when (st) {
                is UiState.AmountEntry -> {
                    val amount = st.raw.toLongOrNull() ?: 0L
                    if (amount <= 0L) UiState.Idle
                    else UiState.Ready(amount = amount, cards = HiLoEngine.drawFiveCards())
                }
                is UiState.Lost -> UiState.Idle
                is UiState.Won -> UiState.Idle
                is UiState.Ready -> st
                is UiState.Playing -> st
                UiState.Idle -> UiState.Idle
            }
        }
    }

    fun onStart() {
        if (inputLocked) return

        _state.update { st ->
            when (st) {
                is UiState.Ready -> {
                    logCards(st.cards)

                    UiState.Playing(
                        amount = st.amount,
                        cards = st.cards,
                        revealedCount = 1,
                        awaitingGuess = true
                    )
                }
                else -> st
            }
        }
    }

    fun onGuess(guess: Guess) {
        if (inputLocked) return

        val st = _state.value
        if (st !is UiState.Playing) return
        if (!st.awaitingGuess) return
        if (st.revealedCount >= 5) return

        val prevIndex = st.revealedCount - 1
        val nextIndex = st.revealedCount
        val prev = st.cards[prevIndex]
        val next = st.cards[nextIndex]

        val cmp = HiLoEngine.compare(prev, next)
        when (cmp) {
            CompareResult.TIE -> {
                // просто открываем следующую карту, сумма без изменений
                val newRevealed = st.revealedCount + 1
                if (newRevealed >= 5) {
                    _state.value = UiState.Won(amount = st.amount, cards = st.cards)
                } else {
                    _state.value = st.copy(revealedCount = newRevealed, awaitingGuess = true)
                }
            }
            CompareResult.HIGHER, CompareResult.LOWER -> {
                val correct = HiLoEngine.isCorrect(guess, prev, next)
                if (!correct) {
                    _state.value = UiState.Lost(
                        lastAmount = st.amount,
                        cards = st.cards,
                        revealedCount = st.revealedCount + 1
                    )
                } else {
                    // правильный ответ
                    val doubled = st.amount * 2L
                    val newRevealed = st.revealedCount + 1

                    if (newRevealed >= 5) {
                        _state.value = UiState.Won(amount = doubled, cards = st.cards)
                    } else {
                        _state.update { cur ->
                            val p = cur as? UiState.Playing ?: return@update cur
                            p.copy(
                                amount = doubled,
                                revealedCount = newRevealed,
                                awaitingGuess = true,
                                showConfetti = true,
                                confettiTick = p.confettiTick + 1
                            )
                        }
                        scheduleConfettiOff()
                    }
                }
            }
        }
    }

    private fun scheduleConfettiOff() {
        confettiJob?.cancel()
        confettiJob = viewModelScope.launch {
            delay(1200)
            _state.update { st ->
                if (st is UiState.Playing) st.copy(showConfetti = false) else st
            }
        }
    }

    private fun logCards(cards: List<Card>) {
        Log.d(TAG, "HiLo Round started")
        cards.forEachIndexed { index, card ->
            Log.d(TAG, "[${index + 1}] ${card.rank.label}${card.suit.symbol}")
        }
    }

}

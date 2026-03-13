package com.zorindisplays.display.ui

import android.content.Context
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
import java.util.Locale

private const val TAG = "HiLoGame"

enum class DeckMode {
    RANDOM_DECK,
    FIXED_RTP
}

private const val PREFS_NAME = "hilo_settings"
private const val KEY_DECK_MODE = "deck_mode"
private const val KEY_FIXED_RTP = "fixed_rtp"

class GameViewModel : ViewModel() {

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state

    private val _deckMode = MutableStateFlow(DeckMode.RANDOM_DECK)
    val deckMode: StateFlow<DeckMode> = _deckMode

    private val _fixedRtpInput = MutableStateFlow("98.00")
    val fixedRtpInput: StateFlow<String> = _fixedRtpInput

    private var settingsLoaded = false

    private var inputLocked: Boolean = false
    private var confettiJob: Job? = null

    fun loadSettings(context: Context) {
        if (settingsLoaded) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val modeName = prefs.getString(KEY_DECK_MODE, DeckMode.RANDOM_DECK.name)
        val rtp = prefs.getString(KEY_FIXED_RTP, "98.00") ?: "98.00"

        _deckMode.value = try {
            DeckMode.valueOf(modeName ?: DeckMode.RANDOM_DECK.name)
        } catch (_: Exception) {
            DeckMode.RANDOM_DECK
        }

        _fixedRtpInput.value = formatRtp(rtp)
        settingsLoaded = true
    }

    private fun saveSettings(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_DECK_MODE, _deckMode.value.name)
            .putString(KEY_FIXED_RTP, _fixedRtpInput.value)
            .apply()
    }

    fun setDeckMode(mode: DeckMode, context: Context? = null) {
        _deckMode.value = mode
        if (context != null) saveSettings(context)
    }

    fun setFixedRtpInput(value: String) {
        val normalized = value.replace(',', '.')
        if (normalized.count { it == '.' } > 1) return

        val filtered = buildString {
            normalized.forEach { ch ->
                if (ch.isDigit() || ch == '.') append(ch)
            }
        }

        val parts = filtered.split('.')
        val sanitized = when {
            parts.size == 1 -> parts[0].take(6)
            else -> {
                val intPart = parts[0].take(6)
                val fracPart = parts[1].take(2)
                "$intPart.$fracPart"
            }
        }

        _fixedRtpInput.value = sanitized
    }

    fun commitFixedRtp(value: String, context: Context? = null) {
        _fixedRtpInput.value = formatRtp(value)
        if (context != null) saveSettings(context)
    }

    fun getFixedRtpOrDefault(): Double {
        val parsed = _fixedRtpInput.value.toDoubleOrNull() ?: 98.0
        return parsed.coerceIn(0.00, 100.00)
    }

    private fun formatRtp(value: String): String {
        val parsed = value.replace(',', '.').toDoubleOrNull() ?: 98.0
        return String.format(Locale.US, "%.2f", parsed.coerceIn(0.00, 100.00))
    }

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
                is UiState.Won -> UiState.AmountEntry(raw = d.toString())
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
                val newRevealed = st.revealedCount + 1
                if (newRevealed >= 5) {
                    _state.value = UiState.Won(amount = st.amount, cards = st.cards, playWinnerSound = true)
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
                    val doubled = st.amount * 2L
                    val newRevealed = st.revealedCount + 1

                    if (newRevealed >= 5) {
                        _state.value = UiState.Won(amount = doubled, cards = st.cards, playWinnerSound = true)
                    } else {
                        _state.update { cur ->
                            val p = cur as? UiState.Playing ?: return@update cur
                            p.copy(
                                amount = doubled,
                                revealedCount = newRevealed,
                                awaitingGuess = true,
                                showConfetti = true,
                                confettiTick = p.confettiTick + 1,
                                playCoinSound = true
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
        Log.d(TAG, "Deck mode = ${_deckMode.value}, fixed RTP = ${_fixedRtpInput.value}")
        cards.forEachIndexed { index, card ->
            Log.d(TAG, "[${index + 1}] ${card.rank.label}${card.suit.symbol}")
        }
    }

    fun onCoinSoundPlayed() {
        _state.update { st ->
            if (st is UiState.Playing && st.playCoinSound) st.copy(playCoinSound = false) else st
        }
    }

    fun onWinnerSoundPlayed() {
        _state.update { st ->
            if (st is UiState.Won && st.playWinnerSound) st.copy(playWinnerSound = false) else st
        }
    }
}
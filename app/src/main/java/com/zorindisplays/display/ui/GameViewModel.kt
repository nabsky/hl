package com.zorindisplays.display.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zorindisplays.display.model.Card
import com.zorindisplays.display.model.CompareResult
import com.zorindisplays.display.model.Guess
import com.zorindisplays.display.model.HiLoEngine
import com.zorindisplays.display.model.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

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

    // блокируем ввод на время анимаций
    private var inputLocked: Boolean = false
    private var confettiJob: Job? = null

    // Fixed RTP round state
    private var fixedShouldWin: Boolean? = null
    private var fixedLoseStep: Int = 4 // проиграть на каком угадывании: 2..4
    private var fixedUsedCards: MutableSet<Card> = mutableSetOf()

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
                    if (amount <= 0L) {
                        UiState.Idle
                    } else {
                        when (_deckMode.value) {
                            DeckMode.RANDOM_DECK -> {
                                resetFixedRoundState()
                                UiState.Ready(
                                    amount = amount,
                                    cards = HiLoEngine.drawFiveCards()
                                )
                            }

                            DeckMode.FIXED_RTP -> {
                                prepareFixedRtpRound()
                                UiState.Ready(
                                    amount = amount,
                                    cards = buildFixedRtpInitialCards()
                                )
                            }
                        }
                    }
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

        when (_deckMode.value) {
            DeckMode.RANDOM_DECK -> handleRandomDeckGuess(st, guess)
            DeckMode.FIXED_RTP -> handleFixedRtpGuess(st, guess)
        }
    }

    private fun handleRandomDeckGuess(st: UiState.Playing, guess: Guess) {
        val prevIndex = st.revealedCount - 1
        val nextIndex = st.revealedCount
        val prev = st.cards[prevIndex]
        val next = st.cards[nextIndex]

        resolveGuess(
            st = st,
            guess = guess,
            prev = prev,
            next = next,
            updatedCards = st.cards
        )
    }

    private fun handleFixedRtpGuess(st: UiState.Playing, guess: Guess) {
        val prevIndex = st.revealedCount - 1
        val nextIndex = st.revealedCount
        val prev = st.cards[prevIndex]

        val newCards = st.cards.toMutableList()

        val next = when {
            // 1-я догадка: карта №2 уже сгенерирована заранее случайно
            nextIndex == 1 -> {
                val existing = newCards[nextIndex]
                fixedUsedCards.add(existing)
                existing
            }

            else -> {
                val shouldWinRound = fixedShouldWin ?: false
                val currentGuessStep = nextIndex // 2..4 для карт 3..5

                when {
                    shouldWinRound -> {
                        val wantHigher = guess == Guess.HIGHER
                        val generated = drawCardRelativeTo(
                            prev = prev,
                            wantHigher = wantHigher,
                            exclude = fixedUsedCards
                        )
                        newCards[nextIndex] = generated
                        fixedUsedCards.add(generated)
                        generated
                    }

                    currentGuessStep == fixedLoseStep -> {
                        val wantHigher = guess != Guess.HIGHER
                        val generated = drawCardRelativeTo(
                            prev = prev,
                            wantHigher = wantHigher,
                            exclude = fixedUsedCards
                        )
                        newCards[nextIndex] = generated
                        fixedUsedCards.add(generated)
                        generated
                    }

                    else -> {
                        val generated = drawRandomNonTieCardRelativeTo(
                            prev = prev,
                            exclude = fixedUsedCards
                        )
                        newCards[nextIndex] = generated
                        fixedUsedCards.add(generated)
                        generated
                    }
                }
            }
        }

        resolveGuess(
            st = st,
            guess = guess,
            prev = prev,
            next = next,
            updatedCards = newCards
        )
    }

    private fun drawRandomNonTieCardRelativeTo(
        prev: Card,
        exclude: Set<Card>
    ): Card {
        repeat(5000) {
            val candidate = HiLoEngine.drawFiveCards().random()

            if (candidate in exclude) return@repeat

            when (HiLoEngine.compare(prev, candidate)) {
                CompareResult.HIGHER, CompareResult.LOWER -> return candidate
                CompareResult.TIE -> { }
            }
        }

        repeat(5000) {
            val candidate = HiLoEngine.drawFiveCards().random()
            when (HiLoEngine.compare(prev, candidate)) {
                CompareResult.HIGHER, CompareResult.LOWER -> return candidate
                CompareResult.TIE -> { }
            }
        }

        return HiLoEngine.drawFiveCards().first()
    }

    private fun resolveGuess(
        st: UiState.Playing,
        guess: Guess,
        prev: Card,
        next: Card,
        updatedCards: List<Card>
    ) {
        val cmp = HiLoEngine.compare(prev, next)

        when (cmp) {
            CompareResult.TIE -> {
                val newRevealed = st.revealedCount + 1
                if (newRevealed >= 5) {
                    _state.value = UiState.Won(
                        amount = st.amount,
                        cards = updatedCards,
                        playWinnerSound = true
                    )
                } else {
                    _state.value = st.copy(
                        cards = updatedCards,
                        revealedCount = newRevealed,
                        awaitingGuess = true
                    )
                }
            }

            CompareResult.HIGHER, CompareResult.LOWER -> {
                val correct = HiLoEngine.isCorrect(guess, prev, next)
                if (!correct) {
                    _state.value = UiState.Lost(
                        lastAmount = st.amount,
                        cards = updatedCards,
                        revealedCount = st.revealedCount + 1
                    )
                } else {
                    val doubled = st.amount * 2L
                    val newRevealed = st.revealedCount + 1

                    if (newRevealed >= 5) {
                        _state.value = UiState.Won(
                            amount = doubled,
                            cards = updatedCards,
                            playWinnerSound = true
                        )
                    } else {
                        _state.update { cur ->
                            val p = cur as? UiState.Playing ?: return@update cur
                            p.copy(
                                cards = updatedCards,
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

    private fun prepareFixedRtpRound() {
        val winProbability = getFixedRtpOrDefault() / 1600.0
        fixedShouldWin = Random.nextDouble() < winProbability

        // Первое угадывание всегда случайное.
        // Контроль начинаем только с 3-й карты, значит проигрыш возможен на 2, 3 или 4-м угадывании.
        fixedLoseStep = if (fixedShouldWin == true) {
            4
        } else {
            pickWeightedLoseStep()
        }

        fixedUsedCards.clear()

        Log.d(
            TAG,
            "Fixed RTP prepared: rtp=${_fixedRtpInput.value}, " +
                    "winProbability=$winProbability, shouldWin=$fixedShouldWin, loseStep=$fixedLoseStep"
        )
    }

    private fun pickWeightedLoseStep(): Int {
        val roll = Random.nextInt(100)

        return when {
            roll < 10 -> 2   // 10%
            roll < 45 -> 3   // 35%
            else -> 4        // 55%
        }
    }

    private fun resetFixedRoundState() {
        fixedShouldWin = null
        fixedLoseStep = 4
        fixedUsedCards.clear()
    }

    private fun buildFixedRtpInitialCards(): List<Card> {
        val first = drawRandomCardExcluding(emptySet())
        val second = drawRandomCardExcluding(setOf(first))

        fixedUsedCards.clear()
        fixedUsedCards.add(first)
        fixedUsedCards.add(second)

        val filler3 = drawRandomCardExcluding(fixedUsedCards)
        fixedUsedCards.add(filler3)

        val filler4 = drawRandomCardExcluding(fixedUsedCards)
        fixedUsedCards.add(filler4)

        val filler5 = drawRandomCardExcluding(fixedUsedCards)
        fixedUsedCards.add(filler5)

        // После заполнения болванки возвращаем used только для реально заданных карт 1 и 2.
        fixedUsedCards.clear()
        fixedUsedCards.add(first)
        fixedUsedCards.add(second)

        return listOf(first, second, filler3, filler4, filler5)
    }

    private fun drawRandomCardExcluding(exclude: Set<Card>): Card {
        repeat(500) {
            val candidate = HiLoEngine.drawFiveCards().random()
            if (candidate !in exclude) return candidate
        }

        var fallback = HiLoEngine.drawFiveCards().first()
        while (fallback in exclude) {
            fallback = HiLoEngine.drawFiveCards().random()
        }
        return fallback
    }

    private fun drawCardRelativeTo(
        prev: Card,
        wantHigher: Boolean,
        exclude: Set<Card>
    ): Card {

        repeat(5000) {
            val candidate = HiLoEngine.drawFiveCards().random()

            if (candidate in exclude) return@repeat

            when (HiLoEngine.compare(prev, candidate)) {
                CompareResult.HIGHER -> if (wantHigher) return candidate
                CompareResult.LOWER -> if (!wantHigher) return candidate
                CompareResult.TIE -> { }
            }
        }

        // fallback если вдруг долго не нашли
        repeat(5000) {
            val candidate = HiLoEngine.drawFiveCards().random()

            when (HiLoEngine.compare(prev, candidate)) {
                CompareResult.HIGHER -> if (wantHigher) return candidate
                CompareResult.LOWER -> if (!wantHigher) return candidate
                CompareResult.TIE -> { }
            }
        }

        return HiLoEngine.drawFiveCards().first()
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
        Log.d(
            TAG,
            "Deck mode=${_deckMode.value}, fixedRtp=${_fixedRtpInput.value}, " +
                    "fixedShouldWin=$fixedShouldWin, fixedLoseStep=$fixedLoseStep"
        )
        cards.forEachIndexed { index, card ->
            Log.d(TAG, "[${index + 1}] ${card.rank.label}${card.suit.symbol}")
        }
    }

    fun onCoinSoundPlayed() {
        _state.update { st ->
            if (st is UiState.Playing && st.playCoinSound) {
                st.copy(playCoinSound = false)
            } else {
                st
            }
        }
    }

    fun onWinnerSoundPlayed() {
        _state.update { st ->
            if (st is UiState.Won && st.playWinnerSound) {
                st.copy(playWinnerSound = false)
            } else {
                st
            }
        }
    }
}
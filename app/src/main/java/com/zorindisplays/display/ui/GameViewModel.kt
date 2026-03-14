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

data class GameStats(
    val gamesCount: Long = 0,
    val winsCount: Long = 0,
    val totalIn: Long = 0,
    val totalOut: Long = 0
)

private const val PREFS_NAME = "hilo_settings"
private const val KEY_DECK_MODE = "deck_mode"
private const val KEY_FIXED_RTP = "fixed_rtp"
private const val KEY_GAMES_COUNT = "games_count"
private const val KEY_WINS_COUNT = "wins_count"
private const val KEY_TOTAL_IN = "total_in"
private const val KEY_TOTAL_OUT = "total_out"

private const val KEY_GAME_STATE_TYPE = "game_state_type"
private const val KEY_GAME_RAW = "game_raw"
private const val KEY_GAME_AMOUNT = "game_amount"
private const val KEY_GAME_LAST_AMOUNT = "game_last_amount"
private const val KEY_GAME_REVEALED_COUNT = "game_revealed_count"
private const val KEY_GAME_AWAITING_GUESS = "game_awaiting_guess"
private const val KEY_GAME_SHOW_CONFETTI = "game_show_confetti"
private const val KEY_GAME_CONFETTI_TICK = "game_confetti_tick"
private const val KEY_GAME_PLAY_COIN_SOUND = "game_play_coin_sound"
private const val KEY_GAME_PLAY_WINNER_SOUND = "game_play_winner_sound"
private const val KEY_GAME_CARDS = "game_cards"

private const val KEY_FIXED_SHOULD_WIN = "fixed_should_win"
private const val KEY_FIXED_SHOULD_WIN_PRESENT = "fixed_should_win_present"
private const val KEY_FIXED_LOSE_STEP = "fixed_lose_step"

class GameViewModel : ViewModel() {

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state

    private val _deckMode = MutableStateFlow(DeckMode.RANDOM_DECK)
    val deckMode: StateFlow<DeckMode> = _deckMode

    private val _fixedRtpInput = MutableStateFlow("98.00")
    val fixedRtpInput: StateFlow<String> = _fixedRtpInput

    private val _stats = MutableStateFlow(GameStats())
    val stats: StateFlow<GameStats> = _stats

    private var settingsLoaded = false
    private var appContext: Context? = null

    private var inputLocked: Boolean = false
    private var confettiJob: Job? = null

    // Fixed RTP state
    private var fixedShouldWin: Boolean? = null
    private var fixedLoseStep: Int = 4
    private var fixedUsedCards: MutableSet<Card> = mutableSetOf()

    fun loadSettings(context: Context) {
        if (settingsLoaded) return

        appContext = context.applicationContext

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val modeName = prefs.getString(KEY_DECK_MODE, DeckMode.RANDOM_DECK.name)
        val rtp = prefs.getString(KEY_FIXED_RTP, "98.00") ?: "98.00"

        _deckMode.value = try {
            DeckMode.valueOf(modeName ?: DeckMode.RANDOM_DECK.name)
        } catch (_: Exception) {
            DeckMode.RANDOM_DECK
        }

        _fixedRtpInput.value = formatRtp(rtp)

        _stats.value = GameStats(
            gamesCount = prefs.getLong(KEY_GAMES_COUNT, 0L),
            winsCount = prefs.getLong(KEY_WINS_COUNT, 0L),
            totalIn = prefs.getLong(KEY_TOTAL_IN, 0L),
            totalOut = prefs.getLong(KEY_TOTAL_OUT, 0L)
        )
        restoreGameState(context)
        settingsLoaded = true
    }

    private fun saveSettings(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_DECK_MODE, _deckMode.value.name)
            .putString(KEY_FIXED_RTP, _fixedRtpInput.value)
            .putLong(KEY_GAMES_COUNT, _stats.value.gamesCount)
            .putLong(KEY_WINS_COUNT, _stats.value.winsCount)
            .putLong(KEY_TOTAL_IN, _stats.value.totalIn)
            .putLong(KEY_TOTAL_OUT, _stats.value.totalOut)
            .apply()
    }

    private fun saveSettingsIfPossible() {
        appContext?.let { saveSettings(it) }
    }

    fun setDeckMode(mode: DeckMode, context: Context? = null) {
        if (context != null) appContext = context.applicationContext

        val changed = _deckMode.value != mode
        _deckMode.value = mode

        if (changed) {
            resetStats()
            _state.value = UiState.Idle
            resetFixedRoundState()
        }

        saveAll()
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
        if (context != null) appContext = context.applicationContext

        val formatted = formatRtp(value)
        val changed = _fixedRtpInput.value != formatted

        _fixedRtpInput.value = formatted

        if (changed) {
            resetStats()
            _state.value = UiState.Idle
            resetFixedRoundState()
        }

        saveAll()
    }

    fun getFixedRtpOrDefault(): Double {
        val parsed = _fixedRtpInput.value.toDoubleOrNull() ?: 98.0
        return parsed.coerceIn(0.00, 100.00)
    }

    private fun formatRtp(value: String): String {
        val parsed = value.replace(',', '.').toDoubleOrNull() ?: 98.0
        return String.format(Locale.US, "%.2f", parsed.coerceIn(0.00, 100.00))
    }

    private fun resetStats() {
        _stats.value = GameStats()
        saveSettingsIfPossible()
    }

    private fun recordGameStarted(amount: Long) {
        _stats.update {
            it.copy(
                gamesCount = it.gamesCount + 1,
                totalIn = it.totalIn + amount
            )
        }
        saveSettingsIfPossible()
    }

    private fun recordGameWon(payout: Long) {
        _stats.update {
            it.copy(
                winsCount = it.winsCount + 1,
                totalOut = it.totalOut + payout
            )
        }
        saveSettingsIfPossible()
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
                is UiState.Won -> st
            }
        }

        saveGameStateIfPossible()
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
        saveGameStateIfPossible()
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
        saveGameStateIfPossible()
    }

    fun onStart() {
        if (inputLocked) return

        _state.update { st ->
            when (st) {
                is UiState.Ready -> {
                    logCards(st.cards)
                    recordGameStarted(st.amount)

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
        saveGameStateIfPossible()
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
        saveGameStateIfPossible()
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
            // Первая догадка: 2-я карта уже заранее случайная
            nextIndex == 1 -> {
                val existing = newCards[nextIndex]
                fixedUsedCards.add(existing)
                existing
            }

            else -> {
                val shouldWinRound = fixedShouldWin ?: false
                val currentGuessStep = nextIndex // 2..4

                when {
                    shouldWinRound -> {
                        val wantHigher = guess == Guess.HIGHER
                        val generated = drawCardRelativeToWithoutEqualRank(
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
                        val generated = drawCardRelativeToWithoutEqualRank(
                            prev = prev,
                            wantHigher = wantHigher,
                            exclude = fixedUsedCards
                        )
                        newCards[nextIndex] = generated
                        fixedUsedCards.add(generated)
                        generated
                    }

                    else -> {
                        val generated = drawRandomNonTieAndNonEqualRankCardRelativeTo(
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
                    recordGameWon(st.amount)
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
                        recordGameWon(doubled)
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
            roll < 15 -> 2
            roll < 50 -> 3
            else -> 4
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

    private fun scheduleConfettiOff() {
        confettiJob?.cancel()
        confettiJob = viewModelScope.launch {
            delay(1200)
            _state.update { st ->
                if (st is UiState.Playing) st.copy(showConfetti = false) else st
            }
            saveGameStateIfPossible()
        }
    }

    private fun logCards(cards: List<Card>) {
        Log.d(TAG, "HiLo Round started")
        Log.d(
            TAG,
            "Deck mode=${_deckMode.value}, fixedRtp=${_fixedRtpInput.value}, " +
                    "fixedShouldWin=$fixedShouldWin, fixedLoseStep=$fixedLoseStep, stats=${_stats.value}"
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
        saveGameStateIfPossible()
    }

    fun onWinnerSoundPlayed() {
        _state.update { st ->
            if (st is UiState.Won && st.playWinnerSound) {
                st.copy(playWinnerSound = false)
            } else {
                st
            }
        }
        saveGameStateIfPossible()
    }

    private fun saveAll() {
        saveSettingsIfPossible()
        saveGameStateIfPossible()
    }

    private fun saveGameStateIfPossible() {
        val context = appContext ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        when (val st = _state.value) {
            UiState.Idle -> {
                editor.putString(KEY_GAME_STATE_TYPE, "IDLE")
                editor.remove(KEY_GAME_RAW)
                editor.remove(KEY_GAME_AMOUNT)
                editor.remove(KEY_GAME_LAST_AMOUNT)
                editor.remove(KEY_GAME_REVEALED_COUNT)
                editor.remove(KEY_GAME_AWAITING_GUESS)
                editor.remove(KEY_GAME_SHOW_CONFETTI)
                editor.remove(KEY_GAME_CONFETTI_TICK)
                editor.remove(KEY_GAME_PLAY_COIN_SOUND)
                editor.remove(KEY_GAME_PLAY_WINNER_SOUND)
                editor.remove(KEY_GAME_CARDS)
            }

            is UiState.AmountEntry -> {
                editor.putString(KEY_GAME_STATE_TYPE, "AMOUNT_ENTRY")
                editor.putString(KEY_GAME_RAW, st.raw)
                editor.remove(KEY_GAME_AMOUNT)
                editor.remove(KEY_GAME_LAST_AMOUNT)
                editor.remove(KEY_GAME_REVEALED_COUNT)
                editor.remove(KEY_GAME_AWAITING_GUESS)
                editor.remove(KEY_GAME_SHOW_CONFETTI)
                editor.remove(KEY_GAME_CONFETTI_TICK)
                editor.remove(KEY_GAME_PLAY_COIN_SOUND)
                editor.remove(KEY_GAME_PLAY_WINNER_SOUND)
                editor.remove(KEY_GAME_CARDS)
            }

            is UiState.Ready -> {
                editor.putString(KEY_GAME_STATE_TYPE, "READY")
                editor.putLong(KEY_GAME_AMOUNT, st.amount)
                editor.putString(KEY_GAME_CARDS, encodeCards(st.cards))
                editor.remove(KEY_GAME_LAST_AMOUNT)
                editor.remove(KEY_GAME_REVEALED_COUNT)
                editor.remove(KEY_GAME_AWAITING_GUESS)
                editor.remove(KEY_GAME_SHOW_CONFETTI)
                editor.remove(KEY_GAME_CONFETTI_TICK)
                editor.remove(KEY_GAME_PLAY_COIN_SOUND)
                editor.remove(KEY_GAME_PLAY_WINNER_SOUND)
            }

            is UiState.Playing -> {
                editor.putString(KEY_GAME_STATE_TYPE, "PLAYING")
                editor.putLong(KEY_GAME_AMOUNT, st.amount)
                editor.putString(KEY_GAME_CARDS, encodeCards(st.cards))
                editor.putInt(KEY_GAME_REVEALED_COUNT, st.revealedCount)
                editor.putBoolean(KEY_GAME_AWAITING_GUESS, st.awaitingGuess)
                editor.putBoolean(KEY_GAME_SHOW_CONFETTI, st.showConfetti)
                editor.putInt(KEY_GAME_CONFETTI_TICK, st.confettiTick)
                editor.putBoolean(KEY_GAME_PLAY_COIN_SOUND, st.playCoinSound)
                editor.remove(KEY_GAME_LAST_AMOUNT)
                editor.remove(KEY_GAME_PLAY_WINNER_SOUND)
            }

            is UiState.Lost -> {
                editor.putString(KEY_GAME_STATE_TYPE, "LOST")
                editor.putLong(KEY_GAME_LAST_AMOUNT, st.lastAmount)
                editor.putString(KEY_GAME_CARDS, encodeCards(st.cards))
                editor.putInt(KEY_GAME_REVEALED_COUNT, st.revealedCount)
                editor.remove(KEY_GAME_AMOUNT)
                editor.remove(KEY_GAME_AWAITING_GUESS)
                editor.remove(KEY_GAME_SHOW_CONFETTI)
                editor.remove(KEY_GAME_CONFETTI_TICK)
                editor.remove(KEY_GAME_PLAY_COIN_SOUND)
                editor.remove(KEY_GAME_PLAY_WINNER_SOUND)
            }

            is UiState.Won -> {
                editor.putString(KEY_GAME_STATE_TYPE, "WON")
                editor.putLong(KEY_GAME_AMOUNT, st.amount)
                editor.putString(KEY_GAME_CARDS, encodeCards(st.cards))
                editor.putBoolean(KEY_GAME_PLAY_WINNER_SOUND, false)
                editor.remove(KEY_GAME_LAST_AMOUNT)
                editor.remove(KEY_GAME_REVEALED_COUNT)
                editor.remove(KEY_GAME_AWAITING_GUESS)
                editor.remove(KEY_GAME_SHOW_CONFETTI)
                editor.remove(KEY_GAME_CONFETTI_TICK)
                editor.remove(KEY_GAME_PLAY_COIN_SOUND)
            }
        }

        editor.putBoolean(KEY_FIXED_SHOULD_WIN_PRESENT, fixedShouldWin != null)
        editor.putBoolean(KEY_FIXED_SHOULD_WIN, fixedShouldWin ?: false)
        editor.putInt(KEY_FIXED_LOSE_STEP, fixedLoseStep)

        editor.apply()
    }

    private fun restoreGameState(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fixedShouldWin = if (prefs.getBoolean(KEY_FIXED_SHOULD_WIN_PRESENT, false)) {
            prefs.getBoolean(KEY_FIXED_SHOULD_WIN, false)
        } else {
            null
        }

        fixedLoseStep = prefs.getInt(KEY_FIXED_LOSE_STEP, 4)

        val type = prefs.getString(KEY_GAME_STATE_TYPE, "IDLE") ?: "IDLE"

        _state.value = when (type) {
            "AMOUNT_ENTRY" -> {
                UiState.AmountEntry(
                    raw = prefs.getString(KEY_GAME_RAW, "") ?: ""
                )
            }

            "READY" -> {
                val cards = decodeCards(prefs.getString(KEY_GAME_CARDS, null))
                if (cards.size == 5) {
                    UiState.Ready(
                        amount = prefs.getLong(KEY_GAME_AMOUNT, 0L),
                        cards = cards
                    )
                } else {
                    UiState.Idle
                }
            }

            "PLAYING" -> {
                val cards = decodeCards(prefs.getString(KEY_GAME_CARDS, null))
                if (cards.size == 5) {
                    UiState.Playing(
                        amount = prefs.getLong(KEY_GAME_AMOUNT, 0L),
                        cards = cards,
                        revealedCount = prefs.getInt(KEY_GAME_REVEALED_COUNT, 1),
                        awaitingGuess = prefs.getBoolean(KEY_GAME_AWAITING_GUESS, true),
                        showConfetti = false,
                        confettiTick = prefs.getInt(KEY_GAME_CONFETTI_TICK, 0),
                        playCoinSound = false
                    )
                } else {
                    UiState.Idle
                }
            }

            "LOST" -> {
                val cards = decodeCards(prefs.getString(KEY_GAME_CARDS, null))
                if (cards.size == 5) {
                    UiState.Lost(
                        lastAmount = prefs.getLong(KEY_GAME_LAST_AMOUNT, 0L),
                        cards = cards,
                        revealedCount = prefs.getInt(KEY_GAME_REVEALED_COUNT, 1)
                    )
                } else {
                    UiState.Idle
                }
            }

            "WON" -> {
                val cards = decodeCards(prefs.getString(KEY_GAME_CARDS, null))
                if (cards.size == 5) {
                    UiState.Won(
                        amount = prefs.getLong(KEY_GAME_AMOUNT, 0L),
                        cards = cards,
                        playWinnerSound = false
                    )
                } else {
                    UiState.Idle
                }
            }

            else -> UiState.Idle
        }
    }

    private fun encodeCards(cards: List<Card>): String {
        return cards.joinToString("|") { "${it.rank.name},${it.suit.name}" }
    }

    private fun decodeCards(raw: String?): List<Card> {
        if (raw.isNullOrBlank()) return emptyList()

        return raw.split("|").mapNotNull { token ->
            val parts = token.split(",")
            if (parts.size != 2) return@mapNotNull null

            try {
                val rank = com.zorindisplays.display.model.Rank.valueOf(parts[0])
                val suit = com.zorindisplays.display.model.Suit.valueOf(parts[1])
                Card(rank = rank, suit = suit)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun drawRandomNonTieAndNonEqualRankCardRelativeTo(
        prev: Card,
        exclude: Set<Card>
    ): Card {
        repeat(5000) {
            val candidate = HiLoEngine.drawFiveCards().random()

            if (candidate in exclude) return@repeat
            if (candidate.rank == prev.rank) return@repeat

            when (HiLoEngine.compare(prev, candidate)) {
                CompareResult.HIGHER, CompareResult.LOWER -> return candidate
                CompareResult.TIE -> { }
            }
        }

        repeat(5000) {
            val candidate = HiLoEngine.drawFiveCards().random()
            if (candidate.rank == prev.rank) return@repeat

            when (HiLoEngine.compare(prev, candidate)) {
                CompareResult.HIGHER, CompareResult.LOWER -> return candidate
                CompareResult.TIE -> { }
            }
        }

        return HiLoEngine.drawFiveCards().first()
    }

    private fun drawCardRelativeToWithoutEqualRank(
        prev: Card,
        wantHigher: Boolean,
        exclude: Set<Card>
    ): Card {
        repeat(5000) {
            val candidate = HiLoEngine.drawFiveCards().random()

            if (candidate in exclude) return@repeat
            if (candidate.rank == prev.rank) return@repeat

            when (HiLoEngine.compare(prev, candidate)) {
                CompareResult.HIGHER -> if (wantHigher) return candidate
                CompareResult.LOWER -> if (!wantHigher) return candidate
                CompareResult.TIE -> { }
            }
        }

        repeat(5000) {
            val candidate = HiLoEngine.drawFiveCards().random()
            if (candidate.rank == prev.rank) return@repeat

            when (HiLoEngine.compare(prev, candidate)) {
                CompareResult.HIGHER -> if (wantHigher) return candidate
                CompareResult.LOWER -> if (!wantHigher) return candidate
                CompareResult.TIE -> { }
            }
        }

        return HiLoEngine.drawFiveCards().first()
    }

    fun onRegisterSoundPlayed() {
        _state.update { st ->
            if (st is UiState.Ready) {
                st.copy(playRegisterSound = false)
            } else st
        }
    }
}
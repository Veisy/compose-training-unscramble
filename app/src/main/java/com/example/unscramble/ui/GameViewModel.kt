package com.example.unscramble.ui

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import com.example.unscramble.data.MAX_NO_OF_WORDS
import com.example.unscramble.data.SCORE_INCREASE
import com.example.unscramble.data.allWords
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState = _uiState.asStateFlow()

    private lateinit var currentWord: String
    private var usedWords: MutableSet<String> = mutableSetOf()

    init {
        resetGame()
    }

    fun updateUserGuessIfLettersMatches(guessedWord: String) {
        val currentScrambledWord = uiState.value.currentScrambledWord
        if (guessedWord.all { char ->
                (currentScrambledWord.contains(char)
                        && currentScrambledWord.count { it == char } >= guessedWord.count { it == char })
            }
            && guessedWord.length <= currentScrambledWord.length
        ) {
            updateUserGuess(guessedWord)
        }
    }

    @VisibleForTesting
    internal fun updateUserGuess(guessedWord: String) {
        _uiState.update { currentState ->
            currentState.copy(userGuess = guessedWord)
        }
    }


    fun checkUserGuessIfValidInput() {
        val userGuess = uiState.value.userGuess.trim()
        if (userGuess.isBlank() || userGuess.length != currentWord.length) {
            return
        }
        checkUserGuess()
    }

    @VisibleForTesting
    internal fun checkUserGuess() {
        val userGuess = uiState.value.userGuess.trim()
        if (userGuess.equals(currentWord, ignoreCase = true)) {
            val updatedScore = uiState.value.score.plus(SCORE_INCREASE)
            updateGameState(updatedScore)
        } else {
            _uiState.update { currentState ->
                currentState.copy(isGuessedWordWrong = true)
            }
        }
        updateUserGuess("")
    }

    fun skipWord() {
        updateGameState(uiState.value.score)
        updateUserGuess("")
    }

    private fun updateGameState(updatedScore: Int) {
        if (usedWords.size == MAX_NO_OF_WORDS) {
            // Last round in the game
            _uiState.update { currentState ->
                currentState.copy(
                    isGuessedWordWrong = false,
                    score = updatedScore,
                    isGameOver = true
                )
            }
        } else {
            // Normal round in the game
            _uiState.update { currentState ->
                currentState.copy(
                    isGuessedWordWrong = false,
                    currentScrambledWord = pickRandomWordAndShuffle(),
                    score = updatedScore,
                    currentWordCount = currentState.currentWordCount.inc()
                )
            }
        }
    }

    private fun pickRandomWordAndShuffle(): String {
        currentWord = allWords.random()
        return if (usedWords.contains(currentWord)) {
            pickRandomWordAndShuffle()
        } else {
            usedWords.add(currentWord)
            shuffleCurrentWord(currentWord)
        }
    }

    private fun shuffleCurrentWord(word: String): String {
        val tempWord = word.toCharArray()
        tempWord.shuffle()
        while (String(tempWord) == word) {
            tempWord.shuffle()
        }
        return String(tempWord)
    }

    fun resetGame() {
        usedWords.clear()
        _uiState.value = GameUiState(currentScrambledWord = pickRandomWordAndShuffle())
    }
}

package com.androidkotlin.generatorprokt.presentation.main.state

/**
 * UI 상태를 나타내는 sealed class
 */
sealed class GeneratorUiState {
    object Loading : GeneratorUiState()
    data class Idle(val message: String) : GeneratorUiState()
    data class Ready(val message: String) : GeneratorUiState()
    data class Exposing(val message: String) : GeneratorUiState()
    data class Success(val message: String) : GeneratorUiState()
    data class Error(val message: String) : GeneratorUiState()
}
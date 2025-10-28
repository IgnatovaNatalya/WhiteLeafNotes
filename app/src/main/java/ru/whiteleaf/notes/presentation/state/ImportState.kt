package ru.whiteleaf.notes.presentation.state

sealed class ImportState {
    object Success : ImportState()
    data class Error(val message: String) : ImportState()
    object Loading : ImportState()
    object Idle : ImportState()
}
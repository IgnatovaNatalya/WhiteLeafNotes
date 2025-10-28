package ru.whiteleaf.notes.presentation.state

sealed class NavigationEvent {
    data class NavigateToNote(val noteId: String) : NavigationEvent()
    object NavigateUp : NavigationEvent()
    data class NavigateToNotebook(val path: String) : NavigationEvent()
}
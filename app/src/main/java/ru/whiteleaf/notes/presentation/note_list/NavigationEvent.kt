package ru.whiteleaf.notes.presentation.note_list

import android.net.Uri

sealed class NavigationEvent {
    object Idle: NavigationEvent()
    data class NavigateToNote(val noteId: String) : NavigationEvent()
    object NavigateUp : NavigationEvent()
    data class NavigateToNotebook(val path: String) : NavigationEvent()
    data class ExportLink(val uri: Uri?) : NavigationEvent()
}
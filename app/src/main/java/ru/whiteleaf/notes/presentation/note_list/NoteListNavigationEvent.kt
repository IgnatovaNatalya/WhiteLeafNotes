package ru.whiteleaf.notes.presentation.note_list

import android.net.Uri

sealed class NoteListNavigationEvent {
    object Idle : NoteListNavigationEvent()
    data class NavigateToNote(val noteId: String) : NoteListNavigationEvent()
    object NavigateUp : NoteListNavigationEvent()
    data class ReopenNotebook(val path: String) : NoteListNavigationEvent()
    data class ExportLink(val uri: Uri?) : NoteListNavigationEvent()
    object ShowBiometric : NoteListNavigationEvent()
}
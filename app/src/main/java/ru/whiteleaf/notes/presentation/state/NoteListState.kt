package ru.whiteleaf.notes.presentation.state

import ru.whiteleaf.notes.domain.model.Note

sealed class NoteListState {
    object Loading : NoteListState()
    data class Success(val isProtected: Boolean, val notes: List<Note>) : NoteListState()
    data class Error(val message: String) : NoteListState()
    object Blocked : NoteListState()
    //data class Blocked(val request:BiometricRequest) : NoteListState()
}
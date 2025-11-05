package ru.whiteleaf.notes.presentation.note_edit

import ru.whiteleaf.notes.domain.model.Note

sealed class NoteEditState {
    object Loading : NoteEditState()
    data class Success(val note: Note) : NoteEditState()
    data class Error(val message: String) : NoteEditState()

}
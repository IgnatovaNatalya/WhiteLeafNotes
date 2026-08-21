package ru.whiteleaf.notes.presentation.note_edit

import ru.whiteleaf.notes.domain.model.Note

sealed class NoteEditState {
    object Loading : NoteEditState()
    data class Success(
        val note: Note,
        val scrollPosition: Int,
        val isEncrypted: Boolean
    ) : NoteEditState()

    data class Error(val message: String) : NoteEditState()
    data class Blocked(val hasUnsavedChanges: Boolean) : NoteEditState()
}
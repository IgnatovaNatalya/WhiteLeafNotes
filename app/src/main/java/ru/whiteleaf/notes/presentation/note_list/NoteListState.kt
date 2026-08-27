package ru.whiteleaf.notes.presentation.note_list

import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.model.NoteFound

sealed class NoteListState {
    object Loading : NoteListState()
    data class Success(val isEncrypted: Boolean, val notes: List<Note>) : NoteListState()
    data class Error(val message: String) : NoteListState()
    object Blocked : NoteListState()
    data class SearchResults(val query: String, val notes: List<NoteFound>) : NoteListState()
}
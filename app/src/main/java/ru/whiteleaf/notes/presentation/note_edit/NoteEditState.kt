package ru.whiteleaf.notes.presentation.note_edit

import ru.whiteleaf.notes.domain.model.Note

sealed class NoteEditState {
    object Loading : NoteEditState()
    data class Success(
        val note: Note,
        val scrollPosition: Int,
        val isEncrypted: Boolean,
        val searchState: SearchState? = null
    ) : NoteEditState()

    data class Error(val message: String) : NoteEditState()
    data class Blocked(val hasUnsavedChanges: Boolean) : NoteEditState()
}

data class SearchState(
    val query: String,                      // текущий поисковый запрос
    val matches: List<Pair<Int, Int>>,      // список пар (start, end) всех совпадений
    val currentMatchIndex: Int              // индекс текущего выделенного совпадения (-1, если нет совпадений)
)

fun SearchState.debugString(): String {
    return ("Search state is: query=${this.query}, ${matches.size}matches, current match=$currentMatchIndex")
}

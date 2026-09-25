package ru.whiteleaf.notes.presentation.note_edit

import android.text.SpannableString
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
    val matches: List<SearchMatch>,         // список всех совпадений включая название и контент
    val currentMatchIndex: Int              // индекс текущего выделенного совпадения (-1, если нет совпадений)
)

data class SearchMatch(
    val start: Int,
    val end: Int,
    val target: SearchMatchTarget
)

enum class SearchMatchTarget { TITLE, CONTENT }

data class NoteSearchHighlights(
    val title: SpannableString,
    val content: SpannableString,
    val matches: List<SearchMatch>
)

fun SearchState.debugString(): String {
    return ("Search state is: query=${this.query}, ${matches.size}matches, current match=$currentMatchIndex")
}
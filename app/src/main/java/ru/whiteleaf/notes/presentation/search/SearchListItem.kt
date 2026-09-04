package ru.whiteleaf.notes.presentation.search

import ru.whiteleaf.notes.domain.model.NoteFound
import ru.whiteleaf.notes.domain.model.Notebook

sealed class SearchListItem {
    data class SearchListNoteTitle(val noteFound: NoteFound) : SearchListItem()
    data class SearchListNoteContent(val noteFound: NoteFound) : SearchListItem()
    data class SearchNotebook(val notebook: Notebook, val query: String) : SearchListItem()


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return when (this) {
            is SearchListNoteTitle -> other is SearchListNoteTitle
            is SearchListNoteContent -> other is SearchListNoteContent
            is SearchNotebook -> other is SearchNotebook
        }
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

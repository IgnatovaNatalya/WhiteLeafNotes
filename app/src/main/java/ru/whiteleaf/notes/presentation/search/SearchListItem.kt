package ru.whiteleaf.notes.presentation.search

import ru.whiteleaf.notes.domain.model.NoteFound
import ru.whiteleaf.notes.domain.model.Notebook

sealed class SearchListItem {
    data class SearchListNoteTitle(val noteFound: NoteFound) : SearchListItem()
    data class SearchListNoteContent(val noteFound: NoteFound) : SearchListItem()
    data class SearchNotebook(val notebook: Notebook) : SearchListItem()
}
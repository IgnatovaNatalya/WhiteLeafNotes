package ru.whiteleaf.notes.presentation.start

import ru.whiteleaf.notes.data.model.RecentNote
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.model.Notebook

sealed class StartListItem {
    object HeaderRecent : StartListItem()
    data class RecentNoteItem(val recentNote: RecentNote) : StartListItem()
    object ShowMoreRecent : StartListItem()

    object HeaderNotebooks : StartListItem()
    data class NotebookItem(val notebook: Notebook) : StartListItem()
    object ShowMoreNotebooks : StartListItem()

    object HeaderRootNotes : StartListItem()
    data class NoteItem(val note: Note) : StartListItem()
    object ShowMoreNotes : StartListItem()

    object Divider : StartListItem()
    object EmptyNotebooks : StartListItem()
    object EmptyNotes : StartListItem()
    object Spacing : StartListItem()

    // Для DiffUtil
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return when (this) {
            HeaderRecent -> other is HeaderRecent
            is RecentNoteItem -> this == other
            ShowMoreRecent -> other is ShowMoreRecent

            HeaderNotebooks -> other is HeaderNotebooks
            is NotebookItem -> this == other
            ShowMoreNotebooks -> other is ShowMoreNotebooks

            HeaderRootNotes -> other is HeaderRootNotes
            is NoteItem -> this == other
            ShowMoreNotes -> other is ShowMoreNotes

            Divider -> other is Divider

            EmptyNotebooks -> other is EmptyNotebooks
            EmptyNotes -> other is EmptyNotes
            Spacing -> other is Spacing
        }
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
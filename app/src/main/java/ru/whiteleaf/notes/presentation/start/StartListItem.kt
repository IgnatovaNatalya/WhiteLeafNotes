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
    object CreateNotebook : StartListItem()
    object DividerLine : StartListItem()

    object HeaderRootNotes : StartListItem()
    data class NoteItem(val note: Note) : StartListItem()
    object ShowMoreNotes : StartListItem()


    object DividerHeader : StartListItem()
    object DividerLineBg : StartListItem()
    object DividerAfter : StartListItem() //show more без кнопки

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
            CreateNotebook -> other is CreateNotebook


            HeaderRootNotes -> other is HeaderRootNotes
            is NoteItem -> this == other
            ShowMoreNotes -> other is ShowMoreNotes

            DividerLineBg -> other is DividerLineBg
            DividerLine -> other is DividerLine
            DividerHeader -> other is DividerHeader
            DividerAfter -> other is DividerAfter
        }
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
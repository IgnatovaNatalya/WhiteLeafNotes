package ru.whiteleaf.notes.presentation.start

import android.view.LayoutInflater
import ru.whiteleaf.notes.domain.model.Notebook
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.common.interfaces.ContextNoteActionHandler
import ru.whiteleaf.notes.common.interfaces.ContextNotebookActionHandler
import ru.whiteleaf.notes.data.model.RecentNote
import ru.whiteleaf.notes.domain.model.Note

class StartAdapter(
    private val onRecentNoteClicked: (RecentNote) -> Unit,
    private val onShowMoreRecent: () -> Unit,

    private val onNotebookClicked: (Notebook) -> Unit,
    private val onShowMoreNotebooks: () -> Unit,
    private val onAllNotebooksClicked: () -> Unit,

    private val onNoteClicked: (Note) -> Unit,
    private val onShowMoreNotes: () -> Unit,
    private val onRootNotesClicked: () -> Unit,

    private val contextNoteActionHandler: ContextNoteActionHandler,
    private val contextNotebookActionHandler: ContextNotebookActionHandler
) : ListAdapter<StartListItem, RecyclerView.ViewHolder>(StartDiffCallback()) {

    companion object {
        private const val TYPE_HEADER_RECENT = 0
        private const val TYPE_RECENT_NOTE = 1
        private const val TYPE_SHOW_MORE_RECENT = 2

        private const val TYPE_HEADER_NOTEBOOKS = 3
        private const val TYPE_NOTEBOOK = 4
        private const val TYPE_SHOW_MORE_NOTEBOOKS = 5

        private const val TYPE_HEADER_NOTES = 6
        private const val TYPE_NOTE = 7
        private const val TYPE_SHOW_MORE_NOTES = 8

        private const val TYPE_DIVIDER = 9
        private const val TYPE_EMPTY = 10
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is StartListItem.HeaderRecent -> TYPE_HEADER_RECENT
            is StartListItem.RecentNoteItem -> TYPE_RECENT_NOTE
            is StartListItem.ShowMoreRecent -> TYPE_SHOW_MORE_RECENT

            is StartListItem.HeaderNotebooks -> TYPE_HEADER_NOTEBOOKS
            is StartListItem.NotebookItem -> TYPE_NOTEBOOK
            is StartListItem.ShowMoreNotebooks -> TYPE_SHOW_MORE_NOTEBOOKS

            is StartListItem.HeaderRootNotes -> TYPE_HEADER_NOTES
            is StartListItem.NoteItem -> TYPE_NOTE
            is StartListItem.ShowMoreNotes -> TYPE_SHOW_MORE_NOTES

            is StartListItem.Divider -> TYPE_DIVIDER
            is StartListItem.EmptyNotebooks, is StartListItem.EmptyNotes, is StartListItem.Spacing -> TYPE_EMPTY

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER_RECENT -> HeaderViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_start_header, parent, false),
                {}
            )

            TYPE_RECENT_NOTE -> RecentNoteViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_start_recent, parent, false),
                onRecentNoteClicked
            )

            TYPE_SHOW_MORE_RECENT -> ShowMoreButtonViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_show_more, parent, false),
                onShowMoreRecent
            )

            TYPE_HEADER_NOTEBOOKS -> HeaderViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_start_header, parent, false),
                onAllNotebooksClicked
            )

            TYPE_NOTEBOOK -> NotebookViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_start_notebook, parent, false),
                onNotebookClicked, contextNotebookActionHandler
            )

            TYPE_SHOW_MORE_NOTEBOOKS -> ShowMoreButtonViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_show_more, parent, false),
                onShowMoreNotebooks
            )

            TYPE_HEADER_NOTES -> HeaderViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_start_header, parent, false),
                onRootNotesClicked
            )

            TYPE_NOTE -> NoteViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_start_note, parent, false),
                onNoteClicked, contextNoteActionHandler
            )

            TYPE_SHOW_MORE_NOTES -> ShowMoreButtonViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_show_more, parent, false),
                onShowMoreNotes
            )

            //тот же xml только кнопка невидимая
            TYPE_DIVIDER -> DividerViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_show_more, parent, false)
            )

            TYPE_EMPTY -> EmptyListViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_start_empty_list, parent, false)
            )

            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is StartListItem.HeaderRecent -> (holder as HeaderViewHolder).bind(
                "НЕДАВНИЕ", false
            )

            is StartListItem.RecentNoteItem -> (holder as RecentNoteViewHolder).bind(item.recentNote)
            is StartListItem.ShowMoreRecent -> (holder as ShowMoreButtonViewHolder).bind()

            is StartListItem.HeaderNotebooks -> (holder as HeaderViewHolder).bind(
                "ЗАПИСНЫЕ КНИЖКИ", true
            )

            is StartListItem.NotebookItem -> (holder as NotebookViewHolder).bind(item.notebook)
            is StartListItem.ShowMoreNotebooks -> (holder as ShowMoreButtonViewHolder).bind()

            is StartListItem.HeaderRootNotes -> (holder as HeaderViewHolder).bind(
                "ЗАМЕТКИ", true
            )

            is StartListItem.NoteItem -> (holder as NoteViewHolder).bind(item.note)
            is StartListItem.ShowMoreNotes -> (holder as ShowMoreButtonViewHolder).bind()

            is StartListItem.Divider -> (holder as DividerViewHolder).bind() // разделитель без кнопки
            is StartListItem.EmptyNotebooks -> (holder as EmptyListViewHolder).bind("Записных книжек пока нет")
            is StartListItem.EmptyNotes -> (holder as EmptyListViewHolder).bind("Заметок пока нет")
            is StartListItem.Spacing -> (holder as EmptyListViewHolder).bind("") //Пустое место после кнопки в конце списка

        }
    }

    class StartDiffCallback : DiffUtil.ItemCallback<StartListItem>() {
        override fun areItemsTheSame(oldItem: StartListItem, newItem: StartListItem): Boolean {
            return when {

                oldItem is StartListItem.RecentNoteItem && newItem is StartListItem.RecentNoteItem ->
                    oldItem.recentNote.recentTitle == newItem.recentNote.recentTitle &&
                            oldItem.recentNote.notebookPath == newItem.recentNote.notebookPath

                oldItem is StartListItem.NotebookItem && newItem is StartListItem.NotebookItem ->
                    oldItem.notebook.path == newItem.notebook.path

                oldItem is StartListItem.NoteItem && newItem is StartListItem.NoteItem ->
                    oldItem.note.title == newItem.note.title

                else -> oldItem == newItem
            }
        }

        override fun areContentsTheSame(oldItem: StartListItem, newItem: StartListItem): Boolean {
            return oldItem == newItem
        }
    }
}
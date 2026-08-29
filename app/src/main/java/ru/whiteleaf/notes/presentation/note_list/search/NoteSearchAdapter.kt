package ru.whiteleaf.notes.presentation.note_list.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.databinding.ItemNoteInListBinding
import ru.whiteleaf.notes.databinding.ItemNoteInSearchContentBinding
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.model.NoteFound
import ru.whiteleaf.notes.domain.model.Notebook
import ru.whiteleaf.notes.domain.model.toNote
import ru.whiteleaf.notes.presentation.note_list.linear.NoteInListViewHolder
import ru.whiteleaf.notes.presentation.start.NotebookViewHolder

class NoteSearchAdapter(
    private val onNoteClicked: (Note) -> Unit,
    private val onFoundNoteClicked: (NoteFound) -> Unit,
    private val onFoundNotebookClicked: (Notebook) -> Unit,
) : ListAdapter<SearchListItem, RecyclerView.ViewHolder>(SearchNotesDiffCallback()) {

    companion object {
        private const val TYPE_NOTE_TITLE = 0
        private const val TYPE_NOTE_CONTENT = 1
        private const val TYPE_NOTEBOOK = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SearchListItem.SearchListNoteContent -> TYPE_NOTE_CONTENT
            is SearchListItem.SearchListNoteTitle -> TYPE_NOTE_TITLE
            is SearchListItem.SearchNotebook -> TYPE_NOTEBOOK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_NOTE_TITLE -> {
                NoteInListViewHolder(
                    ItemNoteInListBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ), onNoteClicked, null
                )
            }

            TYPE_NOTE_CONTENT -> {
                NoteContentInSearchViewHolder(
                    ItemNoteInSearchContentBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ), onFoundNoteClicked
                )
            }

            TYPE_NOTEBOOK -> {
                NotebookViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(ru.whiteleaf.notes.R.layout.item_start_notebook, parent, false),
                    onFoundNotebookClicked,
                    null
                )
            }

            else -> {
                throw IllegalArgumentException("Unknown view type: $viewType")
            }
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is SearchListItem.SearchListNoteTitle -> (holder as NoteInListViewHolder).bind(
                item.noteFound.toNote(), item.noteFound.query
            )

            is SearchListItem.SearchListNoteContent -> (holder as NoteContentInSearchViewHolder).bind(
                item.noteFound
            )
            is SearchListItem.SearchNotebook -> (holder as NotebookViewHolder).bind(item.notebook)
        }
    }

    class SearchNotesDiffCallback : DiffUtil.ItemCallback<SearchListItem>() {
        override fun areItemsTheSame(oldItem: SearchListItem, newItem: SearchListItem): Boolean {
            return when {
                oldItem is SearchListItem.SearchListNoteContent && newItem is SearchListItem.SearchListNoteContent ->
                    oldItem.noteFound.title == newItem.noteFound.title &&
                            oldItem.noteFound.id == newItem.noteFound.id &&
                            oldItem.noteFound.contentSearchPreview == newItem.noteFound.contentSearchPreview

                oldItem is SearchListItem.SearchListNoteTitle && newItem is SearchListItem.SearchListNoteTitle ->
                    oldItem.noteFound.title == newItem.noteFound.title

                oldItem is SearchListItem.SearchNotebook && newItem is SearchListItem.SearchNotebook ->
                    oldItem.notebook.path == newItem.notebook.path

                else -> oldItem == newItem
            }
        }

        override fun areContentsTheSame(oldItem: SearchListItem, newItem: SearchListItem): Boolean {
            return oldItem == newItem
        }
    }
}
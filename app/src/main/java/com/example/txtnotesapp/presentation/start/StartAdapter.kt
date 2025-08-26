package com.example.txtnotesapp.presentation.start

import android.view.LayoutInflater
import com.example.txtnotesapp.domain.model.Notebook
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.txtnotesapp.R
import com.example.txtnotesapp.domain.model.Note

class StartAdapter(
    private val onNotebookClicked: (Notebook) -> Unit,
    private val onNoteClicked: (Note) -> Unit,
    private val onAddNotebookClicked: () -> Unit,
    private val onAddNoteClicked: () -> Unit
) : ListAdapter<StartListItem, RecyclerView.ViewHolder>(StartDiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_NOTEBOOK = 1
        private const val TYPE_NOTE = 2
        private const val TYPE_ADD_NOTEBOOK = 3
        private const val TYPE_ADD_NOTE = 4
        private const val TYPE_DIVIDER = 5
        private const val TYPE_EMPTY = 6
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is StartListItem.Header -> TYPE_HEADER
            is StartListItem.NotebookItem -> TYPE_NOTEBOOK
            is StartListItem.NoteItem -> TYPE_NOTE
            is StartListItem.AddNotebookButton -> TYPE_ADD_NOTEBOOK
            is StartListItem.AddNoteButton -> TYPE_ADD_NOTE
            is StartListItem.Divider -> TYPE_DIVIDER
            is StartListItem.EmptyNotebooks, is StartListItem.EmptyNotes -> TYPE_EMPTY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_start_header, parent, false)
            )

            TYPE_NOTEBOOK -> NotebookViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_start_notebook, parent, false),
                onNotebookClicked
            )

            TYPE_NOTE -> NoteViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_note, parent, false),
                onNoteClicked
            )

            TYPE_ADD_NOTEBOOK -> AddButtonViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_start_add_button, parent, false),
                onAddNotebookClicked,
                "Создать записную книжку"
            )

            TYPE_ADD_NOTE -> AddButtonViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_start_add_button, parent, false),
                onAddNoteClicked,
                "Создать заметку"
            )

            TYPE_DIVIDER -> DividerViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_start_divider, parent, false)
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
            is StartListItem.Header -> (holder as HeaderViewHolder).bind(item.title)
            is StartListItem.NotebookItem -> (holder as NotebookViewHolder).bind(item.notebook)
            is StartListItem.NoteItem -> (holder as NoteViewHolder).bind(item.note)
            is StartListItem.AddNotebookButton -> (holder as AddButtonViewHolder).bind()
            is StartListItem.AddNoteButton -> (holder as AddButtonViewHolder).bind()
            is StartListItem.EmptyNotebooks -> (holder as EmptyListViewHolder).bind("Записных книжек пока нет")
            is StartListItem.EmptyNotes -> (holder as EmptyListViewHolder).bind("Заметок пока нет")
            is StartListItem.Divider -> {} // Ничего не делаем для разделителя
        }
    }

    class StartDiffCallback : DiffUtil.ItemCallback<StartListItem>() {
        override fun areItemsTheSame(oldItem: StartListItem, newItem: StartListItem): Boolean {
            return when {
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
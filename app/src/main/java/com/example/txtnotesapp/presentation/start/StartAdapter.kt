package com.example.txtnotesapp.presentation.start

import android.view.LayoutInflater
import com.example.txtnotesapp.domain.model.Notebook
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
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
                    .inflate(R.layout.item_notebook, parent, false),
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
                "Добавить записную книжку"
            )
            TYPE_ADD_NOTE -> AddButtonViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_start_add_button, parent, false),
                onAddNoteClicked,
                "Добавить заметку"
            )
            TYPE_DIVIDER -> DividerViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_divider, parent, false)
            )
            TYPE_EMPTY -> EmptyViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_empty, parent, false)
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
            is StartListItem.EmptyNotebooks -> (holder as EmptyViewHolder).bind("Записных книжек пока нет")
            is StartListItem.EmptyNotes -> (holder as EmptyViewHolder).bind("Заметок пока нет")
            is StartListItem.Divider -> {} // Ничего не делаем для разделителя
        }
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.title)

        fun bind(headerTitle: String) {
            title.text = headerTitle
        }
    }

    class NotebookViewHolder(
        view: View,
        private val onNotebookClicked: (Notebook) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.notebookName)
        private val noteCount: TextView = view.findViewById(R.id.noteCount)

        fun bind(notebook: Notebook) {
            name.text = notebook.name
            noteCount.text = "${notebook.noteCount} заметок"

            itemView.setOnClickListener {
                onNotebookClicked(notebook)
            }
        }
    }

    class NoteViewHolder(
        view: View,
        private val onNoteClicked: (Note) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.note_title)

        fun bind(note: Note) {
            title.text = note.title
            //date.text = formatDate(note.createdAt)

            itemView.setOnClickListener {
                onNoteClicked(note)
            }
        }
    }

    class AddButtonViewHolder(
        view: View,
        private val onAddClicked: () -> Unit,
        private val buttonText: String
    ) : RecyclerView.ViewHolder(view) {
        private val button: Button = view.findViewById(R.id.addButton)

        fun bind() {
            button.text = buttonText
            button.setOnClickListener {
                onAddClicked()
            }
        }
    }

    class DividerViewHolder(view: View) : RecyclerView.ViewHolder(view)

    class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val text: TextView = view.findViewById(R.id.start_empty_list)

        fun bind(message: String) {
            text.text = message
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
package ru.whiteleaf.notes.presentation.root

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.model.Notebook

class DrawerMenuAdapter(
    private val onNotebookClicked: (Notebook) -> Unit,
    private val onNoteClicked: (Note) -> Unit,
    private val onCreateNotebook: () -> Unit,
    private val onCreateNote: () -> Unit
) : ListAdapter<DrawerMenuItem, RecyclerView.ViewHolder>(DrawerMenuDiffCallback()) {

    companion object {
        private const val TYPE_NOTEBOOK = 1
        private const val TYPE_NOTE = 2
        private const val TYPE_CREATE_NOTEBOOK = 3
        private const val TYPE_CREATE_NOTE = 4
        private const val TYPE_DIVIDER = 5
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DrawerMenuItem.NotebookItem -> TYPE_NOTEBOOK
            is DrawerMenuItem.NoteItem -> TYPE_NOTE
            is DrawerMenuItem.CreateNotebook -> TYPE_CREATE_NOTEBOOK
            is DrawerMenuItem.CreateNote -> TYPE_CREATE_NOTE
            is DrawerMenuItem.Divider -> TYPE_DIVIDER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {

            TYPE_NOTEBOOK -> DrawerNotebookViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_drawer_notebook, parent, false),
                onNotebookClicked
            )
            TYPE_NOTE -> DrawerNoteViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_drawer_note, parent, false),
                onNoteClicked
            )
            TYPE_CREATE_NOTEBOOK -> DrawerCreateButtonViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_drawer_create_button, parent, false),
                onCreateNotebook
            )
            TYPE_CREATE_NOTE -> DrawerCreateButtonViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_drawer_create_button, parent, false),
                onCreateNote
            )
            TYPE_DIVIDER -> DrawerDividerViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_divider, parent, false)
            )

            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is DrawerMenuItem.NotebookItem -> (holder as DrawerNotebookViewHolder).bind(item.notebook)
            is DrawerMenuItem.NoteItem -> (holder as DrawerNoteViewHolder).bind(item.note)
            is DrawerMenuItem.CreateNotebook -> (holder as DrawerCreateButtonViewHolder).bind("Записная книжка")
            is DrawerMenuItem.CreateNote -> (holder as DrawerCreateButtonViewHolder).bind("Заметка")
            is DrawerMenuItem.Divider -> {} // Ничего не делаем для разделителя
        }
    }
}
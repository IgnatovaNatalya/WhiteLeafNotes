package com.example.whiteleafnotes.presentation.start

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.whiteleafnotes.R
import com.example.whiteleafnotes.common.interfaces.ContextNotebookActionHandler
import com.example.whiteleafnotes.common.utils.ContextMenuHelper
import com.example.whiteleafnotes.domain.model.Notebook

class NotebookViewHolder(
    view: View,
    private val onNotebookClicked: (Notebook) -> Unit,
    private val contextActionHandler: ContextNotebookActionHandler
) : RecyclerView.ViewHolder(view) {
    private val name: TextView = view.findViewById(R.id.notebookName)
    private val noteCount: TextView = view.findViewById(R.id.noteCount)

    fun bind(notebook: Notebook) {
        name.text = notebook.name
        noteCount.text = itemView.resources.getQuantityString(R.plurals.notes_count, notebook.noteCount, notebook.noteCount)

        itemView.setOnClickListener {
            onNotebookClicked(notebook)
        }

        itemView.setOnLongClickListener {
            showContextMenu(itemView, notebook)
            true
        }
    }

    private fun showContextMenu(anchorView: View, notebook: Notebook) {
        ContextMenuHelper.showPopupMenu(
            context = anchorView.context,
            anchorView = anchorView,
            items = ContextMenuHelper.getNotebookContextMenuItems(anchorView.context),
            onItemSelected = { itemId ->
                when (itemId) {
                    R.id.note_menu_delete -> contextActionHandler.onDeleteNotebook(notebook)
                    R.id.note_menu_rename -> contextActionHandler.onRenameNotebook(notebook)
                }
            }
        )
    }
}
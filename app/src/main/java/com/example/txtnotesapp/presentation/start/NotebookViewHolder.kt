package com.example.txtnotesapp.presentation.start

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.txtnotesapp.R
import com.example.txtnotesapp.common.interfaces.ContextActionHandler
import com.example.txtnotesapp.common.utils.ContextMenuHelper
import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.model.Notebook

class NotebookViewHolder(
    view: View,
    private val onNotebookClicked: (Notebook) -> Unit,
    private val contextActionHandler: ContextActionHandler
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
        ContextMenuHelper.showContextMenu(
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
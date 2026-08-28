package ru.whiteleaf.notes.presentation.start

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.common.interfaces.ContextNotebookActionHandler
import ru.whiteleaf.notes.common.utils.ContextMenuHelper
import ru.whiteleaf.notes.domain.model.Notebook

class NotebookViewHolder(
    view: View,
    private val onNotebookClicked: (Notebook) -> Unit,
    private val contextActionHandler: ContextNotebookActionHandler?
) : RecyclerView.ViewHolder(view) {
    private val name: TextView = view.findViewById(R.id.notebook_name)
    private val noteCount: TextView = view.findViewById(R.id.note_count)
    private val icon: ImageView = view.findViewById(R.id.notebook_icon)
    private val llNotebook: LinearLayout = view.findViewById(R.id.ll_notebook)
    private val pinIcon : ImageView = view.findViewById(R.id.pin_icon)

    fun bind(notebook: Notebook) {
        name.text = notebook.path
        noteCount.text = itemView.resources.getQuantityString(
            R.plurals.notes_count,
            notebook.noteCount,
            notebook.noteCount
        )

        if (notebook.isEncrypted) {
            icon.setImageResource(R.drawable.ic_folder_protected)
        } else {
            icon.setImageResource(R.drawable.ic_folder)
        }

        pinIcon.visibility = if (notebook.isPinned) View.VISIBLE else View.GONE
        pinIcon.setOnClickListener { contextActionHandler?.onUnpinNotebook(notebook) }

        llNotebook.setOnClickListener { onNotebookClicked(notebook)  }
        icon.setOnClickListener { onNotebookClicked(notebook)  }

        llNotebook.setOnLongClickListener {
            showContextMenu(itemView, notebook)
            true
        }
        icon.setOnLongClickListener {
            showContextMenu(itemView, notebook)
            true
        }
    }

    private fun showContextMenu(anchorView: View, notebook: Notebook) {
        ContextMenuHelper.showPopupMenu(
            context = anchorView.context,
            anchorView = anchorView,
            items = ContextMenuHelper.getNotebookContextMenuItems(anchorView.context, notebook),
            onItemSelected = { itemId ->
                when (itemId) {
                    R.id.context_menu_rename -> contextActionHandler?.onRenameNotebook(notebook)
                    R.id.context_menu_pin -> contextActionHandler?.onPinNotebook(notebook)
                    R.id.context_menu_unpin -> contextActionHandler?.onUnpinNotebook(notebook)
                    R.id.context_menu_encrypt -> contextActionHandler?.onEncryptNotebook(notebook)
                    R.id.context_menu_decrypt -> contextActionHandler?.onDecryptNotebook(notebook)
                    R.id.context_menu_export -> contextActionHandler?.onExportNotebook(notebook)
                    R.id.context_menu_delete -> contextActionHandler?.onDeleteNotebook(notebook)
                }
            }
        )
    }
}
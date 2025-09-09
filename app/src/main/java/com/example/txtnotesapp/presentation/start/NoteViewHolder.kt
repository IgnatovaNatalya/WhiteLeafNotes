package com.example.txtnotesapp.presentation.start

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.txtnotesapp.R
import com.example.txtnotesapp.common.interfaces.NoteActionHandler
import com.example.txtnotesapp.common.utils.ContextMenuHelper
import com.example.txtnotesapp.domain.model.Note

class NoteViewHolder(
    view: View,
    private val onNoteClicked: (Note) -> Unit,
    private val noteActionHandler: NoteActionHandler
) : RecyclerView.ViewHolder(view) {
    private val title: TextView = view.findViewById(R.id.note_title)

    fun bind(note: Note) {
        title.text = note.title
        //date.text = formatDate(note.createdAt)

        itemView.setOnClickListener {
            onNoteClicked(note)
        }

        itemView.setOnLongClickListener {
            showContextMenu(itemView, note)
            true
        }
    }
    private fun showContextMenu(anchorView: View, note: Note) {
        ContextMenuHelper.showContextMenu(
            context = anchorView.context,
            anchorView = anchorView,
            items = ContextMenuHelper.getNoteContextMenuItems(anchorView.context),
            onItemSelected = { itemId ->
                when (itemId) {
                    R.id.note_menu_delete -> noteActionHandler.onDeleteNote(note)
                    R.id.note_menu_move -> noteActionHandler.onMoveNote(note)
                    R.id.note_menu_share -> noteActionHandler.onShareNote(note)
                    R.id.note_menu_rename -> noteActionHandler.onRenameNote(note)
                }
            }
        )
    }
}
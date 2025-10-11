package ru.whiteleaf.notes.presentation.start

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.common.interfaces.ContextNoteActionHandler
import ru.whiteleaf.notes.common.utils.ContextMenuHelper
import ru.whiteleaf.notes.domain.model.Note

class NoteViewHolder(
    view: View,
    private val onNoteClicked: (Note) -> Unit,
    private val contextActionHandler: ContextNoteActionHandler
) : RecyclerView.ViewHolder(view) {
    private val title: TextView = view.findViewById(R.id.note_title)

    fun bind(note: Note) {

        val text = if (note.title!="") note.title else note.content.take(40)

        title.text = text//note.title

        itemView.setOnClickListener {
            onNoteClicked(note)
        }

        itemView.setOnLongClickListener {
            showContextMenu(itemView, note)
            true
        }
    }
    private fun showContextMenu(anchorView: View, note: Note) {
        ContextMenuHelper.showPopupMenu(
            context = anchorView.context,
            anchorView = anchorView,
            items = ContextMenuHelper.getNoteContextMenuItems(anchorView.context),
            onItemSelected = { itemId ->
                when (itemId) {
                    R.id.note_menu_delete -> contextActionHandler.onDeleteNote(note)
                    R.id.note_menu_move -> contextActionHandler.onMoveNote(note)
                    R.id.note_menu_share -> contextActionHandler.onShareNote(note)
                    R.id.note_menu_rename -> contextActionHandler.onRenameNote(note)
                }
            }
        )
    }
}
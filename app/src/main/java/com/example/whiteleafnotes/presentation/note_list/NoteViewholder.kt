package com.example.whiteleafnotes.presentation.note_list

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.whiteleafnotes.R
import com.example.whiteleafnotes.common.interfaces.ContextNoteActionHandler
import com.example.whiteleafnotes.common.utils.ContextMenuHelper
import com.example.whiteleafnotes.databinding.ItemNoteBinding
import com.example.whiteleafnotes.domain.model.Note

class NoteViewHolder(
    private val binding: ItemNoteBinding,
    private val onNoteClicked: (Note) -> Unit,
    private val noteActionHandler: ContextNoteActionHandler
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(note: Note) {
        val text = if (note.title!="") note.title else note.content.take(40)
        binding.noteTitle.text = text

        binding.root.setOnClickListener {
            onNoteClicked(note)
        }

        binding.root.setOnLongClickListener {
            showContextMenu(binding.root, note)
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
                    R.id.note_menu_delete -> noteActionHandler.onDeleteNote(note)
                    R.id.note_menu_move -> noteActionHandler.onMoveNote(note)
                    R.id.note_menu_share -> noteActionHandler.onShareNote(note)
                    R.id.note_menu_rename -> noteActionHandler.onRenameNote(note)
                }
            }
        )
    }
}
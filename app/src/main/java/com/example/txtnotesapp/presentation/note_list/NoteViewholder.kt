package com.example.txtnotesapp.presentation.note_list

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.txtnotesapp.R
import com.example.txtnotesapp.common.interfaces.NoteActionHandler
import com.example.txtnotesapp.common.utils.ContextMenuHelper
import com.example.txtnotesapp.databinding.ItemNoteBinding
import com.example.txtnotesapp.domain.model.Note

class NoteViewHolder(
    private val binding: ItemNoteBinding,
    private val onNoteClicked: (Note) -> Unit,
    private val noteActionHandler: NoteActionHandler
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(note: Note) {
        binding.noteTitle.text = note.title

        binding.root.setOnClickListener {
            onNoteClicked(note)
        }

        binding.root.setOnLongClickListener {
            showContextMenu(binding.root, note)
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
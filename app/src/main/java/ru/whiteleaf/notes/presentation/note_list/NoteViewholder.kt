package ru.whiteleaf.notes.presentation.note_list

import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.common.interfaces.ContextNoteActionHandler
import ru.whiteleaf.notes.common.utils.ContextMenuHelper
import ru.whiteleaf.notes.databinding.ItemNoteBinding
import ru.whiteleaf.notes.domain.model.Note

class NoteViewHolder(
    private val binding: ItemNoteBinding,
    private val onNoteClicked: (Note) -> Unit,
    private val noteActionHandler: ContextNoteActionHandler
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(note: Note) {
        val text = if (note.title != "") note.title else note.content.take(40)
        binding.noteTitle.text = text.trimStart('-')

        val isFeatured = note.title.startsWith('-')

        if (isFeatured) {
            // Особенная заметка - акцентный фон и белый текст
            itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.accent_blue))
            binding.noteTitle.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    android.R.color.white
                )
            )
        } else {

            itemView.background = null
            // Используем атрибуты темы для обычных заметок
            val typedValue = TypedValue()

            itemView.context.theme.resolveAttribute(
                android.R.attr.textColorPrimary,
                typedValue,
                true
            )
            binding.noteTitle.setTextColor(typedValue.data)
        }

        binding.root.setOnClickListener { onNoteClicked(note) }

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
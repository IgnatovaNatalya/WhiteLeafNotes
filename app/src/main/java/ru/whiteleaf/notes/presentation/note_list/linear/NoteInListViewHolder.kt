package ru.whiteleaf.notes.presentation.note_list.linear

import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.common.interfaces.ContextNoteActionHandler
import ru.whiteleaf.notes.common.utils.ContextMenuHelper
import ru.whiteleaf.notes.common.utils.formatDateNoteList
import ru.whiteleaf.notes.common.utils.highlightMatches
import ru.whiteleaf.notes.databinding.ItemNoteInListBinding
import ru.whiteleaf.notes.domain.model.Note

class NoteInListViewHolder(
    private val binding: ItemNoteInListBinding,
    private val onNoteClicked: (Note) -> Unit,
    private val noteActionHandler: ContextNoteActionHandler?
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(note: Note, query: String? = null) {
        val text = if (note.title != "") note.title else note.content.take(40)
        binding.noteInListTitle.text = text.trimStart('-')

        val isFeatured = note.title.startsWith('-')

        if (isFeatured) {
            // Особенная заметка - акцентный фон и белый текст
            itemView.setBackgroundColor(
                ContextCompat.getColor(
                    itemView.context,
                    R.color.accent_blue
                )
            )
            binding.noteInListTitle.setTextColor(
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
            binding.noteInListTitle.setTextColor(typedValue.data)

            binding.noteInListDate.text = formatDateNoteList(note.modifiedAt)
        }

        if (query != null) {
            val color =
                if (isFeatured) ContextCompat.getColor(binding.root.context, R.color.text_primary_light)
                else ContextCompat.getColor(binding.root.context, R.color.accent_blue)

            highlightMatches(
                textView = binding.noteInListTitle,
                text = text,
                query = query,
                highlightColor = color,
                ignoreCase = true
            )
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
                    R.id.context_menu_delete -> noteActionHandler?.onDeleteNote(note)
                    R.id.context_menu_move -> noteActionHandler?.onMoveNote(note)
                    R.id.context_menu_change_date -> noteActionHandler?.onChangeNoteDate(note)
                    R.id.context_menu_share_note -> noteActionHandler?.onShareNote(note)
                    R.id.context_menu_rename -> noteActionHandler?.onRenameNote(note)
                }
            }
        )
    }
}
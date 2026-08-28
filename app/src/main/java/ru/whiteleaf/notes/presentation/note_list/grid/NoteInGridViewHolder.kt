package ru.whiteleaf.notes.presentation.note_list.grid

import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.common.interfaces.ContextNoteActionHandler
import ru.whiteleaf.notes.common.utils.ContextMenuHelper
import ru.whiteleaf.notes.common.utils.formatDateNoteGrid
import ru.whiteleaf.notes.domain.model.Note

class NoteInGridViewHolder(
    itemView: View,
    private val noteActionHandler: ContextNoteActionHandler
) : RecyclerView.ViewHolder(itemView) {

    private val tvDate: TextView = itemView.findViewById(R.id.tv_planer_note_date)
    private val tvTitle: TextView = itemView.findViewById(R.id.tv_planer_note_title)

    fun bind(note: Note, onClick: (Note) -> Unit) {
        val titleText = if (note.title != "") note.title else note.content.take(40)
        val isFeatured = note.title.startsWith('-')

        if (isFeatured) {
            // Особенная заметка - акцентный фон и белый текст
            itemView.background =
                ContextCompat.getDrawable(itemView.context, R.drawable.bg_planner_note_accent)
            tvDate.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
            tvTitle.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    android.R.color.white
                )
            )
        } else {
            itemView.background =
                ContextCompat.getDrawable(itemView.context, R.drawable.bg_planner_note)
            // Используем атрибуты темы для обычных заметок
            val typedValue = TypedValue()

            itemView.context.theme.resolveAttribute(
                android.R.attr.textColorSecondary,
                typedValue,
                true
            )
            tvDate.setTextColor(typedValue.data)

            itemView.context.theme.resolveAttribute(
                android.R.attr.textColorPrimary,
                typedValue,
                true
            )
            tvTitle.setTextColor(typedValue.data)

        }
        tvDate.text = formatDateNoteGrid(note.modifiedAt)
        //tvTitle.text = note.title.trimStart('-')//.ifEmpty { "" }
        tvTitle.text = titleText.trimStart('-')

        itemView.setOnClickListener { onClick(note) }

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
                    R.id.context_menu_delete -> noteActionHandler.onDeleteNote(note)
                    R.id.context_menu_move -> noteActionHandler.onMoveNote(note)
                    R.id.context_menu_change_date -> noteActionHandler.onChangeNoteDate(note)
                    R.id.context_menu_share_note -> noteActionHandler.onShareNote(note)
                    R.id.context_menu_rename -> noteActionHandler.onRenameNote(note)
                }
            }
        )
    }
}
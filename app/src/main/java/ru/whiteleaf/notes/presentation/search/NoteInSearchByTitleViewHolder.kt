package ru.whiteleaf.notes.presentation.search

import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.common.utils.formatDateNoteList
import ru.whiteleaf.notes.common.utils.highlightMatches
import ru.whiteleaf.notes.databinding.ItemNoteInListBinding
import ru.whiteleaf.notes.domain.model.NoteFound

class NoteInSearchByTitleViewHolder(
    private val binding: ItemNoteInListBinding,
    private val onNoteFoundClicked: ((NoteFound) -> Unit),
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(noteFound: NoteFound) {

        binding.noteInListTitle.text = noteFound.title.trimStart('-')

        val isFeatured = noteFound.title.startsWith('-')

        if (isFeatured) {
            // Особенная заметка - акцентный фон и белый текст
            itemView.setBackgroundColor(
                ContextCompat.getColor(itemView.context, R.color.accent_blue)
            )
            binding.noteInListTitle.setTextColor(
                ContextCompat.getColor(itemView.context, android.R.color.white)
            )
        } else {
            itemView.background = null
            val typedValue = TypedValue()

            itemView.context.theme.resolveAttribute(
                android.R.attr.textColorPrimary,
                typedValue,
                true
            )
            binding.noteInListTitle.setTextColor(typedValue.data)

            binding.noteInListDate.text = formatDateNoteList(noteFound.modifiedAt)
        }

        val color =
            if (isFeatured)
                ContextCompat.getColor(binding.root.context, R.color.text_primary_light)
            else ContextCompat.getColor(binding.root.context, R.color.accent_blue)

        highlightMatches(
            textView = binding.noteInListTitle,
            text = noteFound.title,
            query = noteFound.query,
            highlightColor = color,
            ignoreCase = true
        )

        binding.root.setOnClickListener { onNoteFoundClicked(noteFound) }

    }

}
package ru.whiteleaf.notes.presentation.search

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.common.utils.formatDateNoteList
import ru.whiteleaf.notes.common.utils.highlightMatches
import ru.whiteleaf.notes.databinding.ItemNoteInSearchTitlePathBinding
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.model.NoteFound
import ru.whiteleaf.notes.domain.model.toNote

class NoteInSearchGlobalViewHolder(
    private val binding: ItemNoteInSearchTitlePathBinding,
    private val onNoteClicked: (Note) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(noteFound: NoteFound) {

        binding.foundNoteTitle.text = noteFound.title

        highlightMatches(
            textView = binding.foundNoteTitle,
            text =  noteFound.title,
            query = noteFound.query,
            highlightColor = ContextCompat.getColor(binding.root.context, R.color.accent_blue),
            ignoreCase = true
        )

        binding.foundNoteNotebook.text = "/ " + noteFound.notebookPath
        binding.foundNoteDate.text = formatDateNoteList(noteFound.modifiedAt)

        binding.root.setOnClickListener { onNoteClicked(noteFound.toNote()) }
    }

}
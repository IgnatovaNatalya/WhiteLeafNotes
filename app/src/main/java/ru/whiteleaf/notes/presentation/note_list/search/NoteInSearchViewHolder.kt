package ru.whiteleaf.notes.presentation.note_list.search

import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.common.utils.formatDateNoteList
import ru.whiteleaf.notes.databinding.ItemNoteInSearchBinding
import ru.whiteleaf.notes.domain.model.NoteFound

class NoteInSearchViewHolder(
    private val binding: ItemNoteInSearchBinding,
    private val onFoundNoteClicked: (NoteFound) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(noteFound: NoteFound) {
        binding.noteSearchPreview.text = noteFound.contentSearchPreview
        binding.noteTitle.text = noteFound.title
        binding.noteDate.text = formatDateNoteList(noteFound.modifiedAt)

        binding.root.setOnClickListener {
            onFoundNoteClicked(noteFound)
        }
    }

}
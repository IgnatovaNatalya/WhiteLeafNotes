package ru.whiteleaf.notes.presentation.search

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.common.utils.formatDateNoteList
import ru.whiteleaf.notes.common.utils.highlightMatches
import ru.whiteleaf.notes.databinding.ItemNoteInSearchContentBinding
import ru.whiteleaf.notes.domain.model.NoteFound

class NoteContentInSearchViewHolder(
    private val binding: ItemNoteInSearchContentBinding,
    private val onFoundNoteClicked: (NoteFound) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(noteFound: NoteFound, modeGlobal: Boolean) {
        val clearText = noteFound.contentSearchPreview?.replace("\n", " ")?.trimStart()?.trim()
        val contentPreview =
            noteFound.contentPosition?.let { if (it > 5) "..." else "" } + clearText

        binding.noteSearchPreview.text = contentPreview

        highlightMatches(
            textView = binding.noteSearchPreview,
            text = contentPreview,
            query = noteFound.query,
            highlightColor = ContextCompat.getColor(binding.root.context, R.color.accent_blue),
            ignoreCase = true
        )
        binding.noteTitle.text = if (modeGlobal) noteFound.notebookPath + " / " + noteFound.title
        else noteFound.title

        binding.noteDate.text = formatDateNoteList(noteFound.modifiedAt)

        binding.root.setOnClickListener { onFoundNoteClicked(noteFound) }
    }

}
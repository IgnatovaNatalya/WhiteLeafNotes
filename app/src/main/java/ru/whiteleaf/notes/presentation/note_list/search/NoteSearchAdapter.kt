package ru.whiteleaf.notes.presentation.note_list.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import ru.whiteleaf.notes.databinding.ItemNoteInSearchBinding
import ru.whiteleaf.notes.domain.model.NoteFound

class NoteSearchAdapter(
    private val onFoundNoteClicked: (NoteFound) -> Unit
) : ListAdapter<NoteFound, NoteInSearchViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteInSearchViewHolder {
        val binding = ItemNoteInSearchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteInSearchViewHolder(binding, onFoundNoteClicked)
    }

    override fun onBindViewHolder(holder: NoteInSearchViewHolder, position: Int) {
        val note = getItem(position)
        holder.bind(note)
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<NoteFound>() {
        override fun areItemsTheSame(oldItem: NoteFound, newItem: NoteFound): Boolean {
            return oldItem.title == newItem.title && oldItem.id == newItem.id && oldItem.contentSearchPreview == newItem.contentSearchPreview
        }

        override fun areContentsTheSame(oldItem: NoteFound, newItem: NoteFound): Boolean {
            return oldItem == newItem
        }
    }
}
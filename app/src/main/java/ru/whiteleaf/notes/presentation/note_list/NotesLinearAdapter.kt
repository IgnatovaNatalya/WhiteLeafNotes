package ru.whiteleaf.notes.presentation.note_list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import ru.whiteleaf.notes.common.interfaces.ContextNoteActionHandler
import ru.whiteleaf.notes.databinding.ItemNoteInListBinding
import ru.whiteleaf.notes.domain.model.Note

class NotesLinearAdapter(
    private val onNoteClicked: (Note) -> Unit,
    private val contextActionHandler: ContextNoteActionHandler
) : ListAdapter<Note, NoteInListViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteInListViewHolder {
        val binding = ItemNoteInListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteInListViewHolder(binding, onNoteClicked, contextActionHandler)
    }

    override fun onBindViewHolder(holder: NoteInListViewHolder, position: Int) {
        val note = getItem(position)
        holder.bind(note)
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.title == newItem.title && oldItem.id == newItem.id && oldItem.modifiedAt == newItem.modifiedAt
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}
package ru.whiteleaf.notes.presentation.start

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.data.model.RecentNote

class RecentNoteViewHolder(
    view: View,
    private val onRecentNoteClicked: (RecentNote) -> Unit
) : RecyclerView.ViewHolder(view) {
    private val title: TextView = view.findViewById(R.id.note_title)
    private val notebookTitle: TextView = view.findViewById(R.id.notebook_title)

    fun bind(note: RecentNote) {

        title.text = note.title
        notebookTitle.text = note.notebookPath

        itemView.setOnClickListener { onRecentNoteClicked(note) }
    }
}
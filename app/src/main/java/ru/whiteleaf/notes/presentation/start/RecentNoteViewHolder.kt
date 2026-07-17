package ru.whiteleaf.notes.presentation.start

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.common.utils.formatDateRecent
import ru.whiteleaf.notes.data.model.RecentNote

class RecentNoteViewHolder(
    view: View,
    private val onRecentNoteClicked: (RecentNote) -> Unit
) : RecyclerView.ViewHolder(view) {
    private val title: TextView = view.findViewById(R.id.note_title)
    private val notebookTitle: TextView = view.findViewById(R.id.notebook_title)
    private val noteDate: TextView = view.findViewById(R.id.note_date)

    fun bind(note: RecentNote) {

        title.text = note.recentTitle
        notebookTitle.text = "/ ${note.notebookPath.toString()}"
        noteDate.text = formatDateRecent(note.recentDate)

        itemView.setOnClickListener { onRecentNoteClicked(note) }
    }
}
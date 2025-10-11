package ru.whiteleaf.notes.presentation.root

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.domain.model.Note
import android.widget.TextView
import ru.whiteleaf.notes.R

class DrawerNoteViewHolder(
    view: View,
    private val onNoteClicked: (Note) -> Unit
) : RecyclerView.ViewHolder(view) {
    fun bind(note: Note) {
        val title: TextView = itemView.findViewById<TextView>(R.id.drawer_note_title)

        val text = if (note.title!="") note.title else note.content.take(30)
        title.text = text

        itemView.setOnClickListener { onNoteClicked(note) }
    }
}
package com.example.txtnotesapp.presentation.start

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.txtnotesapp.R
import com.example.txtnotesapp.domain.model.Note

class NoteViewHolder(
    view: View,
    private val onNoteClicked: (Note) -> Unit
) : RecyclerView.ViewHolder(view) {
    private val title: TextView = view.findViewById(R.id.note_title)

    fun bind(note: Note) {
        title.text = note.title
        //date.text = formatDate(note.createdAt)

        itemView.setOnClickListener {
            onNoteClicked(note)
        }
    }
}
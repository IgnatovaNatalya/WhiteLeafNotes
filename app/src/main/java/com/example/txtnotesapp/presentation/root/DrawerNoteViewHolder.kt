package com.example.txtnotesapp.presentation.root

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.txtnotesapp.domain.model.Note
import android.widget.TextView
import com.example.txtnotesapp.R

class DrawerNoteViewHolder(
    view: View,
    private val onNoteClicked: (Note) -> Unit
) : RecyclerView.ViewHolder(view) {
    fun bind(note: Note) {
        val title: TextView = itemView.findViewById<TextView>(R.id.drawer_note_title)
        title.text = note.title
        itemView.setOnClickListener { onNoteClicked(note) }
    }
}
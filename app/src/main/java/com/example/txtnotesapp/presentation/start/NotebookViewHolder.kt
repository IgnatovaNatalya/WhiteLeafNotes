package com.example.txtnotesapp.presentation.start

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.txtnotesapp.R
import com.example.txtnotesapp.domain.model.Notebook

class NotebookViewHolder(
    view: View,
    private val onNotebookClicked: (Notebook) -> Unit
) : RecyclerView.ViewHolder(view) {
    private val name: TextView = view.findViewById(R.id.notebookName)
    private val noteCount: TextView = view.findViewById(R.id.noteCount)

    fun bind(notebook: Notebook) {
        name.text = notebook.name
        noteCount.text = itemView.resources.getQuantityString(R.plurals.notes_count, notebook.noteCount, notebook.noteCount)

        itemView.setOnClickListener {
            onNotebookClicked(notebook)
        }
    }
}
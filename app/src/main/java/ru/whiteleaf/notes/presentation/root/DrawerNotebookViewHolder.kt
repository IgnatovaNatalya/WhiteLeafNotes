package ru.whiteleaf.notes.presentation.root

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.domain.model.Notebook

class DrawerNotebookViewHolder (
    view: View,
    private val onNotebookClicked: (Notebook) -> Unit
) : RecyclerView.ViewHolder(view) {
    fun bind(notebook: Notebook) {
        val title: TextView = itemView.findViewById<TextView>(R.id.drawer_notebook_title)
        title.text = notebook.path
        itemView.setOnClickListener { onNotebookClicked(notebook) }
    }
}
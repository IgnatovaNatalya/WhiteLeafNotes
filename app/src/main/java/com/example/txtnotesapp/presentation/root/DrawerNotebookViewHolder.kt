package com.example.txtnotesapp.presentation.root

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.txtnotesapp.R
import com.example.txtnotesapp.domain.model.Notebook

class DrawerNotebookViewHolder (
    view: View,
    private val onNotebookClicked: (Notebook) -> Unit
) : RecyclerView.ViewHolder(view) {
    fun bind(notebook: Notebook) {
        val title: TextView = itemView.findViewById<TextView>(R.id.drawer_note_title)
        title.text = notebook.name
        // Настройка view элемента записной книжки
        itemView.setOnClickListener { onNotebookClicked(notebook) }
    }
}
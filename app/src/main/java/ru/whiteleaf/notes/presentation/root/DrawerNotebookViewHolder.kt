package ru.whiteleaf.notes.presentation.root

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.domain.model.Notebook

class DrawerNotebookViewHolder (
    view: View,
    private val onNotebookClicked: (Notebook) -> Unit
) : RecyclerView.ViewHolder(view) {
    fun bind(notebook: Notebook) {
        val title: TextView = itemView.findViewById(R.id.drawer_notebook_title)
        val icon : ImageView = itemView.findViewById(R.id.drawer_notebook_icon)
        val bg: LinearLayout = itemView.findViewById(R.id.ll_drawer_bg_book)

        title.text = notebook.path

        if (notebook.isEncrypted) {
            if (notebook.isUnlocked) icon.setImageResource(R.drawable.ic_drawer_unlocked)
            else icon.setImageResource(R.drawable.ic_drawer_protected)
        } else {
            icon.setImageResource(R.drawable.ic_book)
        }

        if (notebook.isLastOpened)
            bg.setBackgroundResource(R.drawable.bg_book_current)
        else
            bg.background = null

        itemView.setOnClickListener { onNotebookClicked(notebook) }
    }
}
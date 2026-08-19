package ru.whiteleaf.notes.presentation.start

import android.view.View
import androidx.recyclerview.widget.RecyclerView

class CreateNotebookViewHolder(
    view: View,
    private val onAddClicked: () -> Unit,
) : RecyclerView.ViewHolder(view) {

    fun bind() {
        itemView.setOnClickListener { onAddClicked() }
    }
}
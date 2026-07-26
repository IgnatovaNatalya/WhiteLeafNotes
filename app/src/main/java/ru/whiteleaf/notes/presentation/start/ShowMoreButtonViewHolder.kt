package ru.whiteleaf.notes.presentation.start

import android.view.View
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R

class ShowMoreButtonViewHolder(
    view: View,
    private val onClick: () -> Unit
) : RecyclerView.ViewHolder(view) {

    private val btn = view.findViewById<ImageButton>(R.id.btn_more)

    fun bind() {
        btn.setOnClickListener { onClick() }
    }
}
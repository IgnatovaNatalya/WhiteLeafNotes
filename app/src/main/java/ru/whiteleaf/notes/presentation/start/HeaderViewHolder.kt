package ru.whiteleaf.notes.presentation.start

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R

class HeaderViewHolder(
    view: View,
    private val onClick: () -> Unit
) : RecyclerView.ViewHolder(view) {

    private val title: TextView = view.findViewById(R.id.start_header_title)

    fun bind(headerTitle: String, clickable: Boolean) {
        title.text = headerTitle

        if (clickable) {
            itemView.setOnClickListener { onClick() }
        } else {
            title.setCompoundDrawablesRelative(null, null, null, null)
            itemView.setOnClickListener(null)
        }
    }
}
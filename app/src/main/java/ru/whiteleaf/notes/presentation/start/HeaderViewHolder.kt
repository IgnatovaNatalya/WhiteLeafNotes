package ru.whiteleaf.notes.presentation.start

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R

class HeaderViewHolder(
    view: View,
    private val onClick: () -> Unit
) : RecyclerView.ViewHolder(view) {

    private val title: TextView = view.findViewById(R.id.start_header_title)
    private val button: ImageView = view.findViewById(R.id.header_button)

    fun bind(headerTitle: String, clickable: Boolean) {
        title.text = headerTitle

        if (clickable) {
            button.visibility = View.VISIBLE
            itemView.setOnClickListener { onClick() }
        } else {
            button.visibility = View.GONE
            itemView.setOnClickListener(null)
        }
    }
}
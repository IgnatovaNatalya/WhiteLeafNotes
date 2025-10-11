package ru.whiteleaf.notes.presentation.root

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import ru.whiteleaf.notes.R

class DrawerCreateButtonViewHolder(view: View, private val onButtonClicked: () -> Unit) :
    RecyclerView.ViewHolder(view) {
    fun bind(title: String) {
        val button = itemView.findViewById<TextView>(R.id.drawer_create_button)
        button.text = title
        itemView.setOnClickListener { onButtonClicked() }
    }
}